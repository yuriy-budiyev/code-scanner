/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.budiyev.android.codescanner;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class CodeScanner {
    public static final List<BarcodeFormat> ALL_FORMATS =
            Arrays.asList(BarcodeFormat.AZTEC, BarcodeFormat.CODABAR, BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_93, BarcodeFormat.CODE_128, BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.EAN_8, BarcodeFormat.EAN_13, BarcodeFormat.ITF,
                    BarcodeFormat.MAXICODE, BarcodeFormat.PDF_417, BarcodeFormat.QR_CODE,
                    BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED, BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E, BarcodeFormat.UPC_EAN_EXTENSION);
    private final Lock mLock = new ReentrantLock();//TODO
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final CodeScannerView mScannerView;
    private final SurfaceHolder.Callback mSurfaceCallback;
    private final Camera.PreviewCallback mPreviewCallback;
    private final SurfaceHolder mSurfaceHolder;
    private final int mCameraId;
    private volatile Camera mCamera;
    private volatile Decoder mDecoder;
    private volatile DecodeCallback mDecodeCallback;
    private volatile Point mPreviewSize;
    private volatile int mDisplayOrientation;
    private volatile int mFrameWidth;
    private volatile int mFrameHeight;
    private volatile boolean mPreviewRequested;
    private volatile boolean mInitialized;
    private volatile boolean mPreviewActive;

    @MainThread
    public CodeScanner(@NonNull Context context, @NonNull CodeScannerView scannerView) {
        this(context, scannerView, -1);
    }

    @MainThread
    public CodeScanner(@NonNull Context context, @NonNull CodeScannerView view, int cameraId) {
        mContext = context;
        mMainThreadHandler = new Handler();
        mScannerView = view;
        mSurfaceCallback = new SurfaceCallback();
        mPreviewCallback = new PreviewCallback();
        mSurfaceHolder = view.getPreviewView().getHolder();
        mCameraId = cameraId;
        setFormats(ALL_FORMATS);
        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 && height == 0) {
            view.setLayoutListener(new ScannerLayoutListener());
        } else {
            initialize(width, height);
        }
    }

    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        mDecoder.setFormats(formats);
    }

    public void setFormat(@NonNull BarcodeFormat format) {
        mDecoder.setFormats(Collections.singletonList(format));
    }

    public void setDecodeCallback(@Nullable DecodeCallback decodeCallback) {
        mDecodeCallback = decodeCallback;
    }

    private void initialize(int width, int height) {
        new InitializeThread(width, height).start();
    }

    public void startPreview() {
        if (!mInitialized) {
            mPreviewRequested = true;
            return;
        }
        SurfaceHolder surfaceHolder = mSurfaceHolder;
        if (!mPreviewActive && surfaceHolder != null) {
            mCamera.setPreviewCallback(mPreviewCallback);
            surfaceHolder.addCallback(mSurfaceCallback);
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        mPreviewRequested = false;
        SurfaceHolder surfaceHolder = mSurfaceHolder;
        if (mInitialized && mPreviewActive && surfaceHolder != null) {
            surfaceHolder.removeCallback(mSurfaceCallback);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mPreviewActive = false;
        }
    }

    public void releaseResources() {
        mPreviewRequested = false;
        mPreviewActive = false;
        if (!mInitialized) {
            return;
        }
        mCamera.release();
        mDecoder.shutdown();
    }

    private class ScannerLayoutListener implements LayoutListener {
        @Override
        public void onLayout(int width, int height) {
            initialize(width, height);
            mScannerView.setLayoutListener(null);
        }
    }

    private class PreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Point previewSize = mPreviewSize;
            if (previewSize == null || !mInitialized || mDecoder.isDecoding()) {
                return;
            }
            int frameWidth = mFrameWidth;
            int frameHeight = mFrameHeight;
            if (frameWidth == 0 || frameHeight == 0) {
                return;
            }
            mDecoder.decode(data, previewSize.x, previewSize.y, frameWidth, frameHeight,
                    mDisplayOrientation, mScannerView.isSquareFrame(), mDecodeCallback);
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                mPreviewActive = true;
            } catch (IOException ignored) {
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                mPreviewActive = false;
                return;
            }
            try {
                mCamera.stopPreview();
            } catch (Exception ignored) {
            }
            mPreviewActive = false;
            try {
                mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                mPreviewActive = true;
            } catch (Exception ignored) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mCamera.stopPreview();
            } catch (Exception ignored) {
            }
            mPreviewActive = false;
        }
    }

    private class DecoderStateListener implements Decoder.StateListener {
        @Override
        public void onStateChanged(int state) {

        }
    }

    private class InitializeThread extends Thread {
        private final int mWidth;
        private final int mHeight;

        public InitializeThread(int width, int height) {
            super("Code scanner initialization thread");
            if (isDaemon()) {
                setDaemon(false);
            }
            mWidth = width;
            mHeight = height;
        }

        @Override
        @SuppressWarnings("SuspiciousNameCombination")
        public void run() {
            Camera camera;
            if (mCameraId == -1) {
                camera = Camera.open();
            } else {
                camera = Camera.open(mCameraId);
            }
            if (camera == null) {
                throw new RuntimeException("Unable to access camera");
            }
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, cameraInfo);
            Camera.Parameters parameters = camera.getParameters();
            Point frameSize;
            if (parameters != null) {
                boolean portrait = ScannerHelper.isPortrait(mDisplayOrientation);
                Point previewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters,
                        portrait ? new Point(mHeight, mWidth) : new Point(mWidth, mHeight));
                mPreviewSize = previewSize;
                parameters.setPreviewSize(previewSize.x, previewSize.y);
                frameSize = ScannerHelper.getFrameSize(portrait ? previewSize.y : previewSize.x,
                        portrait ? previewSize.x : previewSize.y, mWidth, mHeight);
                camera.setParameters(ScannerHelper.optimizeParameters(parameters));
            } else {
                frameSize = null;
            }
            int orientation = ScannerHelper.getDisplayOrientation(mContext, cameraInfo.orientation);
            mDisplayOrientation = orientation;
            camera.setDisplayOrientation(orientation);
            mFrameWidth = mWidth;
            mFrameHeight = mHeight;
            mCamera = camera;
            mDecoder = new Decoder(new DecoderStateListener());
            mInitialized = true;
            mMainThreadHandler.post(() -> {
                if (frameSize != null) {
                    mScannerView.setFrameSize(frameSize);
                }
                if (mPreviewRequested) {
                    startPreview();
                }
            });
        }
    }
}

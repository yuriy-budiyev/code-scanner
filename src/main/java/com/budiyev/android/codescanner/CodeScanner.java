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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.SurfaceHolder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CodeScanner {
    public static final List<BarcodeFormat> ALL_FORMATS =
            Arrays.asList(BarcodeFormat.AZTEC, BarcodeFormat.CODABAR, BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_93, BarcodeFormat.CODE_128, BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.EAN_8, BarcodeFormat.EAN_13, BarcodeFormat.ITF,
                    BarcodeFormat.MAXICODE, BarcodeFormat.PDF_417, BarcodeFormat.QR_CODE,
                    BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED, BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E, BarcodeFormat.UPC_EAN_EXTENSION);
    private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceCallback();
    private final Camera.PreviewCallback mPreviewCallback = new PreviewCallback();
    private final Camera mCamera;
    private final Decoder mDecoder;
    private final int mDisplayOrientation;
    private DecodeListener mDecodeListener;
    private CodeScannerView mScannerView;
    private SurfaceHolder mSurfaceHolder;
    private Point mPreviewSize;
    private int mFrameWidth;
    private int mFrameHeight;
    private boolean mPreviewRequested;
    private boolean mFrameValid;
    private boolean mPreviewActive;
    private volatile boolean mDecoding;

    public CodeScanner(@NonNull Context context, @NonNull CodeScannerView view,
            @NonNull Camera camera, @NonNull Camera.CameraInfo cameraInfo) {
        mCamera = camera;
        mDecoder = new Decoder(this);
        Camera.Parameters parameters = camera.getParameters();
        if (parameters != null) {
            camera.setParameters(ScannerHelper.optimizeParameters(parameters));
        }
        setFormats(ALL_FORMATS);
        int displayOrientation =
                ScannerHelper.getDisplayOrientation(context, cameraInfo.orientation);
        mCamera.setDisplayOrientation(displayOrientation);
        mDisplayOrientation = displayOrientation;
        mScannerView = view;
        mSurfaceHolder = view.getPreviewView().getHolder();
        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 && height == 0) {
            view.setLayoutListener(new ScannerLayoutListener());
        } else {
            invalidateFrame(width, height);
        }
    }

    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        mDecoder.setFormats(formats);
    }

    public void setFormat(@NonNull BarcodeFormat format) {
        mDecoder.setFormats(Collections.singletonList(format));
    }

    public void setDecodeListener(@Nullable DecodeListener decodeListener) {
        mDecodeListener = decodeListener;
    }

    @WorkerThread
    void notifyDecodeFinished(@Nullable Result result) {
        mDecoding = false;
        if (result == null) {
            return;
        }
        stopPreview();
        DecodeListener listener = mDecodeListener;
        if (listener != null) {
            listener.onDecodeResult(result);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void invalidateFrame(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters != null) {
            boolean portrait = ScannerHelper.isPortrait(mDisplayOrientation);
            Point previewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters,
                    portrait ? new Point(height, width) : new Point(width, height));
            mPreviewSize = previewSize;
            parameters.setPreviewSize(previewSize.x, previewSize.y);
            mCamera.setParameters(parameters);
            mScannerView.setFrameSize(ScannerHelper
                    .getFrameSize(portrait ? previewSize.y : previewSize.x,
                            portrait ? previewSize.x : previewSize.y, width, height));
        }
        mFrameWidth = width;
        mFrameHeight = height;
        mFrameValid = true;
        if (mPreviewRequested) {
            startPreview();
        }
    }

    public void startPreview() {
        if (!mFrameValid) {
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
        if (mPreviewActive && surfaceHolder != null) {
            surfaceHolder.removeCallback(mSurfaceCallback);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mPreviewActive = false;
        }
    }

    public void releaseResources() {
        mPreviewRequested = false;
        mPreviewActive = false;
        mCamera.release();
        mDecoder.shutdown();
    }

    @NonNull
    public static CodeScanner openCamera(@NonNull Context context,
            @NonNull CodeScannerView scannerView) {
        int count = Camera.getNumberOfCameras();
        for (int id = 0; id < count; id++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(id, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return new CodeScanner(context, scannerView, Camera.open(id), cameraInfo);
            }
        }
        throw new RuntimeException("Camera not found");
    }

    private class ScannerLayoutListener implements LayoutListener {
        @Override
        public void onLayout(int width, int height) {
            invalidateFrame(width, height);
            mScannerView.setLayoutListener(null);
        }
    }

    private class PreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Point previewSize = mPreviewSize;
            if (previewSize == null || mDecoding) {
                return;
            }
            int frameWidth = mFrameWidth;
            int frameHeight = mFrameHeight;
            if (frameWidth == 0 || frameHeight == 0) {
                return;
            }
            mDecoding = true;
            mDecoder.decode(data, previewSize.x, previewSize.y, frameWidth, frameHeight,
                    mDisplayOrientation, mScannerView.isSquareFrame());
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
}

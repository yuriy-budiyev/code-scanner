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

import android.Manifest;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scanner of different codes
 *
 * @see CodeScannerView
 * @see BarcodeFormat
 */
public final class CodeScanner {
    public static final List<BarcodeFormat> ALL_FORMATS =
            Arrays.asList(BarcodeFormat.AZTEC, BarcodeFormat.CODABAR, BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_93, BarcodeFormat.CODE_128, BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.EAN_8, BarcodeFormat.EAN_13, BarcodeFormat.ITF,
                    BarcodeFormat.MAXICODE, BarcodeFormat.PDF_417, BarcodeFormat.QR_CODE,
                    BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED, BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E, BarcodeFormat.UPC_EAN_EXTENSION);
    public static final List<BarcodeFormat> ONE_DIMENSIONAL_FORMATS =
            Arrays.asList(BarcodeFormat.CODABAR, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
                    BarcodeFormat.CODE_128, BarcodeFormat.EAN_8, BarcodeFormat.EAN_13,
                    BarcodeFormat.ITF, BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED,
                    BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.UPC_EAN_EXTENSION);
    public static final List<BarcodeFormat> TWO_DIMENSIONAL_FORMATS =
            Arrays.asList(BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX, BarcodeFormat.MAXICODE,
                    BarcodeFormat.PDF_417, BarcodeFormat.QR_CODE);
    private static final long AUTO_FOCUS_INTERVAL = 1500L;
    private static final int UNSPECIFIED = -1;
    private static final int FOCUS_ATTEMPTS_THRESHOLD = 2;
    private final Lock mInitializeLock = new ReentrantLock();
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final CodeScannerView mScannerView;
    private final SurfaceHolder mSurfaceHolder;
    private final SurfaceHolder.Callback mSurfaceCallback;
    private final Camera.PreviewCallback mPreviewCallback;
    private final Camera.AutoFocusCallback mAutoFocusCallback;
    private final Runnable mAutoFocusTask;
    private final Runnable mStopPreviewTask;
    private final DecoderStateListener mDecoderStateListener;
    private final int mCameraId;
    private volatile List<BarcodeFormat> mFormats = ALL_FORMATS;
    private volatile DecodeCallback mDecodeCallback;
    private volatile Camera mCamera;
    private volatile Decoder mDecoder;
    private volatile Point mPreviewSize;
    private volatile Point mFrameSize;
    private volatile int mDisplayOrientation;
    private volatile boolean mInitialization;
    private volatile boolean mInitialized;
    private volatile boolean mStoppingPreview;
    private boolean mPreviewActive;
    private boolean mFocusing;
    private int mFocusAttemptsCount;

    /**
     * CodeScanner, associated with the first back-facing camera on the device
     *
     * @param context Context
     * @param view    A view to display the preview
     * @see CodeScannerView
     */
    @MainThread
    public CodeScanner(@NonNull Context context, @NonNull CodeScannerView view) {
        this(context, view, UNSPECIFIED);
    }

    /**
     * CodeScanner, associated with particular hardware camera
     *
     * @param context  Context
     * @param view     A view to display the preview
     * @param cameraId Camera id (between {@code 0} and
     *                 {@link Camera#getNumberOfCameras()} - {@code 1})
     * @see CodeScannerView
     */
    @MainThread
    public CodeScanner(@NonNull Context context, @NonNull CodeScannerView view, int cameraId) {
        mContext = context;
        mScannerView = view;
        mSurfaceHolder = view.getPreviewView().getHolder();
        mMainThreadHandler = new Handler();
        mSurfaceCallback = new SurfaceCallback();
        mPreviewCallback = new PreviewCallback();
        mAutoFocusCallback = new AutoFocusCallback();
        mAutoFocusTask = new AutoFocusTask();
        mStopPreviewTask = new StopPreviewTask();
        mDecoderStateListener = new DecoderStateListener();
        mCameraId = cameraId;
    }

    /**
     * Set formats, decoder to react to ({@link #ALL_FORMATS} by default)
     *
     * @param formats Formats
     * @see BarcodeFormat
     * @see #ALL_FORMATS
     * @see #ONE_DIMENSIONAL_FORMATS
     * @see #TWO_DIMENSIONAL_FORMATS
     */
    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        mInitializeLock.lock();
        try {
            if (mInitialized) {
                mDecoder.setFormats(formats);
            } else {
                mFormats = formats;
            }
        } finally {
            mInitializeLock.unlock();
        }
    }

    /**
     * Set format, decoder to react to
     *
     * @param format Format
     * @see BarcodeFormat
     */
    public void setFormat(@NonNull BarcodeFormat format) {
        setFormats(Collections.singletonList(format));
    }

    /**
     * Set callback of the decoding process
     *
     * @param decodeCallback Callback
     * @see DecodeCallback
     */
    public void setDecodeCallback(@Nullable DecodeCallback decodeCallback) {
        mDecodeCallback = decodeCallback;
    }

    /**
     * Whether if preview is active
     */
    public boolean isPreviewActive() {
        return mPreviewActive;
    }

    /**
     * Start camera preview
     * <br>
     * Requires {@link Manifest.permission#CAMERA} permission
     */
    @MainThread
    public void startPreview() {
        mInitializeLock.lock();
        try {
            if (!mInitialized && !mInitialization) {
                mInitialization = true;
                initialize();
                return;
            }
        } finally {
            mInitializeLock.unlock();
        }
        if (!mPreviewActive) {
            mSurfaceHolder.addCallback(mSurfaceCallback);
            startPreviewInternal();
        }
    }

    /**
     * Stop camera preview
     */
    @MainThread
    public void stopPreview() {
        if (mInitialized && mPreviewActive) {
            mSurfaceHolder.removeCallback(mSurfaceCallback);
            stopPreviewInternal();
        }
    }

    /**
     * Release resources, and stop preview if needed
     */
    @MainThread
    public void releaseResources() {
        if (mInitialized) {
            if (mPreviewActive) {
                stopPreview();
            }
            mCamera.release();
            mDecoder.shutdown();
            mCamera = null;
            mDecoder = null;
            mPreviewSize = null;
            mFrameSize = null;
            mDisplayOrientation = 0;
            mInitialization = false;
            mInitialized = false;
            mStoppingPreview = false;
            mPreviewActive = false;
            mFocusing = false;
        }
    }

    private void initialize() {
        if (Utils.isLaidOut(mScannerView)) {
            initialize(mScannerView.getWidth(), mScannerView.getHeight());
        } else {
            mScannerView.setLayoutListener(new ScannerLayoutListener());
        }
    }

    private void initialize(int width, int height) {
        new InitializationThread(width, height).start();
    }

    private void finishInitialization(@NonNull Camera camera, @NonNull Point previewSize,
            @NonNull Point frameSize, int displayOrientation) {
        mInitializeLock.lock();
        try {
            mCamera = camera;
            mDecoder = new Decoder(mDecoderStateListener, mFormats);
            mDecoder.start();
            mPreviewSize = previewSize;
            mFrameSize = frameSize;
            mDisplayOrientation = displayOrientation;
            mInitialization = false;
            mInitialized = true;
        } finally {
            mInitializeLock.unlock();
        }
        mMainThreadHandler.post(new FinishInitializationTask(frameSize));
    }

    private void startPreviewInternal() {
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mStoppingPreview = false;
            mPreviewActive = true;
            mFocusing = false;
            mFocusAttemptsCount = 0;
            scheduleAutoFocusTask();
        } catch (Exception ignored) {
        }
    }

    private void stopPreviewInternal() {
        mCamera.setPreviewCallback(null);
        try {
            mCamera.stopPreview();
        } catch (Exception ignored) {
        }
        mStoppingPreview = false;
        mPreviewActive = false;
        mFocusing = false;
        mFocusAttemptsCount = 0;
    }

    private void autoFocusCamera() {
        if (!mInitialized || !mPreviewActive) {
            return;
        }
        if (mFocusing && mFocusAttemptsCount < FOCUS_ATTEMPTS_THRESHOLD) {
            mFocusAttemptsCount++;
        } else {
            try {
                mCamera.autoFocus(mAutoFocusCallback);
                mFocusAttemptsCount = 0;
                mFocusing = true;
            } catch (Exception e) {
                mFocusing = false;
            }
        }
        scheduleAutoFocusTask();
    }

    private void scheduleAutoFocusTask() {
        mMainThreadHandler.postDelayed(mAutoFocusTask, AUTO_FOCUS_INTERVAL);
    }

    private final class ScannerLayoutListener implements CodeScannerView.LayoutListener {
        @Override
        public void onLayout(int width, int height) {
            initialize(width, height);
            mScannerView.setLayoutListener(null);
        }
    }

    private final class PreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!mInitialized || mStoppingPreview || mDecoder.isProcessing()) {
                return;
            }
            Point previewSize = mPreviewSize;
            Point frameSize = mFrameSize;
            mDecoder.decode(data, previewSize.x, previewSize.y, frameSize.x, frameSize.y,
                    mDisplayOrientation, mScannerView.isSquareFrame(), mDecodeCallback);
        }
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startPreviewInternal();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                mPreviewActive = false;
                return;
            }
            stopPreviewInternal();
            startPreviewInternal();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreviewInternal();
        }
    }

    private final class DecoderStateListener implements Decoder.StateListener {
        @Override
        public void onStateChanged(int state) {
            if (state == Decoder.State.DECODED) {
                mStoppingPreview = true;
                mMainThreadHandler.post(mStopPreviewTask);
            }
        }
    }

    private final class InitializationThread extends Thread {
        private final int mWidth;
        private final int mHeight;

        public InitializationThread(int width, int height) {
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
            Camera camera = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            if (mCameraId == UNSPECIFIED) {
                int numberOfCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        camera = Camera.open(i);
                        break;
                    }
                }
            } else {
                camera = Camera.open(mCameraId);
                Camera.getCameraInfo(mCameraId, cameraInfo);
            }
            if (camera == null) {
                throw new RuntimeException("Unable to access camera");
            }
            Camera.Parameters parameters = camera.getParameters();
            if (parameters == null) {
                throw new RuntimeException("Unable to configure camera");
            }
            int orientation = Utils.getDisplayOrientation(mContext, cameraInfo);
            boolean portrait = Utils.isPortrait(orientation);
            Point previewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters,
                    portrait ? new Point(mHeight, mWidth) : new Point(mWidth, mHeight));
            parameters.setPreviewSize(previewSize.x, previewSize.y);
            Point frameSize = Utils.getFrameSize(portrait ? previewSize.y : previewSize.x,
                    portrait ? previewSize.x : previewSize.y, mWidth, mHeight);
            camera.setParameters(Utils.optimizeParameters(parameters));
            camera.setDisplayOrientation(orientation);
            finishInitialization(camera, previewSize, frameSize, orientation);
        }
    }

    private final class AutoFocusCallback implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mFocusing = false;
        }
    }

    private final class AutoFocusTask implements Runnable {
        @Override
        public void run() {
            autoFocusCamera();
        }
    }

    private final class StopPreviewTask implements Runnable {
        @Override
        public void run() {
            stopPreview();
        }
    }

    private final class FinishInitializationTask implements Runnable {
        private final Point mFrameSize;

        private FinishInitializationTask(@NonNull Point frameSize) {
            mFrameSize = frameSize;
        }

        @Override
        public void run() {
            mScannerView.setFrameSize(mFrameSize);
            startPreview();
        }
    }
}

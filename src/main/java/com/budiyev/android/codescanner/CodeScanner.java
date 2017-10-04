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
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;

import com.google.zxing.BarcodeFormat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    public static final List<BarcodeFormat> ALL_FORMATS = Arrays.asList(BarcodeFormat.values());
    public static final List<BarcodeFormat> ONE_DIMENSIONAL_FORMATS =
            Arrays.asList(BarcodeFormat.CODABAR, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
                    BarcodeFormat.CODE_128, BarcodeFormat.EAN_8, BarcodeFormat.EAN_13,
                    BarcodeFormat.ITF, BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED,
                    BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.UPC_EAN_EXTENSION);
    public static final List<BarcodeFormat> TWO_DIMENSIONAL_FORMATS =
            Arrays.asList(BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX, BarcodeFormat.MAXICODE,
                    BarcodeFormat.PDF_417, BarcodeFormat.QR_CODE);
    public static final int AUTO_FOCUS_MODE_SAFE = 0;
    public static final int AUTO_FOCUS_MODE_CONTINUOUS = 1;
    public static final int UNSPECIFIED = -1;
    private static final int SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD = 2;
    private final Lock mInitializeLock = new ReentrantLock();
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final CodeScannerView mScannerView;
    private final SurfaceHolder mSurfaceHolder;
    private final SurfaceHolder.Callback mSurfaceCallback;
    private final Camera.PreviewCallback mPreviewCallback;
    private final Camera.AutoFocusCallback mSafeAutoFocusCallback;
    private final Runnable mSafeAutoFocusTask;
    private final Runnable mStopPreviewTask;
    private final DecoderStateListener mDecoderStateListener;
    private volatile List<BarcodeFormat> mFormats = ALL_FORMATS;
    private volatile DecodeCallback mDecodeCallback;
    private volatile ErrorCallback mErrorCallback;
    private volatile DecoderWrapper mDecoderWrapper;
    private volatile boolean mInitialization;
    private volatile boolean mInitialized;
    private volatile boolean mStoppingPreview;
    private volatile boolean mAutoFocusEnabled = true;
    private volatile boolean mFlashEnabled;
    private volatile long mSafeAutoFocusInterval = 2000L;
    private volatile int mAutoFocusMode = AUTO_FOCUS_MODE_SAFE;
    private volatile int mCameraId;
    private boolean mPreviewActive;
    private boolean mSafeAutoFocusing;
    private boolean mSafeAutoFocusTaskScheduled;
    private int mSafeAutoFocusAttemptsCount;

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
     *                 {@link Camera#getNumberOfCameras()} - {@code 1}
     *                 or {@link #UNSPECIFIED})
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
        mSafeAutoFocusCallback = new SafeAutoFocusCallback();
        mSafeAutoFocusTask = new SafeAutoFocusTask();
        mStopPreviewTask = new StopPreviewTask();
        mDecoderStateListener = new DecoderStateListener();
        mCameraId = cameraId;
        mScannerView.setCodeScanner(this);
    }

    /**
     * Set camera
     *
     * @param cameraId Camera id (between {@code 0} and
     *                 {@link Camera#getNumberOfCameras()} - {@code 1}
     *                 or {@link #UNSPECIFIED})
     */
    @MainThread
    public void setCamera(int cameraId) {
        mInitializeLock.lock();
        try {
            if (mCameraId != cameraId) {
                mCameraId = cameraId;
                if (mInitialized) {
                    boolean previewActive = mPreviewActive;
                    releaseResources();
                    if (previewActive) {
                        initialize();
                    }
                }
            }
        } finally {
            mInitializeLock.unlock();
        }
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
    @MainThread
    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        mInitializeLock.lock();
        try {
            if (mInitialized) {
                mDecoderWrapper.getDecoder().setFormats(formats);
            } else {
                mFormats = formats;
            }
        } finally {
            mInitializeLock.unlock();
        }
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
    @MainThread
    public void setFormats(@NonNull BarcodeFormat... formats) {
        setFormats(Arrays.asList(formats));
    }

    /**
     * Set format, decoder to react to
     *
     * @param format Format
     * @see BarcodeFormat
     */
    @MainThread
    public void setFormat(@NonNull BarcodeFormat format) {
        setFormats(Collections.singletonList(format));
    }

    /**
     * Set callback of decoding process
     *
     * @param decodeCallback Callback
     * @see DecodeCallback
     */
    public void setDecodeCallback(@Nullable DecodeCallback decodeCallback) {
        mDecodeCallback = decodeCallback;
    }

    /**
     * Set camera initialization error callback.
     * If not set, an exception will be thrown when error will occur.
     *
     * @param errorCallback Callback
     * @see ErrorCallback#SUPPRESS
     * @see ErrorCallback
     */
    public void setErrorCallback(@Nullable ErrorCallback errorCallback) {
        mErrorCallback = errorCallback;
    }

    /**
     * Whether to enable or disable auto focus if it's supported, {@code true} by default
     */
    @MainThread
    public void setAutoFocusEnabled(boolean autoFocusEnabled) {
        mInitializeLock.lock();
        try {
            boolean changed = mAutoFocusEnabled != autoFocusEnabled;
            mAutoFocusEnabled = autoFocusEnabled;
            mScannerView.setAutoFocusEnabled(autoFocusEnabled);
            if (mInitialized && mPreviewActive && changed &&
                    mDecoderWrapper.isAutoFocusSupported()) {
                setAutoFocusEnabledInternal(autoFocusEnabled);
            }
        } finally {
            mInitializeLock.unlock();
        }
    }

    /**
     * Whether if auto focus is currently enabled
     */
    public boolean isAutoFocusEnabled() {
        return mAutoFocusEnabled;
    }

    /**
     * Set auto focus interval in milliseconds for safe mode, 2000 by default
     *
     * @see #setAutoFocusMode(int)
     */
    public void setAutoFocusInterval(long autoFocusInterval) {
        mSafeAutoFocusInterval = autoFocusInterval;
    }

    /**
     * Set auto focus mode, {@link #AUTO_FOCUS_MODE_SAFE} by default
     * <br>
     * <b>Modes:</b>
     * <ul>
     * <li>{@link #AUTO_FOCUS_MODE_SAFE} - auto focus camera with the specified interval</li>
     * <li>{@link #AUTO_FOCUS_MODE_CONTINUOUS} - continuous auto focus, may not work on some devices</li>
     * </ul>
     */
    @MainThread
    public void setAutoFocusMode(@AutoFocusMode int autoFocusMode) {
        mInitializeLock.lock();
        try {
            mAutoFocusMode = autoFocusMode;
            if (mInitialized && mAutoFocusEnabled) {
                setAutoFocusEnabledInternal(true);
            }
        } finally {
            mInitializeLock.unlock();
        }
    }

    /**
     * Whether to enable or disable flash light if it's supported, {@code false} by default
     */
    @MainThread
    public void setFlashEnabled(boolean flashEnabled) {
        mInitializeLock.lock();
        try {
            boolean changed = mFlashEnabled != flashEnabled;
            mFlashEnabled = flashEnabled;
            mScannerView.setFlashEnabled(flashEnabled);
            if (mInitialized && mPreviewActive && changed && mDecoderWrapper.isFlashSupported()) {
                setFlashEnabledInternal(flashEnabled);
            }
        } finally {
            mInitializeLock.unlock();
        }
    }

    /**
     * Whether if flash light is currently enabled
     */
    public boolean isFlashEnabled() {
        return mFlashEnabled;
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
                initialize();
                return;
            }
        } finally {
            mInitializeLock.unlock();
        }
        if (!mPreviewActive) {
            mSurfaceHolder.addCallback(mSurfaceCallback);
            startPreviewInternal(false);
        }
    }

    /**
     * Stop camera preview
     */
    @MainThread
    public void stopPreview() {
        if (mInitialized && mPreviewActive) {
            mSurfaceHolder.removeCallback(mSurfaceCallback);
            stopPreviewInternal(false);
        }
    }

    /**
     * Release resources, and stop preview if needed; call this method in {@link Activity#onPause()}
     */
    @MainThread
    public void releaseResources() {
        if (mInitialized) {
            if (mPreviewActive) {
                stopPreview();
            }
            releaseResourcesInternal();
        }
    }

    private void initialize() {
        mInitialization = true;
        if (Utils.isLaidOut(mScannerView)) {
            initialize(mScannerView.getWidth(), mScannerView.getHeight());
        } else {
            mScannerView.setLayoutListener(new ScannerLayoutListener());
        }
    }

    private void initialize(int width, int height) {
        new InitializationThread(width, height).start();
    }

    private void startPreviewInternal(boolean internal) {
        try {
            DecoderWrapper decoderWrapper = mDecoderWrapper;
            Camera camera = decoderWrapper.getCamera();
            camera.setPreviewCallback(mPreviewCallback);
            camera.setPreviewDisplay(mSurfaceHolder);
            if (!internal && decoderWrapper.isFlashSupported() && mFlashEnabled) {
                setFlashEnabledInternal(true);
            }
            camera.startPreview();
            mStoppingPreview = false;
            mPreviewActive = true;
            mSafeAutoFocusing = false;
            mSafeAutoFocusAttemptsCount = 0;
            if (mAutoFocusMode == AUTO_FOCUS_MODE_SAFE) {
                scheduleSafeAutoFocusTask();
            }
        } catch (Exception ignored) {
        }
    }

    private void startPreviewInternalSafe() {
        if (mInitialized && !mPreviewActive) {
            startPreviewInternal(true);
        }
    }

    private void stopPreviewInternal(boolean internal) {
        try {
            DecoderWrapper decoderWrapper = mDecoderWrapper;
            Camera camera = decoderWrapper.getCamera();
            if (!internal && decoderWrapper.isFlashSupported() && mFlashEnabled) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null &&
                        Utils.setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF)) {
                    camera.setParameters(parameters);
                }
            }
            camera.setPreviewCallback(null);
            camera.stopPreview();
        } catch (Exception ignored) {
        }
        mStoppingPreview = false;
        mPreviewActive = false;
        mSafeAutoFocusing = false;
        mSafeAutoFocusAttemptsCount = 0;
    }

    private void stopPreviewInternalSafe() {
        if (mInitialized && mPreviewActive) {
            stopPreviewInternal(true);
        }
    }

    private void releaseResourcesInternal() {
        mInitialized = false;
        mInitialization = false;
        mStoppingPreview = false;
        mPreviewActive = false;
        mSafeAutoFocusing = false;
        DecoderWrapper decoderWrapper = mDecoderWrapper;
        if (decoderWrapper != null) {
            mDecoderWrapper = null;
            decoderWrapper.release();
        }
    }

    private void setFlashEnabledInternal(boolean flashEnabled) {
        try {
            DecoderWrapper decoderWrapper = mDecoderWrapper;
            Camera camera = decoderWrapper.getCamera();
            Camera.Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }
            boolean changed;
            if (flashEnabled) {
                changed = Utils.setFlashMode(parameters, Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                changed = Utils.setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF);
            }
            if (changed) {
                camera.setParameters(parameters);
            }
        } catch (Exception ignored) {
        }
    }

    private void setAutoFocusEnabledInternal(boolean autoFocusEnabled) {
        try {
            Camera camera = mDecoderWrapper.getCamera();
            Camera.Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }
            boolean changed;
            int autoFocusMode = mAutoFocusMode;
            if (autoFocusEnabled) {
                changed = Utils.setAutoFocusMode(parameters, autoFocusMode);
            } else {
                camera.cancelAutoFocus();
                changed = Utils.disableAutoFocus(parameters);
            }
            if (changed) {
                camera.setParameters(parameters);
            }
            if (autoFocusEnabled) {
                mSafeAutoFocusAttemptsCount = 0;
                mSafeAutoFocusing = false;
                if (autoFocusMode == AUTO_FOCUS_MODE_SAFE) {
                    scheduleSafeAutoFocusTask();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void safeAutoFocusCamera() {
        if (!mInitialized || !mPreviewActive) {
            return;
        }
        if (!mDecoderWrapper.isAutoFocusSupported() || !mAutoFocusEnabled) {
            return;
        }
        if (mSafeAutoFocusing && mSafeAutoFocusAttemptsCount < SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD) {
            mSafeAutoFocusAttemptsCount++;
        } else {
            try {
                Camera camera = mDecoderWrapper.getCamera();
                camera.cancelAutoFocus();
                camera.autoFocus(mSafeAutoFocusCallback);
                mSafeAutoFocusAttemptsCount = 0;
                mSafeAutoFocusing = true;
            } catch (Exception e) {
                mSafeAutoFocusing = false;
            }
        }
        scheduleSafeAutoFocusTask();
    }

    private void scheduleSafeAutoFocusTask() {
        if (mSafeAutoFocusTaskScheduled) {
            return;
        }
        mSafeAutoFocusTaskScheduled = true;
        mMainThreadHandler.postDelayed(mSafeAutoFocusTask, mSafeAutoFocusInterval);
    }

    boolean isAutoFocusSupportedOrUnknown() {
        DecoderWrapper wrapper = mDecoderWrapper;
        return wrapper == null || wrapper.isAutoFocusSupported();
    }

    boolean isFlashSupportedOrUnknown() {
        DecoderWrapper wrapper = mDecoderWrapper;
        return wrapper == null || wrapper.isFlashSupported();
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
            if (!mInitialized || mStoppingPreview) {
                return;
            }
            DecoderWrapper decoderWrapper = mDecoderWrapper;
            Decoder decoder = decoderWrapper.getDecoder();
            if (decoder.isProcessing()) {
                return;
            }
            Point previewSize = decoderWrapper.getPreviewSize();
            Point frameSize = decoderWrapper.getFrameSize();
            decoder.decode(data, previewSize.x, previewSize.y, frameSize.x, frameSize.y,
                    decoderWrapper.getDisplayOrientation(), mScannerView.isSquareFrame(),
                    decoderWrapper.getCameraInfo().facing == Camera.CameraInfo.CAMERA_FACING_FRONT,
                    mDecodeCallback);
        }
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startPreviewInternalSafe();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                mPreviewActive = false;
                return;
            }
            stopPreviewInternalSafe();
            startPreviewInternalSafe();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreviewInternalSafe();
        }
    }

    private final class DecoderStateListener implements Decoder.StateListener {
        @Override
        public void onStateChanged(int state) {
            if (state == Decoder.STATE_DECODED) {
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
            if (getPriority() != Thread.NORM_PRIORITY) {
                setPriority(Thread.NORM_PRIORITY);
            }
            if (isDaemon()) {
                setDaemon(false);
            }
            mWidth = width;
            mHeight = height;
        }

        @Override
        public void run() {
            try {
                initialize();
            } catch (Exception e) {
                releaseResourcesInternal();
                ErrorCallback errorCallback = mErrorCallback;
                if (errorCallback != null) {
                    errorCallback.onError(e);
                } else {
                    throw e;
                }
            }
        }

        private void initialize() {
            Camera camera = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraId = mCameraId;
            if (cameraId == UNSPECIFIED) {
                int numberOfCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        camera = Camera.open(i);
                        mCameraId = i;
                        break;
                    }
                }
            } else {
                camera = Camera.open(cameraId);
                Camera.getCameraInfo(cameraId, cameraInfo);
            }
            if (camera == null) {
                throw new CodeScannerException("Unable to access camera");
            }
            Camera.Parameters parameters = camera.getParameters();
            if (parameters == null) {
                throw new CodeScannerException("Unable to configure camera");
            }
            int orientation = Utils.getDisplayOrientation(mContext, cameraInfo);
            boolean portrait = Utils.isPortrait(orientation);
            Point previewSize =
                    Utils.findSuitablePreviewSize(parameters, portrait ? mHeight : mWidth,
                            portrait ? mWidth : mHeight);
            parameters.setPreviewSize(previewSize.x, previewSize.y);
            Point frameSize = Utils.getFrameSize(portrait ? previewSize.y : previewSize.x,
                    portrait ? previewSize.x : previewSize.y, mWidth, mHeight);
            List<String> focusModes = parameters.getSupportedFocusModes();
            boolean autoFocusSupported = focusModes != null &&
                    (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ||
                            focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE));
            if (!autoFocusSupported) {
                mAutoFocusEnabled = false;
            }
            if (autoFocusSupported && mAutoFocusEnabled) {
                Utils.setAutoFocusMode(parameters, mAutoFocusMode);
            }
            List<String> flashModes = parameters.getSupportedFlashModes();
            boolean flashSupported =
                    flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH);
            if (!flashSupported) {
                mFlashEnabled = false;
            }
            camera.setParameters(Utils.optimizeParameters(parameters));
            camera.setDisplayOrientation(orientation);
            mInitializeLock.lock();
            try {
                Decoder decoder = new Decoder(mDecoderStateListener, mFormats);
                mDecoderWrapper =
                        new DecoderWrapper(camera, cameraInfo, decoder, previewSize, frameSize,
                                orientation, autoFocusSupported, flashSupported);
                decoder.start();
                mInitialization = false;
                mInitialized = true;
            } finally {
                mInitializeLock.unlock();
            }
            mMainThreadHandler.post(new FinishInitializationTask(frameSize));
        }
    }

    private final class SafeAutoFocusCallback implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mSafeAutoFocusing = false;
        }
    }

    private final class SafeAutoFocusTask implements Runnable {
        @Override
        public void run() {
            mSafeAutoFocusTaskScheduled = false;
            if (mAutoFocusMode == AUTO_FOCUS_MODE_SAFE) {
                safeAutoFocusCamera();
            }
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
            if (!mInitialized) {
                return;
            }
            mScannerView.setFrameSize(mFrameSize);
            mScannerView.setCodeScanner(CodeScanner.this);
            startPreview();
        }
    }

    /**
     * New builder instance. Use it to pre-configure scanner. Note that all parameters
     * also can be changed after scanner created and when preview is active.
     *
     * Call {@link Builder#build(Context, CodeScannerView)} to create
     * scanner instance with specified parameters.
     */
    @NonNull
    @MainThread
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Code scanner builder
     */
    public static final class Builder {
        private int mCameraId = UNSPECIFIED;
        private List<BarcodeFormat> mFormats = ALL_FORMATS;
        private DecodeCallback mDecodeCallback;
        private ErrorCallback mErrorCallback;
        private boolean mAutoFocusEnabled = true;
        private long mAutoFocusInterval = 2000L;
        private int mAutoFocusMode = AUTO_FOCUS_MODE_SAFE;
        private boolean mFlashEnabled;

        private Builder() {
        }

        /**
         * Camera that will be used by scanner.
         * First back-facing camera on the device by default ({@link #UNSPECIFIED}).
         *
         * @param cameraId Camera id (between {@code 0} and
         *                 {@link Camera#getNumberOfCameras()} - {@code 1}
         *                 or {@link #UNSPECIFIED})
         */
        @NonNull
        @MainThread
        public Builder camera(int cameraId) {
            mCameraId = cameraId;
            return this;
        }

        /**
         * Formats, decoder to react to ({@link #ALL_FORMATS} by default)
         *
         * @param formats Formats
         * @see BarcodeFormat
         * @see #ALL_FORMATS
         * @see #ONE_DIMENSIONAL_FORMATS
         * @see #TWO_DIMENSIONAL_FORMATS
         */
        @NonNull
        @MainThread
        public Builder formats(@NonNull List<BarcodeFormat> formats) {
            mFormats = formats;
            return this;
        }

        /**
         * Formats, decoder to react to ({@link #ALL_FORMATS} by default)
         *
         * @param formats Formats
         * @see BarcodeFormat
         * @see #ALL_FORMATS
         * @see #ONE_DIMENSIONAL_FORMATS
         * @see #TWO_DIMENSIONAL_FORMATS
         */
        @NonNull
        @MainThread
        public Builder formats(@NonNull BarcodeFormat... formats) {
            mFormats = Arrays.asList(formats);
            return this;
        }

        /**
         * Callback of decoding process
         *
         * @param callback Callback
         * @see DecodeCallback
         */
        @NonNull
        @MainThread
        public Builder onDecoded(@Nullable DecodeCallback callback) {
            mDecodeCallback = callback;
            return this;
        }

        /**
         * Camera initialization error callback.
         * If not set, an exception will be thrown when error will occur.
         *
         * @param callback Callback
         * @see ErrorCallback#SUPPRESS
         * @see ErrorCallback
         */
        @NonNull
        @MainThread
        public Builder onError(@Nullable ErrorCallback callback) {
            mErrorCallback = callback;
            return this;
        }

        /**
         * Whether to enable or disable auto focus if it's supported, {@code true} by default
         */
        @NonNull
        @MainThread
        public Builder autoFocus(boolean enabled) {
            mAutoFocusEnabled = enabled;
            return this;
        }

        /**
         * Auto focus interval in milliseconds for safe mode, 2000 by default
         *
         * @see #setAutoFocusMode(int)
         */
        @NonNull
        @MainThread
        public Builder autoFocusInterval(long interval) {
            mAutoFocusInterval = interval;
            return this;
        }

        /**
         * Auto focus mode, {@link #AUTO_FOCUS_MODE_SAFE} by default
         * <br>
         * <b>Modes:</b>
         * <ul>
         * <li>{@link #AUTO_FOCUS_MODE_SAFE} - auto focus camera with the specified interval</li>
         * <li>{@link #AUTO_FOCUS_MODE_CONTINUOUS} - continuous auto focus, may not work on some devices</li>
         * </ul>
         */
        @NonNull
        @MainThread
        public Builder autoFocusMode(@AutoFocusMode int mode) {
            mAutoFocusMode = mode;
            return this;
        }

        /**
         * Whether to enable or disable flash light if it's supported, {@code false} by default
         */
        @NonNull
        @MainThread
        public Builder flash(boolean enabled) {
            mFlashEnabled = enabled;
            return this;
        }

        /**
         * Create new {@link CodeScanner} instance with specified parameters
         *
         * @param context Context
         * @param view    A view to display the preview
         * @see CodeScannerView
         */
        @NonNull
        @MainThread
        public CodeScanner build(@NonNull Context context, @NonNull CodeScannerView view) {
            CodeScanner scanner = new CodeScanner(context, view, mCameraId);
            scanner.mFormats = mFormats;
            scanner.mDecodeCallback = mDecodeCallback;
            scanner.mErrorCallback = mErrorCallback;
            scanner.mAutoFocusEnabled = mAutoFocusEnabled;
            scanner.mSafeAutoFocusInterval = mAutoFocusInterval;
            scanner.mAutoFocusMode = mAutoFocusMode;
            scanner.mFlashEnabled = mFlashEnabled;
            return scanner;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUTO_FOCUS_MODE_SAFE, AUTO_FOCUS_MODE_CONTINUOUS})
    public @interface AutoFocusMode {
    }
}

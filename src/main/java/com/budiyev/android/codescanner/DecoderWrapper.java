package com.budiyev.android.codescanner;

import android.graphics.Point;
import android.hardware.Camera;
import android.support.annotation.NonNull;

final class DecoderWrapper {
    private final Camera mCamera;
    private final Camera.CameraInfo mCameraInfo;
    private final Decoder mDecoder;
    private final Point mPreviewSize;
    private final Point mFrameSize;
    private final int mDisplayOrientation;
    private final boolean mAutoFocusSupported;
    private final boolean mFlashSupported;

    public DecoderWrapper(@NonNull Camera camera, @NonNull Camera.CameraInfo cameraInfo,
            @NonNull Decoder decoder, @NonNull Point previewSize, @NonNull Point frameSize,
            int displayOrientation, boolean autoFocusSupported, boolean flashSupported) {
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDecoder = decoder;
        mPreviewSize = previewSize;
        mFrameSize = frameSize;
        mDisplayOrientation = displayOrientation;
        mAutoFocusSupported = autoFocusSupported;
        mFlashSupported = flashSupported;
    }

    @NonNull
    public Camera getCamera() {
        return mCamera;
    }

    @NonNull
    public Camera.CameraInfo getCameraInfo() {
        return mCameraInfo;
    }

    @NonNull
    public Decoder getDecoder() {
        return mDecoder;
    }

    @NonNull
    public Point getPreviewSize() {
        return mPreviewSize;
    }

    @NonNull
    public Point getFrameSize() {
        return mFrameSize;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    public boolean isAutoFocusSupported() {
        return mAutoFocusSupported;
    }

    public boolean isFlashSupported() {
        return mFlashSupported;
    }
}

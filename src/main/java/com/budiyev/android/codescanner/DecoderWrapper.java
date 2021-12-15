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

import android.hardware.Camera;

import androidx.annotation.NonNull;

final class DecoderWrapper {

    private final Camera mCamera;
    private final Camera.CameraInfo mCameraInfo;
    private final Decoder mDecoder;
    private final Point mImageSize;
    private final Point mPreviewSize;
    private final Point mViewSize;
    private final int mDisplayOrientation;
    private final boolean mReverseHorizontal;
    private final boolean mAutoFocusSupported;
    private final boolean mFlashSupported;

    public DecoderWrapper(@NonNull final Camera camera, @NonNull final Camera.CameraInfo cameraInfo,
            @NonNull final Decoder decoder, @NonNull final Point imageSize,
            @NonNull final Point previewSize, @NonNull final Point viewSize,
            final int displayOrientation, final boolean autoFocusSupported,
            final boolean flashSupported) {
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDecoder = decoder;
        mImageSize = imageSize;
        mPreviewSize = previewSize;
        mViewSize = viewSize;
        mDisplayOrientation = displayOrientation;
        mReverseHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
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
    public Point getImageSize() {
        return mImageSize;
    }

    @NonNull
    public Point getPreviewSize() {
        return mPreviewSize;
    }

    @NonNull
    public Point getViewSize() {
        return mViewSize;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    public boolean shouldReverseHorizontal() {
        return mReverseHorizontal;
    }

    public boolean isAutoFocusSupported() {
        return mAutoFocusSupported;
    }

    public boolean isFlashSupported() {
        return mFlashSupported;
    }

    public void release() {
        mCamera.release();
        mDecoder.shutdown();
    }
}

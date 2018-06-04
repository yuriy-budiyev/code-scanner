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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

final class Utils {
    private static final float MIN_DISTORTION = 0.3f;
    private static final float MAX_DISTORTION = 3f;
    private static final float DISTORTION_STEP = 0.1f;
    private static final int MIN_PREVIEW_PIXELS = 589824;

    private Utils() {
    }

    public static void optimizeParameters(@NonNull final Camera.Parameters parameters) {
        CameraConfigurationUtils.setBestPreviewFPS(parameters);
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
        CameraConfigurationUtils.setVideoStabilization(parameters);
        parameters.setPreviewFormat(ImageFormat.NV21);
    }

    @NonNull
    public static Point findSuitableImageSize(@NonNull final Camera.Parameters parameters, final int frameWidth,
            final int frameHeight) {
        final List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (sizes != null && !sizes.isEmpty()) {
            Collections.sort(sizes, new CameraSizeComparator());
            final float frameRatio = (float) frameWidth / (float) frameHeight;
            for (float distortion = MIN_DISTORTION; distortion <= MAX_DISTORTION; distortion += DISTORTION_STEP) {
                for (final Camera.Size size : sizes) {
                    final int width = size.width;
                    final int height = size.height;
                    if (width * height >= MIN_PREVIEW_PIXELS &&
                            Math.abs(frameRatio - (float) width / (float) height) <= distortion) {
                        return new Point(width, height);
                    }
                }
            }
        }
        final Camera.Size defaultSize = parameters.getPreviewSize();
        if (defaultSize == null) {
            throw new CodeScannerException("Unable to configure camera preview size");
        }
        return new Point(defaultSize.width, defaultSize.height);
    }

    public static boolean disableAutoFocus(@NonNull final Camera.Parameters parameters) {
        final List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes == null || focusModes.isEmpty()) {
            return false;
        }
        final String focusMode = parameters.getFocusMode();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            if (Camera.Parameters.FOCUS_MODE_FIXED.equals(focusMode)) {
                return false;
            } else {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                return true;
            }
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            if (Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)) {
                return false;
            } else {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                return true;
            }
        }
        return false;
    }

    public static boolean setAutoFocusMode(@NonNull final Camera.Parameters parameters,
            final AutoFocusMode autoFocusMode) {
        final List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes == null || focusModes.isEmpty()) {
            return false;
        }
        if (autoFocusMode == AutoFocusMode.CONTINUOUS) {
            if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(parameters.getFocusMode())) {
                return false;
            }
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                return true;
            }
        }
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(parameters.getFocusMode())) {
            return false;
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            return true;
        } else {
            return false;
        }
    }

    public static boolean setFlashMode(@NonNull final Camera.Parameters parameters, @NonNull final String flashMode) {
        if (flashMode.equals(parameters.getFlashMode())) {
            return false;
        }
        final List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(flashMode)) {
            parameters.setFlashMode(flashMode);
            return true;
        }
        return false;
    }

    public static int getDisplayOrientation(@NonNull final Context context,
            @NonNull final Camera.CameraInfo cameraInfo) {
        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            throw new CodeScannerException("Unable to access window manager");
        }
        final int degrees;
        final int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                if (rotation % 90 == 0) {
                    degrees = (360 + rotation) % 360;
                } else {
                    throw new CodeScannerException("Invalid display rotation");
                }
        }
        return ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 180 : 360) + cameraInfo.orientation -
                degrees) % 360;
    }

    public static boolean isPortrait(final int orientation) {
        return orientation == 90 || orientation == 270;
    }

    public static boolean isLaidOut(@NonNull final View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return view.isLaidOut();
        } else {
            return view.getWidth() > 0 && view.getHeight() > 0;
        }
    }

    @NonNull
    public static Point getPreviewSize(final int imageWidth, final int imageHeight, final int frameWidth,
            final int frameHeight) {
        if (imageWidth == frameWidth && imageHeight == frameHeight) {
            return new Point(frameWidth, frameHeight);
        }
        final int resultWidth = imageWidth * frameHeight / imageHeight;
        if (resultWidth < frameWidth) {
            return new Point(frameWidth, imageHeight * frameWidth / imageWidth);
        } else {
            return new Point(resultWidth, frameHeight);
        }
    }

    @NonNull
    public static Rect getImageFrameRect(final int imageWidth, final int imageHeight, @NonNull final Rect viewFrameRect,
            @NonNull final Point previewSize, @NonNull final Point viewSize) {
        final int previewWidth = previewSize.getX();
        final int previewHeight = previewSize.getY();
        final int viewWidth = viewSize.getX();
        final int viewHeight = viewSize.getY();
        final int wD = (previewWidth - viewWidth) / 2;
        final int hD = (previewHeight - viewHeight) / 2;
        final float wR = (float) imageWidth / (float) previewWidth;
        final float hR = (float) imageHeight / (float) previewHeight;
        return new Rect(Math.max(Math.round((viewFrameRect.getLeft() + wD) * wR), 0),
                Math.max(Math.round((viewFrameRect.getTop() + hD) * hR), 0),
                Math.min(Math.round((viewFrameRect.getRight() + wD) * wR), imageWidth),
                Math.min(Math.round((viewFrameRect.getBottom() + hD) * hR), imageHeight));
    }

    @NonNull
    public static byte[] rotateYuv(@NonNull final byte[] source, final int width, final int height,
            final int rotation) {
        if (rotation == 0 || rotation == 360) {
            return source;
        }
        if (rotation % 90 != 0 || rotation < 0 || rotation > 270) {
            throw new IllegalArgumentException("Invalid rotation (valid: 0, 90, 180, 270)");
        }
        final byte[] output = new byte[source.length];
        final int frameSize = width * height;
        final boolean swap = rotation % 180 != 0;
        final boolean flipX = rotation % 270 != 0;
        final boolean flipY = rotation >= 180;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int yIn = j * width + i;
                final int uIn = frameSize + (j >> 1) * width + (i & ~1);
                final int vIn = uIn + 1;
                final int wOut = swap ? height : width;
                final int hOut = swap ? width : height;
                final int iSwapped = swap ? j : i;
                final int jSwapped = swap ? i : j;
                final int iOut = flipX ? wOut - iSwapped - 1 : iSwapped;
                final int jOut = flipY ? hOut - jSwapped - 1 : jSwapped;
                final int yOut = jOut * wOut + iOut;
                final int uOut = frameSize + (jOut >> 1) * wOut + (iOut & ~1);
                final int vOut = uOut + 1;
                output[yOut] = (byte) (0xff & source[yIn]);
                output[uOut] = (byte) (0xff & source[uIn]);
                output[vOut] = (byte) (0xff & source[vIn]);
            }
        }
        return output;
    }

    public static final class SuppressErrorCallback implements ErrorCallback {
        @Override
        public void onError(@NonNull final Exception error) {
            // Do nothing
        }
    }

    private static final class CameraSizeComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(@NonNull final Camera.Size a, @NonNull final Camera.Size b) {
            final int aPixels = a.height * a.width;
            final int bPixels = b.height * b.width;
            return aPixels < bPixels ? 1 : aPixels == bPixels ? 0 : -1;
        }
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.WindowManager;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class Utils {
    private static final float MIN_DISTORTION = 0.3f;
    private static final float MAX_DISTORTION = 3f;
    private static final float DISTORTION_STEP = 0.1f;
    private static final int MIN_PREVIEW_PIXELS = 589824;
    private static final int MIN_FPS = 10000;
    private static final int MAX_FPS = 30000;

    private Utils() {
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

    public static void configureFpsRange(@NonNull final Camera.Parameters parameters) {
        final int[] currentFpsRange = new int[2];
        parameters.getPreviewFpsRange(currentFpsRange);
        if (currentFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] >= MIN_FPS &&
                currentFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] <= MAX_FPS) {
            return;
        }
        final List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        if (supportedFpsRanges == null || supportedFpsRanges.isEmpty()) {
            return;
        }
        for (final int[] fpsRange : supportedFpsRanges) {
            if (fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] >= MIN_FPS &&
                    fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] <= MAX_FPS) {
                parameters.setPreviewFpsRange(fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                return;
            }
        }
    }

    public static void configureSceneMode(@NonNull final Camera.Parameters parameters) {
        if (!Camera.Parameters.SCENE_MODE_BARCODE.equals(parameters.getSceneMode())) {
            final List<String> supportedSceneModes = parameters.getSupportedSceneModes();
            if (supportedSceneModes != null && supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_BARCODE)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            }
        }
    }

    public static void configureVideoStabilization(@NonNull final Camera.Parameters parameters) {
        if (parameters.isVideoStabilizationSupported() && !parameters.getVideoStabilization()) {
            parameters.setVideoStabilization(true);
        }
    }

    public static void configureFocusArea(@NonNull final Rect area, @NonNull final Camera.Parameters parameters) {
        final List<Camera.Area> areas = new ArrayList<>(1);
        areas.add(new Camera.Area(
                new android.graphics.Rect(area.getLeft(), area.getTop(), area.getRight(), area.getBottom()), 1000));
        parameters.setFocusAreas(areas);
        parameters.setMeteringAreas(areas);
    }

    public static void clearFocusArea(@NonNull final Camera.Parameters parameters) {
        parameters.setFocusAreas(null);
        parameters.setMeteringAreas(null);
    }

/*    private int clamp(final int touchCoordinateInCameraReper, final int focusAreaSize) {
        final int result;
        if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - focusAreaSize / 2;
            } else {
                result = -1000 + focusAreaSize / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize / 2;
        }
        return result;
    }


    private android.graphics.Rect calculateFocusArea(final float x, final float y) {
        final int FOCUS_AREA_SIZE = 300;
        final int left = clamp(Float.valueOf((x / mScannerView.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        final int top = clamp(Float.valueOf((y / mScannerView.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new android.graphics.Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    public static void configureFocusAreas(@NonNull final Camera.Parameters parameters) {
        // return the area to focus
        final android.graphics.Rect rect = calculateFocusArea(event.getX(), event.getY());
        final List<Camera.Area> meteringAreas = new ArrayList<>();
        meteringAreas.add(new Camera.Area(rect, 1000));
        //true if Metering is supported
        if (parameters.getMaxNumMeteringAreas() > 0) {
            parameters.setMeteringAreas(meteringAreas);
        }
        //true if Focus is supported
        if (parameters.getMaxNumFocusAreas() > 0) {
            parameters.setFocusAreas(meteringAreas);
        }
    }*/

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

    public static void setZoom(@NonNull final Camera.Parameters parameters, final int zoom) {
        if (parameters.isZoomSupported()) {
            if (parameters.getZoom() != zoom) {
                final int maxZoom = parameters.getMaxZoom();
                if (zoom <= maxZoom) {
                    parameters.setZoom(zoom);
                } else {
                    parameters.setZoom(maxZoom);
                }
            }
        }
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

    @Nullable
    public static Result decodeLuminanceSource(@NonNull final MultiFormatReader reader,
            @NonNull final LuminanceSource luminanceSource) throws ReaderException {
        try {
            return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(luminanceSource)));
        } catch (final NotFoundException e) {
            return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(luminanceSource.invert())));
        } finally {
            reader.reset();
        }
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
            return Integer.compare(b.height * b.width, a.height * a.width);
        }
    }
}

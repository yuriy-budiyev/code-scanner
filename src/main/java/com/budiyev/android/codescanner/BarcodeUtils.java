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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

public final class BarcodeUtils {
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 90;
    public static final int ROTATION_180 = 180;
    public static final int ROTATION_270 = 270;

    private BarcodeUtils() {
    }

    @Nullable
    public Result decodeBitmap(@NonNull final Bitmap bitmap) {
        return decodeBitmap(bitmap, null);
    }

    @Nullable
    public Result decodeBitmap(@NonNull final Bitmap bitmap, @Nullable final List<BarcodeFormat> formats) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return decodeRgb(pixels, width, height, formats);
    }

    @Nullable
    public Result decodeRgb(@NonNull final int[] pixels, final int width, final int height) {
        return decodeRgb(pixels, width, height, null);
    }

    @Nullable
    public Result decodeRgb(@NonNull final int[] pixels, final int width, final int height,
            @Nullable final List<BarcodeFormat> formats) {
        final MultiFormatReader reader = createReader(formats);
        try {
            return reader.decodeWithState(
                    new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels))));
        } catch (final ReaderException e) {
            return null;
        }
    }

    @Nullable
    public Result decodeYuv(@NonNull final byte[] pixels, final int width, final int height) {
        return decodeYuv(pixels, width, height, ROTATION_0, false, null);
    }

    @Nullable
    @SuppressWarnings("SuspiciousNameCombination")
    public Result decodeYuv(@NonNull final byte[] pixels, final int width, final int height,
            @Rotation final int rotation, final boolean reverseHorizontal,
            @Nullable final List<BarcodeFormat> formats) {
        final MultiFormatReader reader = createReader(formats);
        final byte[] rotatedPixels = Utils.rotateYuv(pixels, width, height, rotation);
        final int rotatedWidth;
        final int rotatedHeight;
        if (rotation == ROTATION_90 || rotation == ROTATION_270) {
            rotatedWidth = height;
            rotatedHeight = width;
        } else {
            rotatedWidth = width;
            rotatedHeight = height;
        }
        try {
            return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(
                    new PlanarYUVLuminanceSource(rotatedPixels, rotatedWidth, rotatedHeight, 0, 0, rotatedWidth,
                            rotatedHeight, reverseHorizontal))));
        } catch (final ReaderException e) {
            return null;
        }
    }

    @NonNull
    public Bitmap createBitmap(@NonNull final BitMatrix matrix) {
        final int width = matrix.getWidth();
        final int height = matrix.getHeight();
        final int length = width * height;
        final int[] pixels = new int[length];
        for (int i = 0; i < length; i++) {
            pixels[i] = matrix.get(i % width, i / height) ? Color.BLACK : Color.WHITE;
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    @NonNull
    private MultiFormatReader createReader(@Nullable final List<BarcodeFormat> formats) {
        final MultiFormatReader reader = new MultiFormatReader();
        final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats != null ? formats : CodeScanner.ALL_FORMATS);
        reader.setHints(hints);
        return reader;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
    public @interface Rotation {
    }
}

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

import android.support.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class DecodeTask {
    private final byte[] mImage;
    private final Point mImageSize;
    private final Point mPreviewSize;
    private final Point mViewSize;
    private final int mOrientation;
    private final boolean mSquareFrame;
    private final boolean mReverseHorizontal;

    public DecodeTask(@NonNull byte[] image, @NonNull Point imageSize, @NonNull Point previewSize,
            @NonNull Point viewSize, int orientation, boolean squareFrame, boolean reverseHorizontal) {
        mImage = image;
        mImageSize = imageSize;
        mPreviewSize = previewSize;
        mViewSize = viewSize;
        mOrientation = orientation;
        mSquareFrame = squareFrame;
        mReverseHorizontal = reverseHorizontal;
    }

    @NonNull
    @SuppressWarnings("SuspiciousNameCombination")
    public Result decode(@NonNull MultiFormatReader reader) throws ReaderException {
        int imageWidth = mImageSize.getX();
        int imageHeight = mImageSize.getY();
        byte[] image;
        int width;
        int height;
        if (mOrientation == 0) {
            image = mImage;
            width = imageWidth;
            height = imageHeight;
        } else {
            image = Utils.rotateNV21(mImage, imageWidth, imageHeight, mOrientation);
            if (mOrientation == 90 || mOrientation == 270) {
                width = imageHeight;
                height = imageWidth;
            } else {
                width = imageWidth;
                height = imageHeight;
            }
        }
        Rect frameRect = Utils.getImageFrameRect(mSquareFrame, width, height, mPreviewSize, mViewSize);
        return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(
                new PlanarYUVLuminanceSource(image, width, height, frameRect.getLeft(), frameRect.getTop(),
                        frameRect.getWidth(), frameRect.getHeight(), mReverseHorizontal))));
    }
}

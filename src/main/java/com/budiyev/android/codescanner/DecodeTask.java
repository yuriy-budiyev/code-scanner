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

import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class DecodeTask {
    private final byte[] mData;
    private final int mDataWidth;
    private final int mDataHeight;
    private final int mFrameWidth;
    private final int mFrameHeight;
    private final int mOrientation;
    private final boolean mSquareFrame;
    private final DecodeCallback mCallback;

    public DecodeTask(@NonNull byte[] data, int dataWidth, int dataHeight, int frameWidth,
            int frameHeight, int orientation, boolean squareFrame,
            @NonNull DecodeCallback callback) {
        mData = data;
        mDataWidth = dataWidth;
        mDataHeight = dataHeight;
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;
        mOrientation = orientation;
        mSquareFrame = squareFrame;
        mCallback = callback;
    }

    @NonNull
    public DecodeCallback getCallback() {
        return mCallback;
    }

    @NonNull
    @SuppressWarnings("SuspiciousNameCombination")
    public Result decode(@NonNull MultiFormatReader reader) throws ReaderException {
        byte[] data;
        int dataWidth;
        int dataHeight;
        if (mOrientation == 0) {
            data = mData;
            dataWidth = mDataWidth;
            dataHeight = mDataHeight;
        } else {
            data = ScannerHelper.rotateNV21(mData, mDataWidth, mDataHeight, mOrientation);
            if (mOrientation == 90 || mOrientation == 270) {
                dataWidth = mDataHeight;
                dataHeight = mDataWidth;
            } else {
                dataWidth = mDataWidth;
                dataHeight = mDataHeight;
            }
        }
        Rect frameRect = ScannerHelper
                .getImageFrameRect(mSquareFrame, dataWidth, dataHeight, mFrameWidth, mFrameHeight);
        return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(
                new PlanarYUVLuminanceSource(data, dataWidth, dataHeight, frameRect.left,
                        frameRect.top, frameRect.width(), frameRect.height(), false))));
    }
}

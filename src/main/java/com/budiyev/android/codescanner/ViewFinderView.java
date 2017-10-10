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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Px;
import android.view.View;

final class ViewFinderView extends View {
    private final Paint mMaskPaint;
    private final Paint mFramePaint;
    private final Path mPath;
    private Rect mFrameRect;
    private boolean mSquareFrame;
    private int mFrameCornerSize;

    public ViewFinderView(@NonNull Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect frameRect = mFrameRect;
        if (frameRect == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        mPath.reset();
        mPath.addRect(0, 0, width, frameRect.getTop(), Path.Direction.CW);
        mPath.addRect(0, frameRect.getTop(), frameRect.getLeft(), frameRect.getBottom(), Path.Direction.CW);
        mPath.addRect(frameRect.getRight(), frameRect.getTop(), width, frameRect.getBottom(), Path.Direction.CW);
        mPath.addRect(0, frameRect.getBottom(), width, height, Path.Direction.CW);
        canvas.drawPath(mPath, mMaskPaint);
        mPath.reset();
        mPath.moveTo(frameRect.getLeft(), frameRect.getTop() + mFrameCornerSize);
        mPath.lineTo(frameRect.getLeft(), frameRect.getTop());
        mPath.lineTo(frameRect.getLeft() + mFrameCornerSize, frameRect.getTop());
        mPath.moveTo(frameRect.getRight() - mFrameCornerSize, frameRect.getTop());
        mPath.lineTo(frameRect.getRight(), frameRect.getTop());
        mPath.lineTo(frameRect.getRight(), frameRect.getTop() + mFrameCornerSize);
        mPath.moveTo(frameRect.getRight(), frameRect.getBottom() - mFrameCornerSize);
        mPath.lineTo(frameRect.getRight(), frameRect.getBottom());
        mPath.lineTo(frameRect.getRight() - mFrameCornerSize, frameRect.getBottom());
        mPath.moveTo(frameRect.getLeft() + mFrameCornerSize, frameRect.getBottom());
        mPath.lineTo(frameRect.getLeft(), frameRect.getBottom());
        mPath.lineTo(frameRect.getLeft(), frameRect.getBottom() - mFrameCornerSize);
        canvas.drawPath(mPath, mFramePaint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {
        mFrameRect = Utils.getFrameRect(mSquareFrame, width, height);
    }

    void setSquareFrame(boolean squareFrame) {
        mSquareFrame = squareFrame;
        if (mFrameRect != null) {
            mFrameRect = Utils.getFrameRect(mSquareFrame, getWidth(), getHeight());
        }
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    boolean isSquareFrame() {
        return mSquareFrame;
    }

    void setMaskColor(@ColorInt int color) {
        mMaskPaint.setColor(color);
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    void setFrameColor(@ColorInt int color) {
        mFramePaint.setColor(color);
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    void setFrameWidth(@Px int width) {
        mFramePaint.setStrokeWidth(width);
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    void setFrameCornersSize(@Px int size) {
        mFrameCornerSize = size;
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }
}

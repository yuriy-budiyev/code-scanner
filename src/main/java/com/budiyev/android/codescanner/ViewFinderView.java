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
    private final Path mFramePath;
    private Rect mFrameRect;
    private boolean mSquareFrame;
    private int mFrameCornerSize;

    public ViewFinderView(@NonNull Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect frameRect = mFrameRect;
        if (frameRect == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int top = frameRect.getTop();
        int left = frameRect.getLeft();
        int right = frameRect.getRight();
        int bottom = frameRect.getBottom();
        canvas.drawRect(0, 0, width, top, mMaskPaint);
        canvas.drawRect(0, top, left, bottom, mMaskPaint);
        canvas.drawRect(right, top, width, bottom, mMaskPaint);
        canvas.drawRect(0, bottom, width, height, mMaskPaint);
        mFramePath.reset();
        mFramePath.moveTo(left, top + mFrameCornerSize);
        mFramePath.lineTo(left, top);
        mFramePath.lineTo(left + mFrameCornerSize, top);
        mFramePath.moveTo(right - mFrameCornerSize, top);
        mFramePath.lineTo(right, top);
        mFramePath.lineTo(right, top + mFrameCornerSize);
        mFramePath.moveTo(right, bottom - mFrameCornerSize);
        mFramePath.lineTo(right, bottom);
        mFramePath.lineTo(right - mFrameCornerSize, bottom);
        mFramePath.moveTo(left + mFrameCornerSize, bottom);
        mFramePath.lineTo(left, bottom);
        mFramePath.lineTo(left, bottom - mFrameCornerSize);
        canvas.drawPath(mFramePath, mFramePaint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mFrameRect = Utils.getViewFrameRect(mSquareFrame, right - left, bottom - top);
    }

    void setSquareFrame(boolean squareFrame) {
        mSquareFrame = squareFrame;
        if (mFrameRect != null) {
            mFrameRect = Utils.getViewFrameRect(mSquareFrame, getWidth(), getHeight());
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

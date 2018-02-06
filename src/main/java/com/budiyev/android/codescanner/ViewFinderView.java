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
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.view.View;

final class ViewFinderView extends View {
    private static final float MAX_FRAME_SIZE = 0.75f;
    private final Paint mMaskPaint;
    private final Paint mFramePaint;
    private final Path mFramePath;
    private Rect mFrameRect;
    private int mFrameCornerSize;
    private float mFrameRatioWidth = 1f;
    private float mFrameRatioHeight = 1f;

    public ViewFinderView(@NonNull Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePath = new Path();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
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
        invalidateFrameRect(right - left, bottom - top);
    }

    @Nullable
    Rect getFrameRect() {
        return mFrameRect;
    }

    void setFrameAspectRatio(@FloatRange(from = 0, fromInclusive = false) float ratioWidth,
            @FloatRange(from = 0, fromInclusive = false) float ratioHeight) {
        mFrameRatioWidth = ratioWidth;
        mFrameRatioHeight = ratioHeight;
        invalidateFrameRect();
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    void setFrameRatioWidth(@FloatRange(from = 0, fromInclusive = false) float ratioWidth) {
        mFrameRatioWidth = ratioWidth;
        invalidateFrameRect();
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
    }

    void setFrameRatioHeight(@FloatRange(from = 0, fromInclusive = false) float ratioHeight) {
        mFrameRatioHeight = ratioHeight;
        invalidateFrameRect();
        if (Utils.isLaidOut(this)) {
            invalidate();
        }
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

    void setFrameThickness(@Px int thickness) {
        mFramePaint.setStrokeWidth(thickness);
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

    private void invalidateFrameRect() {
        invalidateFrameRect(getWidth(), getHeight());
    }

    private void invalidateFrameRect(int width, int height) {
        if (width > 0 && height > 0) {
            float viewAR = (float) width / (float) height;
            float frameAR = mFrameRatioWidth / mFrameRatioHeight;
            int frameWidth;
            int frameHeight;
            if (viewAR <= frameAR) {
                frameWidth = Math.round(width * MAX_FRAME_SIZE);
                frameHeight = Math.round(frameWidth / frameAR);
            } else {
                frameHeight = Math.round(height * MAX_FRAME_SIZE);
                frameWidth = Math.round(frameHeight * frameAR);
            }
            int frameLeft = (width - frameWidth) / 2;
            int frameTop = (height - frameHeight) / 2;
            mFrameRect = new Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
        }
    }
}

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
    private final Paint mMaskPaint;
    private final Paint mFramePaint;
    private final Path mFramePath;
    private Rect mFrameRect;
    private int mFrameCornersSize;
    private float mFrameRatioWidth = 1f;
    private float mFrameRatioHeight = 1f;
    private float mFrameSize = 0.75f;

    public ViewFinderView(@NonNull final Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePath = new Path();
    }

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        final Rect frameRect = mFrameRect;
        if (frameRect == null) {
            return;
        }
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        final int top = frameRect.getTop();
        final int left = frameRect.getLeft();
        final int right = frameRect.getRight();
        final int bottom = frameRect.getBottom();
        canvas.drawRect(0, 0, width, top, mMaskPaint);
        canvas.drawRect(0, top, left, bottom, mMaskPaint);
        canvas.drawRect(right, top, width, bottom, mMaskPaint);
        canvas.drawRect(0, bottom, width, height, mMaskPaint);
        mFramePath.reset();
        mFramePath.moveTo(left, top + mFrameCornersSize);
        mFramePath.lineTo(left, top);
        mFramePath.lineTo(left + mFrameCornersSize, top);
        mFramePath.moveTo(right - mFrameCornersSize, top);
        mFramePath.lineTo(right, top);
        mFramePath.lineTo(right, top + mFrameCornersSize);
        mFramePath.moveTo(right, bottom - mFrameCornersSize);
        mFramePath.lineTo(right, bottom);
        mFramePath.lineTo(right - mFrameCornersSize, bottom);
        mFramePath.moveTo(left + mFrameCornersSize, bottom);
        mFramePath.lineTo(left, bottom);
        mFramePath.lineTo(left, bottom - mFrameCornersSize);
        canvas.drawPath(mFramePath, mFramePaint);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        invalidateFrameRect(right - left, bottom - top);
    }

    @Nullable
    Rect getFrameRect() {
        return mFrameRect;
    }

    void setFrameAspectRatio(@FloatRange(from = 0, fromInclusive = false) final float ratioWidth,
            @FloatRange(from = 0, fromInclusive = false) final float ratioHeight) {
        mFrameRatioWidth = ratioWidth;
        mFrameRatioHeight = ratioHeight;
        invalidateFrameRect();
        if (isLaidOut()) {
            invalidate();
        }
    }

    @FloatRange(from = 0, fromInclusive = false)
    float getFrameAspectRatioWidth() {
        return mFrameRatioWidth;
    }

    void setFrameAspectRatioWidth(@FloatRange(from = 0, fromInclusive = false) final float ratioWidth) {
        mFrameRatioWidth = ratioWidth;
        invalidateFrameRect();
        if (isLaidOut()) {
            invalidate();
        }
    }

    @FloatRange(from = 0, fromInclusive = false)
    float getFrameAspectRatioHeight() {
        return mFrameRatioHeight;
    }

    void setFrameAspectRatioHeight(@FloatRange(from = 0, fromInclusive = false) final float ratioHeight) {
        mFrameRatioHeight = ratioHeight;
        invalidateFrameRect();
        if (isLaidOut()) {
            invalidate();
        }
    }

    @ColorInt
    int getMaskColor() {
        return mMaskPaint.getColor();
    }

    void setMaskColor(@ColorInt final int color) {
        mMaskPaint.setColor(color);
        if (isLaidOut()) {
            invalidate();
        }
    }

    @ColorInt
    int getFrameColor() {
        return mFramePaint.getColor();
    }

    void setFrameColor(@ColorInt final int color) {
        mFramePaint.setColor(color);
        if (isLaidOut()) {
            invalidate();
        }
    }

    @Px
    int getFrameThickness() {
        return (int) mFramePaint.getStrokeWidth();
    }

    void setFrameThickness(@Px final int thickness) {
        mFramePaint.setStrokeWidth(thickness);
        if (isLaidOut()) {
            invalidate();
        }
    }

    @Px
    int getFrameCornersSize() {
        return mFrameCornersSize;
    }

    void setFrameCornersSize(@Px final int size) {
        mFrameCornersSize = size;
        if (isLaidOut()) {
            invalidate();
        }
    }

    @FloatRange(from = 0.1, to = 1.0)
    public float getFrameSize() {
        return mFrameSize;
    }

    void setFrameSize(@FloatRange(from = 0.1, to = 1.0) final float size) {
        mFrameSize = size;
        invalidateFrameRect();
        if (isLaidOut()) {
            invalidate();
        }
    }

    private void invalidateFrameRect() {
        invalidateFrameRect(getWidth(), getHeight());
    }

    private void invalidateFrameRect(final int width, final int height) {
        if (width > 0 && height > 0) {
            final float viewAR = (float) width / (float) height;
            final float frameAR = mFrameRatioWidth / mFrameRatioHeight;
            final int frameWidth;
            final int frameHeight;
            if (viewAR <= frameAR) {
                frameWidth = Math.round(width * mFrameSize);
                frameHeight = Math.round(frameWidth / frameAR);
            } else {
                frameHeight = Math.round(height * mFrameSize);
                frameWidth = Math.round(frameHeight * frameAR);
            }
            final int frameLeft = (width - frameWidth) / 2;
            final int frameTop = (height - frameHeight) / 2;
            mFrameRect = new Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
        }
    }
}

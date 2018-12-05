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
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

final class ViewFinderView extends View {
    private final Paint mMaskPaint;
    private final Paint mFramePaint;
    private final Path mPath;
    private Rect mFrameRect;
    private int mFrameCornersSize = 0;
    private int mFrameCornersRadius = 0;
    private float mFrameRatioWidth = 1f;
    private float mFrameRatioHeight = 1f;
    private float mFrameSize = 0.75f;

    public ViewFinderView(@NonNull final Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
    }

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        final Rect frameRect = mFrameRect;
        if (frameRect == null) {
            return;
        }
        final float width = getWidth();
        final float height = getHeight();
        final float top = frameRect.getTop();
        final float left = frameRect.getLeft();
        final float right = frameRect.getRight();
        final float bottom = frameRect.getBottom();
        final float frameCornersSize = mFrameCornersSize;
        final float frameCornersRadius = mFrameCornersRadius;
        final float normalizedRadius =
                frameCornersRadius > frameCornersSize ? frameCornersSize : frameCornersRadius;
        mPath.reset();
        mPath.moveTo(left, top + normalizedRadius);
        mPath.rQuadTo(0, -normalizedRadius, normalizedRadius, -normalizedRadius);
        mPath.lineTo(right - normalizedRadius, top);
        mPath.rQuadTo(normalizedRadius, 0, normalizedRadius, normalizedRadius);
        mPath.lineTo(right, bottom - normalizedRadius);
        mPath.rQuadTo(0, normalizedRadius, -normalizedRadius, normalizedRadius);
        mPath.lineTo(left + normalizedRadius, bottom);
        mPath.rQuadTo(-normalizedRadius, 0, -normalizedRadius, -normalizedRadius);
        mPath.lineTo(left, top + normalizedRadius);
        mPath.moveTo(0, 0);
        mPath.rLineTo(0, height);
        mPath.rLineTo(width, 0);
        mPath.rLineTo(0, -height);
        mPath.rLineTo(-width, 0);
        canvas.drawPath(mPath, mMaskPaint);
        mPath.reset();
        mPath.moveTo(left, top + frameCornersSize);
        mPath.lineTo(left, top + normalizedRadius);
        mPath.rQuadTo(0, -normalizedRadius, normalizedRadius, -normalizedRadius);
        mPath.lineTo(left + frameCornersSize, top);
        mPath.moveTo(right, top + frameCornersSize);
        mPath.lineTo(right, top + normalizedRadius);
        mPath.rQuadTo(0, -normalizedRadius, -normalizedRadius, -normalizedRadius);
        mPath.lineTo(right - frameCornersSize, top);
        mPath.moveTo(right, bottom - frameCornersSize);
        mPath.lineTo(right, bottom - normalizedRadius);
        mPath.rQuadTo(0, normalizedRadius, -normalizedRadius, normalizedRadius);
        mPath.lineTo(right - frameCornersSize, bottom);
        mPath.moveTo(left, bottom - frameCornersSize);
        mPath.lineTo(left, bottom - normalizedRadius);
        mPath.rQuadTo(0, normalizedRadius, normalizedRadius, normalizedRadius);
        mPath.lineTo(left + frameCornersSize, bottom);
        canvas.drawPath(mPath, mFramePaint);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
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

    void setFrameAspectRatioWidth(
            @FloatRange(from = 0, fromInclusive = false) final float ratioWidth) {
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

    void setFrameAspectRatioHeight(
            @FloatRange(from = 0, fromInclusive = false) final float ratioHeight) {
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

    @Px
    int getFrameCornersRadius() {
        return mFrameCornersRadius;
    }

    void setFrameCornersRadius(@Px final int radius) {
        mFrameCornersRadius = radius;
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
            mFrameRect =
                    new Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
        }
    }
}

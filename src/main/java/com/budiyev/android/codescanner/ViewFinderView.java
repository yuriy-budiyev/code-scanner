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
    private final Path mFramePath;
    private final Path mFrameMaskTopRectPath;
    private final Path mFrameMaskBottomRectPath;
    private Rect mFrameRect;
    private int mFrameCornersSize;
    private float mFrameRatioWidth = 1f;
    private float mFrameRatioHeight = 1f;
    private float mFrameCornersRadiusX = 0f;
    private float mFrameCornersRadiusY = 0f;
    private float mFrameSize = 0.75f;

    public ViewFinderView(@NonNull final Context context) {
        super(context);
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePath = new Path();
        mFrameMaskTopRectPath = new Path();
        mFrameMaskBottomRectPath = new Path();
    }

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        final Rect frameRect = mFrameRect;
        if (frameRect == null) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();
        final int top = frameRect.getTop();
        final int left = frameRect.getLeft();
        final int right = frameRect.getRight();
        final int bottom = frameRect.getBottom();
        final float frameStrokeWidth = mFrameCornersSize == 0 ? 0f : mFramePaint.getStrokeWidth() / 2.0f;
        final float rx = mFrameCornersRadiusX > (mFrameCornersSize / 2.0f) ? mFrameCornersSize / 2.0f
                : mFrameCornersRadiusX;
        final float ry = mFrameCornersRadiusY > (mFrameCornersSize / 2.0f) ? mFrameCornersSize / 2.0f
                : mFrameCornersRadiusY;
        invalidateFrameMaskTopRect(left, top, right, width, frameStrokeWidth, rx, ry);
        invalidateFrameMaskBottomRect(left, top, right, bottom, width, height, frameStrokeWidth, rx, ry);
        invalidateFramePath(left, top, right, bottom, rx, ry);
        canvas.drawPath(mFrameMaskTopRectPath, mMaskPaint);
        canvas.drawPath(mFrameMaskBottomRectPath, mMaskPaint);
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

    @FloatRange(from = 0.0)
    float getFrameCornersRadiusX() {
        return (int) mFrameCornersRadiusX;
    }

    void setFrameCornersRadiusX(@FloatRange(from = 0.0) final float radiusX) {
        mFrameCornersRadiusX = radiusX;
        if (isLaidOut()) {
            invalidate();
        }
    }

    @FloatRange(from = 0.0)
    float getFrameCornersRadiusY() {
        return (int) mFrameCornersRadiusY;
    }

    void setFrameCornersRadiusY(@FloatRange(from = 0.0) final float radiusY) {
        mFrameCornersRadiusY = radiusY;
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

    private void invalidateFrameMaskTopRect(final int left,
            final int top,
            final int right,
            final int width,
            final float frameStrokeWidth,
            final float rx,
            final float ry) {
        mFrameMaskTopRectPath.reset();
        mFrameMaskTopRectPath.moveTo(0, 0);
        mFrameMaskTopRectPath.rLineTo(0, top + mFrameCornersSize);
        mFrameMaskTopRectPath.rLineTo(left + frameStrokeWidth, 0);
        // Top-left corner
        mFrameMaskTopRectPath.lineTo(left + frameStrokeWidth, top + frameStrokeWidth + ry);
        mFrameMaskTopRectPath.quadTo(left + (frameStrokeWidth / 2), top + (frameStrokeWidth / 2),
                left + frameStrokeWidth + rx, top + frameStrokeWidth);
        mFrameMaskTopRectPath.lineTo(left + mFrameCornersSize, top + frameStrokeWidth);
        mFrameMaskTopRectPath.rLineTo(0, -frameStrokeWidth);
        mFrameMaskTopRectPath.lineTo(right - mFrameCornersSize, top);
        mFrameMaskTopRectPath.rLineTo(0, frameStrokeWidth);
        // Top-right corner
        mFrameMaskTopRectPath.lineTo(right - frameStrokeWidth - rx, top + frameStrokeWidth);
        mFrameMaskTopRectPath.quadTo(right - (frameStrokeWidth / 2), top + (frameStrokeWidth / 2),
                right - frameStrokeWidth, top + frameStrokeWidth + ry);
        mFrameMaskTopRectPath.lineTo(right - frameStrokeWidth, top + mFrameCornersSize);
        mFrameMaskTopRectPath.lineTo(width, top + mFrameCornersSize);
        mFrameMaskTopRectPath.lineTo(width, 0);
        mFrameMaskTopRectPath.lineTo(0, 0);
        mFrameMaskTopRectPath.close();
    }

    private void invalidateFrameMaskBottomRect(final int left,
            final int top,
            final int right,
            final int bottom,
            final int width,
            final int height,
            final float frameStrokeWidth,
            final float rx,
            final float ry) {
        mFrameMaskBottomRectPath.reset();
        mFrameMaskBottomRectPath.moveTo(0, height);
        mFrameMaskBottomRectPath.lineTo(0, top + mFrameCornersSize);
        mFrameMaskBottomRectPath.rLineTo(left, 0);
        mFrameMaskBottomRectPath.lineTo(left, bottom - mFrameCornersSize);
        mFrameMaskBottomRectPath.rLineTo(frameStrokeWidth, 0);
        // Bottom left corner
        mFrameMaskBottomRectPath.lineTo(left + frameStrokeWidth, bottom - frameStrokeWidth - ry);
        mFrameMaskBottomRectPath.quadTo(left + (frameStrokeWidth / 2), bottom - (frameStrokeWidth / 2),
                left + frameStrokeWidth + rx, bottom - frameStrokeWidth);
        mFrameMaskBottomRectPath.lineTo(left + mFrameCornersSize, bottom - frameStrokeWidth);
        mFrameMaskBottomRectPath.rLineTo(0, frameStrokeWidth);
        mFrameMaskBottomRectPath.lineTo(right - mFrameCornersSize, bottom);
        mFrameMaskBottomRectPath.rLineTo(0, -frameStrokeWidth);
        // Bottom right corner
        mFrameMaskBottomRectPath.lineTo(right - frameStrokeWidth - rx, bottom - frameStrokeWidth);
        mFrameMaskBottomRectPath.quadTo(right - (frameStrokeWidth / 2), bottom - (frameStrokeWidth / 2),
                right - frameStrokeWidth, bottom - frameStrokeWidth - ry);
        mFrameMaskBottomRectPath.lineTo(right - frameStrokeWidth, bottom - mFrameCornersSize);
        mFrameMaskBottomRectPath.rLineTo(frameStrokeWidth, 0);
        mFrameMaskBottomRectPath.lineTo(right, top + mFrameCornersSize);
        mFrameMaskBottomRectPath.lineTo(width, top + mFrameCornersSize);
        mFrameMaskBottomRectPath.lineTo(width, height);
        mFrameMaskBottomRectPath.lineTo(0, height);
        mFrameMaskBottomRectPath.close();
    }

    private void invalidateFramePath(final int left,
            final int top,
            final int right,
            final int bottom,
            final float rx,
            final float ry) {
        mFramePath.reset();
        mFramePath.moveTo(left, top + mFrameCornersSize);
        mFramePath.lineTo(left, top + ry);
        mFramePath.rQuadTo(0, -ry, rx, -ry);
        mFramePath.lineTo(left + mFrameCornersSize, top);
        mFramePath.moveTo(right, top + mFrameCornersSize);
        mFramePath.lineTo(right, top + ry);
        mFramePath.rQuadTo(0, -ry, -rx, -ry);
        mFramePath.lineTo(right - mFrameCornersSize, top);
        mFramePath.moveTo(right, bottom - mFrameCornersSize);
        mFramePath.lineTo(right, bottom - ry);
        mFramePath.rQuadTo(0, ry, -rx, ry);
        mFramePath.lineTo(right - mFrameCornersSize, bottom);
        mFramePath.moveTo(left, bottom - mFrameCornersSize);
        mFramePath.lineTo(left, bottom - ry);
        mFramePath.rQuadTo(0, ry, rx, ry);
        mFramePath.lineTo(left + mFrameCornersSize, bottom);
    }
}

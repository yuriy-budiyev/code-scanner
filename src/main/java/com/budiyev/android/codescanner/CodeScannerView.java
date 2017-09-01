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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * A view to display code scanner preview
 *
 * @see CodeScanner
 */
public final class CodeScannerView extends ViewGroup {
    private static final boolean DEFAULT_SQUARE_FRAME = false;
    private static final int DEFAULT_MASK_COLOR = 0x77000000;
    private static final int DEFAULT_FRAME_COLOR = Color.WHITE;
    private static final float DEFAULT_FRAME_WIDTH_DP = 2f;
    private static final float DEFAULT_FRAME_CORNER_SIZE_DP = 50f;
    private SurfaceView mPreviewView;
    private ViewFinderView mViewFinderView;
    private Point mFrameSize;
    private LayoutListener mLayoutListener;

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context) {
        super(context);
        initialize(context, null);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        mPreviewView = new SurfaceView(context);
        mPreviewView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mViewFinderView = new ViewFinderView(context);
        mViewFinderView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (attributeSet == null) {
            mViewFinderView.setSquareFrame(DEFAULT_SQUARE_FRAME);
            mViewFinderView.setMaskColor(DEFAULT_MASK_COLOR);
            mViewFinderView.setFrameColor(DEFAULT_FRAME_COLOR);
            mViewFinderView
                    .setFrameWidth(Math.round(DEFAULT_FRAME_WIDTH_DP * displayMetrics.density));
            mViewFinderView.setFrameCornersSize(
                    Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * displayMetrics.density));
        } else {
            TypedArray attributes = null;
            try {
                attributes = context.getTheme()
                        .obtainStyledAttributes(attributeSet, R.styleable.CodeScannerView, 0, 0);
                mViewFinderView.setSquareFrame(attributes
                        .getBoolean(R.styleable.CodeScannerView_squareFrame, DEFAULT_SQUARE_FRAME));
                mViewFinderView.setMaskColor(attributes
                        .getColor(R.styleable.CodeScannerView_maskColor, DEFAULT_MASK_COLOR));
                mViewFinderView.setFrameColor(attributes
                        .getColor(R.styleable.CodeScannerView_frameColor, DEFAULT_FRAME_COLOR));
                mViewFinderView.setFrameWidth(attributes
                        .getDimensionPixelSize(R.styleable.CodeScannerView_frameWidth,
                                Math.round(DEFAULT_FRAME_WIDTH_DP * displayMetrics.density)));
                mViewFinderView.setFrameCornersSize(attributes
                        .getDimensionPixelSize(R.styleable.CodeScannerView_frameCornersSize,
                                Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * displayMetrics.density)));
            } finally {
                if (attributes != null) {
                    attributes.recycle();
                }
            }
        }
        addView(mPreviewView);
        addView(mViewFinderView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        Point frameSize = mFrameSize;
        if (frameSize == null) {
            mPreviewView.layout(left, top, right, bottom);
        } else {
            int frameLeft = left;
            int frameTop = top;
            int frameRight = right;
            int frameBottom = bottom;
            if (frameSize.x > width) {
                int d = (frameSize.x - width) / 2;
                frameLeft -= d;
                frameRight += d;
            }
            if (frameSize.y > height) {
                int d = (frameSize.y - height) / 2;
                frameTop -= d;
                frameBottom += d;
            }
            mPreviewView.layout(frameLeft, frameTop, frameRight, frameBottom);
        }
        mViewFinderView.layout(left, top, right, bottom);
        LayoutListener listener = mLayoutListener;
        if (listener != null) {
            listener.onLayout(width, height);
        }
    }

    /**
     * Set whether frame is square or a rectangle
     *
     * @param squareFrame is {@code true}, the frame will be square, rectangle otherwise
     */
    public void setSquareFrame(boolean squareFrame) {
        mViewFinderView.setSquareFrame(squareFrame);
    }

    /**
     * Set color of the space outside of the framing rect
     *
     * @param color Mask color
     */
    public void setMaskColor(@ColorInt int color) {
        mViewFinderView.setMaskColor(color);
    }

    /**
     * Set color of the frame
     *
     * @param color Frame color
     */
    public void setFrameColor(@ColorInt int color) {
        mViewFinderView.setFrameColor(color);
    }

    /**
     * Set frame width
     *
     * @param width Frame width in pixels
     */
    public void setFrameWidth(@Px int width) {
        mViewFinderView.setFrameWidth(width);
    }

    /**
     * Set length on the frame corners
     *
     * @param size Length in pixels
     */
    public void setFrameCornersSize(@Px int size) {
        mViewFinderView.setFrameCornersSize(size);
    }

    boolean isSquareFrame() {
        return mViewFinderView.isSquareFrame();
    }

    SurfaceView getPreviewView() {
        return mPreviewView;
    }

    ViewFinderView getViewFinderView() {
        return mViewFinderView;
    }

    void setFrameSize(@Nullable Point frameSize) {
        mFrameSize = frameSize;
        requestLayout();
    }

    void setLayoutListener(@Nullable LayoutListener layoutListener) {
        mLayoutListener = layoutListener;
    }

    interface LayoutListener {
        void onLayout(int width, int height);
    }
}

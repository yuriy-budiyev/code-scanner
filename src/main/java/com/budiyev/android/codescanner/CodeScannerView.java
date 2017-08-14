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
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class CodeScannerView extends ViewGroup {
    private SurfaceView mPreviewView;
    private ViewFinderView mViewFinderView;
    private Point mFrameSize;
    private LayoutListener mLayoutListener;

    public CodeScannerView(@NonNull Context context) {
        super(context);
        initialize(context, null);
    }

    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attrs) {
        mPreviewView = new SurfaceView(context);
        mPreviewView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mPreviewView);
        mViewFinderView = new ViewFinderView(context);
        mViewFinderView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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

    public void setSquareFrame(boolean squareFrame) {
        mViewFinderView.setSquareFrame(squareFrame);
    }

    public boolean isSquareFrame() {
        return mViewFinderView.isSquareFrame();
    }

    SurfaceView getPreviewView() {
        return mPreviewView;
    }

    ViewFinderView getViewFinderView() {
        return mViewFinderView;
    }

    void setFrameSize(@NonNull Point frameSize) {
        mFrameSize = frameSize;
        requestLayout();
    }

    void setLayoutListener(@Nullable LayoutListener layoutListener) {
        mLayoutListener = layoutListener;
    }

}

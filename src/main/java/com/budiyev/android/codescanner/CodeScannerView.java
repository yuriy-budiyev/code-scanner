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
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * A view to display code scanner preview
 *
 * @see CodeScanner
 */
public final class CodeScannerView extends ViewGroup {
    private static final boolean DEFAULT_SQUARE_FRAME = false;
    private static final boolean DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE = true;
    private static final boolean DEFAULT_FLASH_BUTTON_VISIBLE = true;
    private static final int DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY = VISIBLE;
    private static final int DEFAULT_FLASH_BUTTON_VISIBILITY = VISIBLE;
    private static final int DEFAULT_MASK_COLOR = 0x77000000;
    private static final int DEFAULT_FRAME_COLOR = Color.WHITE;
    private static final int DEFAULT_AUTO_FOCUS_BUTTON_COLOR = Color.WHITE;
    private static final int DEFAULT_FLASH_BUTTON_COLOR = Color.WHITE;
    private static final float DEFAULT_FRAME_WIDTH_DP = 2f;
    private static final float DEFAULT_FRAME_CORNER_SIZE_DP = 50f;
    private static final float BUTTON_SIZE_DP = 56f;
    private SurfaceView mPreviewView;
    private ViewFinderView mViewFinderView;
    private ImageView mAutoFocusButton;
    private ImageView mFlashButton;
    private Point mFrameSize;
    private LayoutListener mLayoutListener;
    private CodeScanner mCodeScanner;
    private int mButtonSize;

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public CodeScannerView(Context context, AttributeSet attrs, @AttrRes int defStyleAttr,
            @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        mPreviewView = new SurfaceView(context);
        mPreviewView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mViewFinderView = new ViewFinderView(context);
        mViewFinderView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mButtonSize = Math.round(displayMetrics.density * BUTTON_SIZE_DP);
        mAutoFocusButton = new ImageView(context);
        mAutoFocusButton.setLayoutParams(new LayoutParams(mButtonSize, mButtonSize));
        mAutoFocusButton.setScaleType(ImageView.ScaleType.CENTER);
        mAutoFocusButton.setImageResource(R.drawable.ic_code_scanner_auto_focus_on);
        mAutoFocusButton.setOnClickListener(new AutoFocusClickListener());
        mFlashButton = new ImageView(context);
        mFlashButton.setLayoutParams(new LayoutParams(mButtonSize, mButtonSize));
        mFlashButton.setScaleType(ImageView.ScaleType.CENTER);
        mFlashButton.setImageResource(R.drawable.ic_code_scanner_flash_on);
        mFlashButton.setOnClickListener(new FlashClickListener());
        if (attrs == null) {
            mViewFinderView.setSquareFrame(DEFAULT_SQUARE_FRAME);
            mViewFinderView.setMaskColor(DEFAULT_MASK_COLOR);
            mViewFinderView.setFrameColor(DEFAULT_FRAME_COLOR);
            mViewFinderView
                    .setFrameWidth(Math.round(DEFAULT_FRAME_WIDTH_DP * displayMetrics.density));
            mViewFinderView.setFrameCornersSize(
                    Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * displayMetrics.density));
            mAutoFocusButton.setColorFilter(DEFAULT_AUTO_FOCUS_BUTTON_COLOR);
            mFlashButton.setColorFilter(DEFAULT_FLASH_BUTTON_COLOR);
            mAutoFocusButton.setVisibility(DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY);
            mFlashButton.setVisibility(DEFAULT_FLASH_BUTTON_VISIBILITY);
        } else {
            TypedArray attributes = null;
            try {
                attributes = context.getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.CodeScannerView, defStyleAttr,
                                defStyleRes);
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
                mAutoFocusButton.setColorFilter(attributes
                        .getColor(R.styleable.CodeScannerView_autoFocusButtonColor,
                                DEFAULT_AUTO_FOCUS_BUTTON_COLOR));
                mFlashButton.setColorFilter(attributes
                        .getColor(R.styleable.CodeScannerView_flashButtonColor,
                                DEFAULT_FLASH_BUTTON_COLOR));
                mAutoFocusButton.setVisibility(attributes
                        .getBoolean(R.styleable.CodeScannerView_autoFocusButtonVisible,
                                DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE) ? VISIBLE : INVISIBLE);
                mFlashButton.setVisibility(attributes
                        .getBoolean(R.styleable.CodeScannerView_flashButtonVisible,
                                DEFAULT_FLASH_BUTTON_VISIBLE) ? VISIBLE : INVISIBLE);
            } finally {
                if (attributes != null) {
                    attributes.recycle();
                }
            }
        }
        addView(mPreviewView);
        addView(mViewFinderView);
        addView(mAutoFocusButton);
        addView(mFlashButton);
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
        int buttonSize = mButtonSize;
        mAutoFocusButton.layout(left, top, left + buttonSize, top + buttonSize);
        mFlashButton.layout(right - buttonSize, top, right, top + buttonSize);
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

    /**
     * Set whether auto focus button is visible or not
     *
     * @param visible Visibility
     */
    public void setAutoFocusButtonVisible(boolean visible) {
        if (visible) {
            mAutoFocusButton.setVisibility(VISIBLE);
        } else {
            mAutoFocusButton.setVisibility(INVISIBLE);
        }
    }

    /**
     * Set whether flash button is visible or not
     *
     * @param visible Visibility
     */
    public void setFlashButtonVisible(boolean visible) {
        if (visible) {
            mFlashButton.setVisibility(VISIBLE);
        } else {
            mFlashButton.setVisibility(INVISIBLE);
        }
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

    void setCodeScanner(@Nullable CodeScanner codeScanner) {
        mCodeScanner = codeScanner;
        if (codeScanner == null) {
            return;
        }
        setAutoFocusEnabled(codeScanner.isAutoFocusEnabled());
        setFlashEnabled(codeScanner.isFlashEnabled());
    }

    void setAutoFocusEnabled(boolean enabled) {
        if (enabled) {
            mAutoFocusButton.setImageResource(R.drawable.ic_code_scanner_auto_focus_on);
        } else {
            mAutoFocusButton.setImageResource(R.drawable.ic_code_scanner_auto_focus_off);
        }
    }

    void setFlashEnabled(boolean enabled) {
        if (enabled) {
            mFlashButton.setImageResource(R.drawable.ic_code_scanner_flash_on);
        } else {
            mFlashButton.setImageResource(R.drawable.ic_code_scanner_flash_off);
        }
    }

    interface LayoutListener {
        void onLayout(int width, int height);
    }

    private final class AutoFocusClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            CodeScanner scanner = mCodeScanner;
            if (scanner == null || !scanner.isAutoFocusSupportedOrUnknown()) {
                return;
            }
            boolean enabled = !scanner.isAutoFocusEnabled();
            scanner.setAutoFocusEnabled(enabled);
            setAutoFocusEnabled(enabled);
        }
    }

    private final class FlashClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            CodeScanner scanner = mCodeScanner;
            if (scanner == null || !scanner.isFlashSupportedOrUnknown()) {
                return;
            }
            boolean enabled = !scanner.isFlashEnabled();
            scanner.setFlashEnabled(enabled);
            setFlashEnabled(enabled);
        }
    }
}

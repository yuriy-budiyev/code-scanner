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
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
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
public class CodeScannerView extends ViewGroup {
    private static final boolean DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE = true;
    private static final boolean DEFAULT_FLASH_BUTTON_VISIBLE = true;
    private static final int DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY = VISIBLE;
    private static final int DEFAULT_FLASH_BUTTON_VISIBILITY = VISIBLE;
    private static final int DEFAULT_MASK_COLOR = 0x77000000;
    private static final int DEFAULT_FRAME_COLOR = Color.WHITE;
    private static final int DEFAULT_AUTO_FOCUS_BUTTON_COLOR = Color.WHITE;
    private static final int DEFAULT_FLASH_BUTTON_COLOR = Color.WHITE;
    private static final float DEFAULT_FRAME_THICKNESS_DP = 2f;
    private static final float DEFAULT_FRAME_ASPECT_RATIO_WIDTH = 1f;
    private static final float DEFAULT_FRAME_ASPECT_RATIO_HEIGHT = 1f;
    private static final float DEFAULT_FRAME_CORNER_SIZE_DP = 50f;
    private static final float DEFAULT_FRAME_SIZE = 0.75f;
    private static final float BUTTON_SIZE_DP = 56f;
    private SurfaceView mPreviewView;
    private ViewFinderView mViewFinderView;
    private ImageView mAutoFocusButton;
    private ImageView mFlashButton;
    private Point mPreviewSize;
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
    public CodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public CodeScannerView(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr,
            @StyleRes int defStyleRes) {
        mPreviewView = new SurfaceView(context);
        mPreviewView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mViewFinderView = new ViewFinderView(context);
        mViewFinderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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
            mViewFinderView.setFrameAspectRatio(DEFAULT_FRAME_ASPECT_RATIO_WIDTH, DEFAULT_FRAME_ASPECT_RATIO_HEIGHT);
            mViewFinderView.setMaskColor(DEFAULT_MASK_COLOR);
            mViewFinderView.setFrameColor(DEFAULT_FRAME_COLOR);
            mViewFinderView.setFrameThickness(Math.round(DEFAULT_FRAME_THICKNESS_DP * displayMetrics.density));
            mViewFinderView.setFrameCornersSize(Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * displayMetrics.density));
            mViewFinderView.setFrameSize(DEFAULT_FRAME_SIZE);
            mAutoFocusButton.setColorFilter(DEFAULT_AUTO_FOCUS_BUTTON_COLOR);
            mFlashButton.setColorFilter(DEFAULT_FLASH_BUTTON_COLOR);
            mAutoFocusButton.setVisibility(DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY);
            mFlashButton.setVisibility(DEFAULT_FLASH_BUTTON_VISIBILITY);
        } else {
            TypedArray a = null;
            try {
                a = context.getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.CodeScannerView, defStyleAttr, defStyleRes);
                setMaskColor(a.getColor(R.styleable.CodeScannerView_maskColor, DEFAULT_MASK_COLOR));
                setFrameColor(a.getColor(R.styleable.CodeScannerView_frameColor, DEFAULT_FRAME_COLOR));
                setFrameThickness(a.getDimensionPixelOffset(R.styleable.CodeScannerView_frameThickness,
                        Math.round(DEFAULT_FRAME_THICKNESS_DP * displayMetrics.density)));
                setFrameCornersSize(a.getDimensionPixelOffset(R.styleable.CodeScannerView_frameCornersSize,
                        Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * displayMetrics.density)));
                setFrameAspectRatio(
                        a.getFloat(R.styleable.CodeScannerView_frameAspectRatioWidth, DEFAULT_FRAME_ASPECT_RATIO_WIDTH),
                        a.getFloat(R.styleable.CodeScannerView_frameAspectRatioHeight,
                                DEFAULT_FRAME_ASPECT_RATIO_HEIGHT));
                setFrameSize(a.getFloat(R.styleable.CodeScannerView_frameSize, DEFAULT_FRAME_SIZE));
                setAutoFocusButtonVisible(a.getBoolean(R.styleable.CodeScannerView_autoFocusButtonVisible,
                        DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE));
                setFlashButtonVisible(
                        a.getBoolean(R.styleable.CodeScannerView_flashButtonVisible, DEFAULT_FLASH_BUTTON_VISIBLE));
                setAutoFocusButtonColor(
                        a.getColor(R.styleable.CodeScannerView_autoFocusButtonColor, DEFAULT_AUTO_FOCUS_BUTTON_COLOR));
                setFlashButtonColor(
                        a.getColor(R.styleable.CodeScannerView_flashButtonColor, DEFAULT_FLASH_BUTTON_COLOR));
            } finally {
                if (a != null) {
                    a.recycle();
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
        Point previewSize = mPreviewSize;
        if (previewSize == null) {
            mPreviewView.layout(0, 0, width, height);
        } else {
            int frameLeft = 0;
            int frameTop = 0;
            int frameRight = width;
            int frameBottom = height;
            int previewWidth = previewSize.getX();
            if (previewWidth > width) {
                int d = (previewWidth - width) / 2;
                frameLeft -= d;
                frameRight += d;
            }
            int previewHeight = previewSize.getY();
            if (previewHeight > height) {
                int d = (previewHeight - height) / 2;
                frameTop -= d;
                frameBottom += d;
            }
            mPreviewView.layout(frameLeft, frameTop, frameRight, frameBottom);
        }
        mViewFinderView.layout(0, 0, width, height);
        int buttonSize = mButtonSize;
        mAutoFocusButton.layout(0, 0, buttonSize, buttonSize);
        mFlashButton.layout(width - buttonSize, 0, width, buttonSize);
        LayoutListener listener = mLayoutListener;
        if (listener != null) {
            listener.onLayout(width, height);
        }
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
     * Set frame thickness
     *
     * @param thickness Frame thickness in pixels
     */
    public void setFrameThickness(@Px int thickness) {
        if (thickness < 0) {
            throw new IllegalArgumentException("Frame thickness can't be negative");
        }
        mViewFinderView.setFrameThickness(thickness);
    }

    /**
     * Set length on the frame corners
     *
     * @param size Length in pixels
     */
    public void setFrameCornersSize(@Px int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Frame corners size can't be negative");
        }
        mViewFinderView.setFrameCornersSize(size);
    }

    /**
     * Set relative frame size where 1.0 means full size
     *
     * @param size Relative frame size between 0.1 and 1.0
     */
    public void setFrameSize(@FloatRange(from = 0.1, to = 1) float size) {
        if (size < 0.1 || size > 1) {
            throw new IllegalArgumentException("Max frame size value should be between 0.1 and 1, inclusive");
        }
        mViewFinderView.setFrameSize(size);
    }

    /**
     * Set frame aspect ratio (ex. 4:3, 15:10, 16:9, 1:1)
     *
     * @param ratioWidth  Frame aspect ratio width
     * @param ratioHeight Frame aspect ratio height
     */
    public void setFrameAspectRatio(@FloatRange(from = 0, fromInclusive = false) float ratioWidth,
            @FloatRange(from = 0, fromInclusive = false) float ratioHeight) {
        if (ratioWidth <= 0 || ratioHeight <= 0) {
            throw new IllegalArgumentException("Frame aspect ratio values should be greater than zero");
        }
        mViewFinderView.setFrameAspectRatio(ratioWidth, ratioHeight);
    }

    /**
     * Set whether auto focus button is visible or not
     *
     * @param visible Visibility
     */
    public void setAutoFocusButtonVisible(boolean visible) {
        mAutoFocusButton.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    /**
     * Set whether flash button is visible or not
     *
     * @param visible Visibility
     */
    public void setFlashButtonVisible(boolean visible) {
        mFlashButton.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    /**
     * Set auto focus button color
     *
     * @param color Color
     */
    public void setAutoFocusButtonColor(@ColorInt int color) {
        mAutoFocusButton.setColorFilter(color);
    }

    /**
     * Set flash button color
     *
     * @param color Color
     */
    public void setFlashButtonColor(@ColorInt int color) {
        mFlashButton.setColorFilter(color);
    }

    @NonNull
    SurfaceView getPreviewView() {
        return mPreviewView;
    }

    @NonNull
    ViewFinderView getViewFinderView() {
        return mViewFinderView;
    }

    @Nullable
    Rect getFrameRect() {
        return mViewFinderView.getFrameRect();
    }

    void setPreviewSize(@Nullable Point previewSize) {
        mPreviewSize = previewSize;
        requestLayout();
    }

    void setLayoutListener(@Nullable LayoutListener layoutListener) {
        mLayoutListener = layoutListener;
    }

    void setCodeScanner(@NonNull CodeScanner codeScanner) {
        if (mCodeScanner != null) {
            throw new IllegalStateException("Code scanner has already been set");
        }
        mCodeScanner = codeScanner;
        setAutoFocusEnabled(codeScanner.isAutoFocusEnabled());
        setFlashEnabled(codeScanner.isFlashEnabled());
    }

    void setAutoFocusEnabled(boolean enabled) {
        mAutoFocusButton.setImageResource(
                enabled ? R.drawable.ic_code_scanner_auto_focus_on : R.drawable.ic_code_scanner_auto_focus_off);
    }

    void setFlashEnabled(boolean enabled) {
        mFlashButton
                .setImageResource(enabled ? R.drawable.ic_code_scanner_flash_on : R.drawable.ic_code_scanner_flash_off);
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

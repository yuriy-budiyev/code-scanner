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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;

/**
 * A view to display code scanner preview
 *
 * @see CodeScanner
 */
public final class CodeScannerView extends ViewGroup {
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
    private static final float DEFAULT_FRAME_CORNERS_RADIUS_DP = 0f;
    private static final float DEFAULT_FRAME_SIZE = 0.75f;
    private static final float BUTTON_SIZE_DP = 56f;
    private static final float FOCUS_AREA_SIZE_DP = 20f;
    private SurfaceView mPreviewView;
    private ViewFinderView mViewFinderView;
    private ImageView mAutoFocusButton;
    private ImageView mFlashButton;
    private Point mPreviewSize;
    private SizeListener mSizeListener;
    private CodeScanner mCodeScanner;
    private int mButtonSize;
    private int mAutoFocusButtonColor;
    private int mFlashButtonColor;
    private int mFocusAreaSize;

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull final Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    public CodeScannerView(@NonNull final Context context, @Nullable final AttributeSet attrs,
            @AttrRes final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    /**
     * A view to display code scanner preview
     *
     * @see CodeScanner
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public CodeScannerView(final Context context, final AttributeSet attrs,
            @AttrRes final int defStyleAttr, @StyleRes final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(@NonNull final Context context, @Nullable final AttributeSet attrs,
            @AttrRes final int defStyleAttr, @StyleRes final int defStyleRes) {
        mPreviewView = new SurfaceView(context);
        mPreviewView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mViewFinderView = new ViewFinderView(context);
        mViewFinderView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        final float density = context.getResources().getDisplayMetrics().density;
        mButtonSize = Math.round(density * BUTTON_SIZE_DP);
        mFocusAreaSize = Math.round(density * FOCUS_AREA_SIZE_DP);
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
            mViewFinderView.setFrameAspectRatio(DEFAULT_FRAME_ASPECT_RATIO_WIDTH,
                    DEFAULT_FRAME_ASPECT_RATIO_HEIGHT);
            mViewFinderView.setMaskColor(DEFAULT_MASK_COLOR);
            mViewFinderView.setFrameColor(DEFAULT_FRAME_COLOR);
            mViewFinderView.setFrameThickness(Math.round(DEFAULT_FRAME_THICKNESS_DP * density));
            mViewFinderView.setFrameCornersSize(Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * density));
            mViewFinderView
                    .setFrameCornersRadius(Math.round(DEFAULT_FRAME_CORNERS_RADIUS_DP * density));
            mViewFinderView.setFrameSize(DEFAULT_FRAME_SIZE);
            mAutoFocusButton.setColorFilter(DEFAULT_AUTO_FOCUS_BUTTON_COLOR);
            mFlashButton.setColorFilter(DEFAULT_FLASH_BUTTON_COLOR);
            mAutoFocusButton.setVisibility(DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY);
            mFlashButton.setVisibility(DEFAULT_FLASH_BUTTON_VISIBILITY);
        } else {
            TypedArray a = null;
            try {
                a = context.getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.CodeScannerView, defStyleAttr,
                                defStyleRes);
                setMaskColor(a.getColor(R.styleable.CodeScannerView_maskColor, DEFAULT_MASK_COLOR));
                setFrameColor(
                        a.getColor(R.styleable.CodeScannerView_frameColor, DEFAULT_FRAME_COLOR));
                setFrameThickness(
                        a.getDimensionPixelOffset(R.styleable.CodeScannerView_frameThickness,
                                Math.round(DEFAULT_FRAME_THICKNESS_DP * density)));
                setFrameCornersSize(
                        a.getDimensionPixelOffset(R.styleable.CodeScannerView_frameCornersSize,
                                Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * density)));
                setFrameCornersRadius(
                        a.getDimensionPixelOffset(R.styleable.CodeScannerView_frameCornersRadius,
                                Math.round(DEFAULT_FRAME_CORNERS_RADIUS_DP * density)));
                setFrameAspectRatio(a.getFloat(R.styleable.CodeScannerView_frameAspectRatioWidth,
                        DEFAULT_FRAME_ASPECT_RATIO_WIDTH),
                        a.getFloat(R.styleable.CodeScannerView_frameAspectRatioHeight,
                                DEFAULT_FRAME_ASPECT_RATIO_HEIGHT));
                setFrameSize(a.getFloat(R.styleable.CodeScannerView_frameSize, DEFAULT_FRAME_SIZE));
                setAutoFocusButtonVisible(
                        a.getBoolean(R.styleable.CodeScannerView_autoFocusButtonVisible,
                                DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE));
                setFlashButtonVisible(a.getBoolean(R.styleable.CodeScannerView_flashButtonVisible,
                        DEFAULT_FLASH_BUTTON_VISIBLE));
                setAutoFocusButtonColor(a.getColor(R.styleable.CodeScannerView_autoFocusButtonColor,
                        DEFAULT_AUTO_FOCUS_BUTTON_COLOR));
                setFlashButtonColor(a.getColor(R.styleable.CodeScannerView_flashButtonColor,
                        DEFAULT_FLASH_BUTTON_COLOR));
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
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        performLayout(right - left, bottom - top);
    }

    @Override
    protected void onSizeChanged(final int width, final int height, final int oldWidth,
            final int oldHeight) {
        performLayout(width, height);
        final SizeListener listener = mSizeListener;
        if (listener != null) {
            listener.onSizeChanged(width, height);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        final CodeScanner codeScanner = mCodeScanner;
        final Rect frameRect = getFrameRect();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (codeScanner != null && frameRect != null &&
                codeScanner.isAutoFocusSupportedOrUnknown() && codeScanner.isTouchFocusEnabled() &&
                event.getAction() == MotionEvent.ACTION_DOWN && frameRect.isPointInside(x, y)) {
            final int areaSize = mFocusAreaSize;
            codeScanner.performTouchFocus(
                    new Rect(x - areaSize, y - areaSize, x + areaSize, y + areaSize)
                            .fitIn(frameRect));
        }
        return super.onTouchEvent(event);
    }

    /**
     * Get current mask color
     *
     * @see #setMaskColor
     */
    @ColorInt
    public int getMaskColor() {
        return mViewFinderView.getMaskColor();
    }

    /**
     * Set color of the space outside of the framing rect
     *
     * @param color Mask color
     */
    public void setMaskColor(@ColorInt final int color) {
        mViewFinderView.setMaskColor(color);
    }

    /**
     * Get current frame color
     *
     * @see #setFrameColor
     */
    @ColorInt
    public int getFrameColor() {
        return mViewFinderView.getFrameColor();
    }

    /**
     * Set color of the frame
     *
     * @param color Frame color
     */
    public void setFrameColor(@ColorInt final int color) {
        mViewFinderView.setFrameColor(color);
    }

    /**
     * Get current frame thickness
     *
     * @see #setFrameThickness
     */
    @Px
    public int getFrameThickness() {
        return mViewFinderView.getFrameThickness();
    }

    /**
     * Set frame thickness
     *
     * @param thickness Frame thickness in pixels
     */
    public void setFrameThickness(@Px final int thickness) {
        if (thickness < 0) {
            throw new IllegalArgumentException("Frame thickness can't be negative");
        }
        mViewFinderView.setFrameThickness(thickness);
    }

    /**
     * Get current frame corners size
     *
     * @see #setFrameCornersSize
     */
    @Px
    public int getFrameCornersSize() {
        return mViewFinderView.getFrameCornersSize();
    }

    /**
     * Set size of the frame corners
     *
     * @param size Size in pixels
     */
    public void setFrameCornersSize(@Px final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Frame corners size can't be negative");
        }
        mViewFinderView.setFrameCornersSize(size);
    }

    /**
     * Get current frame corners radius
     *
     * @see #setFrameCornersRadius
     */
    @Px
    public int getFrameCornersRadius() {
        return mViewFinderView.getFrameCornersRadius();
    }

    /**
     * Set current frame corners radius
     *
     * @param radius Frame corners radius in pixels
     */
    public void setFrameCornersRadius(@Px final int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Frame corners radius can't be negative");
        }
        mViewFinderView.setFrameCornersRadius(radius);
    }

    /**
     * Get current frame size
     *
     * @see #setFrameSize
     */
    @FloatRange(from = 0.1, to = 1.0)
    public float getFrameSize() {
        return mViewFinderView.getFrameSize();
    }

    /**
     * Set relative frame size where 1.0 means full size
     *
     * @param size Relative frame size between 0.1 and 1.0
     */
    public void setFrameSize(@FloatRange(from = 0.1, to = 1) final float size) {
        if (size < 0.1 || size > 1) {
            throw new IllegalArgumentException(
                    "Max frame size value should be between 0.1 and 1, inclusive");
        }
        mViewFinderView.setFrameSize(size);
    }

    /**
     * Get current frame aspect ratio width
     *
     * @see #setFrameAspectRatioWidth
     * @see #setFrameAspectRatio
     */
    @FloatRange(from = 0, fromInclusive = false)
    public float getFrameAspectRatioWidth() {
        return mViewFinderView.getFrameAspectRatioWidth();
    }

    /**
     * Set frame aspect ratio width
     *
     * @param ratioWidth Frame aspect ratio width
     * @see #setFrameAspectRatio
     */
    public void setFrameAspectRatioWidth(
            @FloatRange(from = 0, fromInclusive = false) final float ratioWidth) {
        if (ratioWidth <= 0) {
            throw new IllegalArgumentException(
                    "Frame aspect ratio values should be greater than zero");
        }
        mViewFinderView.setFrameAspectRatioWidth(ratioWidth);
    }

    /**
     * Get current frame aspect ratio height
     *
     * @see #setFrameAspectRatioHeight
     * @see #setFrameAspectRatio
     */
    @FloatRange(from = 0, fromInclusive = false)
    public float getFrameAspectRatioHeight() {
        return mViewFinderView.getFrameAspectRatioHeight();
    }

    /**
     * Set frame aspect ratio height
     *
     * @param ratioHeight Frame aspect ratio width
     * @see #setFrameAspectRatio
     */
    public void setFrameAspectRatioHeight(
            @FloatRange(from = 0, fromInclusive = false) final float ratioHeight) {
        if (ratioHeight <= 0) {
            throw new IllegalArgumentException(
                    "Frame aspect ratio values should be greater than zero");
        }
        mViewFinderView.setFrameAspectRatioHeight(ratioHeight);
    }

    /**
     * Set frame aspect ratio (ex. 1:1, 15:10, 16:9, 4:3)
     *
     * @param ratioWidth  Frame aspect ratio width
     * @param ratioHeight Frame aspect ratio height
     */
    public void setFrameAspectRatio(
            @FloatRange(from = 0, fromInclusive = false) final float ratioWidth,
            @FloatRange(from = 0, fromInclusive = false) final float ratioHeight) {
        if (ratioWidth <= 0 || ratioHeight <= 0) {
            throw new IllegalArgumentException(
                    "Frame aspect ratio values should be greater than zero");
        }
        mViewFinderView.setFrameAspectRatio(ratioWidth, ratioHeight);
    }

    /**
     * Whether if auto focus button is currently visible
     *
     * @see #setAutoFocusButtonVisible
     */
    public boolean isAutoFocusButtonVisible() {
        return mAutoFocusButton.getVisibility() == VISIBLE;
    }

    /**
     * Set whether auto focus button is visible or not
     *
     * @param visible Visibility
     */
    public void setAutoFocusButtonVisible(final boolean visible) {
        mAutoFocusButton.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    /**
     * Whether if flash button is currently visible
     *
     * @see #setFlashButtonVisible
     */
    public boolean isFlashButtonVisible() {
        return mFlashButton.getVisibility() == VISIBLE;
    }

    /**
     * Set whether flash button is visible or not
     *
     * @param visible Visibility
     */
    public void setFlashButtonVisible(final boolean visible) {
        mFlashButton.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    /**
     * Get current auto focus button color
     *
     * @see #setAutoFocusButtonColor
     */
    @ColorInt
    public int getAutoFocusButtonColor() {
        return mAutoFocusButtonColor;
    }

    /**
     * Set auto focus button color
     *
     * @param color Color
     */
    public void setAutoFocusButtonColor(@ColorInt final int color) {
        mAutoFocusButtonColor = color;
        mAutoFocusButton.setColorFilter(color);
    }

    /**
     * Get current flash button color
     *
     * @see #setFlashButtonColor
     */
    @ColorInt
    public int getFlashButtonColor() {
        return mFlashButtonColor;
    }

    /**
     * Set flash button color
     *
     * @param color Color
     */
    public void setFlashButtonColor(@ColorInt final int color) {
        mFlashButtonColor = color;
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

    void setPreviewSize(@Nullable final Point previewSize) {
        mPreviewSize = previewSize;
        requestLayout();
    }

    void setSizeListener(@Nullable final SizeListener sizeListener) {
        mSizeListener = sizeListener;
    }

    void setCodeScanner(@NonNull final CodeScanner codeScanner) {
        if (mCodeScanner != null) {
            throw new IllegalStateException("Code scanner has already been set");
        }
        mCodeScanner = codeScanner;
        setAutoFocusEnabled(codeScanner.isAutoFocusEnabled());
        setFlashEnabled(codeScanner.isFlashEnabled());
    }

    void setAutoFocusEnabled(final boolean enabled) {
        mAutoFocusButton.setImageResource(enabled ? R.drawable.ic_code_scanner_auto_focus_on :
                R.drawable.ic_code_scanner_auto_focus_off);
    }

    void setFlashEnabled(final boolean enabled) {
        mFlashButton.setImageResource(enabled ? R.drawable.ic_code_scanner_flash_on :
                R.drawable.ic_code_scanner_flash_off);
    }

    private void performLayout(final int width, final int height) {
        final Point previewSize = mPreviewSize;
        if (previewSize == null) {
            mPreviewView.layout(0, 0, width, height);
        } else {
            int frameLeft = 0;
            int frameTop = 0;
            int frameRight = width;
            int frameBottom = height;
            final int previewWidth = previewSize.getX();
            if (previewWidth > width) {
                final int d = (previewWidth - width) / 2;
                frameLeft -= d;
                frameRight += d;
            }
            final int previewHeight = previewSize.getY();
            if (previewHeight > height) {
                final int d = (previewHeight - height) / 2;
                frameTop -= d;
                frameBottom += d;
            }
            mPreviewView.layout(frameLeft, frameTop, frameRight, frameBottom);
        }
        mViewFinderView.layout(0, 0, width, height);
        final int buttonSize = mButtonSize;
        mAutoFocusButton.layout(0, 0, buttonSize, buttonSize);
        mFlashButton.layout(width - buttonSize, 0, width, buttonSize);
    }

    interface SizeListener {
        void onSizeChanged(int width, int height);
    }

    private final class AutoFocusClickListener implements OnClickListener {
        @Override
        public void onClick(final View view) {
            final CodeScanner scanner = mCodeScanner;
            if (scanner == null || !scanner.isAutoFocusSupportedOrUnknown()) {
                return;
            }
            final boolean enabled = !scanner.isAutoFocusEnabled();
            scanner.setAutoFocusEnabled(enabled);
            setAutoFocusEnabled(enabled);
        }
    }

    private final class FlashClickListener implements OnClickListener {
        @Override
        public void onClick(final View view) {
            final CodeScanner scanner = mCodeScanner;
            if (scanner == null || !scanner.isFlashSupportedOrUnknown()) {
                return;
            }
            final boolean enabled = !scanner.isFlashEnabled();
            scanner.setFlashEnabled(enabled);
            setFlashEnabled(enabled);
        }
    }
}

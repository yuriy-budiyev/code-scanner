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

import android.graphics.Matrix;

import androidx.annotation.NonNull;

final class Rect {

    private final int mLeft;
    private final int mTop;
    private final int mRight;
    private final int mBottom;

    public Rect(final int left, final int top, final int right, final int bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    public int getLeft() {
        return mLeft;
    }

    public int getTop() {
        return mTop;
    }

    public int getRight() {
        return mRight;
    }

    public int getBottom() {
        return mBottom;
    }

    public int getWidth() {
        return mRight - mLeft;
    }

    public int getHeight() {
        return mBottom - mTop;
    }

    public boolean isPointInside(final int x, final int y) {
        return mLeft < x && mTop < y && mRight > x && mBottom > y;
    }

    @NonNull
    public Rect sort() {
        int left = mLeft;
        int top = mTop;
        int right = mRight;
        int bottom = mBottom;
        if (left <= right && top <= bottom) {
            return this;
        }
        if (left > right) {
            final int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            final int temp = top;
            top = bottom;
            bottom = temp;
        }
        return new Rect(left, top, right, bottom);
    }

    @NonNull
    public Rect bound(final int left, final int top, final int right, final int bottom) {
        final int l = mLeft;
        final int t = mTop;
        final int r = mRight;
        final int b = mBottom;
        if (l >= left && t >= top && r <= right && b <= bottom) {
            return this;
        }
        return new Rect(Math.max(l, left), Math.max(t, top), Math.min(r, right),
                Math.min(b, bottom));
    }

    @NonNull
    public Rect rotate(final float angle, final float x, final float y) {
        final Matrix matrix = new Matrix();
        final float[] rect = new float[] {mLeft, mTop, mRight, mBottom};
        matrix.postRotate(angle, x, y);
        matrix.mapPoints(rect);
        int left = (int) rect[0];
        int top = (int) rect[1];
        int right = (int) rect[2];
        int bottom = (int) rect[3];
        if (left > right) {
            final int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            final int temp = top;
            top = bottom;
            bottom = temp;
        }
        return new Rect(left, top, right, bottom);
    }

    @NonNull
    public Rect fitIn(@NonNull final Rect area) {
        int left = mLeft;
        int top = mTop;
        int right = mRight;
        int bottom = mBottom;
        final int width = getWidth();
        final int height = getHeight();
        final int areaLeft = area.mLeft;
        final int areaTop = area.mTop;
        final int areaRight = area.mRight;
        final int areaBottom = area.mBottom;
        final int areaWidth = area.getWidth();
        final int areaHeight = area.getHeight();
        if (left >= areaLeft && top >= areaTop && right <= areaRight && bottom <= areaBottom) {
            return this;
        }
        final int fitWidth = Math.min(width, areaWidth);
        final int fitHeight = Math.min(height, areaHeight);
        if (left < areaLeft) {
            left = areaLeft;
            right = left + fitWidth;
        } else if (right > areaRight) {
            right = areaRight;
            left = right - fitWidth;
        }
        if (top < areaTop) {
            top = areaTop;
            bottom = top + fitHeight;
        } else if (bottom > areaBottom) {
            bottom = areaBottom;
            top = bottom - fitHeight;
        }
        return new Rect(left, top, right, bottom);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * mLeft + mTop) + mRight) + mBottom;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rect) {
            final Rect other = (Rect) obj;
            return mLeft == other.mLeft && mTop == other.mTop && mRight == other.mRight &&
                    mBottom == other.mBottom;
        } else {
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "[(" + mLeft + "; " + mTop + ") - (" + mRight + "; " + mBottom + ")]";
    }
}

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
            return mLeft == other.mLeft && mTop == other.mTop && mRight == other.mRight && mBottom == other.mBottom;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[(" + mLeft + "; " + mTop + ") - (" + mRight + "; " + mBottom + ")]";
    }
}

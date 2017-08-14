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

import android.support.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class Decoder {
    private final DecodeThread mDecodeThread;
    private final MultiFormatReader mReader;

    public Decoder(@NonNull CodeScanner scanner) {
        mReader = new MultiFormatReader();
        mDecodeThread = new DecodeThread(scanner, mReader);
        mDecodeThread.start();
    }

    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mReader.setHints(hints);
    }

    public void decode(@NonNull byte[] data, int dataWidth, int dataHeight, int frameWidth,
            int frameHeight, int orientation, boolean squareFrame) {
        mDecodeThread.decode(new DecodeAction(data, dataWidth, dataHeight, frameWidth, frameHeight,
                orientation, squareFrame));
    }

    public void shutdown() {
        mDecodeThread.quit();
    }
}

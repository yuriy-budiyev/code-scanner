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

import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class DecodeThread extends Thread {
    private final BlockingQueue<DecodeAction> mQueue = new LinkedBlockingQueue<>();
    private final CodeScanner mScanner;
    private final MultiFormatReader mReader;

    public DecodeThread(@NonNull CodeScanner scanner, @NonNull MultiFormatReader reader) {
        super("Code scanner decode thread");
        mScanner = scanner;
        mReader = reader;
    }

    public void decode(@NonNull DecodeAction action) {
        mQueue.add(action);
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                Result result = null;
                try {
                    result = mQueue.take().decode(mReader);
                } catch (ReaderException ignored) {
                } finally {
                    mScanner.notifyDecodeFinished(result);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void quit() {
        mQueue.clear();
        interrupt();
    }
}

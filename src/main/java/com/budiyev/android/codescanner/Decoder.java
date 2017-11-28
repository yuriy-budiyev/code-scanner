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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.support.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

final class Decoder {
    private final BlockingQueue<DecodeTask> mDecodeQueue = new LinkedBlockingQueue<>();
    private final MultiFormatReader mReader;
    private final DecoderThread mDecoderThread;
    private final StateListener mStateListener;
    private final Map<DecodeHintType, Object> mHints;

    public Decoder(@NonNull StateListener stateListener, @NonNull List<BarcodeFormat> formats) {
        mStateListener = stateListener;
        mReader = new MultiFormatReader();
        mDecoderThread = new DecoderThread();
        mHints = new EnumMap<>(DecodeHintType.class);
        mHints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mReader.setHints(mHints);
    }

    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        mHints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mReader.setHints(mHints);
    }

    public void decode(@NonNull DecodeTask task) {
        mDecodeQueue.add(task);
    }

    public void start() {
        mDecoderThread.start();
    }

    public void shutdown() {
        mDecoderThread.interrupt();
        mDecodeQueue.clear();
    }

    private final class DecoderThread extends Thread {
        public DecoderThread() {
            super("Code scanner decode thread");
            if (getPriority() != Thread.MIN_PRIORITY) {
                setPriority(Thread.MIN_PRIORITY);
            }
            if (isDaemon()) {
                setDaemon(false);
            }
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    mStateListener.onStateChanged(Decoder.State.IDLE);
                    Result result = null;
                    DecodeCallback callback = null;
                    try {
                        DecodeTask task = mDecodeQueue.take();
                        mStateListener.onStateChanged(Decoder.State.DECODING);
                        result = task.decode(mReader);
                        callback = task.getCallback();
                    } catch (ReaderException ignored) {
                    } finally {
                        if (result != null) {
                            mDecodeQueue.clear();
                            mStateListener.onStateChanged(Decoder.State.DECODED);
                            if (callback != null) {
                                callback.onDecoded(result);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public interface StateListener {
        void onStateChanged(@NonNull State state);
    }

    public enum State {
        IDLE,
        DECODING,
        DECODED
    }
}

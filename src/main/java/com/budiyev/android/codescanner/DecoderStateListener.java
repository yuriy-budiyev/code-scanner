package com.budiyev.android.codescanner;

import android.support.annotation.WorkerThread;

interface DecoderStateListener {
    @WorkerThread
    void onStateChanged(@DecoderState int state);
}

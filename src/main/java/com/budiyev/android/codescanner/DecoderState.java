package com.budiyev.android.codescanner;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({DecoderState.IDLE, DecoderState.DECODING, DecoderState.DECODED})
@interface DecoderState {
    int IDLE = 0;
    int DECODING = 1;
    int DECODED = 2;
}

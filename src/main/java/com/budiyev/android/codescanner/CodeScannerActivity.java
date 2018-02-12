package com.budiyev.android.codescanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

/**
 * Common code scanner activity
 */
public class CodeScannerActivity extends Activity {
    /**
     * Text representation of decode result ({@link String)}
     *
     * @see #RESULT_DECODED
     */
    public static final String EXTRA_RESULT_TEXT = "result_text";

    /**
     * Raw byte array representation of decode result (byte array)
     *
     * @see #RESULT_DECODED
     */
    public static final String EXTRA_RESULT_BYTES = "result_bytes";

    /**
     * Format of decoded code ({@link BarcodeFormat})
     *
     * @see #RESULT_DECODED
     */
    public static final String EXTRA_RESULT_FORMAT = "result_format";

    /**
     * Code scanner error ({@link Exception})
     *
     * @see #RESULT_SCANNER_ERROR
     */
    public static final String EXTRA_SCANNER_ERROR = "scanner_error";

    /**
     * Activity result, code decoded successfully
     */
    public static final int RESULT_DECODED = 2;

    /**
     * Activity result, camera permission denied
     *
     * @see Manifest.permission#CAMERA
     */
    public static final int RESULT_PERMISSION_DENIED = 3;

    /**
     * Activity result, code scanner error
     */
    public static final int RESULT_SCANNER_ERROR = 4;

    private static final int RC_PERMISSION = 10;
    private CodeScanner mCodeScanner;
    private boolean mPermissionGranted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CodeScannerView scannerView = new CodeScannerView(this);
        scannerView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_DECODED, new Intent().putExtra(EXTRA_RESULT_TEXT, result.getText())
                                .putExtra(EXTRA_RESULT_BYTES, result.getRawBytes())
                                .putExtra(EXTRA_RESULT_FORMAT, result.getBarcodeFormat()));
                        finish();
                    }
                });
            }
        });
        mCodeScanner.setErrorCallback(new ErrorCallback() {
            @Override
            public void onError(@NonNull final Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_SCANNER_ERROR, new Intent().putExtra(EXTRA_SCANNER_ERROR, error));
                        finish();
                    }
                });
            }
        });
        setContentView(scannerView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = false;
                requestPermissions(new String[] {Manifest.permission.CAMERA}, RC_PERMISSION);
            } else {
                mPermissionGranted = true;
            }
        } else {
            mPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == RC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = true;
                mCodeScanner.startPreview();
            } else {
                mPermissionGranted = false;
                setResult(RESULT_PERMISSION_DENIED);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPermissionGranted) {
            mCodeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        mCodeScanner.stopPreview();
        super.onPause();
    }

    /**
     * Intent builder, to configure scanner activity
     */
    @NonNull
    public static IntentBuilder intent() {
        return new IntentBuilder();
    }

    /**
     * Default intent
     *
     * @param context Package context
     */
    @NonNull
    public static Intent intent(@NonNull Context context) {
        return new Intent(context, CodeScannerActivity.class);
    }

    public static final class IntentBuilder {
        private int mCameraId = CodeScanner.DEFAULT_CAMERA;
        private List<BarcodeFormat> mFormats = CodeScanner.DEFAULT_FORMATS;
        private boolean mAutoFocusEnabled = CodeScanner.DEFAULT_AUTO_FOCUS_ENABLED;
        private ScanMode mScanMode = CodeScanner.DEFAULT_SCAN_MODE;
        private AutoFocusMode mAutoFocusMode = CodeScanner.DEFAULT_AUTO_FOCUS_MODE;
        private long mAutoFocusInterval = CodeScanner.DEFAULT_SAFE_AUTO_FOCUS_INTERVAL;
        private boolean mFlashEnabled = CodeScanner.DEFAULT_FLASH_ENABLED;

        private IntentBuilder() {
        }

        /**
         * Camera that will be used by scanner.
         * First back-facing camera on the device by default.
         *
         * @param cameraId Camera id (between {@code 0} and
         *                 {@link Camera#getNumberOfCameras()} - {@code 1})
         */
        @NonNull
        @MainThread
        public IntentBuilder camera(int cameraId) {
            mCameraId = cameraId;
            return this;
        }

        /**
         * Formats, decoder to react to ({@link CodeScanner#ALL_FORMATS} by default)
         *
         * @param formats Formats
         * @see BarcodeFormat
         * @see CodeScanner#ALL_FORMATS
         * @see CodeScanner#ONE_DIMENSIONAL_FORMATS
         * @see CodeScanner#TWO_DIMENSIONAL_FORMATS
         */
        @NonNull
        @MainThread
        public IntentBuilder formats(@NonNull List<BarcodeFormat> formats) {
            mFormats = formats;
            return this;
        }

        /**
         * Formats, decoder to react to ({@link CodeScanner#ALL_FORMATS} by default)
         *
         * @param formats Formats
         * @see BarcodeFormat
         * @see CodeScanner#ALL_FORMATS
         * @see CodeScanner#ONE_DIMENSIONAL_FORMATS
         * @see CodeScanner#TWO_DIMENSIONAL_FORMATS
         */
        @NonNull
        @MainThread
        public IntentBuilder formats(@NonNull BarcodeFormat... formats) {
            mFormats = Arrays.asList(formats);
            return this;
        }

        /**
         * Format, decoder to react to
         *
         * @param format Format
         * @see BarcodeFormat
         */
        @NonNull
        @MainThread
        public IntentBuilder format(@NonNull BarcodeFormat format) {
            mFormats = Collections.singletonList(format);
            return this;
        }

        /**
         * Whether to enable or disable auto focus if it's supported, {@code true} by default
         */
        @NonNull
        @MainThread
        public IntentBuilder autoFocus(boolean enabled) {
            mAutoFocusEnabled = enabled;
            return this;
        }

        /**
         * Set auto focus mode, {@link AutoFocusMode#SAFE} by default
         *
         * @see AutoFocusMode
         */
        @NonNull
        @MainThread
        public IntentBuilder autoFocusMode(@NonNull AutoFocusMode mode) {
            mAutoFocusMode = mode;
            return this;
        }

        /**
         * Set auto focus interval in milliseconds for {@link AutoFocusMode#SAFE} mode,
         * 2000 by default
         *
         * @see #autoFocusMode(AutoFocusMode)
         */
        @NonNull
        @MainThread
        public IntentBuilder autoFocusInterval(long interval) {
            mAutoFocusInterval = interval;
            return this;
        }

        /**
         * Whether to enable or disable flash light if it's supported, {@code false} by default
         */
        @NonNull
        @MainThread
        public IntentBuilder flash(boolean enabled) {
            mFlashEnabled = enabled;
            return this;
        }

        /**
         * Create new {@link CodeScanner} instance with specified parameters
         *
         * @param context Context
         * @see CodeScannerView
         */
        @NonNull
        @MainThread
        public Intent build(@NonNull Context context) {
            return new Intent(context, CodeScannerActivity.class);
        }
    }
}

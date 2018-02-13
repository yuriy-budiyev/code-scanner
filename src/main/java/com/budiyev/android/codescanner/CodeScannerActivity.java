package com.budiyev.android.codescanner;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
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
}

package com.budiyev.android.codescanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.ViewGroup;

public class CodeScannerActivity extends Activity {
    private static final int RC_PERMISSION = 1;
    private CodeScanner mCodeScanner;
    private boolean mPermissionGranted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CodeScannerView scannerView = new CodeScannerView(this);
        scannerView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCodeScanner = new CodeScanner(this, scannerView);
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
    @RequiresApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

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

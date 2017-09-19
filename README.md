# Code Scanner
[ ![Download](https://api.bintray.com/packages/yuriy-budiyev/maven/code-scanner/images/download.svg) ](https://bintray.com/yuriy-budiyev/maven/code-scanner/_latestVersion) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Code%20Scanner-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/6095)

Code scanner library based on [ZXing](https://github.com/zxing/zxing)

### Usage
Add dependency:
```
dependencies {
    implementation 'com.budiyev.android:code-scanner:1.0.8'
}
```
Add camera permission to AndroidManifest.xml (Don't forget about dynamic permissions on API >= 23):
```
<uses-permission android:name="android.permission.CAMERA"/>
```
Define a view in your layout file:
```
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.budiyev.android.codescanner.CodeScannerView
        android:id="@+id/scanner_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:frameColor="@android:color/white"
        app:frameCornersSize="50dp"
        app:frameWidth="2dp"
        app:maskColor="#77000000"
        app:squareFrame="true"/>
</FrameLayout>
```
And add following code to your activity:
```
public class MainActivity extends AppCompatActivity {
    private CodeScanner mCodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}
```

# Code Scanner
[![Download](https://api.bintray.com/packages/yuriy-budiyev/maven/code-scanner/images/download.svg)](https://bintray.com/yuriy-budiyev/maven/code-scanner/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Code%20Scanner-blue.svg?style=flat)](https://android-arsenal.com/details/1/6095)
[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/04f32141b2ef480580f709883541b469)](https://www.codacy.com/app/yuriy-budiyev/code-scanner?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriy-budiyev/code-scanner&amp;utm_campaign=Badge_Grade)

Code scanner library for [Android](https://developer.android.com), based on [ZXing](https://github.com/zxing/zxing)

### Features
* Auto focus and flash light control
* Portrait and landscape screen orientations
* Back and front facing cameras
* Customizable viewfinder

### Supported formats
| 1D product | 1D industrial | 2D
| ---------- | ------------- | --------------
| UPC-A      | Code 39       | QR Code
| UPC-E      | Code 93       | Data Matrix
| EAN-8      | Code 128      | Aztec
| EAN-13     | Codabar       | PDF 417
|            | ITF           | MaxiCode
|            | RSS-14        |
|            | RSS-Expanded  |

### Usage
Add dependency:
```
dependencies {
    implementation 'com.budiyev.android:code-scanner:1.5.4'
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
        app:squareFrame="true"
        app:autoFocusButtonColor="@android:color/white"
        app:flashButtonColor="@android:color/white"
        app:autoFocusButtonVisible="true"
        app:flashButtonVisible="true"/>
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
        // Use builder
        mCodeScanner =
                CodeScanner.builder().autoFocus(true).flash(false).onDecoded(new DecodeCallback() {
                    @Override
                    public void onDecoded(@NonNull final Result result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, result.getText(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).build(this, scannerView);
        // Or use constructor to create scanner with default parameters
        // All parameters can be changed after scanner created
        // mCodeScanner = new CodeScanner(this, scannerView);                
        // mCodeScanner.setDecodeCallback(...);                
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
### Preview
![Preview screenshot](https://raw.githubusercontent.com/yuriy-budiyev/code-scanner/master/images/code_scanner_preview.png)

package com.zebra.rfid.demo.sdksample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zebra.rfid.api3.TagData;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScannerActivity extends AppCompatActivity {

    RFIDHandler rfidHandler;
    RFIDHandler.RFIDHandlerInterface rfidHandlerInterface;
    ScannerHandler scannerHandler;

    private TextView et_results;
    private ScrollView sv_results;
    private String mResults = "";

    private Handler mScrollDownHandler = null;
    private Runnable mScrollDownRunnable = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        et_results = (TextView)findViewById(R.id.et_ScannerActivity);
        sv_results = (ScrollView)findViewById(R.id.sv_ScannerActivity);

        findViewById(R.id.btScanActivityClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        scannerHandler = new ScannerHandler(this, new ScannerHandler.ScannerHandlerInterface() {
            @Override
            public void onBarcodeData(String val, int symbo) {
                addLineToResults(val);
            }
        });

        rfidHandlerInterface = new RFIDHandler.RFIDHandlerInterface() {
            @Override
            public void onReaderConnected(String message) {
                MainApplication.rfidHandler.ConfigureReaderForScanning();
            }

            @Override
            public void onTagData(TagData[] tagData) {

            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void handleTriggerPress(boolean press) {
                ScannerActivity.this.handleTriggerPress(press);
            }

            @Override
            public void onReaderDisconnected() {

            }
        };

        MainApplication.rfidHandler.onCreate(this, rfidHandlerInterface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //String result = MainApplication.rfidHandler.onResume(rfidHandlerInterface);
        scannerHandler.onResume();
        mScrollDownHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        MainApplication.rfidHandler.onPause();
        if(mScrollDownRunnable != null)
        {
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            mScrollDownRunnable = null;
            mScrollDownHandler = null;
        }
        scannerHandler.onPause();
        super.onPause();
    }

    private void addLineToResults(final String lineToAdd)
    {
        mResults += lineToAdd + "\n";
        updateAndScrollDownTextView();
    }

    private void updateAndScrollDownTextView() {
        if (mScrollDownRunnable == null) {
            mScrollDownRunnable = new Runnable() {
                @Override
                public void run() {
                    ScannerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            et_results.setText(mResults);
                            sv_results.post(new Runnable() {
                                @Override
                                public void run() {
                                    sv_results.fullScroll(ScrollView.FOCUS_DOWN);
                                }
                            });
                        }
                    });
                }
            };
        } else {
            // A new line has been added while we were waiting to scroll down
            // reset handler to repost it....
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
        }
        if(mScrollDownHandler != null)
            mScrollDownHandler.postDelayed(mScrollDownRunnable, 300);
    }

    private void handleTriggerPress(boolean press) {
        if(press)
        {
            scannerHandler.pullTrigger();
        }
        else
        {
            scannerHandler.releaseTrigger();
        }
    }
}
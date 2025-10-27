package com.zebra.rfid.demo.sdksample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileenums.SC_E_SCANNER_IDENTIFIER;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintents.DWScanReceiver;
import com.zebra.datawedgeprofileintentshelpers.CreateProfileHelper;
import com.zebra.rfid.api3.TagData;

import java.util.HashMap;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScannerActivity extends AppCompatActivity {

    RFIDHandler.RFIDHandlerInterface rfidHandlerInterface;

    private boolean isUsingDatawedge = false;
    /**
     * Scanner data receiver
     */
    DWScanReceiver mScanReceiver;


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

        findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainApplication.scannerHandler.pullTrigger();
            }
        });

        if(mScrollDownHandler == null)
            mScrollDownHandler = new Handler(Looper.getMainLooper());

        if(MainApplication.scannerHandler.hasDetectedScanner()) {
            // We can use the scannerHandler to manage the scanner of this device
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
                    ScannerActivity.this.handleTriggerPressWithScannerHandler(press);
                }

                @Override
                public void onReaderDisconnected() {
                }
            };

            MainApplication.rfidHandler.onCreate(this, rfidHandlerInterface);

            isUsingDatawedge = false;
        }
        else
        {
            // We did not find a scanner using the BarcodeScanner SDK
            // Let's create a datawedge profile for this device
            // TODO: check the device model to ensure it has a scanner
            initializeDataWedge();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MainApplication.scannerHandler.hasDetectedScanner()) {
            MainApplication.scannerHandler.onResume(new ScannerHandler.ScannerHandlerInterface() {
                @Override
                public void onBarcodeData(String val, int symbo) {
                    addLineToResults(val);
                }
            });
            MainApplication.rfidHandler.onResume(rfidHandlerInterface);
        }
        else if(isUsingDatawedge)
        {
            registerDWReceiverAndStartReceive();
        }
        if(mScrollDownHandler == null)
            mScrollDownHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        if(MainApplication.scannerHandler.hasDetectedScanner()) {
            MainApplication.scannerHandler.onPause();
            MainApplication.rfidHandler.onPause();
        }
        else if(isUsingDatawedge)
        {
            unregisterDWReceiver();
        }
        if(mScrollDownRunnable != null)
        {
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            mScrollDownRunnable = null;
            mScrollDownHandler = null;
        }
        super.onPause();
    }

    private void initializeDataWedge()
    {
        addLineToResults("No scanner found using DSC SDK, using DataWedge");
        String packageName = ScannerActivity.this.getPackageName();
        DWProfileSetConfigSettings setConfigSettings = new DWProfileSetConfigSettings()
        {{
            mProfileName = packageName;
            mTimeOutMS = 5000;
            MainBundle.APP_LIST = new HashMap<>();
            MainBundle.APP_LIST.put(packageName, null);
            MainBundle.CONFIG_MODE = MB_E_CONFIG_MODE.CREATE_IF_NOT_EXIST;
            IntentPlugin.intent_action = packageName + ".RECVR";
            IntentPlugin.intent_category = "android.intent.category.DEFAULT";
            IntentPlugin.intent_output_enabled = true;
            IntentPlugin.intent_delivery = INT_E_DELIVERY.BROADCAST;
            KeystrokePlugin.keystroke_output_enabled = false;
            ScannerPlugin.scanner_selection_by_identifier = SC_E_SCANNER_IDENTIFIER.AUTO;
            ScannerPlugin.scanner_input_enabled = true;
            ScannerPlugin.Decoders.decoder_aztec = true;
            ScannerPlugin.Decoders.decoder_micropdf = true;
        }};
        CreateProfileHelper.createProfile(ScannerActivity.this, setConfigSettings, new CreateProfileHelper.CreateProfileHelperCallback() {
            @Override
            public void onSuccess(String profileName) {
                addLineToResults("Easy creation of profile:" + profileName + " succeeded.");
                isUsingDatawedge = true;
                // First initialization, register receiver if it is not created
                registerDWReceiverAndStartReceive();
            }

            @Override
            public void onError(String profileName, String error, String errorMessage) {
                addLineToResults("Error while trying to create profile:" + profileName);
                addLineToResults("Error:" + error);
                addLineToResults("ErrorMessage:" + errorMessage);
                isUsingDatawedge = false;
            }

            @Override
            public void ondebugMessage(String profileName, String message) {
                addLineToResults("Debug:" + message);
            }
        });
    }

    private void registerDWReceiverAndStartReceive()
    {
        if(isUsingDatawedge &&  mScanReceiver == null) {
            mScanReceiver = new DWScanReceiver(this,
                    this.getPackageName() + ".RECVR",
                    "android.intent.category.DEFAULT",
                    false,
                    new DWScanReceiver.onScannedData() {
                        @Override
                        public void scannedData(String source, String data, String typology) {
                            addLineToResults("Source: " + source);
                            addLineToResults("Typology: " + typology + ", Data: " + data);
                        }
                    }
            );
            mScanReceiver.startReceive();
        }
    }

    private void unregisterDWReceiver()
    {
        if(isUsingDatawedge && mScanReceiver != null)
        {
            mScanReceiver.stopReceive();
            mScanReceiver = null;
        }
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

    private void handleTriggerPressWithScannerHandler(boolean press) {
        if(press)
        {
            MainApplication.scannerHandler.pullTrigger();
        }
        else
        {
            MainApplication.scannerHandler.releaseTrigger();
        }
    }
}
package com.zebra.rfid.demo.sdksample;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWProfileCommandBase;
import com.zebra.datawedgeprofileintents.DWScanReceiver;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.TagData;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class EPCWriteActivity extends AppCompatActivity {

    String mTagID = "";

    EditText etNewEPC;
    TextView tv_status;
    TextView tv_EPC;

    DWScanReceiver mScanReceiver;

    MEMORY_BANK memoryBank = MEMORY_BANK.MEMORY_BANK_EPC;

    protected static ScannerHandler scannerHandler;
    RFIDHandler.RFIDHandlerInterface mHandlerInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epc_write);

        // Force portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Bundle b = getIntent().getExtras();
        String epc = ""; // or other values
        if(b != null)
            mTagID = b.getString("TagID");
        else
        {
            Toast.makeText(this, "Error, EPC should be passed to this activity using TagID extra.", Toast.LENGTH_LONG).show();
            finish();
        }


        tv_EPC = findViewById(R.id.tvEPC);
        tv_EPC.setText(mTagID);

        tv_status = findViewById(R.id.tv_status);
        etNewEPC = findViewById(R.id.et_NewEPC);
        etNewEPC.setText(mTagID);

        mHandlerInterface = new RFIDHandler.RFIDHandlerInterface() {
            @Override
            public void onReaderConnected(String message) {
                MainApplication.rfidHandler.ConfigureReaderForReadWrite();
            }

            @Override
            public void onReaderDisconnected() {

            }

            @Override
            public void onTagData(TagData[] tagData) {

            }

            @Override
            public void onMessage(String message) {

            }

            @Override
            public void handleTriggerPress(boolean press) {

            }
        };

        Button btWrite = findViewById(R.id.btWriteATag);
        btWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(etNewEPC.getText().toString().isEmpty() == false)
                {
                    if(etNewEPC.getText().length() != 24)
                    {
                        Toast.makeText(EPCWriteActivity.this, "Error EPC Data length: " + etNewEPC.getText().length() + "\nmust be equal to 24", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String dataToWrite = etNewEPC.getText().toString();
                    EPCWriteActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_status.setText("Writing data on TAG");
                        }
                    });


                    MainApplication.rfidHandler.writeNewEpc(mTagID, dataToWrite, null,new RFIDHandler.TagAccessCallback() {
                        @Override
                        public void onSuccess(String tagID) {
                            tv_status.setText("New EPC " + dataToWrite + " written successfully over old EPC: " + mTagID);
                            tv_EPC.setText(dataToWrite);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            tv_status.setText("Error writing data on tag:" + mTagID + "\nError:" + errorMessage);
                        }
                    });
                }
                else
                {
                    tv_status.setText("Can not write empty EPC.");
                }
            }
        });

        Button btDWScan = findViewById(R.id.btDWScan);
        btDWScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DWScannerStartScan dwstartscan = new DWScannerStartScan(EPCWriteActivity.this);
                DWProfileBaseSettings settings = new DWProfileBaseSettings()
                {{
                    mProfileName = EPCWriteActivity.this.getPackageName();
                }};

                dwstartscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
                    @Override
                    public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                        if(result.equalsIgnoreCase("SUCCESS"))
                        {
                            Log.v(MainApplication.TAG, "Scanner started with success for profile:" + profileName);
                        }
                        else
                        {
                            Log.e(MainApplication.TAG, "Error Starting Scanner on profile: " + profileName + "\n" + resultInfo);
                        }
                    }

                    @Override
                    public void timeout(String profileName) {

                    }
                });
            }
        });

        mScanReceiver = new DWScanReceiver(this,
                getPackageName() + ".RECVR",
                "android.intent.category.DEFAULT",
                false,
                new DWScanReceiver.onScannedData() {
                    @Override
                    public void scannedData(String source, String data, String symbo) {
                            {
                                EPCWriteActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        etNewEPC.setText(data);
                                    }
                                });
                            }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.rfidHandler.onResume(mHandlerInterface);
        if(scannerHandler != null && scannerHandler.hasDetectedScanner()) {
            scannerHandler.onResume(new ScannerHandler.ScannerHandlerInterface() {
                @Override
                public void onBarcodeData(String val, int symbo) {
                    {
                        EPCWriteActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                etNewEPC.setText(val);
                            }
                        });
                    }
                }
            });
        }
        else
        {
            mScanReceiver.startReceive();
        }
    }

    @Override
    protected void onPause() {
        if(scannerHandler != null && scannerHandler.hasDetectedScanner())
        {
            scannerHandler.onPause();
        }
        else {
            mScanReceiver.stopReceive();
        }
        MainApplication.rfidHandler.onPause();
        super.onPause();
    }
}
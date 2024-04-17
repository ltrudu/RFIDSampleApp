package com.zebra.rfid.demo.sdksample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWProfileCommandBase;
import com.zebra.datawedgeprofileintents.DWScanReceiver;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;
import com.zebra.rfid.api3.MEMORY_BANK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagReadUserMemoryActivity extends AppCompatActivity {

    String mTagID = "";

    TextView tvUserMemory;


    private ArrayList<BarcodeDataModel> mBarcodeDataModelArrayList = new ArrayList<>();
    RecyclerView rvBarcodesList;
    private BarcodeDataAdapter mBarcodeDataAdapter;

    DWScanReceiver mScanReceiver;

    MEMORY_BANK memoryBank = MEMORY_BANK.MEMORY_BANK_USER;

    protected static ScannerHandler scannerHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_user_memory_read);

        Bundle b = getIntent().getExtras();
        String epc = ""; // or other values
        if(b != null)
            epc = b.getString("TagID");

        TextView txtTagID = findViewById(R.id.tvEPC);
        txtTagID.setText(epc);
        mTagID = epc;

        tvUserMemory = findViewById(R.id.tvUserMemory);
        rvBarcodesList = findViewById(R.id.rvBarcodesList);

        mBarcodeDataAdapter = new BarcodeDataAdapter(mBarcodeDataModelArrayList);
        rvBarcodesList.setAdapter(mBarcodeDataAdapter);
        rvBarcodesList.setLayoutManager(new LinearLayoutManager(this));

        Button btClear = findViewById(R.id.btClear);
        btClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainApplication.rfidHandler.writeData(mTagID,  "", memoryBank, new RFIDHandler.TagUserMemoryAccessCallback() {
                    @Override
                    public void onSuccess(String tagID) {
                        Toast.makeText(TagReadUserMemoryActivity.this, "Data written with success for tag:" + tagID, Toast.LENGTH_LONG).show();
                        readDataOnTag();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(TagReadUserMemoryActivity.this, "Error writing data on tag:" + mTagID + "\nError:" + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                mBarcodeDataModelArrayList.clear();
                rvBarcodesList.post(new Runnable() {
                    @Override
                    public void run() {
                        mBarcodeDataAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        Button btWrite = findViewById(R.id.btWriteATag);
        btWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBarcodeDataModelArrayList.size() > 0) {
                    String dataToWrite = "";
                    for (BarcodeDataModel model : mBarcodeDataModelArrayList) {
                        dataToWrite += model.getBarcodeData() + "FF";
                    }
                    // Remove leading FF
                    dataToWrite = dataToWrite.substring(0, dataToWrite.length()-2);
                    MainApplication.rfidHandler.writeData(mTagID, dataToWrite, memoryBank, new RFIDHandler.TagUserMemoryAccessCallback() {
                        @Override
                        public void onSuccess(String tagID) {
                            Toast.makeText(TagReadUserMemoryActivity.this, mBarcodeDataModelArrayList.size() + " barcodes written successfully to tag:" + mTagID, Toast.LENGTH_LONG).show();
                            readDataOnTag();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(TagReadUserMemoryActivity.this, "Error writing data on tag:" + mTagID + "\nError:" + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else
                {
                    Toast.makeText(TagReadUserMemoryActivity.this, "Can not write empty list.", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btDWScan = findViewById(R.id.btDWScan);
        btDWScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DWScannerStartScan dwstartscan = new DWScannerStartScan(TagReadUserMemoryActivity.this);
                DWProfileBaseSettings settings = new DWProfileBaseSettings()
                {{
                    mProfileName = TagReadUserMemoryActivity.this.getPackageName();
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
                        if(mBarcodeDataModelArrayList.size() <= 7 && symbo.equalsIgnoreCase("EAN13")) {
                            BarcodeDataModel model = new BarcodeDataModel(data);
                            mBarcodeDataModelArrayList.add(model);
                            rvBarcodesList.post(new Runnable() {
                                @Override
                                public void run() {
                                    mBarcodeDataAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                        else
                        {
                            if(Looper.myLooper() == null)
                                Looper.prepare();
                            Toast.makeText(TagReadUserMemoryActivity.this, "Too many barcodes.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        scannerHandler = new ScannerHandler(this, new ScannerHandler.ScannerHandlerInterface() {
            @Override
            public void onBarcodeData(String val, int symbo) {
                if(mBarcodeDataModelArrayList.size() <= 7 && symbo == 11) {
                    BarcodeDataModel model = new BarcodeDataModel(val);
                    mBarcodeDataModelArrayList.add(model);
                    rvBarcodesList.post(new Runnable() {
                        @Override
                        public void run() {
                            mBarcodeDataAdapter.notifyDataSetChanged();
                        }
                    });
                }
                else
                {
                    if(Looper.myLooper() == null)
                        Looper.prepare();
                    Toast.makeText(TagReadUserMemoryActivity.this, "Too many barcodes.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        readDataOnTag();
        mScanReceiver.startReceive();
        scannerHandler.onResume();
    }

    @Override
    protected void onPause() {
        mScanReceiver.stopReceive();
        scannerHandler.onPause();
        super.onPause();
    }

    private void readDataOnTag()
    {
        MainApplication.rfidHandler.readData(mTagID, memoryBank, new RFIDHandler.TagUserMemoryAccessCallback() {
            @Override
            public void onSuccess(String memoryRead) {
                    tvUserMemory.setText(memoryRead);
                    updateBarcodeList(memoryRead);
            }

            @Override
            public void onError(String errorMessage) {
                tvUserMemory.setText(errorMessage);
                updateBarcodeList("51651616FF6516516FF5184651FF65165468FF65164684FF65131351FF35153431");
            }
        });
    }

    private void updateBarcodeList(String userData)
    {
        mBarcodeDataModelArrayList.clear();
        List<String> barcodeList = Arrays.asList(userData.split("FF"));
        if(barcodeList.size() != 0)
        {
            for (String barcode : barcodeList) {
                BarcodeDataModel model = new BarcodeDataModel(barcode);
                mBarcodeDataModelArrayList.add(model);
            }
        }
        rvBarcodesList.post(new Runnable() {
            @Override
            public void run() {
                mBarcodeDataAdapter.notifyDataSetChanged();
            }
        });

    }
}
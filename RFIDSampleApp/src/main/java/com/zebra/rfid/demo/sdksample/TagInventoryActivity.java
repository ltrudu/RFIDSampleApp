package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileenums.SC_E_SCANNER_IDENTIFIER;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintentshelpers.CreateProfileHelper;
import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Sample app to connect to the reader,to do inventory and basic barcode scan
 * We can also set antenna settings and singulation control
 * */

public class TagInventoryActivity extends AppCompatActivity {

    public TextView statusTextViewRFID = null;

    final static String TAG = "RFID_SAMPLE";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    TagDataAdapter mTagDataAdapter;

    RecyclerView mTagDataRecyclerView;
    private ArrayList<TagDataModel> mTagDataList = new ArrayList<>();

    RFIDHandler.RFIDHandlerInterface mHandlerInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // RFID Handler
        statusTextViewRFID = (TextView) findViewById(R.id.textViewStatusrfid);
        //rfidHandler.onCreate(this);

        mHandlerInterface = new RFIDHandler.RFIDHandlerInterface() {
            @Override
            public void onReaderConnected(String message) {
                statusTextViewRFID.setText(message);
            }

            @Override
            public void onTagData(TagData[] tagData) {
                TagInventoryActivity.this.handleTagdata(tagData);
            }

            @Override
            public void onMessage(String message) {
                TagInventoryActivity.this.sendToast(message);
            }

            @Override
            public void handleTriggerPress(boolean press) {
                TagInventoryActivity.this.handleTriggerPress(press);
            }
        };

        //Scanner Initializations
        //Handling Runtime BT permissions for Android 12 and higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }else{
                MainApplication.rfidHandler.onCreate(this, mHandlerInterface);
            }

        }else{
            MainApplication.rfidHandler.onCreate(this, mHandlerInterface);
        }

        mTagDataRecyclerView = findViewById(R.id.rv_results);
        mTagDataAdapter = new TagDataAdapter(mTagDataList, new TagDataAdapter.OnItemClickListener() {
            @Override
            public void onClickItem(int position, String epc) {
                Toast.makeText(TagInventoryActivity.this, "Selected item:" + String.valueOf(position), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(TagInventoryActivity.this, TagReadUserMemoryActivity.class);
                Bundle b = new Bundle();
                b.putString("TagID", epc); //Your id
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
            }
        });
        mTagDataRecyclerView.setAdapter(mTagDataAdapter);

        mTagDataRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        DWProfileSetConfigSettings setConfigSettings = new DWProfileSetConfigSettings()
        {{
            mProfileName = getPackageName();
            mTimeOutMS = 10000;
            MainBundle.APP_LIST = new HashMap<>();
            MainBundle.APP_LIST.put(getPackageName(), null);
            MainBundle.CONFIG_MODE = MB_E_CONFIG_MODE.CREATE_IF_NOT_EXIST;
            IntentPlugin.intent_action = getPackageName() + ".RECVR";
            IntentPlugin.intent_category = "android.intent.category.DEFAULT";
            IntentPlugin.intent_output_enabled = true;
            IntentPlugin.intent_delivery = INT_E_DELIVERY.BROADCAST;
            KeystrokePlugin.keystroke_output_enabled = false;
            ScannerPlugin.scanner_selection_by_identifier = SC_E_SCANNER_IDENTIFIER.AUTO;
            ScannerPlugin.scanner_input_enabled = true;
        }};
        CreateProfileHelper.createProfile(this, setConfigSettings, new CreateProfileHelper.CreateProfileHelperCallback() {
            @Override
            public void onSuccess(String profileName) {
                Log.d(TagInventoryActivity.TAG, "Profile " + profileName + " created with success.");
            }

            @Override
            public void onError(String profileName, String error, String errorMessage) {
                Log.e(TagInventoryActivity.TAG, "Error creating profile " + profileName + " :\n" + error + "\n" + errorMessage);
            }

            @Override
            public void ondebugMessage(String profileName, String message) {
                Log.v(TagInventoryActivity.TAG, message);
            }
        });

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MainApplication.rfidHandler.onCreate(this, mHandlerInterface);
            }
            else {
                Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.antenna_settings) {
            String result = MainApplication.rfidHandler.setConfigAntenna();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.Singulation_control) {
            String result = MainApplication.rfidHandler.setConfigSingulationControl();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.Default) {
            String result = MainApplication.rfidHandler.setConfigDefaults();
            Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        String result = MainApplication.rfidHandler.onResume();
        statusTextViewRFID.setText(result);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainApplication.rfidHandler.onDestroy();
    }

    public void StartInventory(View view)
    {
        mTagDataList.clear();
                mTagDataRecyclerView.post(new Runnable()
                {
                    @Override
                    public void run() {
                        mTagDataAdapter.notifyDataSetChanged();
                    }
                });
        MainApplication.rfidHandler.performInventory();
        //   rfidHandler.MultiTag();
    }





    public void StopInventory(View view){
        MainApplication.rfidHandler.stopInventory();
    }

    public void handleTagdata(TagData[] tagData) {
        boolean notifyAllSetChanged = false;
        ArrayList<Integer> itemchanged = new ArrayList<>();

        for (int index = 0; index < tagData.length; index++) {
            int tagIndex = findEPC(tagData[index]);
            if(tagIndex != -1)
            {
                mTagDataList.get(tagIndex).mTagID = tagData[index].getTagID();
                mTagDataList.get(tagIndex).mRssi = tagData[index].getPeakRSSI();
                Log.v(TAG, "TagID=" + tagData[index].getTagID());
                Log.v(TAG, "=" + tagData[index].getMemoryBankData());
                itemchanged.add(index);
            }
            else
            {
                TagDataModel newData = new TagDataModel(tagData[index].getTagID(), tagData[index].getPeakRSSI());
                mTagDataList.add(newData);
                notifyAllSetChanged = true;
            }
        }
        if(notifyAllSetChanged)
        {
                    mTagDataRecyclerView.post(new Runnable()
                    {
                        @Override
                        public void run() {
                            mTagDataAdapter.notifyDataSetChanged();
                        }
                    });
        }
        else
        {
            if(itemchanged.size() > 0)
            {
                for(int indexToChange : itemchanged)
                {
                            mTagDataRecyclerView.post(new Runnable()
                            {
                                @Override
                                public void run() {
                                    mTagDataAdapter.notifyItemChanged(indexToChange);
                                }
                            });
                }
            }
        }
    }

    private int findEPC(TagData tagData)
    {
        for(int index = 0; index <  mTagDataList.size(); index++)
        {
            if(mTagDataList.get(index).getTagID().equals(tagData.getTagID()))
            {
                return index;
            }
        }
        return -1;
    }

    public void handleTriggerPress(boolean pressed) {
        if (pressed) {
            mTagDataList.clear();
                    mTagDataRecyclerView.post(new Runnable()
                    {
                        @Override
                        public void run() {
                            mTagDataAdapter.notifyDataSetChanged();
                        }
                    });
            MainApplication.rfidHandler.performInventory();
        } else
            MainApplication.rfidHandler.stopInventory();
    }

    public void sendToast(String val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TagInventoryActivity.this,val,Toast.LENGTH_SHORT).show();
            }
        });

    }



}

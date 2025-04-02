package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileenums.SC_E_SCANNER_IDENTIFIER;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintentshelpers.CreateProfileHelper;
import com.zebra.rfid.api3.TagData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


/**
 * Sample app to connect to the reader,to do inventory and basic barcode scan
 * We can also set antenna settings and singulation control
 * */

public class TagInventoryActivity extends AppCompatActivity {

    private TextView statusTextViewRFID = null;

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    TagDataAdapter mTagDataAdapter;

    RecyclerView mTagDataRecyclerView;
    private ArrayList<TagDataModel> mTagDataList = new ArrayList<>();
    TextView tvNbItems;

    RFIDHandler.RFIDHandlerInterface mHandlerInterface;

    public static boolean bAllowLocationing = true;

    ActivityResultLauncher<Intent> resultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        MainApplication.rfidHandler.keepConnexion = false;
                    });

    private ConstraintLayout clQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Force portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setTitle(R.string.app_title);

        // RFID Handler
        statusTextViewRFID = (TextView) findViewById(R.id.textViewStatusrfid);
        //rfidHandler.onCreate(this);

        tvNbItems = findViewById(R.id.tvNbItems);
        tvNbItems.setText("0");

        clQuestion = findViewById(R.id.cl_question);
        clQuestion.setVisibility(View.GONE);

        String model = Build.MODEL;
        if(model.equalsIgnoreCase("EM45") == true)
        {
            bAllowLocationing = false;
        }

        mHandlerInterface = new RFIDHandler.RFIDHandlerInterface() {
            @Override
            public void onReaderConnected(String message) {
                MainApplication.rfidHandler.ConfigureReaderForInventory();
                statusTextViewRFID.setText(message);
            }

            @Override
            public void onReaderDisconnected() {
                statusTextViewRFID.setText("Reader Disconnected");
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
                MainApplication.rfidHandler.keepConnexion = true;
                Toast.makeText(TagInventoryActivity.this, "Selected item:" + String.valueOf(position), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(TagInventoryActivity.this, TagLocateActivity.class);
                Bundle b = new Bundle();
                b.putString("TagID", epc); //Your id
                intent.putExtras(b); //Put your id to your next Intent
                resultLauncher.launch(intent);
            }
        });
        mTagDataRecyclerView.setAdapter(mTagDataAdapter);

        mTagDataRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainApplication.rfidHandler.onPause();
                finish();
            }
        });

        findViewById(R.id.btNo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clQuestion.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btYes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = ((EditText)findViewById(R.id.etFilename)).getText().toString();
                if(filename.endsWith(".txt"))
                {
                    filename.replace(".txt", "");
                }

                if(filename.isEmpty())
                {
                    Toast.makeText(TagInventoryActivity.this, "Please enter a filename.", Toast.LENGTH_LONG).show();
                    return;
                }
                writeFile(filename);
            }
        });

        findViewById(R.id.btStartInventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartInventory();
            }
        });

        findViewById(R.id.btStopInventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StopInventory();
            }
        });

        findViewById(R.id.btScanActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Avoid disconnecting the scanner in the onPause of this activity
                MainApplication.rfidHandler.keepConnexion = true;
                Intent intent = new Intent(TagInventoryActivity.this, ScannerActivity.class);
                resultLauncher.launch(intent);
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

        if( id == R.id.Disconnect)
        {
            if(MainApplication.rfidHandler != null && MainApplication.rfidHandler.isReaderConnected())
            {
                String response = MainApplication.rfidHandler.disconnect();
                statusTextViewRFID.setText(response);
            }
        }

        if( id == R.id.Connect)
        {
            if(MainApplication.rfidHandler != null && MainApplication.rfidHandler.isReaderConnected() == false)
            {
                String response = MainApplication.rfidHandler.connect();
                statusTextViewRFID.setText(response);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        MainApplication.rfidHandler.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.rfidHandler.onResume(mHandlerInterface);
        findViewById(R.id.btStartInventory).setEnabled(true);
        if(mTagDataList.size() > 0) {
            findViewById(R.id.ExportTXT).setEnabled(true);
            findViewById(R.id.btShareTo).setEnabled(true);
        }
        else {
            findViewById(R.id.ExportTXT).setEnabled(false);
            findViewById(R.id.btShareTo).setEnabled(false);
        }
        findViewById(R.id.btStopInventory).setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainApplication.rfidHandler.onDestroy();
    }

    public void StartInventory()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTagDataList.clear();
                mTagDataAdapter.notifyDataSetChanged();
                tvNbItems.setText("0");
                findViewById(R.id.btStartInventory).setEnabled(false);
                findViewById(R.id.ExportTXT).setEnabled(false);
                findViewById(R.id.btStopInventory).setEnabled(true);
            }
        });
        MainApplication.rfidHandler.performInventory();
    }





    public void StopInventory(){
        MainApplication.rfidHandler.stopInventory();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.btStartInventory).setEnabled(true);
                if(mTagDataList.size() > 0) {
                    findViewById(R.id.ExportTXT).setEnabled(true);
                    findViewById(R.id.btShareTo).setEnabled(true);
                }
                else
                {
                    findViewById(R.id.ExportTXT).setEnabled(false);
                    findViewById(R.id.btShareTo).setEnabled(false);
                }
                findViewById(R.id.btStopInventory).setEnabled(false);
            }
        });
    }

    public void ExportTXT(View view)
    {
        String fileName = createNewFileName("data");
        ((EditText)findViewById(R.id.etFilename)).setText(fileName);
        clQuestion.setVisibility(View.VISIBLE);
  }

    private void writeFile(String fileName)
    {
        String txtToExport = "Inventory:\n";
        for(TagDataModel model : mTagDataList)
        {
            txtToExport += model.mTagID + "\n";
        }

        File fileToWrite = new File(getTodayFolder(), fileName + ".txt");
        if(fileToWrite.exists())
            fileToWrite.delete();

        try {
            // Create a FileWriter
            FileWriter fileWriter = new FileWriter(fileToWrite);

            // Write the string to the file
            fileWriter.write(txtToExport);

            // Close the FileWriter
            fileWriter.close();

            Toast.makeText(this, "File exported with success:\n" + fileName + ".txt", Toast.LENGTH_LONG).show();

            statusTextViewRFID.setText("File exported to: " + fileToWrite.getPath());

            clQuestion.setVisibility(View.GONE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getTodayDateString()
    {
        Date nowDate = new Date();
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String currentDate = sdf2.format(nowDate);
        return currentDate;
    }

    private File getTodayFolder()
    {
        File targetFolder = null;
        File dateFolder = null;
        try {
            targetFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), getString(R.string.app_name));
            if (targetFolder.exists() == false) {
                targetFolder.mkdirs();
            }
            dateFolder = new File(targetFolder, getTodayDateString());
            if (dateFolder.exists() == false) {
                dateFolder.mkdirs();
            }
            return dateFolder;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public String createNewFileName(String prefix)
    {
        Date nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH_mm_ss");
        String currentDateandTime = sdf.format(nowDate);
        String newFileName = prefix + currentDateandTime;
        return newFileName;
    }

    public void shareTo(View view)
    {
        String txtToExport = "Inventory:\n";
        for(TagDataModel model : mTagDataList)
        {
            txtToExport += model.mTagID + "\n";
        }
        shareText(txtToExport);
    }

    private void shareText(String text) {
        // Create an intent to share the text
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setType("text/plain");

        // Start the sharing chooser
        Intent chooser = Intent.createChooser(shareIntent, "Share via");
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
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
                Log.v(MainApplication.TAG, "TagID=" + tagData[index].getTagID());
                Log.v(MainApplication.TAG, "=" + tagData[index].getMemoryBankData());
                itemchanged.add(index);
            }
            else
            {
                TagDataModel newData = new TagDataModel(tagData[index].getTagID(), tagData[index].getPeakRSSI());
                mTagDataList.add(newData);
                notifyAllSetChanged = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvNbItems.setText(String.valueOf(mTagDataList.size()));
                    }
                });
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
            StartInventory();
        } else
            StopInventory();
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

package com.zebra.rfid.demo.sdksample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.TagData;

import java.util.Timer;
import java.util.TimerTask;

public class TagLocateActivity extends AppCompatActivity {

    RFIDHandler.RFIDHandlerInterface mHandlerInterface;

    RangeGraph rangeGraph;

    FloatingActionButton btLocate;
    private TextView statusTextViewRFID = null;

    private short mTagProximityPercent = -1;

    private static final int BEEP_DELAY_TIME_MIN = 0;
    private static final int BEEP_DELAY_TIME_MAX = 300;

    public static BEEPER_VOLUME beeperVolume = BEEPER_VOLUME.HIGH_BEEP;

    private boolean beepONLocate = false;
    private boolean isLocating = false;
    public Timer locatebeep;

    public static ToneGenerator toneGenerator;

    TextView tvTagID;

    String mTagID = "";

    private boolean isLocatingTag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_locate);

        // Force portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        rangeGraph = (RangeGraph)findViewById(R.id.rgLocationBar);
        statusTextViewRFID = (TextView) findViewById(R.id.textViewStatusrfid);
        btLocate = (FloatingActionButton)findViewById(R.id.fabtn_locate);
        btLocate.setImageResource(android.R.drawable.ic_media_play);

        Bundle b = getIntent().getExtras();
        String epc = ""; // or other values
        if(b != null)
            epc = b.getString("TagID");

        tvTagID = findViewById(R.id.tvEPC);
        tvTagID.setText(epc);
        mTagID = epc;

        mHandlerInterface = new RFIDHandler.RFIDHandlerInterface() {
            @Override
            public void onReaderConnected(String message) {
                statusTextViewRFID.setText(message);
                MainApplication.rfidHandler.ConfigureReaderForLocationing();
            }

            @Override
            public void onTagData(TagData[] tagData) {
                TagLocateActivity.this.handleTagData(tagData);
            }

            @Override
            public void onMessage(String message) {
                TagLocateActivity.this.sendToast(message);
            }

            @Override
            public void handleTriggerPress(boolean press) {
                if(press)
                {
                    if(isLocating == false)
                    {
                        isLocating = true;
                        MainApplication.rfidHandler.startLocationing(mTagID);
                        TagLocateActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btLocate.setImageResource(R.drawable.ic_play_stop);
                            }
                        });
                    }
                }
                else
                {
                    if(isLocating == true)
                    {
                        isLocating = false;
                        MainApplication.rfidHandler.stopLocationing();
                        TagLocateActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btLocate.setImageResource(android.R.drawable.ic_media_play);
                            }
                        });
                    }
                }
            }

            @Override
            public void onReaderDisconnected() {

            }
        };

        int streamType = AudioManager.STREAM_DTMF;
        int percentageVolume = 100;
        try {
            toneGenerator = new ToneGenerator(streamType, percentageVolume);
        } catch (RuntimeException exception) {
            toneGenerator = null;
        }

        MainApplication.rfidHandler.onCreate(this, mHandlerInterface);

        btLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLocating == false)
                {
                    isLocating = true;
                    MainApplication.rfidHandler.startLocationing(mTagID);
                    btLocate.setImageResource(R.drawable.ic_play_stop);
                }
                else
                {
                    isLocating = false;
                    MainApplication.rfidHandler.stopLocationing();
                    btLocate.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.rfidHandler.onResume(mHandlerInterface);
    }

    @Override
    protected void onPause() {
        MainApplication.rfidHandler.onPause();
        super.onPause();
    }

    private void handleTagData(TagData[] tagData) {
        if(MainApplication.rfidHandler != null)
        {
            for (int index = 0; index < tagData.length; index++) {
                if (tagData[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                        tagData[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                }
                if (tagData[index].isContainsLocationInfo()) {
                    final int tag = index;
                    mTagProximityPercent = tagData[tag].LocationInfo.getRelativeDistance();
                    if (mTagProximityPercent > 0) {
                        startlocatebeepingTimer(mTagProximityPercent);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mTagProximityPercent != -1 && rangeGraph != null)
                                {
                                    rangeGraph.setValue(mTagProximityPercent);
                                    rangeGraph.invalidate();
                                    rangeGraph.requestLayout();
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    public boolean isLocatingTag()
    {
        return isLocatingTag;
    }

    public  void startlocatebeepingTimer(int proximity) {
        if (beeperVolume != BEEPER_VOLUME.QUIET_BEEP) {
            int POLLING_INTERVAL1 = BEEP_DELAY_TIME_MIN + (((BEEP_DELAY_TIME_MAX - BEEP_DELAY_TIME_MIN) * (100 - proximity)) / 100);
            if (!beepONLocate) {
                beepONLocate = true;
                beep();
                if (locatebeep == null) {
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            stoplocatebeepingTimer();
                            beepONLocate = false;
                        }
                    };
                    locatebeep = new Timer();
                    locatebeep.schedule(task, POLLING_INTERVAL1, 10);
                }
            }
        }
    }

    /**
     * method to stop timer locate beep
     */
    public void stoplocatebeepingTimer() {
        if (locatebeep != null && toneGenerator != null) {
            toneGenerator.stopTone();
            locatebeep.cancel();
            locatebeep.purge();
        }
        locatebeep = null;
    }

    public void beep() {
        if (toneGenerator != null) {
            int toneType = ToneGenerator.TONE_PROP_BEEP;
            toneGenerator.startTone(toneType);
        }
    }

    public void locationingButtonClicked(View view) {
        if(tvTagID.getText().toString().isEmpty() == false)
        {
            mTagID = tvTagID.getText().toString();

        }
    }

    public void sendToast(String val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TagLocateActivity.this,val,Toast.LENGTH_SHORT).show();
            }
        });

    }
}
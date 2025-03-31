package com.zebra.rfid.demo.sdksample;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.zebra.criticalpermissionshelper.CriticalPermissionsHelper;
import com.zebra.criticalpermissionshelper.EPermissionType;
import com.zebra.criticalpermissionshelper.IResultCallbacks;
import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileenums.SC_E_SCANNER_IDENTIFIER;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintentshelpers.CreateProfileHelper;

import java.util.HashMap;

public class MainApplication extends Application {

    final static String TAG = "RFID_SAMPLE";

    protected static DWProfileSetConfigSettings mSetConfigSettings;

    protected static RFIDHandler rfidHandler;

    public interface iMainApplicationCallback
    {
        void onPermissionSuccess(String message);
        void onPermissionError(String message);
        void onPermissionDebug(String message);
    }

    public static boolean permissionGranted = false;
    public static String sErrorMessage = null;

    public static iMainApplicationCallback iMainApplicationCallback = null;

    // Let's Add a fake delay of 2000 milliseconds just for the show ;)
    // Otherwise Splash Screen is too fast
    private final static int S_FAKE_DELAY = 2000;

    @Override
    public void onCreate() {
        super.onCreate();
        rfidHandler = new RFIDHandler();
        /*
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                CriticalPermissionsHelper.grantPermission(MainApplication.this, EPermissionType.MANAGE_EXTERNAL_STORAGE, new IResultCallbacks() {
                    @Override
                    public void onSuccess(String message, String resultXML) {
                        permissionGranted = true;
                        sErrorMessage = null;
                        if(MainApplication.iMainApplicationCallback != null)
                        {
                            MainApplication.iMainApplicationCallback.onPermissionSuccess(message);
                        }
                    }

                    @Override
                    public void onError(String message, String resultXML) {
                        Toast.makeText(MainApplication.this, message, Toast.LENGTH_LONG).show();
                        permissionGranted = true;
                        sErrorMessage = message;
                        if(MainApplication.iMainApplicationCallback != null)
                        {
                            MainApplication.iMainApplicationCallback.onPermissionError(message);
                        }
                    }

                    @Override
                    public void onDebugStatus(String message) {
                        if(MainApplication.iMainApplicationCallback != null)
                        {
                            MainApplication.iMainApplicationCallback.onPermissionDebug(message);
                        }
                    }
                });
            }
        }, S_FAKE_DELAY); // Let's add some S_FAKE_DELAY like in music production
        */

        permissionGranted = true;
        sErrorMessage = null;
        if(MainApplication.iMainApplicationCallback != null)
        {
            MainApplication.iMainApplicationCallback.onPermissionSuccess("Success");
        }

        /*
        //Scanner Initializations
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
                Log.d(MainApplication.TAG, "Profile " + profileName + " created with success.");
            }

            @Override
            public void onError(String profileName, String error, String errorMessage) {
                Log.e(MainApplication.TAG, "Error creating profile " + profileName + " :\n" + error + "\n" + errorMessage);
            }

            @Override
            public void ondebugMessage(String profileName, String message) {
                Log.v(MainApplication.TAG, message);
            }
        });
        */

    }
}

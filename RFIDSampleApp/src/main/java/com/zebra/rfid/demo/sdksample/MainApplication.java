package com.zebra.rfid.demo.sdksample;

import android.app.Application;
import android.os.Build;
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

    //protected static DWProfileSetConfigSettings mSetConfigSettings;

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
        String manufacturer = Build.MANUFACTURER;
        if(manufacturer.contains("Zebra")) {
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

        }
        else {
            // Do nothing, handle the permission requests in the splash activity
            permissionGranted = true;
            sErrorMessage = null;
            if (MainApplication.iMainApplicationCallback != null) {
                MainApplication.iMainApplicationCallback.onPermissionSuccess("Success");
            }
        }
    }
}

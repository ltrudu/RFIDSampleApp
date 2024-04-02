package com.zebra.rfid.demo.sdksample;

import android.app.Application;
import android.util.Log;

import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileenums.SC_E_SCANNER_IDENTIFIER;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintentshelpers.CreateProfileHelper;

import java.util.HashMap;

public class MainApplication extends Application {
    protected static DWProfileSetConfigSettings mSetConfigSettings;

    protected static RFIDHandler rfidHandler;


    @Override
    public void onCreate() {
        super.onCreate();
        rfidHandler = new RFIDHandler();
    }
}

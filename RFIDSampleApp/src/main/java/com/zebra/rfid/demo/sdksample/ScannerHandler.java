package com.zebra.rfid.demo.sdksample;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;

class ScannerHandler implements IDcsSdkApiDelegate {

    final static String TAG = "SCAN_HANDLER";
    private Context context;
    private SDKHandler sdkHandler;
    private ArrayList<DCSScannerInfo> scannerList;
    private int scannerID;
    static MyAsyncTask cmdExecTask = null;

    public interface ScannerHandlerInterface {
        void onBarcodeData(String val, int symbo);
    }

    private ScannerHandlerInterface scannerHandlerCallback;

    // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
    // If barcode scan is available in RFD8500, for barcode scanning change mode using mode button on RFD8500 device. By default it is set to RFID mode
    String scannerName = "RFD40";

    public ScannerHandler(Context context, ScannerHandlerInterface scannerHandlerCallback)
    {
        this.context = context;
        this.scannerHandlerCallback = scannerHandlerCallback;
        scannerList = new ArrayList<>();
    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventScannerDisappeared(int i) {

    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int i) {

    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        String s = new String(barcodeData);
        if(scannerHandlerCallback != null)
            scannerHandlerCallback.onBarcodeData(s, barcodeType);
        Log.d(TAG,"barcaode ="+ s + "\nSymbo=" + String.valueOf(barcodeType));
    }

    @Override
    public void dcssdkEventImage(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventVideo(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventBinaryData(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {

    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo dcsScannerInfo, DCSScannerInfo dcsScannerInfo1) {

    }

    //
    //  Activity life cycle behavior
    //

    void onResume() {
        setupScannerSDK();
    }

    void onPause() {
        disconnect();
    }

    public void setupScannerSDK(){
        if (sdkHandler == null)
        {
            sdkHandler = new SDKHandler(context);
            //For cdc device
            DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);

            //For bluetooth device
            DCSSDKDefs.DCSSDK_RESULT btResult = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
            DCSSDKDefs.DCSSDK_RESULT btNormalResult = sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);

            Log.d(TAG,btNormalResult+ " results "+ btResult);
            sdkHandler.dcssdkSetDelegate(this);

            int notifications_mask = 0;
            // We would like to subscribe to all scanner available/not-available events
            notifications_mask |= DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value;

            // We would like to subscribe to all scanner connection events
            notifications_mask |= DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;


            // We would like to subscribe to all barcode events
            // subscribe to events set in notification mask
            sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
        }
        if (sdkHandler != null)
        {
            ArrayList<DCSScannerInfo> availableScanners = new ArrayList<>();
            availableScanners  = (ArrayList<DCSScannerInfo>) sdkHandler.dcssdkGetAvailableScannersList();

            scannerList.clear();
            if (availableScanners != null)
            {
                for (DCSScannerInfo scanner : availableScanners)
                {

                    scannerList.add(scanner);
                }
            }
            else
                Log.d(TAG,"Available scanners null");

        }

        for (DCSScannerInfo device : scannerList)
        {
            if (device.getScannerName().contains(scannerName))
            {
                try
                {
                    sdkHandler.dcssdkEstablishCommunicationSession(device.getScannerID());
                    scannerID= device.getScannerID();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "Disconnect");
        try {
                if (sdkHandler != null) {
                    sdkHandler.dcssdkTerminateCommunicationSession(scannerID);
                    scannerList = null;
                }
                //reader = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void scanCode(){
        String in_xml = "<inArgs><scannerID>" + scannerID+ "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    private class MyAsyncTask extends AsyncTask<String, Integer, Boolean> {
        int scannerId;
        StringBuilder outXML;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        ///private CustomProgressDialog progressDialog;

        public MyAsyncTask(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, StringBuilder outXML) {
            this.scannerId = scannerId;
            this.opcode = opcode;
            this.outXML = outXML;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected Boolean doInBackground(String... strings) {
            return executeCommand(opcode, strings[0], outXML, scannerId);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
        }
    }
    public boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
        if (sdkHandler != null)
        {
            if(outXML == null){
                outXML = new StringBuilder();
            }
            DCSSDKDefs.DCSSDK_RESULT result=sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode,inXML,outXML,scannerID);
            Log.d(TAG, "execute command returned " + result.toString() );
            if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
                return true;
            else if(result==DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
                return false;
        }
        return false;
    }



}

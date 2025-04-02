package com.zebra.rfid.demo.sdksample;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RegulatoryConfig;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;


class RFIDHandler implements Readers.RFIDReaderEventHandler {

final static String TAG = "RFID_HANDLER";
    private Readers readers;
    private ArrayList<ReaderDevice> availableRFIDReaderList;
    private ReaderDevice readerDevice;
    private RFIDReader reader;
    private EventHandler eventHandler;
    private Context context;
    private int MAX_POWER = 270;

    // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
    // If barcode scan is available in RFD8500, for barcode scanning change mode using mode button on RFD8500 device. By default it is set to RFID mode
    String readerName = "RFD40";

    public boolean keepConnexion = false;

    public interface RFIDHandlerInterface
    {
        void onReaderConnected(String message);
        void onTagData(TagData[] tagData);

        void onMessage(String message);

        void handleTriggerPress(boolean press);

        void onReaderDisconnected();
    }

    private RFIDHandlerInterface connectionInterface;

    void onCreate(Context context, RFIDHandlerInterface connectionInterface) {
        this.context = context;
        this.connectionInterface = connectionInterface;
    }


// TEST BUTTON functionality
    // following two tests are to try out different configurations features

    public String setConfigAntenna() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // set antenna configurations - reducing power to 100
        try {
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(40);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Antenna power Set to 40";
    }



    public String setConfigSingulationControl() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // Set the singulation control to S2 which will read each tag once only
        try {
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S2);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Session set to S2";
    }

    public String setConfigDefaults() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";;
        try {
            // Power to 270
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            // singulation to S0
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S0);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Default settings applied";
    }

    private boolean isReaderConnected() {
        if (reader != null && reader.isConnected())
            return true;
        else {
            Log.d(TAG, "reader is not connected");
            return false;
        }
    }

    //
    //  Activity life cycle behavior
    //

    void onResume(RFIDHandlerInterface connectionInterface) {
        this.connectionInterface = connectionInterface;
        if (readers == null) {
            new CreateInstanceTask().executeAsync();
        }
        else
            connectReader();
    }

    void onPause() {
        if(!keepConnexion)
            disconnectReader();
        else
        {
            if(connectionInterface != null)
            {
                connectionInterface.onReaderDisconnected();
            }
        }
    }

     void onDestroy() {
        dispose();
    }

    // Enumerates SDK based on host device
    private class CreateInstanceTask extends ExecutorTask<Void, Void, Void> {
        private InvalidUsageException invalidUsageException = null;
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "CreateInstanceTask");
            try {
                readers = new Readers(context, ENUM_TRANSPORT.SERVICE_USB);
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in SERVICE_USB Transport trying with BLUETOOTH transport");
                    readers.setTransport(ENUM_TRANSPORT.BLUETOOTH);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in BLUETOOTH Transport trying with SERVICE_SERIAL transport");
                    readers.setTransport(ENUM_TRANSPORT.SERVICE_SERIAL);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
                if(availableRFIDReaderList.isEmpty()) {
                    Log.d(TAG, "Reader not available in SERVICE_SERIAL Transport trying with RE_SERIAL transport");
                    readers.setTransport(ENUM_TRANSPORT.RE_SERIAL);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                }
            } catch (InvalidUsageException e) {
                invalidUsageException = e;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (invalidUsageException != null) {
                if(connectionInterface != null)
                {
                    connectionInterface.onMessage("Failed to get Available Readers\n"+invalidUsageException.getInfo());
                }
                //context.sendToast("Failed to get Available Readers\n"+invalidUsageException.getInfo());
                readers = null;
            } else if (availableRFIDReaderList.isEmpty()) {
                if(connectionInterface != null)
                {
                    connectionInterface.onMessage("No Available Readers to proceed");
                }
                //context.sendToast("No Available Readers to proceed");
                readers = null;
            } else {
                connectReader();
            }
        }
    }

    private synchronized void connectReader(){
        if(!isReaderConnected()){
            new ConnectionTask().executeAsync();
        }
        else
        {
            if(connectionInterface != null)
            {
                connectionInterface.onReaderConnected("Reader connected");
            }
        }
    }

    private class ConnectionTask extends ExecutorTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "ConnectionTask");
            if(reader == null)
                GetAvailableReader();
            if (reader != null)
                return connect();
            return "Failed to find or connect reader";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(connectionInterface != null)
            {
                connectionInterface.onReaderConnected(result);
            }
        }
    }

    private synchronized void GetAvailableReader() {
        Log.d(TAG, "GetAvailableReader");
        if (readers != null) {
            readers.attach(this);
            try {
                if (readers.GetAvailableRFIDReaderList() != null) {
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                    if (availableRFIDReaderList.size() != 0) {
                        // if single reader is available then connect it
                        if (availableRFIDReaderList.size() == 1) {
                            readerDevice = availableRFIDReaderList.get(0);
                            reader = readerDevice.getRFIDReader();
                        } else {
                            // search reader specified by name
                            for (ReaderDevice device : availableRFIDReaderList) {
                                Log.d(TAG,"device: "+device.getName());
                                if (device.getName().startsWith(readerName)) {

                                    readerDevice = device;
                                    reader = readerDevice.getRFIDReader();

                                }
                            }
                        }
                    }
                }
            }catch (InvalidUsageException ie){
                ie.printStackTrace();
            }

        }
    }

    // handler for receiving reader appearance events
    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
        if(connectionInterface != null)
            connectionInterface.onMessage("RFIDReaderAppeared");
        connectReader();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
        if(connectionInterface != null)
            connectionInterface.onMessage("RFIDReaderDisappeared");
        if (readerDevice.getName().equals(reader.getHostName()))
            disconnect();
    }

    public boolean isConnected()
    {
        if(reader == null)
            return false;
        return reader.isConnected();
    }


    private String connect() {
        if (reader != null) {
            Log.d(TAG, "connect " + reader.getHostName());
            try {
                if (!reader.isConnected()) {
                    // Establish connection to the RFID Reader
                    reader.connect();

                    if(reader.isConnected()){
                        if(connectionInterface != null)
                        {
                            connectionInterface.onReaderConnected("Reader connected");
                        }
                        return "Connected: " + reader.getHostName();
                    }

                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                String des = e.getResults().toString();
                return "Connection failed" + e.getVendorMessage() + " " + des;
            }
        }
        return "";
    }


    public void ConfigureReaderForScanning()
    {
        Log.d(TAG, "ConfigureReaderForScanning " + reader.getHostName());
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null) {
                    eventHandler = new EventHandler();
                    reader.Events.addEventsListener(eventHandler);
                }

                // set trigger mode as scanner
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, false);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);

            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }

    }

    public void ConfigureReaderForInventory() {
        Log.d(TAG, "ConfigureReaderForInventory " + reader.getHostName());
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

            try {
                // receive events from reader
                if (eventHandler == null) {
                    eventHandler = new EventHandler();
                    reader.Events.addEventsListener(eventHandler);
                }
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);

                //TODO: Check documentation
                // https://techdocs.zebra.com/dcs/rfid/android/2-0-2-94/tutorials/antenna/#code1
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);

                reader.Config.setUniqueTagReport(false);

                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                // TODO: Sessions are defined by the EPCglobal Gen2 (ISO 18000-6C) standard, which governs how RFID tags and readers interact.
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                //
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    public void ConfigureReaderForLocationing() {
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null) {
                    eventHandler = new EventHandler();
                    reader.Events.addEventsListener(eventHandler);
                }
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                //
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void disconnectReader()
    {
        if(isReaderConnected())
        {
            new DisconnectTask().executeAsync();
        }
    }

    private class DisconnectTask extends ExecutorTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            return disconnect();
        }
    }

    private String disconnect() {
        Log.d(TAG, "Disconnect");
        try {
            if (reader != null) {
                if (eventHandler != null)
                    reader.Events.removeEventsListener(eventHandler);

                reader.disconnect();
                if(connectionInterface != null)
                    connectionInterface.onMessage("Disconnecting reader");

                //reader = null;
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return "Reader disconnected";
    }

    private synchronized void dispose() {
        disconnect();
        try {
            if (reader != null) {
                //Toast.makeText(getApplicationContext(), "Disconnecting reader", Toast.LENGTH_LONG).show();
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface TagUserMemoryAccessCallback
    {
        void onSuccess(String tagID);
        void onError(String errorMessage);
    }

    synchronized void readData(String tagID, MEMORY_BANK memoryBank, TagUserMemoryAccessCallback readDataCallback)
    {
        try {
            TagAccess tagAccess = new TagAccess();
            TagAccess.ReadAccessParams readAccessParams = tagAccess.new ReadAccessParams();
            TagData readAccessTag;
            readAccessParams.setAccessPassword(0);
            // read 4 words
            //readAccessParams.setCount(4);
            // user memory bank
            readAccessParams.setMemoryBank(memoryBank);
            // start reading from word offset 0
            readAccessParams.setOffset(0);
            // read operation
            TagData tagData = reader.Actions.TagAccess.readWait(tagID, readAccessParams, null);

            if (tagData != null) {
                ACCESS_OPERATION_CODE readAccessOperation = tagData.getOpCode();
                if (readAccessOperation != null) {
                    if (tagData.getOpStatus() != null && !tagData.getOpStatus().equals(ACCESS_OPERATION_STATUS.ACCESS_SUCCESS)) {
                        String strErr = tagData.getOpStatus().toString().replaceAll("_", " ");
                        Log.d(TAG, strErr.toLowerCase());
                        if(readDataCallback != null)
                        {
                            readDataCallback.onError(strErr);
                        }
                    } else {
                        if (tagData.getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ) {
                            if(readDataCallback != null)
                            {
                                switch(memoryBank.toString())
                                {
                                    case "MEMORY_BANK_EPC":
                                        readDataCallback.onSuccess(tagData.getTagID());
                                        break;
                                    case "MEMORY_BANK_TID":
                                        readDataCallback.onSuccess(tagData.getTID());
                                        break;
                                    case "MEMORY_BANK_USER":
                                        // We need to unbase64 the content of the memory data bank
                                        String dataString = tagData.getMemoryBankData();
                                        // Remove padding and last characters
                                        String parts[] = dataString.split("FFFF", 2);
                                        readDataCallback.onSuccess(parts[0]);
                                        break;
                                }
                            }
                        } else {
                        }
                    }
                } else {
                    Log.d(TAG, "Read failed, Memory Data is null.");
                    if(readDataCallback != null)
                    {
                        readDataCallback.onError("Read failed, Memory Data is null.");
                    }
                }
            } else {
                Log.d(TAG, "Read access failed.");
                if(readDataCallback != null)
                {
                    readDataCallback.onError("Read access failed.");
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            if(readDataCallback != null)
            {
                readDataCallback.onError(e.getMessage());
            }
        }
    }

    public static String rightPad(String input, int length, String padStr) {

        if(input == null || padStr == null){
            return null;
        }

        if(input.length() >= length){
            return input;
        }

        int padLength = length - input.length();

        StringBuilder paddedString = new StringBuilder();
        paddedString.append(input);
        paddedString.append(padStr.repeat(padLength));

        return paddedString.toString();
    }

    public String padDataToTheNextByte(String data)
    {
        String paddedString = data;
        int paddedStringLength = data.length();
        int moduloString = paddedStringLength % 4;
        int expectedPaddedLength = paddedStringLength + ((moduloString >= 0) ? (4-moduloString) : 0);
        return rightPad(data, expectedPaddedLength, " ");
    }

    public String removePadding(String data)
    {
        return data.trim();
    }

    public String convertStringToHex(String str) {
        char[] chars = str.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }

    public String convertHexToString(String hexString) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hexString.length(); i += 2) {
            String hexPair = hexString.substring(i, i + 2);
            int decimalValue = Integer.parseInt(hexPair, 16);
            result.append((char) decimalValue);
        }
        return result.toString();
    }

    synchronized void writeData(String tagID, String writeData,MEMORY_BANK memoryBank,TagUserMemoryAccessCallback readDataCallback)
    {
        String dataToWrite = writeData;
        if(memoryBank == MEMORY_BANK.MEMORY_BANK_USER) {
            dataToWrite += "FFFF";
            dataToWrite = padDataToTheNextByte(dataToWrite);
        }

        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        try {
            reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.DISABLE);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }


        // Write user memory bank data
        TagData tagData = null;
        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
        writeAccessParams.setAccessPassword(0);
        writeAccessParams.setMemoryBank(memoryBank);
        writeAccessParams.setOffset(0); // start writing from word offset 0
        writeAccessParams.setWriteData(dataToWrite);
        writeAccessParams.setWriteRetries(3);

        // data length in words
        writeAccessParams.setWriteDataLength(dataToWrite.length() / 4);
        // antenna Info is null – performs on all antenna
        try {
            reader.Actions.TagAccess.writeWait(tagID, writeAccessParams, null, tagData);
            if(readDataCallback != null)
            {
                readDataCallback.onSuccess(tagID);
            }
        } catch (InvalidUsageException e) {
            if(readDataCallback != null)
            {
                readDataCallback.onError("Invalid usage exception:" + e.getMessage());
                Toast.makeText(context, "Invalid usage Exception: \n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (OperationFailureException e) {
            if(readDataCallback != null)
            {
                readDataCallback.onError("Operation failure exception:" + e.getMessage());
                Toast.makeText(context, "Operation failure exception: \n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        finally {
            try {
                reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }

        }
    }

    synchronized void performInventory() {
        try {
            reader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    synchronized void stopInventory() {
        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }


    synchronized void startLocationing(String tagID)
    {
        try {
            reader.Actions.TagLocationing.Perform(tagID,null,null);
        } catch (InvalidUsageException e) {
            if(connectionInterface != null)
            {
                connectionInterface.onMessage(e.getMessage());
            }
            //throw new RuntimeException(e);
        } catch (OperationFailureException e) {
            if(connectionInterface != null)
            {
                connectionInterface.onMessage(e.getMessage());
            }
            //throw new RuntimeException(e);
        }
    }

    synchronized void stopLocationing()
    {
        try {
            reader.Actions.TagLocationing.Stop();
        } catch (InvalidUsageException e) {
            if(connectionInterface != null)
            {
                connectionInterface.onMessage(e.getMessage());
            }
            //throw new RuntimeException(e);
        } catch (OperationFailureException e) {
            if(connectionInterface != null)
            {
                connectionInterface.onMessage(e.getMessage());
            }
            //throw new RuntimeException(e);
        }
    }

    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    //  Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                    Log.d(TAG, "Tag ID" + myTags[index].getTagID() +"RSSI value "+ myTags[index].getPeakRSSI());
                    Log.d(TAG, "RSSI value "+ myTags[index].getPeakRSSI());
                    /* To get the RSSI value*/   //   Log.d(TAG, "RSSI value "+ myTags[index].getPeakRSSI());

                }
                new AsyncDataUpdate().executeAsync(myTags);
            }
        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            Log.d(TAG,"HANDHELD_TRIGGER_PRESSED");
                            if(connectionInterface != null)
                                connectionInterface.handleTriggerPress(true);
                            return null;
                        }
                    }.execute();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            if(connectionInterface != null)
                                connectionInterface.handleTriggerPress(false);                            Log.d(TAG,"HANDHELD_TRIGGER_RELEASED");
                            return null;
                        }
                    }.execute();
                }
            }
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        if(connectionInterface != null)
                        {
                            connectionInterface.onReaderDisconnected();
                        }
                        disconnect();
                        return null;
                    }
                }.execute();
            }

        }
    }

    private class AsyncDataUpdate extends ExecutorTask<TagData[], Void, Void> {
        @Override
        protected Void doInBackground(TagData[]... params) {
            if(connectionInterface != null)
                connectionInterface.onTagData(params[0]);
            return null;
        }
    }
}

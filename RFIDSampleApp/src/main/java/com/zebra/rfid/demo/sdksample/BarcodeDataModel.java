package com.zebra.rfid.demo.sdksample;

public class BarcodeDataModel {

    protected String mBarcodeData;

    public BarcodeDataModel(String barcodeData)
    {
        mBarcodeData = barcodeData;
    }

    public String getBarcodeData()
    {
        return mBarcodeData;
    }

}

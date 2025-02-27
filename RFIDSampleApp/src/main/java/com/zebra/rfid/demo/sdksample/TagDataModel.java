package com.zebra.rfid.demo.sdksample;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class TagDataModel {

    private final static AtomicInteger atomicInteger = new AtomicInteger(0);

    protected String mTagID;

    protected short mRssi;

    protected int mUniqueID;

    protected Date mTimeStamp;

    public TagDataModel(String tagID, short rssi, Date timeStamp)
    {
        mTagID = tagID;
        mRssi = rssi;
        mTimeStamp = timeStamp;
        mUniqueID = atomicInteger.incrementAndGet();
    }

    public String getTagID()
    {
        return mTagID;
    }

    public short getRSSI()
    {
        return mRssi;
    }


    public int getUniqueID()
    {
        return mUniqueID;
    }
}

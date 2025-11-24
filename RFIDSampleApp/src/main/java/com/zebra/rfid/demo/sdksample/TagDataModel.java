package com.zebra.rfid.demo.sdksample;

import java.util.concurrent.atomic.AtomicInteger;

public class TagDataModel {

    private final static AtomicInteger atomicInteger = new AtomicInteger(0);

    protected String mTagID;

    protected short mRssi;

    protected int mUniqueID;

    protected boolean mbHasUserMemory = false;

    public TagDataModel(String tagID, short rssi)
    {
        mTagID = tagID;
        mRssi = rssi;
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

    public boolean getHasUserMemory() { return mbHasUserMemory; }
}

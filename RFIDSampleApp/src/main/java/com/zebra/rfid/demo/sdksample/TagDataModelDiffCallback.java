package com.zebra.rfid.demo.sdksample;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

public class TagDataModelDiffCallback extends DiffUtil.Callback{
    private List<TagDataModel> mOldList;
    private List<TagDataModel> mNewList;

    public TagDataModelDiffCallback(List<TagDataModel> oldList, List<TagDataModel> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
    }
    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).getUniqueID() == mNewList.get(newItemPosition).getUniqueID();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        TagDataModel oldData = mOldList.get(oldItemPosition);
        TagDataModel newData = mNewList.get(newItemPosition);

        return oldData.getRSSI() == newData.getRSSI() && oldData.getTagID() == newData.getTagID();
    }
}

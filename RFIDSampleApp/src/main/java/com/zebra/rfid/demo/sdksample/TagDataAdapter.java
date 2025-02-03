package com.zebra.rfid.demo.sdksample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class TagDataAdapter extends RecyclerView.Adapter<TagDataAdapter.TagDataViewHolder> {

    public interface OnItemClickListener{
        void onClickItem(int position, String epc);
    }

    private List<TagDataModel> mTagData;

    private final OnItemClickListener mItemRWClickListener;
    private final OnItemClickListener mItemLocateClickListener;

    public TagDataAdapter(List<TagDataModel> tagData)
    {
        this(tagData, null, null);
    }

    public TagDataAdapter(List<TagDataModel> tagData, OnItemClickListener itemRWClickListener, OnItemClickListener itemLocateClickListener)
    {
        mTagData = tagData;
        mItemRWClickListener = itemRWClickListener;
        mItemLocateClickListener = itemLocateClickListener;
    }

    @NonNull
    @Override
    public TagDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_tagdata, parent, false);

        // Return a new holder instance
        TagDataViewHolder viewHolder = new TagDataViewHolder(contactView, mItemRWClickListener, mItemLocateClickListener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TagDataViewHolder holder, int position) {
        TagDataModel data = mTagData.get(position);

        holder.mEPC.setText(data.getTagID());
        holder.mRssi.setText(String.valueOf(data.getRSSI()));

        if(position %2 == 0)
        {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        }
        else
        {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return mTagData.size();
    }

    public void swapItems(List<TagDataModel> contacts) {
        // compute diffs
        final TagDataModelDiffCallback diffCallback = new TagDataModelDiffCallback(this.mTagData, contacts);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        // clear contacts and add
        this.mTagData.clear();
        this.mTagData.addAll(contacts);

        diffResult.dispatchUpdatesTo(this); // calls adapter's notify methods after diff is computed
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class TagDataViewHolder extends RecyclerView.ViewHolder{
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView mEPC;
        public TextView mRssi;

        Button btRWClickListener;
        Button btLocateClickListener;

        private OnItemClickListener itemRWClickListener;
        private OnItemClickListener itemLocateClickListener;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public TagDataViewHolder(View itemView, OnItemClickListener itemRWClickListener, OnItemClickListener itemLocateClickListener) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any TagDataViewHolder instance.
            super(itemView);

            mEPC = (TextView) itemView.findViewById(R.id.tv_epc);
            mRssi = (TextView) itemView.findViewById(R.id.tv_rssi);

            btRWClickListener = (Button)itemView.findViewById(R.id.bt_read_write);
            btLocateClickListener = (Button)itemView.findViewById(R.id.bt_locate);

            this.itemRWClickListener = itemRWClickListener;
            this.itemLocateClickListener = itemLocateClickListener;

            if(itemRWClickListener != null)
            {
                btRWClickListener.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        itemRWClickListener.onClickItem(getAdapterPosition(), mEPC.getText().toString());
                    }
                });
            }

            if(itemLocateClickListener != null)
            {
                btLocateClickListener.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        itemLocateClickListener.onClickItem(getAdapterPosition(), mEPC.getText().toString());
                    }
                });
            }

            if(TagInventoryActivity.bAllowWrite == false)
                btRWClickListener.setVisibility(View.GONE);

            if(TagInventoryActivity.bAllowLocationing == false)
            {
                btLocateClickListener.setVisibility(View.GONE);
            }
        }
    }
}

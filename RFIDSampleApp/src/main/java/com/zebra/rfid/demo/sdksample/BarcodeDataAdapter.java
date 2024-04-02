package com.zebra.rfid.demo.sdksample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BarcodeDataAdapter extends RecyclerView.Adapter<BarcodeDataAdapter.BarcodeDataViewHolder> {

    private List<BarcodeDataModel> barcodeDataModelList;

    public BarcodeDataAdapter(List<BarcodeDataModel> barcodeDataModelList)
    {
        this.barcodeDataModelList = barcodeDataModelList;
    }

    @NonNull
    @Override
    public BarcodeDataAdapter.BarcodeDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View barcodeDataView = inflater.inflate(R.layout.item_barcode, parent, false);

        // Return a new holder instance
        BarcodeDataAdapter.BarcodeDataViewHolder viewHolder = new BarcodeDataAdapter.BarcodeDataViewHolder(barcodeDataView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BarcodeDataAdapter.BarcodeDataViewHolder holder, int position) {
        BarcodeDataModel data = barcodeDataModelList.get(position);
        holder.tvBarcodeData.setText(data.getBarcodeData());
    }

    @Override
    public int getItemCount() {
        return barcodeDataModelList.size();
    }

    public class BarcodeDataViewHolder extends RecyclerView.ViewHolder {

        public TextView tvBarcodeData;

        public BarcodeDataViewHolder(View itemView)
        {
            super(itemView);
            tvBarcodeData = itemView.findViewById(R.id.tvBarcodeData);
        }

    }

}

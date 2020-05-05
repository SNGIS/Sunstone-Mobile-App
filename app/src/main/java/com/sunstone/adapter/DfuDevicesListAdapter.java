package com.sunstone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sunstone.R;
import com.sunstone.model.DfuDeviceStatus;

import java.util.ArrayList;

public class DfuDevicesListAdapter extends ArrayAdapter<DfuDeviceStatus> {
    private static final String TAG = "DfuDevicesListAdapter";

    private ArrayList<DfuDeviceStatus> dataSet;
    private Context context;
    private int resId;

    public DfuDevicesListAdapter(ArrayList<DfuDeviceStatus> mData, Context mContext, int resID) {
        super(mContext, resID);
        this.dataSet = mData;
        this.context = mContext;
        this.resId = resID;
    }

    @Override
    public int getCount() {
        if(dataSet.size() > 0){
            return dataSet.size();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceId;
        TextView tvStatus;
    }

    private int lastPosition = -1;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        DfuDeviceStatus dfuDeviceStatus = dataSet.get(position);
        ViewHolder viewHolder;

        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resId, parent, false);
            viewHolder.tvDeviceName = convertView.findViewById(R.id.tv_dfu_device_name);
            viewHolder.tvDeviceId = convertView.findViewById(R.id.tv_dfu_device_id);
            viewHolder.tvStatus = convertView.findViewById(R.id.tv_dfu_device_status);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        lastPosition = position;

        if(dataSet.size() > 0){
            viewHolder.tvDeviceName.setText(dfuDeviceStatus.getDeviceName());
            viewHolder.tvDeviceId.setText(dfuDeviceStatus.getDeviceId());
            viewHolder.tvStatus.setText(dfuDeviceStatus.getCurrentStatus());
        }

        return convertView;
    }

    public void clearDataSet(){
        this.dataSet.clear();
        notifyDataSetChanged();
    }
}

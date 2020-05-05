package com.sunstone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sunstone.R;
import com.sunstone.model.LolanReadValue;

import java.util.ArrayList;

public class CutsomAdapterListViewRead  extends ArrayAdapter<LolanReadValue> implements View.OnClickListener{
    private static final String TAG = "CutsomAdapterListRead";

    private ArrayList<LolanReadValue> dataSet;
    private Context context;

    public CutsomAdapterListViewRead(ArrayList<LolanReadValue> data, Context context) {
        super(context, R.layout.lolan_alert_row_read);
        this.dataSet = data;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
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
        TextView tvReadValue;
    }

    private int lastPosition = -1;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LolanReadValue lolanReadValue = dataSet.get(position);
        ViewHolder viewHolder;

        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.lolan_alert_row_read, parent, false);
            viewHolder.tvDeviceName = convertView.findViewById(R.id.tv_alert_row_device);
            viewHolder.tvDeviceId = convertView.findViewById(R.id.tv_alert_row_id);
            viewHolder.tvReadValue = convertView.findViewById(R.id.tv_alert_row_value);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        lastPosition = position;

        if(dataSet.size() > 0){
            viewHolder.tvDeviceName.setText(lolanReadValue.getDeviceName());
            viewHolder.tvDeviceId.setText(lolanReadValue.getDeviceId());
            viewHolder.tvReadValue.setText(lolanReadValue.getReadValue());
        }

        return convertView;
    }

    public void clearDataSet(){
        this.dataSet.clear();
    }
}

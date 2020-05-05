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
import com.sunstone.model.LolanObjectToUpdate;

import java.util.ArrayList;

public class CustomAdapterListViewSet extends ArrayAdapter<LolanObjectToUpdate> implements View.OnClickListener{
    private static final String TAG = "CustomAdapterListView";

    private ArrayList<LolanObjectToUpdate> dataSet;
    private Context context;


    public CustomAdapterListViewSet(ArrayList<LolanObjectToUpdate> data, Context context){
        super(context, R.layout.lolan_alert_row);
        this.dataSet = data;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public int getCount() {
        if(dataSet.size() > 0)
            return dataSet.size();
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView tvLolanPath;
        TextView tvChangeLolanValue;
        ImageView ivTrash;
    }

    private int lastPosition = -1;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LolanObjectToUpdate objectToUpdate = dataSet.get(position);
        ViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.lolan_alert_row, parent, false);
            viewHolder.tvLolanPath = (TextView) convertView.findViewById(R.id.tv_alert_row_path);
            viewHolder.tvChangeLolanValue = (TextView) convertView.findViewById(R.id.et_alert_row_value);
            viewHolder.ivTrash = (ImageView) convertView.findViewById(R.id.iv_alert_row_delete);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        lastPosition = position;
        if(dataSet.size() > 0) {
            viewHolder.tvLolanPath.setText(context.getString(R.string.str_viewholder_path, objectToUpdate.getlolanPathName()));

            viewHolder.tvChangeLolanValue.setText(objectToUpdate.getValueToUpdate());
            viewHolder.ivTrash.setOnClickListener(v -> {
                dataSet.remove(objectToUpdate);
                notifyDataSetChanged();
            });
        }

        return convertView;
    }
}

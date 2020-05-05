package com.sunstone.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sunstone.R;
import com.sunstone.model.HeaderConfigData;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;
import static com.sunstone.utility.PrefsHelper.CURRENT_LOLAN_HEADER;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;

public class HeaderConfigListAdapter extends ArrayAdapter<HeaderConfigData> implements View.OnClickListener{
    private static final String TAG = "HeaderConfigListAdapter";

    private ArrayList<HeaderConfigData> dataSet;
    private Context context;
    private String header;
    private char[] modifiedHeader;

    public HeaderConfigListAdapter(Context context, ArrayList<HeaderConfigData> data, String currentHeader){
        super(context, R.layout.header_config_row);
        this.dataSet = data;
        this.context = context;
        this.header = currentHeader;
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

    @Nullable
    @Override
    public HeaderConfigData getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView tvAttributeName;
        TextView tvAttributeMeaning;
        CheckBox checkBox;
    }

    private int lastPosition = -1;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        HeaderConfigData headerConfigData = dataSet.get(position);
        ViewHolder viewHolder;

        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.header_config_row, parent, false);
            viewHolder.tvAttributeName = convertView.findViewById(R.id.tv_attribute_name);
            viewHolder.tvAttributeMeaning = convertView.findViewById(R.id.tv_attribute_maning);
            viewHolder.checkBox = convertView.findViewById(R.id.cb_config);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        lastPosition = position;
        if(dataSet.size() > 0) {
            viewHolder.tvAttributeName.setText(headerConfigData.getAttributeName());
            viewHolder.tvAttributeMeaning.setText(headerConfigData.getAttributeMeaning());
            viewHolder.checkBox.setChecked(headerConfigData.isChecked());

            viewHolder.checkBox.setClickable(false);
        }
        return convertView;
    }

    public void configureHeader(int pos){
        HeaderConfigData headerConfigData = getItem(pos);
        headerConfigData.setChecked(!headerConfigData.isChecked());
        notifyDataSetChanged();
        Log.d(TAG, "configureHeader: position: " + pos);
        Log.d(TAG, "configureHeader: checked? " + headerConfigData.isChecked());
    }




    public byte getHeader1(){
        return 0x05;
    }

    public byte getHeader2(){
        return 0x74;
    }

    private String getCurrentHeader(){
        return this.header;
    }

    private void saveHeader(){

    }

    private String getModifiedHeader(){
        return "";
    }

}

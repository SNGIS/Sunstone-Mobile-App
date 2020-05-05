package com.sunstone.screens;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import com.sunstone.R;
import com.sunstone.adapter.HeaderConfigListAdapter;
import com.sunstone.model.HeaderConfigData;

import java.util.ArrayList;

import static com.sunstone.model.HeaderDb.fillHeaderDB;
import static com.sunstone.model.HeaderDb.setDbValues;
import static com.sunstone.utility.ParserUtils.binToHex;
import static com.sunstone.utility.ParserUtils.reverseString;
import static com.sunstone.utility.PrefsHelper.CURRENT_LOLAN_HEADER;
import static com.sunstone.utility.PrefsHelper.SCAN_DURATION_PREFS;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;

public class LolanHeaderActivity extends AppCompatActivity {
    private static final String TAG = "LolanHeaderActivity";

    private Context context;

    private ListView listViewConfig;
    private HeaderConfigListAdapter adapter;
    private ArrayList<HeaderConfigData> headerConfigDataArrayList = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private String currentHeader;

    private Button btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lolan_header);

        context = this;

        loadSharedData();
        initGui();

    }

    private void initGui(){
        final Toolbar toolbar = findViewById(R.id.toolbar_header);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btnSave = findViewById(R.id.btn_save_header_config);
        btnCancel = findViewById(R.id.btn_cancel_header_config);

        fillHeaderDB(headerConfigDataArrayList);
        Log.d(TAG, "initGui: headerConfigDataArrayList size: " + headerConfigDataArrayList.size());
        setDbValues(currentHeader, headerConfigDataArrayList);
        for(HeaderConfigData h : headerConfigDataArrayList){
            Log.d(TAG, "initGui: headerConfigDataArrayList: getAttributeName: " + h.getAttributeName());
            Log.d(TAG, "initGui: headerConfigDataArrayList: isChecked: " + h.isChecked());
        }

        listViewConfig = findViewById(R.id.lv_config_header);
        adapter = new HeaderConfigListAdapter(context, headerConfigDataArrayList, currentHeader);
        listViewConfig.setAdapter(adapter);

        listViewConfig.setOnItemClickListener((parent, view, position, id) -> {
            adapter.configureHeader(position);
            modifyHeaderString(position);
        });

        btnSave.setOnClickListener(v -> {
            saveSharedPreferences();
            Log.d(TAG, "initGui: saving header byte 1: " + currentHeader.substring(0, 5));
            Log.d(TAG, "initGui: saving header byte 2: " + currentHeader.substring(5));
            onBackPressed();
        });

        btnCancel.setOnClickListener(v -> onBackPressed());
    }

    private void loadSharedData() {
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        currentHeader = sharedPreferences.getString(CURRENT_LOLAN_HEADER, "0111010000000");
        Log.d(TAG, "loadSharedData: loaded HEADER byte 2: " + binToHex(reverseString(currentHeader.substring(5))));
    }

    private void saveSharedPreferences(){
        sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(CURRENT_LOLAN_HEADER, currentHeader);
        sharedPreferencesEditor.apply();
        sharedPreferencesEditor.commit();
    }

    private void modifyHeaderString(int position){
        char[] chars = currentHeader.toCharArray();
        chars[position] = currentHeader.charAt(position) == "1".charAt(0) ? "0".charAt(0) : "1".charAt(0);
        currentHeader = String.valueOf(chars);
        Log.d(TAG, "modifyHeaderString: AFTER: " + currentHeader);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

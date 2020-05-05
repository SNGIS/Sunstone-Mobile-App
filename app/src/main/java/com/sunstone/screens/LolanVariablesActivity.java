package com.sunstone.screens;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunstone.R;
import com.sunstone.lolan_db.LolanEntryAlert;
import com.sunstone.model.LolanDbData;

import java.util.ArrayList;
import java.util.List;

public class LolanVariablesActivity extends AppCompatActivity {
    private static final String TAG = "LolanVariablesActivity";

    private static String INTENT_BTLE_DEVICE = "btle_device";
    private static String LOLAN_DEVICE_ADDRESS = "btle_devices_address";

    private BluetoothManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice chosenDevice;

    private Context context;

    private int lolanDeviceAddress;

    private TextView tvInfo, tvTagSettings, tvDw1000, tvControl, tvStatus, tvDeviceName, tvDeviceAddress;
    private LinearLayout llInfo, llTagSettings, llDw1000, llControl, llStatus;

    private List<LolanEntryAlert> lolanDbEntries = new ArrayList<>();

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lolan_vars);

        context = this;
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            mBluetoothAdapter = manager.getAdapter();
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 101);
        }

        intent = getIntent();
        try {
            if (intent.hasExtra(INTENT_BTLE_DEVICE)) {
                Log.d(TAG, "onCreate: got device extra");
                chosenDevice = intent.getParcelableExtra(INTENT_BTLE_DEVICE);
            }
            if (intent.hasExtra(LOLAN_DEVICE_ADDRESS)) {
                lolanDeviceAddress = intent.getIntExtra(LOLAN_DEVICE_ADDRESS, 0);
                Log.d(TAG, "onCreate: got lolan device address extra: " + lolanDeviceAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupToolbar();
        setupGui();
    }


    private void setupGui(){

        tvInfo = findViewById(R.id.tv_lolanvar_info);
        tvTagSettings = findViewById(R.id.tv_lolanvar_tag_settings);
        tvDw1000 = findViewById(R.id.tv_lolanvar_dw1000settings);
        tvControl = findViewById(R.id.tv_lolanvar_control);
        tvStatus = findViewById(R.id.tv_lolanvar_status);

        tvDeviceName = findViewById(R.id.tv_lolanvars_device_name);
        tvDeviceName.setText(chosenDevice.getName().toString());
        tvDeviceAddress = findViewById(R.id.tv_lolanvars_device_address);
        tvDeviceAddress.setText(chosenDevice.getAddress().toString());

        llInfo = findViewById(R.id.ll_lolanvar_info);
        llInfo.setVisibility(View.GONE);
        llTagSettings = findViewById(R.id.ll_lolanvar_settings);
        llTagSettings.setVisibility(View.GONE);
        llDw1000 = findViewById(R.id.ll_lolanvar_dw1000settings);
        llDw1000.setVisibility(View.GONE);
        llControl = findViewById(R.id.ll_lolanvar_control);
        llControl.setVisibility(View.GONE);
        llStatus = findViewById(R.id.ll_lolanvar_status);
        llStatus.setVisibility(View.GONE);

        tvInfo.setOnClickListener(view -> switchView(llInfo));
        tvTagSettings.setOnClickListener(view -> switchView(llTagSettings));
        tvDw1000.setOnClickListener(view -> switchView(llDw1000));
        tvControl.setOnClickListener(view -> switchView(llControl));
        tvStatus.setOnClickListener(view -> switchView(llStatus));

        LolanDbData.fillDatabase(lolanDbEntries, findViewById(android.R.id.content).getRootView(), context);

        for(LolanEntryAlert e : lolanDbEntries){
            e.getView().setOnClickListener(view -> {
                try {
                    e.showAlert(view.getContext(), chosenDevice, lolanDeviceAddress);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }


    private void setupToolbar(){
        final Toolbar toolbar = findViewById(R.id.toolbar_header);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }


    private void switchView(View v) {
        if(v.getVisibility() == View.GONE) {
            v.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        chosenDevice = null;
        intent = null;
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

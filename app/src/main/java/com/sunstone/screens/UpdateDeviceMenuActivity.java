package com.sunstone.screens;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.sunstone.R;
import com.sunstone.dfu.DfuActivity;
import com.sunstone.scanner.DeviceScannerActivity;
import com.sunstone.scanner.DeviceScannerMultiDfuActivity;
import com.sunstone.scanner.DeviceScannerMultiActivity;

public class UpdateDeviceMenuActivity extends AppCompatActivity {

    private Button btnUpdateSingleVar, btnUpdateMultiVar, btnDfuSingle, btnDfuMulti, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_device_menu);

        btnUpdateSingleVar = findViewById(R.id.btn_update_menu_single_var);
        btnUpdateMultiVar = findViewById(R.id.btn_update_menu_multi_var);
        btnDfuSingle = findViewById(R.id.btn_update_menu_single_dfu);
        btnDfuMulti = findViewById(R.id.btn_update_menu_multi_dfu);
        btnBack = findViewById(R.id.btn_update_menu_back);

        initGui();

    }

    private void initGui(){
        btnUpdateSingleVar.setOnClickListener(v ->
                startActivity(new Intent(UpdateDeviceMenuActivity.this, DeviceScannerActivity.class)));
        btnUpdateMultiVar.setOnClickListener(v ->
                startActivity(new Intent(UpdateDeviceMenuActivity.this, DeviceScannerMultiActivity.class)));
        btnDfuSingle.setOnClickListener(v ->
                startActivity(new Intent(UpdateDeviceMenuActivity.this, DfuActivity.class)));
        btnDfuMulti.setOnClickListener(v ->
                startActivity(new Intent(UpdateDeviceMenuActivity.this, DeviceScannerMultiDfuActivity.class)));

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(UpdateDeviceMenuActivity.this, MainMenuActivity.class));
            finish();
        });
    }

}

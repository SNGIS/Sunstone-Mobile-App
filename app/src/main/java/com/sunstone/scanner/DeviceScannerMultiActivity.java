package com.sunstone.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.sunstone.R;
import com.sunstone.adapter.DeviceListMultiAdapter;
import com.sunstone.screens.LolanMultiVariablesActivity;
import com.sunstone.model.ExtendedBluetoothDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.sentry.Sentry;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static com.sunstone.utility.PrefsHelper.SCAN_DURATION_PREFS;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;
import static com.sunstone.utility.PrefsHelper.USER_ID_TAGS;

import static com.sunstone.utility.BluetoothUtils.MANUFACTURER_ID;

public class DeviceScannerMultiActivity extends AppCompatActivity {
    private static final String TAG = "DeviceScannerMulti";

    private static String INTENT_BTLE_DEVICE = "btle_device";
    private static String INTENT_BTLE_DEVICE_LIST = "btle_devices_list";
    private static String LOLAN_DEVICE_ADDRESS = "btle_devices_address";
    private static final String DEVICES_TO_UPDATE = "devices_to_update";

    private final static int REQUEST_PERMISSION_REQ_CODE = 42;

    private BluetoothAdapter mBluetoothAdapter;
    private ScannerFragment.OnDeviceSelectedListener mListener;
    private DeviceListMultiAdapter mAdapter;
    private final Handler mHandler = new Handler();

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;
    private Set<String> userDevicesSet;
    private int SCAN_DURATION_SHARED;

    private List<ScanResult> sunstoneDevicesList = new ArrayList<>();
    private ArrayList<ExtendedBluetoothDevice> devicesToUpdate = new ArrayList<>();

    private BluetoothGatt mGatt;
    private ParcelUuid mUuid;

    Button btnScan, btnSelect;
    private ProgressBar progressBar;

    private boolean mIsScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scanner_multi);

        initGui();
        loadSharedData();
        mIsScanning = true;

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBluetoothAdapter = manager.getAdapter();
        }

        if(mGatt != null)
            mGatt.disconnect();

        if(savedInstanceState == null)
            startScan();
    }

    private void startScan() {

        runOnUiThread(() -> {
            btnScan.setText(R.string.scanner_action_cancel);
            btnSelect.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        });

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        final ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
        scanner.startScan(filters, settings, scanCallback);

        mIsScanning = true;
        mHandler.postDelayed(() -> {
            if (mIsScanning) {
                stopScan();
            }
        }, SCAN_DURATION_SHARED);
    }

    private void stopScan() {
        if (mIsScanning) {
            runOnUiThread(() -> {
                btnScan.setText(R.string.scanner_action_scan);
                btnScan.setVisibility(View.VISIBLE);
                btnSelect.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            });
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
            mIsScanning = false;
            if(mGatt != null)
                mGatt.disconnect();
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            try {
                for (ScanResult sr : results) {
                    ScanRecord scanRecord = sr.getScanRecord();
                    if ((scanRecord != null) && (scanRecord.getManufacturerSpecificData(MANUFACTURER_ID) != null)) {
                        sunstoneDevicesList.add(sr);
//                        addUserDevice(sr, scanRecord);
                    }
                }
                if (sunstoneDevicesList.size() > 0) {
                    mAdapter.update(sunstoneDevicesList);
                }
            } catch (Exception e) {
                Sentry.capture(e);
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
        }
    };

    private void initGui(){
        final Toolbar toolbar = findViewById(R.id.toolbar_header);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        progressBar = findViewById(R.id.pb_lolan_scanner_cb);
        progressBar.setVisibility(View.INVISIBLE);
        btnScan = findViewById(R.id.btn_scan_cancel_cb);
        btnSelect = findViewById(R.id.btn_scan_select_devices);

        final ListView listview = findViewById(R.id.lv_scanned_devices_checkboxes);
        mAdapter = new DeviceListMultiAdapter(this);
        listview.setAdapter(mAdapter);
        listview.setOnItemClickListener((parent, view, position, id) -> {
            mAdapter.addDeviceToUpdate((ExtendedBluetoothDevice) mAdapter.getItem(position));
        });

        btnScan.setOnClickListener(v -> {
            if(mIsScanning) {
                stopScan();
            } else {
                startScan();
            }
        });

        btnSelect.setOnClickListener(v -> {
            setDevicesToUpdate();
        });
    }

    private void setDevicesToUpdate(){
        devicesToUpdate = mAdapter.getDevicesToUpdate();
        if(devicesToUpdate.size() < 1) {
            runOnUiThread(() -> Toast.makeText(DeviceScannerMultiActivity.this, "No devices selected", Toast.LENGTH_SHORT).show());
        } else {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(DEVICES_TO_UPDATE, devicesToUpdate);
            Intent intent = new Intent(DeviceScannerMultiActivity.this, LolanMultiVariablesActivity.class);
            intent.putExtra(INTENT_BTLE_DEVICE_LIST, bundle);
            startActivity(intent);
        }
    }

    public void loadSharedData() {
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        userDevicesSet = sharedPreferences.getStringSet(USER_ID_TAGS, new ArraySet<String>());
        SCAN_DURATION_SHARED = sharedPreferences.getInt(SCAN_DURATION_PREFS, 7500);
    }

    private void addUserDevice(ScanResult sr, ScanRecord scanRecord){
        byte[] bytes;
        if (userDevicesSet.size() > 0) {
            bytes = scanRecord.getManufacturerSpecificData(MANUFACTURER_ID);
            int currentDeviceId = getDeviceId(bytes);
            for (String s : userDevicesSet) {
                int userDeviceID = Integer.parseInt(s);
                if (userDeviceID == currentDeviceId) {
                    sunstoneDevicesList.add(sr);
                }
            }
        }
    }

    private int getDeviceId(byte[] receivedManufcturerData) {
        int deviceId = 0;
        if(receivedManufcturerData != null && receivedManufcturerData.length == 11) {
            StringBuilder sbManufacturerDataBin = new StringBuilder();
            for (int i = 0; i < receivedManufcturerData.length; i++) {
                String currentNumBinary = String.format("%8s", Integer.toBinaryString(receivedManufcturerData[i] &0xff)).replace(' ', '0');
                sbManufacturerDataBin.append(currentNumBinary);
            }

            String idSubstring1 = sbManufacturerDataBin.substring(48, 56);
            String idSubstring2 = sbManufacturerDataBin.substring(56, 64);
            String idSubstring3 = sbManufacturerDataBin.substring(64, 72);
            String idSubstring4 = sbManufacturerDataBin.substring(72, 80);
            String idSubstring = idSubstring4 + idSubstring3 + idSubstring2 + idSubstring1;

            deviceId = Integer.parseInt(idSubstring, 2);
        }
        return deviceId;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scanner_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scanner_time_1:
                Toast.makeText(this, "Duration set to 5s", Toast.LENGTH_SHORT).show();
                sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, 5000);
                sharedPreferencesEditor.apply();
                SCAN_DURATION_SHARED = 5000;
                return true;
            case R.id.menu_scanner_time_2:
                Toast.makeText(this, "Duration set to 7s", Toast.LENGTH_SHORT).show();
                sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, 7000);
                sharedPreferencesEditor.apply();
                SCAN_DURATION_SHARED = 7000;
                return true;
            case R.id.menu_scanner_time_3:
                Toast.makeText(this, "Duration set to 10s", Toast.LENGTH_SHORT).show();
                sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, 10000);
                sharedPreferencesEditor.apply();
                SCAN_DURATION_SHARED = 10000;
                return true;
            case R.id.menu_scanner_time_4:
                Toast.makeText(this, "Duration set to 15s", Toast.LENGTH_SHORT).show();
                sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, 15000);
                sharedPreferencesEditor.apply();
                SCAN_DURATION_SHARED = 15000;
                return true;
            case R.id.menu_scanner_time_other:
                scanDurationAlert();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scanDurationAlert(){
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.alert_timer, null);
        EditText etScanDuration = dialogView.findViewById(R.id.et_alert_scan_duration);
        Button btnScanDurationSet = dialogView.findViewById(R.id.btn_alert_scan_duration_set);
        Button btnScanDurationOk = dialogView.findViewById(R.id.btn_alert_scan_duration_cancel);
        TextView tvReply = dialogView.findViewById(R.id.tv_alert_scan_duration_reply);

        btnScanDurationOk.setOnClickListener(view -> {
            dialogBuilder.dismiss();
        });

        btnScanDurationSet.setOnClickListener(view -> {
            if(etScanDuration.getText() != null
                    && !etScanDuration.getText().toString().equals("")){
                try {
                    int currentValueToSet = Integer.parseInt(etScanDuration.getText().toString()) * 1000;
                    sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, currentValueToSet);
                    sharedPreferencesEditor.apply();
                    SCAN_DURATION_SHARED = currentValueToSet;
                    tvReply.setText("Duration set to: " + (currentValueToSet / 1000) + "s" );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}

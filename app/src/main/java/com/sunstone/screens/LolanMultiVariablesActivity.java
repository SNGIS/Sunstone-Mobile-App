package com.sunstone.screens;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.sunstone.R;
import com.sunstone.adapter.DfuDevicesListAdapter;
import com.sunstone.lolan_db.LolanEntryAlertMulti;
import com.sunstone.adapter.CustomAdapterListViewSet;
import com.sunstone.model.DfuDeviceStatus;
import com.sunstone.model.LolanDbData;
import com.sunstone.model.LolanObjectToUpdate;
import com.sunstone.model.ExtendedBluetoothDevice;
import com.sunstone.utility.ParserUtils;
import com.sunstone.utility.SlipCborUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import co.nstant.in.cbor.CborException;
import io.sentry.Sentry;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static com.sunstone.utility.BluetoothUtils.CBOR_ERROR;
import static com.sunstone.utility.BluetoothUtils.COMPLETED;
import static com.sunstone.utility.BluetoothUtils.DELAY_LOW;
import static com.sunstone.utility.BluetoothUtils.DELAY_STANDARD;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_HIGH;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_LOW;
import static com.sunstone.utility.BluetoothUtils.ERROR;
import static com.sunstone.utility.BluetoothUtils.LOLAN_HEADER_ATTRIBUTES;
import static com.sunstone.utility.BluetoothUtils.LOLAN_HEADER_INFORM;
import static com.sunstone.utility.BluetoothUtils.RX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.SLIP_END_BYTE;
import static com.sunstone.utility.BluetoothUtils.STR_DISCONNECTED;
import static com.sunstone.utility.BluetoothUtils.STR_TIMEOUT;
import static com.sunstone.utility.BluetoothUtils.TX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.TX_DESCRIPTOR;
import static com.sunstone.utility.BluetoothUtils.UART_SERVICE_UUID;
import static com.sunstone.utility.ParserUtils.binToHex;
import static com.sunstone.utility.ParserUtils.hexStringToByteArray;
import static com.sunstone.utility.ParserUtils.parseDebug;
import static com.sunstone.utility.ParserUtils.reverseString;
import static com.sunstone.utility.PrefsHelper.CURRENT_LOLAN_HEADER;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;
import static com.sunstone.utility.SlipCborUtils.decodeCborWriteMulti;

public class LolanMultiVariablesActivity extends AppCompatActivity {
    private static final String TAG = "LolanMultiVariables";

    private static String INTENT_BTLE_DEVICE = "btle_device";
    private static String INTENT_BTLE_DEVICE_LIST = "btle_devices_list";
    private static String LOLAN_DEVICE_ADDRESS = "btle_devices_address";
    private static final String DEVICES_TO_UPDATE = "devices_to_update";

    private BluetoothGattCharacteristic txCharacteristic, rxCharacteristic;
    private BluetoothGattDescriptor txDescriptor;
    private BluetoothGattService uartService;

    private BluetoothManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice chosenDevice;
    private BluetoothGatt mGatt;

    private int gattDelay;
    private int gattInitialDelay;
    private int gattDelayWrite;

    private Context context;

    private ArrayList<ExtendedBluetoothDevice> devicesToUpdate;

    private TextView tvInfo, tvTagSettings, tvDw1000, tvControl, tvStatus, tvDeviceName, tvDeviceAddress;
    private LinearLayout llInfo, llTagSettings, llDw1000, llControl, llStatus, llHeaderSet, llHeaderReply;
    private Button btnStart;
    private TextView tvCurrentStatus;
    private TextView tvReply;
    private ProgressBar progressBar;
    private Button btnCancel, btnSet, btnReset;
    private ListView listViewLoLaN;
    private CustomAdapterListViewSet customAdapterListViewSet;

    private List<LolanEntryAlertMulti> lolanDbEntries = new ArrayList<>();
    private ArrayList<LolanObjectToUpdate> lolanObjectsToUpdateList = new ArrayList<>();

    private WindowManager.LayoutParams layoutParams;

    private ArrayList<String> newPacketValuesToUpdate;
    private ArrayList<int[]> newPacketPathsToUpdate;
    private List<Byte> replyBytes = new ArrayList<Byte>();
    private String deviceLolanName;
    private String deviceLolanGlobalId;
    private String lolanVariableValue;

    private int currentlyUpdatedDevice;
    private int deviceLolanAddress;

    private Handler guiHandler;

    private boolean isRecvBytesArrayFull;

    private Bundle bundle;

    private ListView replyListView;
    private DfuDevicesListAdapter replyListAdapter;
    private ArrayList<DfuDeviceStatus> replyStatusList;

    private int timeoutCounter = 0;
    private byte headerByteInform1;
    private byte headerByteInform2;

    @Override
    protected void onResume() {
        super.onResume();
        currentlyUpdatedDevice = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lolan_vars_multi);

        initGui();

        currentlyUpdatedDevice = 0;
        context = this;
        guiHandler = new Handler();

        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBluetoothAdapter = manager.getAdapter();
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 101);
        }

        getDevicesFromIntent();

        LolanDbData.fillDatabaseMulti(lolanDbEntries, findViewById(android.R.id.content).getRootView(), context);
        for(LolanEntryAlertMulti e : lolanDbEntries){
            e.getView().setOnClickListener(view -> {
                try {
                    e.showAlert(view.getContext(), devicesToUpdate, lolanObjectsToUpdateList);
                } catch ( InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
        }

    }

    private void getDevicesFromIntent(){
        if(getIntent().hasExtra(INTENT_BTLE_DEVICE_LIST)) {
            bundle = getIntent().getBundleExtra(INTENT_BTLE_DEVICE_LIST);
            devicesToUpdate = bundle.getParcelableArrayList(DEVICES_TO_UPDATE);

            String recvDevicesIDs = "Received devices:\n";
            for(ExtendedBluetoothDevice ebd : devicesToUpdate){
                recvDevicesIDs += ebd.global_id + "\n";
            }
            final String toastDevicesReceived = recvDevicesIDs;
            runOnUiThread(() -> Toast.makeText(LolanMultiVariablesActivity.this, toastDevicesReceived, Toast.LENGTH_LONG).show());
        } else {
            devicesToUpdate = new ArrayList<>();
            runOnUiThread(() -> Toast.makeText(LolanMultiVariablesActivity.this, "No devices received", Toast.LENGTH_LONG).show());
        }
    }

    private void initGui(){
        final Toolbar toolbar = findViewById(R.id.toolbar_header);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        tvInfo = findViewById(R.id.tv_lolanvar_info);
        tvTagSettings = findViewById(R.id.tv_lolanvar_tag_settings);
        tvDw1000 = findViewById(R.id.tv_lolanvar_dw1000settings);
        tvControl = findViewById(R.id.tv_lolanvar_control);
        tvStatus = findViewById(R.id.tv_lolanvar_status);
        tvDeviceName = findViewById(R.id.tv_lolanvars_device_name);
        tvDeviceAddress = findViewById(R.id.tv_lolanvars_device_address);

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

        btnStart = findViewById(R.id.btn_lolanvar_select);

        tvInfo.setOnClickListener(view -> switchView(llInfo));
        tvTagSettings.setOnClickListener(view -> switchView(llTagSettings));
        tvDw1000.setOnClickListener(view -> switchView(llDw1000));
        tvControl.setOnClickListener(view -> switchView(llControl));
        tvStatus.setOnClickListener(view -> switchView(llStatus));

        btnStart.setOnClickListener(v -> {
            if(devicesToUpdate.size() < 1){
                Toast.makeText(context, "Choose variables to update", Toast.LENGTH_SHORT).show();
            }
            else {
                showSetAlert();
            }
        });
    }

    private void switchView(View v) {
        if(v.getVisibility() == View.GONE) {
            v.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.GONE);
        }
    }

    private void showSetAlert(){

        final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.lolan_alert_set_multi_list, null);

        progressBar = dialogView.findViewById(R.id.pb_lolan_alert_set);
        progressBar.setVisibility(View.GONE);

        listViewLoLaN = dialogView.findViewById(R.id.lv_alert_lolanvar_set_listView);
        customAdapterListViewSet = new CustomAdapterListViewSet(lolanObjectsToUpdateList, context);
        listViewLoLaN.setAdapter(customAdapterListViewSet);
        llHeaderSet = dialogView.findViewById(R.id.ll_alert_lolanvar_set_header);

        replyStatusList = new ArrayList<>();
        replyListView = dialogView.findViewById(R.id.lv_alert_lolanvar_set_list_response);
        replyListAdapter = new DfuDevicesListAdapter(replyStatusList, context, R.layout.status_list_row);
        replyListView.setAdapter(replyListAdapter);
        llHeaderReply = dialogView.findViewById(R.id.ll_alert_lolanvar_reply_header);

        llHeaderReply.setVisibility(View.GONE);
        replyListView.setVisibility(View.GONE);


        btnCancel = dialogView.findViewById(R.id.btn_alert_lolan_set_cancel);
        btnSet = dialogView.findViewById(R.id.btn_alert_lolan_set_start);
        btnReset = dialogView.findViewById(R.id.btn_alert_lolan_set_reset);

        tvCurrentStatus = dialogView.findViewById(R.id.tv_lolanvar_set_currently_updated_device);
        tvReply = dialogView.findViewById(R.id.tv_alert_lolan_set_response_val);
        tvReply.setVisibility(View.GONE);

        gattInitialDelay = DELAY_VERY_HIGH;
        gattDelay = DELAY_LOW;
        gattDelayWrite = DELAY_LOW;

        tvStatus = dialogView.findViewById(R.id.tv_lolanvar_set_status);

        btnCancel.setOnClickListener(v -> {
            if(devicesToUpdate.size() > 0) {
                if (mGatt != null) {
                    mGatt.disconnect();
                    mGatt.close();
                }
            }
            if(tvCurrentStatus.getText().toString().contains(COMPLETED)
                    || tvCurrentStatus.getText().toString().contains(ERROR)
                    || tvCurrentStatus.getText().toString().contains("Timeout")){
                lolanObjectsToUpdateList.clear();
            }
            dialogBuilder.dismiss();
        });

        btnSet.setOnClickListener(v -> {
            if(devicesToUpdate.size() > 0 && lolanObjectsToUpdateList.size() > 0) {
                try {
                    replyStatusList.clear();
                    replyListAdapter.notifyDataSetChanged();
                    replyBytes.clear();
                    currentlyUpdatedDevice = 0;
                    isRecvBytesArrayFull = false;
                    runOnUiThread(() -> {
                        tvReply.setText("");
                        showProgressBar();
//                        switchLists();
                        hideSetList();
                    });
                    connectToDeviceToWrite(context, devicesToUpdate.get(currentlyUpdatedDevice).device);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(() -> Toast.makeText(context, "Select variables to update", Toast.LENGTH_SHORT).show());
            }
        });

        btnReset.setOnClickListener(v -> {
            lolanObjectsToUpdateList.clear();
            customAdapterListViewSet.notifyDataSetChanged();
            replyStatusList.clear();
            replyListAdapter.notifyDataSetChanged();
            replyBytes.clear();
            tvReply.setText("");
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }


    private void connectToDeviceToWrite(Context context, BluetoothDevice lolanDevice) throws InterruptedException {
        guiHandler.post(() -> {
            disableButtons();
            showProgressBar();
        });
        if (mGatt == null) {
            mGatt = lolanDevice.connectGatt(context, false, gattCallbackWrite, BluetoothDevice.TRANSPORT_LE);
        } else {
            mGatt.disconnect();
            mGatt.close();
            Thread.sleep(400);
            mGatt = lolanDevice.connectGatt(context, false, gattCallbackWrite, BluetoothDevice.TRANSPORT_LE);
        }
    }

    private void gattTimeout(BluetoothGatt gatt, int status, int newState){
        guiHandler.post(() -> {
            tvCurrentStatus.setText(getString(R.string.str_current_status_timeout,
                    devicesToUpdate.get(currentlyUpdatedDevice).name,
                    devicesToUpdate.get(currentlyUpdatedDevice).global_id,
                    timeoutCounter));
        });

        timeoutCounter++;
        if(timeoutCounter > 2){
            try {
                timeOut(gatt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private final BluetoothGattCallback gattCallbackWrite = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if(status == GATT_SUCCESS) {
                        gattWriteConnected(gatt);
                    } else {
                        gattWriteReconnect(gatt);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (status == GATT_SUCCESS) {
                        gattWriteDisconnected(gatt);
                        break;
                    } else {
                        gattWriteReconnect(gatt);
                        break;
                    }

                default:
                    if(status == 19) {
                        gattTimeout(gatt, status, newState);
                        break;
                    } else {
                        gatt.disconnect();
                        break;
                    }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if(status == GATT_SUCCESS){
                gattWriteMtuChanged(gatt);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == GATT_SUCCESS) {
                gattWriteServicesDiscovered(gatt);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == GATT_SUCCESS) {
                gattWriteDescriptorWrote(gatt);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(!isRecvBytesArrayFull) {
                try {
                    Thread.sleep(DELAY_STANDARD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (status == GATT_SUCCESS) {
                    gattCharacteristicWrote(gatt);
                } else {
                    gattWriteReconnect(gatt);
                }
            } else {
                try {
                    Thread.sleep(300);
                    gatt.disconnect();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == GATT_SUCCESS){
                gattCharacteristicRead(gatt, characteristic);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            try {
                Thread.sleep(gattDelayWrite);
                onCharacteristicWrite(gatt, characteristic, 0);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };


    private void gattWriteConnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
//            tvStatus.setText("CONNECTED TO " + devicesToUpdate.get(currentlyUpdatedDevice).device.getAddress());
            tvStatus.setText(getString(R.string.str_connected_to, devicesToUpdate.get(currentlyUpdatedDevice).device.getAddress()));
            tvStatus.setTextColor(Color.rgb(0,0,0));
//            tvCurrentStatus.setText("UPDATING " + devicesToUpdate.get(currentlyUpdatedDevice).global_id);
            tvCurrentStatus.setText(getString(R.string.str_updating, devicesToUpdate.get(currentlyUpdatedDevice).global_id));
            tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
            showProgressBar();
            disableButtons();
        });
        try {
            isRecvBytesArrayFull = false;
            Thread.sleep(1200);
            gatt.requestMtu(131);
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void gattWriteDisconnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            // NO REPLY -> RECONNECT
            if (tvCurrentStatus != null &&
                    tvCurrentStatus.getText().toString().contains(devicesToUpdate.get(currentlyUpdatedDevice).global_id) &&
                    (!tvCurrentStatus.getText().toString().contains("COMPLETED")) && (!tvCurrentStatus.getText().toString().contains("ERROR"))) {
                tvStatus.setText(getString(R.string.str_refreshing));
                showProgressBar();
                disableButtons();
                try {
                    connectToDeviceToWrite(context, devicesToUpdate.get(currentlyUpdatedDevice).device);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // CONNECT TO NEXT DEVICE
            } else if (tvCurrentStatus != null
                    && tvCurrentStatus.getText().toString().contains(devicesToUpdate.get(currentlyUpdatedDevice).global_id)
                    && (tvCurrentStatus.getText().toString().contains("COMPLETED") || tvCurrentStatus.getText().toString().contains("ERROR") || tvCurrentStatus.getText().toString().contains("Timeout"))
                    && (currentlyUpdatedDevice < devicesToUpdate.size() - 1)) {
                try {
                    connectToDeviceToWrite(context, devicesToUpdate.get(++currentlyUpdatedDevice).device);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        if ((currentlyUpdatedDevice == devicesToUpdate.size() - 1) &&
                !tvStatus.getText().toString().contains("Refreshing...")) {
            guiHandler.post(() -> {
                tvStatus.setText(STR_DISCONNECTED);
                hideProgressBar();
                enableButtons();
            });
        }
        try {
            mGatt.disconnect();
            Thread.sleep(300);
            mGatt.close();
            mGatt = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void timeOut(BluetoothGatt gatt) throws InterruptedException{
        guiHandler.post(() -> {
            tvStatus.setText(STR_DISCONNECTED);
            tvCurrentStatus.setText(STR_TIMEOUT);
            replyStatusList.add(new DfuDeviceStatus(devicesToUpdate.get(currentlyUpdatedDevice).name + " "
                    + devicesToUpdate.get(currentlyUpdatedDevice).global_id, "ERROR", "Timeout"));
            replyListAdapter.notifyDataSetChanged();
            timeoutCounter = 0;
            if (currentlyUpdatedDevice < devicesToUpdate.size() - 1) {
                try {
                    connectToDeviceToWrite(context, devicesToUpdate.get(++currentlyUpdatedDevice).device);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                gatt.disconnect();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gatt.close();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mGatt.disconnect();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mGatt.close();
                enableButtons();
                hideProgressBar();
            }
        });
    }

    private void gattWriteMtuChanged(BluetoothGatt gatt){
        try {
            Thread.sleep(DELAY_LOW);
            gatt.discoverServices();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void gattWriteReconnect(BluetoothGatt gatt){
        try {
            gatt.disconnect();
            Thread.sleep(DELAY_LOW);
            gatt.close();
            Thread.sleep(DELAY_LOW);
            connectToDeviceToWrite(context, devicesToUpdate.get(currentlyUpdatedDevice).device);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void gattWriteServicesDiscovered(BluetoothGatt gatt){
        try {
            uartService = gatt.getService(UART_SERVICE_UUID);
            Thread.sleep(DELAY_VERY_LOW);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(uartService != null) {
            txCharacteristic = gatt.getService(UART_SERVICE_UUID).getCharacteristic(TX_CHARACTERISTIC);
            gatt.setCharacteristicNotification(txCharacteristic, true);
            txCharacteristic.setWriteType(WRITE_TYPE_DEFAULT);
            rxCharacteristic = gatt.getService(UART_SERVICE_UUID).getCharacteristic(RX_CHARACTERISTIC);
            txDescriptor = txCharacteristic.getDescriptor(TX_DESCRIPTOR);
            txDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(txDescriptor);
        } else {
            try {
                connectToDeviceToWrite(context, devicesToUpdate.get(currentlyUpdatedDevice).device);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void gattWriteDescriptorWrote(BluetoothGatt gatt){
        String stringHeXDataPacket = "";
        try {
            deviceLolanAddress = Integer.parseInt(devicesToUpdate.get(currentlyUpdatedDevice).global_id) % 10000;
            newPacketValuesToUpdate = new ArrayList<>();
            newPacketPathsToUpdate = new ArrayList<>();
            for (LolanObjectToUpdate l : lolanObjectsToUpdateList) {
                newPacketValuesToUpdate.add(l.getValueToUpdate());
                newPacketPathsToUpdate.add(l.getPathToUpdate());
            }

            String generateSlipPacketSet = SlipCborUtils.generateSlipPacketSetNew(newPacketPathsToUpdate, newPacketValuesToUpdate, deviceLolanAddress, context);
            if(!generateSlipPacketSet.equals(CBOR_ERROR)) {
                stringHeXDataPacket = generateSlipPacketSet;
                byte[] dataPacketByteArray = ParserUtils.hexStringToByteArray(stringHeXDataPacket);
                rxCharacteristic.setValue(dataPacketByteArray);
                rxCharacteristic.setWriteType(WRITE_TYPE_DEFAULT);
                try {
                    Thread.sleep(DELAY_STANDARD);
                    gatt.writeCharacteristic(rxCharacteristic);
                    Thread.sleep(DELAY_STANDARD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                guiHandler.post(() -> tvCurrentStatus.setText(CBOR_ERROR));
                try {
                    gatt.disconnect();
                    Thread.sleep(DELAY_LOW);
                    gatt.close();
                    Thread.sleep(DELAY_LOW);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }

        } catch (CborException e) {
            Sentry.capture(e);
            e.printStackTrace();
        }
    }

    private void gattCharacteristicWrote(BluetoothGatt gatt){
        Log.d(TAG, "gattCharacteristicWrote: ");

        if (txCharacteristic.getValue() != null) {
            checkRecvPacket(txCharacteristic, gatt);
        } else {
            waitForGatt(gatt, txCharacteristic, 500);
        }
    }

    private void gattCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        if(characteristic.getValue() != null && characteristic.getValue().length > 0) {
            checkRecvPacket(characteristic, gatt);
        } else {
            waitForGatt(gatt, txCharacteristic, 500);
        }
    }


    private void checkRecvPacket(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt){

        //1. 0274...C0
        if (characteristic.getValue()[0] == LOLAN_HEADER_INFORM && characteristic.getValue()[1] == LOLAN_HEADER_ATTRIBUTES
                && characteristic.getValue()[characteristic.getValue().length - 1] == SLIP_END_BYTE) {
            for (byte b : characteristic.getValue()) {
                replyBytes.add(b);
            }
            try {
                replyBytes = SlipCborUtils.slipDecodeData(replyBytes);

                deviceLolanAddress = Integer.parseInt(devicesToUpdate.get(currentlyUpdatedDevice).global_id) % 10000;
                deviceLolanName = devicesToUpdate.get(currentlyUpdatedDevice).name;
                deviceLolanGlobalId = devicesToUpdate.get(currentlyUpdatedDevice).global_id;

                lolanVariableValue = decodeCborWriteMulti(gatt, replyBytes, guiHandler, tvReply,
                        tvCurrentStatus, deviceLolanName, deviceLolanGlobalId);

                HashMap<String, String> statusMap = SlipCborUtils.statusMap;
                Iterator<Map.Entry<String, String>> entrySet = statusMap.entrySet().iterator();

                while (entrySet.hasNext()) {
                    java.util.Map.Entry<String, String> entry = entrySet.next();
                    replyStatusList.add(new DfuDeviceStatus(deviceLolanName + " " + deviceLolanGlobalId, entry.getKey(), entry.getValue()));
                }

                isRecvBytesArrayFull = true;
                replyBytes.clear();

                runOnUiThread(() -> {
                    replyListAdapter.notifyDataSetChanged();
                });

                Thread.sleep(DELAY_LOW);
                gatt.disconnect();
            } catch (CborException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
            // 0274... =>
            // if replyBytes is empty => ADD & WAIT
            // else => WAIT
        } else if(characteristic.getValue()[0] == LOLAN_HEADER_INFORM && characteristic.getValue()[1] == LOLAN_HEADER_ATTRIBUTES
                && characteristic.getValue()[characteristic.getValue().length - 1] != SLIP_END_BYTE){
            if(replyBytes.isEmpty()){
                for (byte b : characteristic.getValue()) {
                    replyBytes.add(b);
                }
                waitForGatt(gatt, characteristic, 500);
            } else {
                waitForGatt(gatt, characteristic, 500);
            }
            // ...C0 =>
            // if replyBytes(0) == LOLAN_HEADER_INFORM && replyBytes(1) ==  LOLAN_HEADER_ATTRIBUTES => ADD & PROCESS
            // else => WAIT
        } else if ( characteristic.getValue()[0] != LOLAN_HEADER_INFORM && characteristic.getValue()[0] != LOLAN_HEADER_ATTRIBUTES
                && characteristic.getValue()[characteristic.getValue().length - 1] == SLIP_END_BYTE){
            if(replyBytes.size() > 2 && replyBytes.get(0) == LOLAN_HEADER_INFORM && replyBytes.get(1) == LOLAN_HEADER_ATTRIBUTES){
                for (byte b : txCharacteristic.getValue()) {
                    replyBytes.add(b);
                }
                try {
                    replyBytes = SlipCborUtils.slipDecodeData(replyBytes);

                    deviceLolanAddress = Integer.parseInt(devicesToUpdate.get(currentlyUpdatedDevice).global_id) % 10000;
                    deviceLolanName = devicesToUpdate.get(currentlyUpdatedDevice).name;
                    deviceLolanGlobalId = devicesToUpdate.get(currentlyUpdatedDevice).global_id;

                    lolanVariableValue = decodeCborWriteMulti(gatt, replyBytes, guiHandler, tvReply,
                            tvCurrentStatus, deviceLolanName, deviceLolanGlobalId);

                    HashMap<String, String> statusMap = SlipCborUtils.statusMap;
                    Iterator<Map.Entry<String, String>> entrySet = statusMap.entrySet().iterator();
                    while (entrySet.hasNext()) {
                        java.util.Map.Entry<String, String> entry = entrySet.next();
                        replyStatusList.add(new DfuDeviceStatus(deviceLolanName + " " + deviceLolanGlobalId, entry.getKey(), entry.getValue()));
                    }

                    isRecvBytesArrayFull = true;
                    replyBytes.clear();

                    runOnUiThread(() -> {
                        replyListAdapter.notifyDataSetChanged();
                    });

                    Thread.sleep(DELAY_LOW);
                    gatt.disconnect();
                } catch (CborException | IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                waitForGatt(gatt, txCharacteristic, 500);
            }

            // ... => ADD & WAIT
        } else if(replyBytes.size() > 2 && replyBytes.get(0) == LOLAN_HEADER_INFORM && replyBytes.get(1) == LOLAN_HEADER_ATTRIBUTES
                && characteristic.getValue()[0] != LOLAN_HEADER_INFORM && characteristic.getValue()[0] != LOLAN_HEADER_ATTRIBUTES
                && characteristic.getValue()[characteristic.getValue().length - 1] != SLIP_END_BYTE){
            for (byte b : characteristic.getValue()) {
                replyBytes.add(b);
            }
            waitForGatt(gatt, characteristic, 500);
        }
    }

    private void enableButtons(){
        if(btnCancel != null){
            btnCancel.setEnabled(true);
            btnCancel.setText(getString(R.string.str_ok));
        }
        if(btnSet != null){
            btnSet.setEnabled(true);
        }
        if(btnReset != null){
            btnReset.setEnabled(true);
        }
    }

    private void disableButtons(){
        if(btnCancel != null){
//            btnCancel.setEnabled(false);
            btnCancel.setText(getString(R.string.str_stop));
        }
        if(btnSet != null){
            btnSet.setEnabled(false);
        }
        if(btnReset != null){
            btnReset.setEnabled(false);
        }
    }


    private void showProgressBar(){
        if(progressBar != null){
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar(){
        if(progressBar != null){
            progressBar.setVisibility(View.GONE);
        }
    }

    private void hideSetList(){
        runOnUiThread(() -> {
            if (llHeaderSet.getVisibility() == View.VISIBLE) {
                llHeaderSet.setVisibility(View.GONE);
                listViewLoLaN.setVisibility(View.GONE);
                llHeaderReply.setVisibility(View.VISIBLE);
                replyListView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showSetList(){
        runOnUiThread(() -> {
            if (llHeaderSet.getVisibility() != View.VISIBLE) {
                llHeaderSet.setVisibility(View.VISIBLE);
                listViewLoLaN.setVisibility(View.VISIBLE);
                llHeaderReply.setVisibility(View.GONE);
                replyListView.setVisibility(View.GONE);
            }
        });
    }

    private void switchLists(){
        runOnUiThread(() ->{
            if(llHeaderSet.getVisibility() == View.VISIBLE){
                llHeaderSet.setVisibility(View.GONE);
                listViewLoLaN.setVisibility(View.GONE);
                llHeaderReply.setVisibility(View.VISIBLE);
                replyListView.setVisibility(View.VISIBLE);
            } else {
                llHeaderSet.setVisibility(View.VISIBLE);
                listViewLoLaN.setVisibility(View.VISIBLE);
                llHeaderReply.setVisibility(View.GONE);
                replyListView.setVisibility(View.GONE);
            }
        });
    }




    private void waitForGatt(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int timeMS){
        try {
            gatt.readCharacteristic(characteristic);
            Thread.sleep(timeMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


// TODO: LoLaN header configuration
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.lolan_menu, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()){
//            case R.id.action_config_header:
//                startActivity(new Intent(LolanMultiVariablesActivity.this, LolanHeaderActivity.class));
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        lolanObjectsToUpdateList.clear();
        devicesToUpdate.clear();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

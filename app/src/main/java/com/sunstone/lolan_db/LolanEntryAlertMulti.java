package com.sunstone.lolan_db;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.sunstone.R;
import com.sunstone.adapter.CutsomAdapterListViewRead;
import com.sunstone.model.LolanObjectToUpdate;
import com.sunstone.model.LolanReadValue;
import com.sunstone.model.ExtendedBluetoothDevice;
import com.sunstone.utility.ParserUtils;
import com.sunstone.utility.SlipCborUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.nstant.in.cbor.CborException;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static com.sunstone.utility.BluetoothUtils.DELAY_LOW;
import static com.sunstone.utility.BluetoothUtils.DELAY_STANDARD;
import static com.sunstone.utility.BluetoothUtils.DELAY_SUPER_HIGH;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_HIGH;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_LOW;
import static com.sunstone.utility.BluetoothUtils.RX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.SLIP_END_BYTE;
import static com.sunstone.utility.BluetoothUtils.STR_CONNECTED;
import static com.sunstone.utility.BluetoothUtils.STR_DISCONNECTED;
import static com.sunstone.utility.BluetoothUtils.STR_OK;
import static com.sunstone.utility.BluetoothUtils.STR_REFRESHING;
import static com.sunstone.utility.BluetoothUtils.STR_STOP;
import static com.sunstone.utility.BluetoothUtils.TX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.TX_DESCRIPTOR;
import static com.sunstone.utility.BluetoothUtils.UART_SERVICE_UUID;
import static com.sunstone.utility.ParserUtils.binToHex;
import static com.sunstone.utility.ParserUtils.hexStringToByteArray;
import static com.sunstone.utility.ParserUtils.reverseString;
import static com.sunstone.utility.PrefsHelper.CURRENT_LOLAN_HEADER;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;
import static com.sunstone.utility.SlipCborUtils.decodeCborReadMulti;

public class LolanEntryAlertMulti {
    private static final String TAG = "LolanEntryAlertMulti";

    private final String CBOR_ERROR = "ERROR OCCURED";
    private final String VALUE_OVER_THE_RANGE = "Value over the range";

    private BluetoothGattCharacteristic txCharacteristic, rxCharacteristic;
    private BluetoothGattDescriptor txDescriptor;
    private BluetoothGattService uartService;
    
    private BluetoothGatt mGatt;

    private Handler mHandler;
    private Handler guiHandler;

    private int gattInitialDelay;
    private int getGattDelayWrite;
    
    private Context context;

    private String currentValueToSet;

    private ArrayList<ExtendedBluetoothDevice> devicesToUpdateArrayList;
    private List<Byte> fullByteList = new ArrayList<Byte>();
    private int currentlyUpdatedDevice;

    private ArrayList<LolanObjectToUpdate> objectsToUpdate;
    private CutsomAdapterListViewRead customAdapterListViewRead;
    private ArrayList<LolanReadValue> lolanReadValueArrayList = new ArrayList<>();

    private int packetCounter = 1;

    private TextView tvLolanVal, tvStatus, tvResponse, tvInfoContent;
    private EditText etLolanVar;
    private ProgressBar progressBar;
    private LinearLayout llInfo;
    private Button btnOk, btnReconnect, btnSet, btnRead;

    private String varType;
    private int readOrWrite;
    private String varPath;
    private int[] pathInt;
    private View view;
    private int deviceLolanAddress;
    private CharSequence lolanVariableInfo;

    private boolean isReadingStopped;
    private int timeoutCounter = 0;

    /**
     * @param varType: uint8, uint16, uint32, int16, data80, str23,
     * @param readOrWrite: 0 - Read only, 1 - Write only, 2 - Read & write
     * @param varPath: LoLaN db path, ie "5,3,1" - path [5,3,1] STATUS/lastaccel/x (int16)
     * @param pathInt: LoLaN db path, ie [5,3,1] - path [5,3,1] STATUS/lastaccel/x (int16)
     * @param view: TextView that initiated alert
     */

    public LolanEntryAlertMulti(String varType, int readOrWrite, String varPath,
                                int[] pathInt, View view, CharSequence variableInfo) {
        this.varType = varType;
        this.readOrWrite = readOrWrite;
        this.varPath = varPath;
        this.pathInt = pathInt;
        this.view = view;
        this.lolanVariableInfo = variableInfo;
    }

    private int getReadOrWrite() {
        return readOrWrite;
    }

    private String getVarType() {
        return varType;
    }

    private int[] getPathInt() {
        return pathInt;
    }

    public View getView() {
        return view;
    }

    public void setView(View v) {
        this.view = v;
    }


    public void showAlert(Context alertContext,
                          ArrayList<ExtendedBluetoothDevice> devicesToUpdate,
                          ArrayList<LolanObjectToUpdate> objectsToUpdate) throws InterruptedException {
        
        this.context = alertContext;
        this.devicesToUpdateArrayList = devicesToUpdate;
        this.currentlyUpdatedDevice = 0;
        this.objectsToUpdate = objectsToUpdate;

        mHandler = new Handler();
        guiHandler = new Handler();

        gattInitialDelay = DELAY_VERY_HIGH;
        getGattDelayWrite = DELAY_VERY_LOW;


        // READ ONLY
        if(this.getReadOrWrite()== 0) {
            final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.lolan_alert_r_multi_list, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_lolanvar_title);
            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());

            llInfo = dialogView.findViewById(R.id.ll_alert_lolan_info_r);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_lolan_info_content_r);
            tvInfoContent.setText(lolanVariableInfo);
            tvStatus = dialogView.findViewById(R.id.tv_lolanvar_connection_status);
            progressBar = dialogView.findViewById(R.id.pb_lolan_alert_r);

            ListView listViewRead = dialogView.findViewById(R.id.lv_alert_lolanvar_r_listView);
            customAdapterListViewRead = new CutsomAdapterListViewRead(lolanReadValueArrayList, context);
            listViewRead.setAdapter(customAdapterListViewRead);

            btnReconnect = dialogView.findViewById(R.id.btn_alert_lolan_reconnect_r);
            btnOk = dialogView.findViewById(R.id.btn_alert_lolan_ok);

            showProgressBar();
            disableButtons();
            connectToDevice(context, devicesToUpdateArrayList.get(0).device);

            btnOk.setOnClickListener(view -> {
                if(mGatt != null){
                    mGatt.disconnect();
                    mGatt.close();
                }
                guiHandler.post(lolanReadValueArrayList::clear);
                guiHandler.post(customAdapterListViewRead::notifyDataSetChanged);
                dialogBuilder.dismiss();
            });

            btnReconnect.setOnClickListener(v -> {
                try {
                    guiHandler.post(() -> {
                        lolanReadValueArrayList.clear();
                        customAdapterListViewRead.notifyDataSetChanged();
                        currentlyUpdatedDevice = 0;
                        disableButtons();
                        showProgressBar();
                    });
                    connectToDevice(context, devicesToUpdate.get(0).device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            dialogBuilder.setView(dialogView);
            dialogBuilder.show();

        } else if (this.getReadOrWrite()== 1) {
            //WRITE ONLY
            final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.lolan_alert_w_multi, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_lolanvar_w_title);
            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());

            llInfo = dialogView.findViewById(R.id.ll_alert_lolan_info_w);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_lolan_info_content_w);
            tvInfoContent.setText(lolanVariableInfo);
            tvLolanVal = dialogView.findViewById(R.id.tv_alert_lolan_read_val_w);
            tvLolanVal.setText("");
            etLolanVar= dialogView.findViewById(R.id.et_alert_w_setval);
            etLolanVar.setHint(this.getVarType());
            progressBar = dialogView.findViewById(R.id.pb_lolan_alert_w);
            hideProgressBar();

            tvStatus = dialogView.findViewById(R.id.tv_lolanvar_connection_status);
            tvResponse = dialogView.findViewById(R.id.tv_alert_lolan_w_response_val);
            btnSet = dialogView.findViewById(R.id.btn_alert_lolan_w_set);
            btnOk = dialogView.findViewById(R.id.btn_alert_lolan_w_cancel);

            btnOk.setOnClickListener(view -> {
                if(mGatt != null){
                    mGatt.disconnect();
                    mGatt.close();
                }
                dialogBuilder.dismiss();
            });

            btnSet.setOnClickListener(view -> {
                    if(etLolanVar.getText() != null
                            && !etLolanVar.getText().toString().equals("") && verifyValueToSet(etLolanVar.getText().toString())) {
                    currentlyUpdatedDevice = 0;
                    currentValueToSet = etLolanVar.getText().toString();

                    for(int i=0; i<objectsToUpdate.size(); i++){
                        if(objectsToUpdate.get(i).getlolanPathName().equals(varPath)){
                            objectsToUpdate.remove(objectsToUpdate.get(i));
                        }
                    }
                    objectsToUpdate.add(new LolanObjectToUpdate(pathInt, etLolanVar.getText().toString(), varPath));
                    guiHandler.post(() -> tvResponse.setText(context.getString(R.string.str_added_to_set, varPath, etLolanVar.getText().toString())));
                }
            });

            dialogBuilder.setView(dialogView);
            dialogBuilder.show();

        } else if (this.getReadOrWrite()== 2) {
            // READ AND WRITE
            final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.lolan_alert_rw_multi_s, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_rw_lolanvar_title);

            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());
            progressBar = dialogView.findViewById(R.id.pb_lolan_alert_rw);
            progressBar.setVisibility(View.INVISIBLE);

            ListView listViewRead = dialogView.findViewById(R.id.lv_alert_lolanvar_rw_listView);
            customAdapterListViewRead = new CutsomAdapterListViewRead(lolanReadValueArrayList, context);
            listViewRead.setAdapter(customAdapterListViewRead);
            listViewRead.setVisibility(View.GONE);

            LinearLayout llReadVals = dialogView.findViewById(R.id.ll_alert_lolanvar_rw_vals);
            llReadVals.setVisibility(View.GONE);

            tvStatus = dialogView.findViewById(R.id.tv_lolanvar_rw_connection_status);
            llInfo = dialogView.findViewById(R.id.ll_alert_lolan_info_rw);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_lolan_info_content_rw);

            tvInfoContent.setText(lolanVariableInfo);
            tvInfoContent.setMovementMethod(new ScrollingMovementMethod());

            etLolanVar = dialogView.findViewById(R.id.et_alert_lolan_rw);
            etLolanVar.setHint(this.getVarType());

            tvResponse = dialogView.findViewById(R.id.tv_alert_lolan_rw_response_val);

            btnSet = dialogView.findViewById(R.id.btn_alert_lolan_set_rw);
            btnOk = dialogView.findViewById(R.id.btn_alert_lolan_cancel_rw);
            btnRead = dialogView.findViewById(R.id.btn_alert_lolan_read_rw);

            showProgressBar();
            disableButtons();
            llReadVals.setVisibility(View.VISIBLE);
            listViewRead.setVisibility(View.VISIBLE);

            connectToDevice(context, devicesToUpdateArrayList.get(0).device);

            btnRead.setOnClickListener(view -> {
                currentlyUpdatedDevice = 0;
                lolanReadValueArrayList.clear();
                customAdapterListViewRead.notifyDataSetChanged();

                isReadingStopped = false;
                showProgressBar();
                disableButtons();
                llReadVals.setVisibility(View.VISIBLE);
                listViewRead.setVisibility(View.VISIBLE);
                try {
                    connectToDevice(context, devicesToUpdate.get(0).device);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            btnOk.setOnClickListener(view -> {
                stopReading(dialogBuilder);
            });

            btnSet.setOnClickListener(view -> {
                if (etLolanVar.getText() != null
                        && !etLolanVar.getText().toString().equals("") && verifyValueToSet(etLolanVar.getText().toString())) {
                    for(int i=0; i<objectsToUpdate.size(); i++){
                        if(objectsToUpdate.get(i).getlolanPathName().equals(varPath)){
                            objectsToUpdate.remove(objectsToUpdate.get(i));
                        }
                    }
                    objectsToUpdate.add(new LolanObjectToUpdate(pathInt, etLolanVar.getText().toString(), varPath));
                    guiHandler.post(() -> tvResponse.setText(context.getString(R.string.str_added_to_set, varPath, etLolanVar.getText().toString())));

                }
            });
            dialogBuilder.setView(dialogView);
            dialogBuilder.show();
        }
    }


    private void connectToDevice(Context context, BluetoothDevice lolanDevice) throws InterruptedException {
        if (mGatt == null) {
            mGatt = lolanDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mGatt.disconnect();
            mGatt.close();
            Thread.sleep(DELAY_STANDARD);
            mGatt = lolanDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gattReadConnected(gatt);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                        gattReadDisconnected(gatt);
                        break;
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
            if(status == GATT_SUCCESS) {
                gattReadMtuChanged(gatt, mtu);
            } else {
                gattReadReconnect(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == GATT_SUCCESS) {
                gattReadServicesDiscovered(gatt);
            } else {
                gattReadReconnect(gatt);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == GATT_SUCCESS) {
                gattReadDescriptorWrote(gatt);
            } else {
                gattReadReconnect(gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead: " + status);
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            gattReadCharacteristicChanged(gatt, characteristic);
        }
    };

    private void gattReadReconnect(BluetoothGatt gatt){
        mHandler.post(gatt::disconnect);
        mHandler.postDelayed(gatt::close, DELAY_LOW);
        mHandler.postDelayed(() -> {
            try {
                connectToDevice(context, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, DELAY_LOW);
    }

    private void gattTimeout(BluetoothGatt gatt, int status, int newState){
        guiHandler.post(() -> {
            tvStatus.setText(context.getString(R.string.str_current_status_timeout,
                    devicesToUpdateArrayList.get(currentlyUpdatedDevice).name,
                    devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id,
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

    private void timeOut(BluetoothGatt gatt) throws InterruptedException{
        guiHandler.post(() -> {
            tvStatus.setText("DISCONNECTED: TIMEOUT");
            lolanReadValueArrayList.add(new LolanReadValue(devicesToUpdateArrayList.get(currentlyUpdatedDevice).name,
                    devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id, "Timeout"));
            customAdapterListViewRead.notifyDataSetChanged();
            timeoutCounter = 0;
            if (currentlyUpdatedDevice < devicesToUpdateArrayList.size() - 1) {
                try {
                    connectToDevice(context, devicesToUpdateArrayList.get(++currentlyUpdatedDevice).device);
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

    private void gattReadConnected(BluetoothGatt gatt){
        if(!isReadingStopped) {
            guiHandler.post(() -> {
                tvStatus.setText(context.getString(R.string.str_status_connected, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device.getAddress()));
                showProgressBar();
                disableButtons();
            });
            mHandler.postDelayed(gatt::discoverServices, DELAY_VERY_HIGH);
        } else {
            guiHandler.post(() -> {
               tvStatus.setText(STR_DISCONNECTED);
            });
        }
    }

    private void gattReadDisconnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            if(isReadingStopped){
                hideProgressBar();
                enableButtons();
                tvStatus.setText(STR_DISCONNECTED);
                disconnectGatt();
            }
            if(lolanReadValueArrayList != null && lolanReadValueArrayList.size() < 1
                    && (currentlyUpdatedDevice < devicesToUpdateArrayList.size() - 1)){
                tvStatus.setText(STR_REFRESHING);
                showProgressBar();
                disableButtons();
                mHandler.postDelayed(gatt::disconnect, DELAY_LOW);
                mHandler.postDelayed(() -> {
                    gatt.close();
                    try {
                        connectToDevice(context, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, DELAY_VERY_HIGH);
            } else if(lolanReadValueArrayList != null
                && isAlreadyRead(devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id)
                && (currentlyUpdatedDevice < devicesToUpdateArrayList.size() - 1)){
                mHandler.post(() -> {
                    try {
                        connectToDevice(context, devicesToUpdateArrayList.get(++currentlyUpdatedDevice).device);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else if (lolanReadValueArrayList != null
                    && (!isAlreadyRead(devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id)
                    ||  (currentlyUpdatedDevice < devicesToUpdateArrayList.size() - 1))){
                tvStatus.setText(context.getString(R.string.str_status_refreshing, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device.getAddress()));
                mHandler.post(() -> {
                    try {
                        connectToDevice(context, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            if (currentlyUpdatedDevice == (devicesToUpdateArrayList.size() - 1) &&
                    !tvStatus.getText().toString().contains(STR_REFRESHING)) {
                hideProgressBar();
                enableButtons();
                tvStatus.setText(STR_DISCONNECTED);
            }
            disconnectGatt();
        });
    }

    private void gattReadMtuChanged(BluetoothGatt gatt, int mtu){
        try {
            mHandler.postDelayed(gatt::discoverServices, 600);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gattReadServicesDiscovered(BluetoothGatt gatt){
        try {
            mHandler.postDelayed(() -> {
                try {
                    uartService = gatt.getService(UART_SERVICE_UUID);
                    Thread.sleep(100);
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
                        connectToDevice(context, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, DELAY_LOW);

        } catch (NullPointerException e) {
            e.printStackTrace();
            try {
                connectToDevice(context, devicesToUpdateArrayList.get(currentlyUpdatedDevice).device);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void gattReadDescriptorWrote(BluetoothGatt gatt){
        StringBuilder hexDataPacket = new StringBuilder();
        try {
            deviceLolanAddress = Integer.parseInt(devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id) % 10000;
            String generateSlipPacketGet = SlipCborUtils.generateSlipPacketGet(getPathInt(), deviceLolanAddress, ++packetCounter, context);
            if (!generateSlipPacketGet.equals(CBOR_ERROR)) {
                hexDataPacket.append(generateSlipPacketGet);
                byte[] dataPacketByteArray = ParserUtils.hexStringToByteArray(hexDataPacket.toString());
                rxCharacteristic.setValue(dataPacketByteArray);
            } else {
                guiHandler.post(() -> tvLolanVal.setText(CBOR_ERROR));
                mHandler.post(gatt::disconnect);
            }
        } catch (CborException e) {
            e.printStackTrace();
        }

        try {
            mHandler.postDelayed(() -> gatt.writeCharacteristic(rxCharacteristic), 200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gattReadCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        try {
            for(byte b : characteristic.getValue()) {
                fullByteList.add(b);
            }
            if(fullByteList.size() > 0 && fullByteList.get(fullByteList.size()-1) == SLIP_END_BYTE){
                fullByteList = SlipCborUtils.slipDecodeData(fullByteList);
                deviceLolanAddress = Integer.parseInt(devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id) % 10000;
                String deviceLolanName = devicesToUpdateArrayList.get(currentlyUpdatedDevice).name;
                String deviceLolanGlobalId = devicesToUpdateArrayList.get(currentlyUpdatedDevice).global_id;
                String decodedCborValue = decodeCborReadMulti(fullByteList);
                LolanReadValue lolanReadValue = new LolanReadValue(deviceLolanName, deviceLolanGlobalId, decodedCborValue);
                guiHandler.post(() -> {
                    lolanReadValueArrayList.add(lolanReadValue);
                    customAdapterListViewRead.notifyDataSetChanged();
                });
                fullByteList.clear();
                gatt.disconnect();
            } else {
                gatt.writeCharacteristic(rxCharacteristic);
                Thread.sleep(100);
            }

        } catch (CborException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void disconnectGatt(){
        try {
            mGatt.disconnect();
            Thread.sleep(300);
            mGatt.close();
            mGatt = null;
        } catch (InterruptedException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isAlreadyRead(String currentDeviceId){
        for(int i=0; i<lolanReadValueArrayList.size(); i++)
            if(lolanReadValueArrayList.get(i).getDeviceId().equals(currentDeviceId))
                return true;
        return false;
    }


    private void enableButtons(){
        if(btnOk != null){
            btnOk.setEnabled(true);
            btnOk.setText(STR_OK);
        }
        if(btnSet != null){
            btnSet.setEnabled(true);
        }
        if(btnReconnect != null){
            btnReconnect.setEnabled(true);
        }
        if(btnRead != null)
            btnRead.setEnabled(true);
    }

    private void disableButtons(){
        if(btnOk != null){
            btnOk.setText(STR_STOP);
        }
        if(btnSet != null){
            btnSet.setEnabled(false);
        }
        if(btnReconnect != null){
            btnReconnect.setEnabled(false);
        }
        if(btnRead != null)
            btnRead.setEnabled(false);
    }

    private void showProgressBar(){
        if(progressBar != null){
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar(){
        if(progressBar != null){
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private boolean verifyValueToSet(String valueToSet){
        boolean isRangeCorrect = true;
        switch(this.varType){
            case "uint8":
                if(Long.parseLong(valueToSet) < 0 || Long.parseLong(valueToSet) > 255){
                    guiHandler.post(() -> tvResponse.setText(VALUE_OVER_THE_RANGE));
                    isRangeCorrect = false;
                }
                break;
            case "uint16":
                if(Long.parseLong(valueToSet) < 0 || Long.parseLong(valueToSet) > 65535){
                    guiHandler.post(() -> tvResponse.setText(VALUE_OVER_THE_RANGE));
                    isRangeCorrect = false;
                }
                break;
            case "uint32":
                if((Long.parseLong(valueToSet) < 0) || (Long.parseLong(valueToSet) > 4294967295L)){
                    guiHandler.post(() -> tvResponse.setText(VALUE_OVER_THE_RANGE));
                    isRangeCorrect = false;
                }
                break;
        }

        return isRangeCorrect;
    }

    private void stopReading(AlertDialog d){
        if(btnOk.getText().toString().equals(STR_STOP)){
            // STOP READING VALUES
            currentlyUpdatedDevice = devicesToUpdateArrayList.size() - 1;
            isReadingStopped = true;

            guiHandler.post(() -> {
                tvStatus.setText(STR_DISCONNECTED);
                enableButtons();
                hideProgressBar();
            });

            disconnectGatt();

        } else {
            isReadingStopped = false;

            disconnectGatt();
            guiHandler.post(lolanReadValueArrayList::clear);
            guiHandler.post(customAdapterListViewRead::notifyDataSetChanged);
            d.dismiss();
        }
    }


}

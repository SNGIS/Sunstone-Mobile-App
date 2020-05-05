package com.sunstone.lolan_db;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sunstone.R;
import com.sunstone.utility.SlipCborUtils;
import com.sunstone.utility.ParserUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.appcompat.app.AlertDialog;

import co.nstant.in.cbor.CborException;
import io.sentry.Sentry;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static com.sunstone.utility.BluetoothUtils.CBOR_ERROR;
import static com.sunstone.utility.BluetoothUtils.DELAY_STANDARD;
import static com.sunstone.utility.BluetoothUtils.DELAY_SUPER_HIGH;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_HIGH;
import static com.sunstone.utility.BluetoothUtils.DELAY_VERY_LOW;
import static com.sunstone.utility.BluetoothUtils.DELAY_LOW;
import static com.sunstone.utility.BluetoothUtils.RX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.SLIP_END_BYTE;
import static com.sunstone.utility.BluetoothUtils.STR_DISCONNECTED;
import static com.sunstone.utility.BluetoothUtils.STR_OK;
import static com.sunstone.utility.BluetoothUtils.STR_REFRESHING;
import static com.sunstone.utility.BluetoothUtils.STR_STOP;
import static com.sunstone.utility.BluetoothUtils.TX_CHARACTERISTIC;
import static com.sunstone.utility.BluetoothUtils.TX_DESCRIPTOR;
import static com.sunstone.utility.BluetoothUtils.UART_SERVICE_UUID;
import static com.sunstone.utility.BluetoothUtils.VALUE_OVER_THE_RANGE;
import static com.sunstone.utility.SlipCborUtils.decodeCborReadSingle;
import static com.sunstone.utility.SlipCborUtils.decodeCborWriteSingle;
import static com.sunstone.utility.SlipCborUtils.slipDecodeData;

public class LolanEntryAlert {
    private static final String TAG = "LolanEntryAlert";

    private BluetoothGattCharacteristic txCharacteristic, rxCharacteristic;
    private BluetoothGattDescriptor txDescriptor;
    private BluetoothGattService uartService;

    private BluetoothGatt mGatt;
    private BluetoothDevice lolanDevice;

    private Handler mHandler;
    private Handler guiHandler;

    private Context context;

    private int gattDelay;
    private int gattInitialDelay;
    private int getGattDelayWrite;

    private String varType;
    private int readOrWrite;
    private String varPath;
    private int[] pathInt;
    private View view;
    private int deviceLolanAddress;
    private CharSequence lolanVariableInfo;

    private String lolanVariableValue;
    private String currentValueToSet;

    private TextView tvLolanVal, tvStatus, tvResponse, tvInfoContent;
    private EditText etLolanVar;
    private ProgressBar progressBar;
    private LinearLayout llInfo;
    private Button btnOk, btnReconnect, btnSet;

    private List<Byte> fullByteList = new ArrayList<Byte>();
    private List<Byte> replyBytes = new ArrayList<Byte>();

    private int packetCounter = 1;

    /**
     * @param varType: uint8, uint16, uint32, int16, data80, str23
     * @param readOrWrite: 0 - Read only, 1 - Write only, 2 - Read & write
     * @param varPath: LoLaN db path, ie "5,3,1" - path [5,3,1] STATUS/lastaccel/x (int16)
     * @param pathInt: LoLaN db path, ie [5,3,1] - path [5,3,1] STATUS/lastaccel/x (int16)
     * @param view: TextView that initiated alert
     */
    public LolanEntryAlert(String varType, int readOrWrite,
                           String varPath, int[] pathInt,
                           View view, CharSequence variableInfo) {
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


    public void showAlert(Context alertContext, BluetoothDevice lolanDevice, int deviceLolanAddress) throws InterruptedException {

        this.context = alertContext;
        this.lolanDevice = lolanDevice;
        this.deviceLolanAddress = deviceLolanAddress;
        this.mHandler = new Handler();
        this.guiHandler = new Handler();

        gattInitialDelay = DELAY_VERY_HIGH;
        gattDelay = DELAY_LOW;
        getGattDelayWrite = DELAY_VERY_LOW;

        // READ ONLY
        if(this.getReadOrWrite()== 0) {

            final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.lolan_alert_r, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_r_title);
            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());

            llInfo = dialogView.findViewById(R.id.ll_alert_r_info);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_r_info_content);
            tvInfoContent.setText(lolanVariableInfo);
            tvStatus = dialogView.findViewById(R.id.tv_alert_r_connection_status);
            tvLolanVal = dialogView.findViewById(R.id.tv_alert_r_value);
            progressBar = dialogView.findViewById(R.id.pb_alert_r);
            btnReconnect = dialogView.findViewById(R.id.btn_alert_r_reconnect);
            btnOk = dialogView.findViewById(R.id.btn_alert_r_ok);

            showProgressBar();
            disableButtons();
            connectToDevice(context, lolanDevice);

            btnOk.setOnClickListener(view -> dialogBuilder.dismiss());

            btnReconnect.setOnClickListener(v -> {
                try {
                    tvLolanVal.setText("");
                    connectToDevice(context, lolanDevice);
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
            View dialogView = inflater.inflate(R.layout.lolan_alert_w, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_w_title);
            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());
            llInfo = dialogView.findViewById(R.id.ll_alert_w_info);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_w_info_content);
            tvInfoContent.setText(lolanVariableInfo);
            tvLolanVal = dialogView.findViewById(R.id.tv_alert_w_value);
            tvLolanVal.setText("");
            etLolanVar= dialogView.findViewById(R.id.et_alert_w_setval);
            etLolanVar.setHint(this.getVarType());
            progressBar = dialogView.findViewById(R.id.pb_alert_w);
            hideProgressBar();
            tvStatus = dialogView.findViewById(R.id.tv_alert_w_connection_status);
            tvResponse = dialogView.findViewById(R.id.tv_alert_w_response);
            btnSet = dialogView.findViewById(R.id.btn_alert_lolan_w_set);
            btnOk = dialogView.findViewById(R.id.btn_alert_lolan_w_cancel);

            if(this.varPath.equals("4,1,0")){
                gattDelay = DELAY_STANDARD;
                getGattDelayWrite = DELAY_SUPER_HIGH;
            }
            if(this.varPath.equals("4,2,2")){
                gattInitialDelay = DELAY_STANDARD;
            }

            btnOk.setOnClickListener(view -> dialogBuilder.dismiss());

            btnSet.setOnClickListener(view -> {
                if(etLolanVar.getText() != null
                        && !etLolanVar.getText().toString().equals("")
                        && verifyValueToSet(etLolanVar.getText().toString())) {
                    disableButtons();
                    showProgressBar();
                    currentValueToSet = etLolanVar.getText().toString();
                    tvResponse.setText("");
                    try {
                        connectToDeviceToWrite(context, lolanDevice);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            dialogBuilder.setView(dialogView);
            dialogBuilder.show();
        } else if (this.getReadOrWrite()== 2){
            // READ AND WRITE
            final AlertDialog dialogBuilder = new AlertDialog.Builder(context).create();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.lolan_alert_rw, null);
            final TextView tvLolanTitle = dialogView.findViewById(R.id.tv_alert_rw_title);
            tvLolanTitle.setText(((TextView) this.getView()).getText().toString());

            etLolanVar = dialogView.findViewById(R.id.et_alert_rw_setval);
            etLolanVar.setHint(this.getVarType());
            tvLolanVal = dialogView.findViewById(R.id.tv_alert_rw_value);
            tvStatus = dialogView.findViewById(R.id.tv_alert_rw_connection_status);
            progressBar = dialogView.findViewById(R.id.pb_alert_rw);
            tvResponse = dialogView.findViewById(R.id.tv_alert_rw_response);
            btnSet = dialogView.findViewById(R.id.btn_alert_lolan_set_rw);
            btnReconnect = dialogView.findViewById(R.id.btn_alert_lolan_reconnect_rw);
            btnOk = dialogView.findViewById(R.id.btn_alert_lolan_cancel_rw);
            llInfo = dialogView.findViewById(R.id.ll_alert_rw_info);
            tvInfoContent = dialogView.findViewById(R.id.tv_alert_rw_info_content);
            tvInfoContent.setText(lolanVariableInfo);

            disableButtons();
            showProgressBar();

            connectToDevice(context, lolanDevice);

            btnReconnect.setOnClickListener(v -> {
                tvLolanVal.setText("");
                try {
                    connectToDevice(context, lolanDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            btnOk.setOnClickListener(view -> dialogBuilder.dismiss());

            btnSet.setOnClickListener(view -> {
                if(etLolanVar.getText() != null
                        && !etLolanVar.getText().toString().equals("")
                        && verifyValueToSet(etLolanVar.getText().toString())) {
                    disableButtons();
                    showProgressBar();
                    currentValueToSet = etLolanVar.getText().toString();
                    tvResponse.setText("");
                    try {
                        connectToDeviceToWrite(context, lolanDevice);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            dialogBuilder.setView(dialogView);
            dialogBuilder.show();
        }
    }


    private void connectToDevice(Context context, BluetoothDevice lolanDevice) throws InterruptedException {
        guiHandler.post(() -> {
            disableButtons();
            showProgressBar();
        });
        if (mGatt == null) {
            Thread.sleep(400);
            mGatt = lolanDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mGatt.disconnect();
            mGatt.close();
            Thread.sleep(400);
            mGatt = lolanDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }
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
                    Log.d(TAG, "gattCallback: STATE_OTHER");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == GATT_SUCCESS) {
                gattReadServiceDiscovered(gatt);
            } else {
                gattReadReconnect(gatt);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == GATT_SUCCESS){
                gattReadDescriptorWrote(gatt);
            } else {
                gattReadReconnect(gatt);
            }
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {
            gattReadCharacteristicChanged(gatt, characteristic);

        }

//        @Override
//        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.d(TAG, "onCharacteristicWrite status: " + status);
//        }

//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            super.onCharacteristicRead(gatt, characteristic, status);
//        }
    };



    private final BluetoothGattCallback gattCallbackWrite = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gattWriteConnected(gatt);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    gattWriteDisconnected(gatt);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == GATT_SUCCESS) {
                gattWriteServiceDiscovered(gatt);
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
            try {
                Thread.sleep(getGattDelayWrite);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(status == GATT_SUCCESS) {
                gattWriteCharacteristicWrote(gatt);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == GATT_SUCCESS){
                gattWriteCharacteristicRead(gatt, characteristic);
            } else {
                gattWriteReconnect(gatt);
            }
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            gattWriteCharacteristicChanged(gatt, characteristic);
        }
    };



    private void gattWriteConnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            tvStatus.setText("CONNECTED " + gatt.getDevice().getAddress());
            showProgressBar();
            disableButtons();
        });
        mHandler.post(() -> {
            try {
                Thread.sleep(gattInitialDelay);
                gatt.discoverServices();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void gattWriteDisconnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            tvStatus.setText(STR_DISCONNECTED);
            if(tvLolanVal != null && tvLolanVal.getText().equals("") ||
                    (currentValueToSet != null && tvLolanVal != null && !tvLolanVal.getText().toString().equals(currentValueToSet)) &&
                            (!tvLolanVal.getText().toString().contains("ERROR"))) {
                tvStatus.setText(STR_REFRESHING);
                showProgressBar();
                disableButtons();
                gattWriteReconnect(gatt);
            }
        });
        mHandler.post(() -> {
            try {
                mGatt.disconnect();
                Thread.sleep(300);
                mGatt.close();
                mGatt = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        guiHandler.post(() -> {
            hideProgressBar();
            enableButtons();
        });
    }

    private void gattWriteServiceDiscovered(BluetoothGatt gatt){
        guiHandler.post(() -> {
            try {
                Thread.sleep(600);
                uartService = gatt.getService(UART_SERVICE_UUID);
                txCharacteristic = uartService.getCharacteristic(TX_CHARACTERISTIC);

                Thread.sleep(DELAY_LOW);
                gatt.setCharacteristicNotification(txCharacteristic, true);
                txCharacteristic.setWriteType(WRITE_TYPE_DEFAULT);
                rxCharacteristic = gatt.getService(UART_SERVICE_UUID)
                    .getCharacteristic(RX_CHARACTERISTIC);
                txDescriptor = txCharacteristic.getDescriptor(TX_DESCRIPTOR);
                txDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                Thread.sleep(200);
                Log.d(TAG, "gattWriteServiceDiscovered writeDescriptor: " + gatt.writeDescriptor(txDescriptor));

            } catch (InterruptedException | NullPointerException e) {
                e.printStackTrace();
                try {
                    connectToDeviceToWrite(context, lolanDevice);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void gattWriteDescriptorWrote(BluetoothGatt gatt){
        String stringHexDataPacket;
        try {
            if(currentValueToSet != null && !currentValueToSet.equals("")) {
                String generatedSlipPacketSet = SlipCborUtils.generateSlipPacketSetLegacy(getPathInt(), currentValueToSet, deviceLolanAddress);
                if(!generatedSlipPacketSet.equals(CBOR_ERROR)) {
                    stringHexDataPacket = generatedSlipPacketSet;
                    byte[] dataPacketByteArray = ParserUtils.hexStringToByteArray(stringHexDataPacket);
                    rxCharacteristic.setValue(dataPacketByteArray);
                    rxCharacteristic.setWriteType(WRITE_TYPE_DEFAULT);
                    gatt.writeCharacteristic(rxCharacteristic);
                } else {
                    guiHandler.post(() -> tvLolanVal.setText(CBOR_ERROR));
                    gatt.disconnect();
                }
            } else {
                gatt.disconnect();
                gatt.close();
            }
        } catch (CborException e) {
            Sentry.capture(e);
            e.printStackTrace();
        }
    }

    private void gattWriteCharacteristicWrote(BluetoothGatt gatt){
        if(txCharacteristic.getValue() != null) {
            for (byte b : txCharacteristic.getValue()) {
                replyBytes.add(b);
            }
        } else {
            gatt.writeCharacteristic(rxCharacteristic);
        }

        try {
            if(replyBytes.size() > 0 && replyBytes.get(replyBytes.size() - 1) == SLIP_END_BYTE) {
                replyBytes = slipDecodeData(replyBytes);
                lolanVariableValue = decodeCborWriteSingle(replyBytes, guiHandler, tvResponse, tvLolanVal, currentValueToSet, deviceLolanAddress);
                fullByteList.clear();
                replyBytes.clear();
                Thread.sleep(DELAY_VERY_LOW);
                gatt.disconnect();
            } else {
                Thread.sleep(DELAY_STANDARD);
                gatt.writeCharacteristic(rxCharacteristic);
            }
        } catch (CborException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void gattWriteCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.getValue() != null) {
            for (byte b : characteristic.getValue()) {
                replyBytes.add(b);
            }
        }

        try {
            if(replyBytes.size() > 0 && replyBytes.get(replyBytes.size()-1) == SLIP_END_BYTE){
                replyBytes = slipDecodeData(replyBytes);
                lolanVariableValue = decodeCborWriteSingle(replyBytes, guiHandler, tvResponse, tvLolanVal, currentValueToSet, deviceLolanAddress);
                fullByteList.clear();
                Thread.sleep(300);
                gatt.disconnect();
            } else {
                Thread.sleep(300);
                gatt.readCharacteristic(characteristic);
            }
        } catch (InterruptedException | IOException | CborException e) {
            e.printStackTrace();
        }
    }

    private void gattWriteCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        try {
            Thread.sleep(getGattDelayWrite);
            gattCallbackWrite.onCharacteristicWrite(gatt, characteristic, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void gattWriteReconnect(BluetoothGatt gatt){
        mHandler.postDelayed(gatt::disconnect, DELAY_LOW);
        mHandler.postDelayed(gatt::close, DELAY_LOW);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    connectToDeviceToWrite(context, lolanDevice);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, DELAY_LOW);
    }


    private void gattReadConnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            tvStatus.setText("CONNECTED " + gatt.getDevice().getAddress());
            showProgressBar();
            disableButtons();
        });
        mHandler.postDelayed(gatt::discoverServices, 1200);
    }

    private void gattReadDisconnected(BluetoothGatt gatt){
        guiHandler.post(() -> {
            tvStatus.setText(STR_DISCONNECTED);
            if(tvLolanVal != null && tvLolanVal.getText().toString().equals("")){
                tvStatus.setText(STR_REFRESHING);
                showProgressBar();
                disableButtons();
                mHandler.postDelayed(gatt::disconnect, DELAY_LOW);
                mHandler.postDelayed(gatt::close, DELAY_LOW);
                try {
                    connectToDevice(context, lolanDevice);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    mHandler.postDelayed(gatt::disconnect, 300);
                    mHandler.postDelayed(gatt::close, 300);
                    mGatt = null;
                    hideProgressBar();
                    enableButtons();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void gattReadServiceDiscovered(BluetoothGatt gatt){
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
                        connectToDevice(context, lolanDevice);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }, DELAY_LOW);

        } catch (NullPointerException e) {
            e.printStackTrace();
            gattReadReconnect(gatt);
        }
    }

    private void gattReadDescriptorWrote(BluetoothGatt gatt){
        StringBuilder hexDataPacket = new StringBuilder();
        try {
            String generatedSlipPacketGet = SlipCborUtils.generateSlipPacketGet(getPathInt(), deviceLolanAddress, ++packetCounter, context);
            if(!generatedSlipPacketGet.equals(CBOR_ERROR)) {
                hexDataPacket.append(generatedSlipPacketGet);
                byte[] dataPacketByteArray = ParserUtils.hexStringToByteArray(hexDataPacket.toString());
                rxCharacteristic.setValue(dataPacketByteArray);
            } else {
                guiHandler.post(() -> tvLolanVal.setText(CBOR_ERROR));
                gatt.disconnect();
            }
        } catch (CborException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(200);
            gatt.writeCharacteristic(rxCharacteristic);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void gattReadCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        try {
            for(byte b : characteristic.getValue())
                fullByteList.add(b);

            if(fullByteList.size() > 0 && (fullByteList.get(fullByteList.size()-1) == SLIP_END_BYTE)){
                fullByteList = SlipCborUtils.slipDecodeData(fullByteList);
                decodeCborReadSingle(fullByteList, guiHandler, tvLolanVal);
                fullByteList.clear();
                gatt.disconnect();
            } else {
                Thread.sleep(100);
                gatt.writeCharacteristic(rxCharacteristic);
            }
        } catch (InterruptedException | IOException | CborException e) {
            Sentry.capture(e);
            e.printStackTrace();
        }
    }

    private void gattReadReconnect(BluetoothGatt gatt){
        mHandler.postDelayed(gatt::disconnect, DELAY_LOW);
        mHandler.postDelayed(gatt::close, DELAY_LOW);
        mHandler.postDelayed(() -> {
            try {
                connectToDevice(context, lolanDevice);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, DELAY_LOW);
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
}

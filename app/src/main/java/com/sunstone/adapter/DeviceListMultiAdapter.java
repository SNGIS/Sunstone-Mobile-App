/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sunstone.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sunstone.R;
import com.sunstone.model.ExtendedBluetoothDevice;
import com.sunstone.utility.ParserUtils;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static com.sunstone.utility.BluetoothUtils.MANUFACTURER_ID;


/**
 * DeviceListAdapter class is list adapter for showing scanned Devices name, address and RSSI image based on RSSI values.
 */
public class DeviceListMultiAdapter extends BaseAdapter {
	private static final String TAG = "CustomDeviceListMulti";

	/* list of scanned devices */
	private final ArrayList<ExtendedBluetoothDevice> mListDevices = new ArrayList<>();
	/* list of devices chosen to update */
	private final ArrayList<ExtendedBluetoothDevice> mDevicesToUpdate = new ArrayList<>();
	private final Context mContext;

	private ViewHolder holder;

	public DeviceListMultiAdapter(final Context context) {
		mContext = context;
	}

	public void update(final List<ScanResult> results) {
		for (final ScanResult result : results) {
			ExtendedBluetoothDevice device = findDevice(result);
			if (device == null) {
				mListDevices.add(new ExtendedBluetoothDevice(result));
			} else {
				byte[] manufacturerData = result.getScanRecord().getManufacturerSpecificData(MANUFACTURER_ID);
				if(manufacturerData != null && manufacturerData.length > 0) {
					String fwSubstring= ParserUtils.byteToBin(manufacturerData[5])
							.concat(ParserUtils.byteToBin(manufacturerData[4]));
					device.fw_version = String.valueOf(Integer.parseInt(fwSubstring, 2));
				}
			}
		}
		notifyDataSetChanged();
	}

	public void addDeviceToUpdate(ExtendedBluetoothDevice newDevice) {
		final ExtendedBluetoothDevice d = findDeviceToUpdate(newDevice);
		if (d == null) {
			newDevice.setChecked();
			mDevicesToUpdate.add(newDevice);
		} else {
			newDevice.setUnChecked();
			mDevicesToUpdate.remove(newDevice);
		}
		notifyDataSetChanged();
	}

	public ArrayList<ExtendedBluetoothDevice> getDevicesToUpdate(){
		return mDevicesToUpdate;
	}

	private ExtendedBluetoothDevice findDeviceToUpdate(final ExtendedBluetoothDevice result) {
		for (final ExtendedBluetoothDevice device : mDevicesToUpdate)
			if (device.matches(result))
				return device;
		return null;
	}

	private ExtendedBluetoothDevice findDevice(final ScanResult result) {
		for (final ExtendedBluetoothDevice device : mListDevices)
			if (device.matches(result))
				return device;
		return null;
	}

	public void clearDevices() {
		mListDevices.clear();
		mDevicesToUpdate.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
        return mListDevices.size();
	}

	@Override
	public Object getItem(int position) {
		return mListDevices.get(position);
	}


	public ExtendedBluetoothDevice getItemDevice(int position) {
		return mListDevices.get(position);
	}


	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public View getView(int position, View oldView, ViewGroup parent) {
		final ExtendedBluetoothDevice device = (ExtendedBluetoothDevice) getItem(position);

		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = oldView;

		if (view == null) {
			view = inflater.inflate(R.layout.device_list_row_multi, parent, false);
			holder = new ViewHolder();
			holder.name = view.findViewById(R.id.name);
			holder.address = view.findViewById(R.id.address);
			holder.device_type = view.findViewById(R.id.device_type);
			holder.device_id = view.findViewById(R.id.device_id);
			holder.fw_version = view.findViewById(R.id.fw_version);
			holder.bl_version = view.findViewById(R.id.bl_version);
			holder.sd_version = view.findViewById(R.id.sd_version);
			holder.checkBox = view.findViewById(R.id.iv_cb_scanner);
			holder.llScaner = view.findViewById(R.id.ll_device_scanner_multivar);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		final String name = device.name;
		holder.name.setText(name != null ? name : mContext.getString(R.string.not_available));
		holder.device_type.setText(device.device_type);
		holder.device_id.setText(device.global_id);
		holder.fw_version.setText(device.fw_version);
		holder.bl_version.setText(device.bl_version);
		holder.sd_version.setText(device.sd_version);
		holder.checkBox.setChecked(device.isChecked());

		holder.checkBox.setClickable(false);

		return view;
	}

	public int getDeviceId(int position) {
		if (mListDevices.get(position) != null) {
			return Integer.parseInt(mListDevices.get(position).global_id) % 10000;
		} else {
			return -1;
		}
	}

	private class ViewHolder {
		private TextView name;
		private TextView address;
		private CheckBox checkBox;
		private TextView device_type;
		private TextView device_id;
		private TextView fw_version;
		private TextView bl_version;
		private TextView sd_version;
		private LinearLayout llScaner;
	}
}

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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunstone.R;
import com.sunstone.model.ExtendedBluetoothDevice;
import com.sunstone.utility.ParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static com.sunstone.utility.BluetoothUtils.MANUFACTURER_ID;


/**
 * DeviceListAdapter class is list adapter for showing scanned Devices name, address and RSSI image based on RSSI values.
 */
public class CustomDeviceListAdapter extends BaseAdapter {
	private static final String TAG = "CustomDeviceListAdapter";

	private static final int TYPE_TITLE = 0;
	private static final int TYPE_ITEM = 1;
	private static final int TYPE_EMPTY = 2;


	private final ArrayList<ExtendedBluetoothDevice> mListDevices = new ArrayList<>();

	private final Context mContext;

	public CustomDeviceListAdapter(final Context context) {
		mContext = context;
	}

	/**
	 * Updates the list of not bonded devices.
	 * @param results list of results from the scanner
	 */
	public void update(final List<ScanResult> results) {
		for (final ScanResult result : results) {
			final ExtendedBluetoothDevice device = findDevice(result);
			if (device == null) {
				mListDevices.add(new ExtendedBluetoothDevice(result));
			} else {
				byte[] manufacturerData = result.getScanRecord().getManufacturerSpecificData(MANUFACTURER_ID);
				if(manufacturerData != null && manufacturerData.length > 6) {
					String fwSubstring= ParserUtils.byteToBin(manufacturerData[5])
							.concat(ParserUtils.byteToBin(manufacturerData[4]));
					int deviceFw = Integer.parseInt(fwSubstring, 2);
					device.fw_version = String.valueOf(deviceFw);
				}
			}
		}
		notifyDataSetChanged();
	}

	private ExtendedBluetoothDevice findDevice(final ScanResult result) {
		for (final ExtendedBluetoothDevice device : mListDevices)
			if (device.matches(result))
				return device;
		return null;
	}

	public void clearDevices() {
		mListDevices.clear();
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

	public BluetoothDevice getItemDevice(int position) {
		return mListDevices.get(position).device;
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) == TYPE_ITEM;
	}

	@Override
	public int getItemViewType(int position) {
		if (mListDevices.isEmpty())
			return TYPE_EMPTY;
		return TYPE_ITEM;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View oldView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		final int type = getItemViewType(position);
		View view = oldView;
		switch (type) {
            case TYPE_EMPTY:
                if (view == null) {
                    view = inflater.inflate(R.layout.device_list_empty, parent, false);
                }
                break;
            case TYPE_TITLE:
                if (view == null) {
                    view = inflater.inflate(R.layout.device_list_title, parent, false);
                }
                final TextView title = (TextView) view;
                title.setText((Integer) getItem(position));
                break;
            default:
                if (view == null) {
                    view = inflater.inflate(R.layout.device_list_row, parent, false);
                    final ViewHolder holder = new ViewHolder();
                    holder.name = view.findViewById(R.id.name);
                    holder.address = view.findViewById(R.id.address);
                    holder.rssi = view.findViewById(R.id.rssi);
                    holder.device_type = view.findViewById(R.id.device_type);
                    holder.device_id = view.findViewById(R.id.device_id);
                    holder.fw_version = view.findViewById(R.id.fw_version);
                    holder.bl_version = view.findViewById(R.id.bl_version);
                    holder.sd_version = view.findViewById(R.id.sd_version);
                    view.setTag(holder);
                }

                final ExtendedBluetoothDevice device = (ExtendedBluetoothDevice) getItem(position);
                final ViewHolder holder = (ViewHolder) view.getTag();
                final String name = device.name;
                holder.name.setText(name != null ? name : mContext.getString(R.string.not_available));
                holder.device_type.setText(device.device_type);
                holder.device_id.setText(device.global_id);
                holder.fw_version.setText(device.fw_version);
                holder.bl_version.setText(device.bl_version);
                holder.sd_version.setText(device.sd_version);
                if (!device.isBonded || device.rssi != ExtendedBluetoothDevice.NO_RSSI) {
                    final int rssiPercent = (int) (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f));
                    holder.rssi.setImageLevel(rssiPercent);
                    holder.rssi.setVisibility(View.VISIBLE);
                } else {
                    holder.rssi.setVisibility(View.GONE);
                }
                break;
		}
		return view;
	}

	public int getRequestedPublicDeviceId(int position) {
		if (mListDevices.get(position) != null) {
			return Integer.parseInt(mListDevices.get(position).global_id) % 10000;
		} else {
			return -1;
		}
	}

	private class ViewHolder {
		private TextView name;
		private TextView address;
		private ImageView rssi;
		private TextView device_type;
		private TextView device_id;
		private TextView fw_version;
		private TextView bl_version;
		private TextView sd_version;
	}
}

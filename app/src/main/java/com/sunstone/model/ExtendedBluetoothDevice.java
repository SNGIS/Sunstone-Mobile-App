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
package com.sunstone.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.sunstone.utility.ParserUtils;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ExtendedBluetoothDevice implements Parcelable {
	private static final String TAG = "ExtendedBluetoothDevice";

	public final static int NO_RSSI = -1000;

	/** Device manufacturer's specific id **/
	private int MANUFACTURER_ID = 2065;

	public final BluetoothDevice device;

	/** The name is not parsed by some Android devices, f.e. Sony Xperia Z1 with Android 4.3 (C6903). It needs to be parsed manually. */
	public String name;
	public int rssi;
	public boolean isBonded;
	public boolean isMyDevice;
	public String device_type;
	public String device_id;
	public String fw_version;
	public String bl_version;
	public String sd_version;
	public String global_id;

	private boolean checked;

	private byte[] manufacturerData;

	public ExtendedBluetoothDevice(final ScanResult scanResult) {

		if(scanResult.getScanRecord().getManufacturerSpecificData(MANUFACTURER_ID) != null)
			this.manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(MANUFACTURER_ID);

		if(manufacturerData != null && manufacturerData.length > 0) {
			this.device = scanResult.getDevice();
			this.name = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : null;
			this.rssi = scanResult.getRssi();
			this.isBonded = false;
			this.isMyDevice = false;

			StringBuilder binaryManufacturerData = new StringBuilder();

			for (byte b : manufacturerData) {
				binaryManufacturerData.append(ParserUtils.byteToBin(b));
			}

			try {
				String blSubstring = ParserUtils.byteToBin(manufacturerData[1])
						.concat(ParserUtils.byteToBin(manufacturerData[0]));
				int deviceBl = Integer.parseInt(blSubstring, 2);
				this.bl_version = String.valueOf(deviceBl);

				String sdSubstring= ParserUtils.byteToBin(manufacturerData[3])
						.concat(ParserUtils.byteToBin(manufacturerData[2]));
				int deviceSd = Integer.parseInt(sdSubstring, 2);
				this.sd_version = String.valueOf(deviceSd);

				String fwSubstring= ParserUtils.byteToBin(manufacturerData[5])
						.concat(ParserUtils.byteToBin(manufacturerData[4]));
				int deviceFw = Integer.parseInt(fwSubstring, 2);
				this.fw_version = String.valueOf(deviceFw);

				String idSubstring = ParserUtils.byteToBin(manufacturerData[7])
						.concat(ParserUtils.byteToBin(manufacturerData[6]));

				int deviceId = Integer.parseInt(idSubstring, 2);
				this.device_id = String.valueOf(deviceId);


				String gidSubstring = ParserUtils.byteToBin(manufacturerData[9])
						.concat(ParserUtils.byteToBin(manufacturerData[8]))
						.concat(ParserUtils.byteToBin(manufacturerData[7]))
						.concat(ParserUtils.byteToBin(manufacturerData[6]));


				int globalId = Integer.parseInt(gidSubstring, 2);
				this.global_id = String.valueOf(globalId);

				String pidSubstring = binaryManufacturerData.substring(72, 80);
				int devicePid = Integer.parseInt(pidSubstring, 2);
				switch (devicePid) {
					case 1:
						this.device_type = "ProTag";
						break;
					case 2:
						this.device_type = "MiniTag";
						break;
					case 3:
						this.device_type = "DuraTag";
						break;
					default:
						this.device_type = "UnrecognizedTag";
						break;
				}
				this.isMyDevice = true;

			} catch (Exception e) {
				Log.d(TAG, "no ManufacturerSpecificData: " + e.getLocalizedMessage());
			}
		} else {
			this.device = null;
		}
	}

	public ExtendedBluetoothDevice(final BluetoothDevice device) {
		this.device = device;
		this.name = device.getName();
		this.rssi = NO_RSSI;
		this.isBonded = true;
		this.isMyDevice = false;
	}

	protected ExtendedBluetoothDevice(Parcel in) {
		MANUFACTURER_ID = in.readInt();
		device = in.readParcelable(BluetoothDevice.class.getClassLoader());
		name = in.readString();
		rssi = in.readInt();
		isBonded = in.readByte() != 0;
		isMyDevice = in.readByte() != 0;
		device_type = in.readString();
		device_id = in.readString();
		fw_version = in.readString();
		bl_version = in.readString();
		sd_version = in.readString();
		global_id = in.readString();
	}

	public static final Creator<ExtendedBluetoothDevice> CREATOR = new Creator<ExtendedBluetoothDevice>() {
		@Override
		public ExtendedBluetoothDevice createFromParcel(Parcel in) {
			return new ExtendedBluetoothDevice(in);
		}

		@Override
		public ExtendedBluetoothDevice[] newArray(int size) {
			return new ExtendedBluetoothDevice[size];
		}
	};

	public boolean matches(final ScanResult scanResult) {
		return this.device.getAddress().equals(scanResult.getDevice().getAddress());
	}

	public boolean matches(final ExtendedBluetoothDevice btDevice) {
		return this.device.getAddress().equals(btDevice.device.getAddress());
	}

	private String getGlobalId(){
		return this.global_id;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(MANUFACTURER_ID);
		dest.writeParcelable(device, flags);
		dest.writeString(name);
		dest.writeInt(rssi);
		dest.writeByte((byte) (isBonded ? 1 : 0));
		dest.writeByte((byte) (isMyDevice ? 1 : 0));
		dest.writeString(device_type);
		dest.writeString(device_id);
		dest.writeString(fw_version);
		dest.writeString(bl_version);
		dest.writeString(sd_version);
		dest.writeString(global_id);
	}

	public void setChecked(){
		this.checked = true;
	}

	public void setUnChecked(){
		this.checked = false;
	}

	public boolean isChecked(){
		return this.checked;
	}
}

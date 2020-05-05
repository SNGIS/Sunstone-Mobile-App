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

package com.sunstone.utility;

import android.util.Log;

import java.math.BigInteger;
import java.util.UUID;

public class ParserUtils extends no.nordicsemi.android.ble.utils.ParserUtils {
	private static final String TAG = "ParserUtils";

	public static String parseDebug(final byte[] data) {
		if (data == null || data.length == 0)
			return "";

		final char[] out = new char[data.length * 2];
		for (int i = 0; i < data.length; i++) {
			int val = data[i] & 0xFF;
			out[i * 2] = HEX_ARRAY[val >>> 4];
			out[i * 2 + 1] = HEX_ARRAY[val & 0x0F];
		}
//		return "0x" + new String(out);
		return new String(out);
	}

	private UUID convertFromInteger(int i) {
		final long MSB = 0x0000000000001000L;
		final long LSB = 0x800000805f9b34fbL;
		long value = i & 0xFFFFFFFF;
		return new UUID(MSB | (value << 32), LSB);
	}

	public static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	public static byte[] stringToBytesASCII(String str) {
		if(str == null)
			return new byte[0];
		char[] buffer = str.toCharArray();
		byte[] bytes = new byte[buffer.length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) buffer[i];
		}
		return bytes;
	}

	public static String bytesToStringUTFCustom(byte[] bytes) {
		char[] buffer = new char[bytes.length >> 1];
		for(int i = 0; i < buffer.length; i++) {
			int bufferPosition = i << 1;
			char c = (char)(((bytes[bufferPosition] & 0x00FF) << 8) + (bytes[bufferPosition + 1] & 0x00FF));
			buffer[i] = c;
		}
		return new String(buffer);
	}

	//
	public static String byteToBin(byte b){
		String out = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		return out;
	}

	public static String intToBin(int i){
		byte b = (byte) i;
		String out = String.format("%8s", Integer.toBinaryString(b & 0xFF )).replace(' ', '0');
		return out;
	}

	public static String byteArrayToBin(byte[] byteArray){
		String out = "";
		for(byte b : byteArray){
			out += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		}
		return out;
	}

	public static String byteArrayToBinInvert(byte[] byteArray){
		String out = "";
		for(byte b : byteArray){
			out += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		}
		return out;
	}


	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
//		return new BigInteger(s,16).toByteArray();
	}

	public static String binToHex(String binaryString){
		Log.d(TAG, "binToHex: " + String.format("%02X", new BigInteger(binaryString, 2)));
		return String.format("%02X", new BigInteger(binaryString, 2));
	}



	public static String reverseString(String s){
		return new StringBuilder(s).reverse().toString();
	}
}

package com.sunstone.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.DataItemListener;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnsignedInteger;

import static com.sunstone.utility.ParserUtils.binToHex;
import static com.sunstone.utility.ParserUtils.byteToBin;
import static com.sunstone.utility.ParserUtils.hexStringToByteArray;
import static com.sunstone.utility.ParserUtils.reverseString;
import static com.sunstone.utility.PrefsHelper.CURRENT_LOLAN_HEADER;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;

public class SlipCborUtils {
    private static final String TAG = "SlipCborUtils";

    private static final String LOLAN_PACKET_INFORM_HEX = "02";
    private static final String LOLAN_PACKET_GET_HEX = "05";
    private static final String LOLAN_PACKET_SET_HEX = "06";

    private static String LOLAN_HEADER_GET_1 = "101";
    private static String LOLAN_HEADER_GET_2 = "01110100";

    private static String LOLAN_HEADER_SET_1 = "110";
    private static String LOLAN_HEADER_SET_2 = "01110100";

    private static final String CBOR_ERROR = "CBOR ERROR OCCURED";
    private static final String CRC_ERROR = "CRC ERROR OCCURED";


    private static final byte SLIP_END_BYTE = (byte) 0xC0;
    private static final byte SLIP_ESC_BYTE = (byte) 0xDB;
    private static final byte SLIP_ESC_END_BYTE = (byte) 0xDC;
    private static final byte SLIP_ESC_ESC_BYTE = (byte) 0xDD;


    private static String decodedValue;
    private static int packetsCounter = 1;

    /**
     * PACKET HEADER:
     * 2 bytes attributes
     *      packet type:
     *      0 - BEACON
     *      1 - DATA
     *      2 - ACK
     *      3 - MAC
     *      4 - LoLaN INFORM
     *      5 - LoLaN GET
     *      6 - LoLaN SET
     *      7 - LoLaN CONTROL
     *  1 byte packet counter
     *  2 bytes src. address
     *  2 bytes dst. address
     */

    public static String generateSlipPacketGet(int[] lolanPath, int dstAddress, int packetCounter, Context context) throws CborException {
        StringBuilder lolanPacketHexStringBuilder = new StringBuilder();
        /* header 1 */
        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_GET_1));

        // load header 1
//         lolanPacketHexStringBuilder.append(getHeaderByte1(context, LOLAN_HEADER_GET_1));

//        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_SET_2));
        // load header 2
//         lolanPacketHexStringBuilder.append(getHeaderByte2(context));

        /* header 2: 01110100 */
        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_GET_2));

        /* packet counter */
        lolanPacketHexStringBuilder.append(String.format("%02X", packetsCounter++));
        /* src address */
        lolanPacketHexStringBuilder.append(String.format("%04X", 1));
        /* dst address */
        lolanPacketHexStringBuilder.append(String.format("%04X", dstAddress));
        /* CBOR encoded data */
        String cborPathHexStringGet = bytesToHex(generateCborPathGet(lolanPath));
        lolanPacketHexStringBuilder.append(cborPathHexStringGet);
        /* CRC */
        String cborCrcHexStringGet = CRC_calc(hexStringToByteArray(lolanPacketHexStringBuilder.toString()),
                lolanPacketHexStringBuilder.toString().length());
        lolanPacketHexStringBuilder.append(cborCrcHexStringGet);
        /* Slip packet */
        String slipPacketGet = generateHexSlipPacket(lolanPacketHexStringBuilder.toString());
        Log.d(TAG, "generateSlipPacketGet: " + slipPacketGet);
        return slipPacketGet;
    }

    private static byte[] generateCborPathGet(int[] lolanPath) throws CborException {

        int pathSize = lolanPath.length;
        int counter = 0;
        int[] newPath = new int[3];

        if (lolanPath.length == 3 && lolanPath[2] != 0) {
            pathSize = 3;
            newPath = lolanPath;
        } else if ((lolanPath.length == 3 && lolanPath[2] == 0) || (lolanPath.length == 2 && lolanPath[1] != 0)){
            pathSize = 2;
            newPath[0] = lolanPath[0];
            newPath[1] = lolanPath[1];
        } else if (lolanPath.length == 2 && lolanPath[1] == 0) {
            pathSize = 1;
            newPath[0] = lolanPath[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(pathSize == 3) {
            newPath = lolanPath;
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .putArray(counter++)
                    .add(newPath[0])
                    .add(newPath[1])
                    .add(newPath[2])
                    .end()
                    .end()
                    .build());
        } else if(pathSize == 2){
            newPath = lolanPath;
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .putArray(counter++)
                    .add(newPath[0])
                    .add(newPath[1])
                    .end()
                    .end()
                    .build());
        } else if(pathSize == 1){
            newPath = lolanPath;
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .putArray(counter++)
                    .add(newPath[0])
                    .end()
                    .end()
                    .build());
        }
        return baos.toByteArray();
    }

    public static String generateSlipPacketSetLegacy(int[] lolanPath, String valueToSet, int dstAddress) throws CborException {

        StringBuilder lolanPacketHexStringBuilder = new StringBuilder();
        // 2 bytes attributes SET: 06
        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_SET_1));
        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_SET_2));
        // 1 byte packet counter
        lolanPacketHexStringBuilder.append(String.format("%02X", packetsCounter++));
        // 2 bytes src. address
        lolanPacketHexStringBuilder.append(String.format("%04X", 0));
        // 2 bytes dst. address
         lolanPacketHexStringBuilder.append(String.format("%04X", dstAddress));
        // CBOR PAYLOAD
        String cborPayloadHexString = bytesToHex(generateCborPathSetLegacy(lolanPath, valueToSet));
        if(!cborPayloadHexString.equals("ERROR")){
            lolanPacketHexStringBuilder.append(cborPayloadHexString);
        } else {
            return CBOR_ERROR;
        }
        // 2 bytes CRC
        String hexSlipCrc = CRC_calc(hexStringToByteArray(lolanPacketHexStringBuilder.toString()),
                lolanPacketHexStringBuilder.toString().length());
        lolanPacketHexStringBuilder.append(hexSlipCrc);

        String slipPacketSetLegacy = generateHexSlipPacket(lolanPacketHexStringBuilder.toString());
        Log.d(TAG, "generateSlipPacketSetLegacy: " + slipPacketSetLegacy);

        return slipPacketSetLegacy;
    }


    private static byte[] generateCborPathSetLegacy(int[] lolanPath, Object valueToEncode) throws CborException {

        int pathSize = lolanPath.length;
        int counter = 0;

        int[] newPath = new int[3];

        if(lolanPath.length == 3 && lolanPath[2] != 0){
            newPath = lolanPath;
        }

        if (lolanPath.length == 3 && lolanPath[2] == 0) {
            pathSize = 2;
            newPath[0] = lolanPath[0];
            newPath[1] = lolanPath[1];
        }

        if (lolanPath.length == 2 && lolanPath[1] != 0) {
            pathSize = 2;
            newPath[0] = lolanPath[0];
            newPath[1] = lolanPath[1];
        }
        if (lolanPath.length == 2 && lolanPath[1] == 0) {
            pathSize = 1;
            newPath[0] = lolanPath[0];
        }
        if (lolanPath.length == 1 && lolanPath[0] != 0) {
            pathSize = 1;
            newPath[0] = lolanPath[0];
        }

        long intLolanValue = 0;
        if (valueToEncode instanceof String){
            try {
                intLolanValue = Long.parseLong(valueToEncode.toString());
            } catch (NumberFormatException e){
                return new byte[] {-1};
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(pathSize == 3) {
            new CborEncoder(baos).encode(new CborBuilder()
                        .addMap()
                            .putArray(counter++)
                                .add(newPath[0])
                                .add(newPath[1])
                            .end()
                            .put(newPath[2], intLolanValue)
                        .end()
                    .build());
        } else if (pathSize == 2) {
            new CborEncoder(baos).encode(new CborBuilder()
                        .addMap()
                            .putArray(counter++)
                                .add(newPath[0])
                            .end()
                            .put(newPath[1], intLolanValue)
                        .end()
                    .build());
        } else if (pathSize == 1) {
            new CborEncoder(baos).encode(new CborBuilder()
                        .addMap()
                            .put(newPath[0], intLolanValue)
                        .end()
                    .build());
        }

        return baos.toByteArray();
    }

    public static String generateSlipPacketSetNew(ArrayList<int[]> lolanPaths, ArrayList<String> valueToSet, int dstAddress, Context context) throws CborException {
        StringBuilder lolanPacketHexStringBuilder = new StringBuilder();
        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_SET_1));
        // load header 1
//         lolanPacketHexStringBuilder.append(getHeaderByte1(context, LOLAN_HEADER_SET_1));

        lolanPacketHexStringBuilder.append(binToHex(LOLAN_HEADER_SET_2));
        // load header 2
//         lolanPacketHexStringBuilder.append(getHeaderByte2(context));


        lolanPacketHexStringBuilder.append(String.format("%02X", packetsCounter++));
        lolanPacketHexStringBuilder.append(String.format("%04X", 1));
        lolanPacketHexStringBuilder.append(String.format("%04X", dstAddress));
        String hexCborPathSet = "";
        try {
            hexCborPathSet = bytesToHex(generateCborPathSetNew(lolanPaths, valueToSet));
        } catch (Exception e) {
            e.printStackTrace();
        }
        lolanPacketHexStringBuilder.append(hexCborPathSet);

        lolanPacketHexStringBuilder.append(CRC_calc(
                hexStringToByteArray(lolanPacketHexStringBuilder.toString()),
                lolanPacketHexStringBuilder.toString().length()));

        return generateHexSlipPacket(lolanPacketHexStringBuilder.toString());
    }

    private static Map createMapRecurrent(Map map, int[] path, int elementItNumber,
                                           int mainItNumber, List<String> valuesToEncode) {

        int element = path[elementItNumber];
        if (elementItNumber == path.length - 1) {
            return map.put(new UnsignedInteger(element), new UnsignedInteger(Long.parseLong(valuesToEncode.get(mainItNumber))));
        } else {
            if (!map.getKeys().contains(new UnsignedInteger(element)))
                map.put(new UnsignedInteger(element), new Map());

            if (path[elementItNumber+1] == 0)
                return map.put(new UnsignedInteger(element), new UnsignedInteger(Long.parseLong(valuesToEncode.get(mainItNumber))));

            return map.put(new UnsignedInteger(element), createMapRecurrent((Map) map.get(new UnsignedInteger(element)),
                    path, elementItNumber+1, mainItNumber, valuesToEncode));
        }
    }

    private static byte[] generateCborPathSetNew(ArrayList<int[]> lolanPaths, List<String> valuesToEncode) throws CborException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map outerMap = new Map();
        outerMap.put(new UnsignedInteger(0), new UnsignedInteger(1));

        int mainIteration = 0;
        for (int[] path: lolanPaths) {
            outerMap = createMapRecurrent(outerMap, path, 0, mainIteration, valuesToEncode);
            mainIteration++;
        }

        new CborEncoder(baos).encode(outerMap);
        return baos.toByteArray();
    }

    private static String getHeaderByte1(Context context, String packetTypeHex){
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        String res = binToHex(reverseString(preferences.getString(CURRENT_LOLAN_HEADER, "0111010000000").substring(0, 5)) + packetTypeHex);
        Log.d(TAG, "getHeaderByte1: RES: " + res);
        return res;
    }

    private static String getHeaderByte2(Context context){
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        String res = binToHex(reverseString(preferences.getString(CURRENT_LOLAN_HEADER, "0111010000000").substring(5)));
        Log.d(TAG, "getHeaderByte2: RES: " + res);
        return res;
    }

    private static byte getHeaderByte2Byte(Context context){
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        String res = binToHex(reverseString(preferences.getString(CURRENT_LOLAN_HEADER, "0111010000000").substring(5)));
        Log.d(TAG, "getHeaderByte2: RES: " + res);
        return hexStringToByteArray(res)[0];
    }


    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        if(bytes.length < 2){
            return CBOR_ERROR;
        }
        char[] hexChars = new char[bytes.length * 2];
        for(int i=0; i<bytes.length; i++){
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String CRC_calc(byte[] data, int size) {

        int uint16crc = 0, uint16q = 0;
        int uint8c = 0;
        size = data.length;

        for(int i=0; i<size; i++){
            uint8c = data[i] & 0xff;
            uint16q = (short)((uint16crc ^ uint8c) & 0x0F) & 0xffff;
            uint16crc = (short)((uint16crc >> 4) ^ (uint16q * 0x1081)) & 0xffff;
            uint16q = (short) ((uint16crc ^ (uint8c >> 4)) & 0x0F) & 0xffff;
            uint16crc = (short) ((uint16crc >> 4) ^ (uint16q * 0x1081)) & 0xffff;
        }

        int res = (short) (((uint16crc << 8) & 0xFF00) | ((uint16crc >> 8) & 0x00FF)) & 0xFFFF;
        String result = String.format("%04X", res);

        return result;
    }

    private static boolean verifyCRC(byte[] bytesToVerify){

        byte[] slipPacketWithoutCrc = Arrays.copyOf(bytesToVerify, bytesToVerify.length - 2);
        String crcFromRecvBytes = CRC_calc(slipPacketWithoutCrc, slipPacketWithoutCrc.length);
        Log.d(TAG, "verifyCRC: crcFromRecvBytes: " + crcFromRecvBytes);

        byte[] recvCRC = new byte[2];
        recvCRC[0] = bytesToVerify[bytesToVerify.length-2];
        String recvCRCStr1 = String.format("%02X", recvCRC[0]);
        recvCRC[1] = bytesToVerify[bytesToVerify.length-1];
        String recvCRCStr2 = String.format("%02X", recvCRC[1]);
        String recvCrcStr = recvCRCStr1.concat(recvCRCStr2);
        Log.d(TAG, "verifyCRC: recvCrcStr " + recvCrcStr);

        return crcFromRecvBytes.equals(recvCrcStr);
    }

    public static String decodeCborReadSingle(List<Byte> inputBytes, Handler guiHandler, TextView tvLolanVal)
            throws CborException, IOException {

        String attr1 = byteToBin(inputBytes.get(0));
        String attr2 = byteToBin(inputBytes.get(1));
        String binaryAttributes = attr2.concat(attr1);

        byte[] inputBytesToVerify = new byte[inputBytes.size() - 1];
        for(int i=0; i<inputBytes.size() - 1; i++)
            inputBytesToVerify[i] = inputBytes.get(i);

        if(inputBytes.size() > 10 && verifyCRC(inputBytesToVerify)) {

            /*
            String strPacketCounter = byteToBin(inputBytes.get(2));

            String srcAdress1 = byteToBin(inputBytes.get(3));
            String srcAdress2 = byteToBin(inputBytes.get(4));
            String strSrcAddress = srcAdress2.concat(srcAdress1);

            String dstAdress1 = byteToBin(inputBytes.get(5));
            String dstAdress2 = byteToBin(inputBytes.get(6));
            String dstSrcAddress = dstAdress2.concat(dstAdress1);

            int packetTypeInt = Integer.parseInt(binaryAttributes.substring(binaryAttributes.length() - 3), 2);
            final String packetType;
            switch (packetTypeInt) {
                case 0:
                    packetType = "802.15.4 BEACON";
                    break;
                case 1:
                    packetType = "802.15.4 DATA";
                    break;
                case 2:
                    packetType = "802.15.4 ACK";
                    break;
                case 3:
                    packetType = "802.15.4 MAC";
                    break;
                case 4:
                    packetType = "LoLaN INFORM";
                    break;
                case 5:
                    packetType = "LoLaN GET";
                    break;
                case 6:
                    packetType = "LoLaN SET";
                    break;
                case 7:
                    packetType = "LoLaN CONTROL";
                    break;
                default:
                    packetType = "NO INFO";
                    break;
            }
            */

            byte[] bytesToDecode = new byte[inputBytes.size() - 10];
            for (int i = 7, j = 0; i < inputBytes.size() - 3; i++, j++) {
                bytesToDecode[j] = inputBytes.get(i);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDecode);
            try {
                new CborDecoder(bais).decode(dataItem -> {
                    // MAP
                    if (dataItem.getMajorType().getValue() == 5) {
                        Map result = (Map) dataItem;
                        final String mapItemsToDisplay = parseDataItemMap(result, new StringBuilder());
                        guiHandler.post(() -> {
                            if (tvLolanVal != null) {
                                tvLolanVal.setTextColor(Color.rgb(0,0,0));
                                tvLolanVal.append(mapItemsToDisplay);
                            }
                        });
                        decodedValue = mapItemsToDisplay;
                    } else if (dataItem.getMajorType().getValue() == -1) {
                        // ERROR
                        guiHandler.post(() -> {
                            tvLolanVal.append(CBOR_ERROR);
                            tvLolanVal.setTextColor(Color.rgb(200,0,0));
                        });
                    } else if (dataItem.getMajorType().getValue() == 7) {
                        // TYPE SPECIAL
                        Log.d(TAG, "SPECIAL: " + dataItem.toString());
                    } else if (dataItem.getMajorType().getValue() == 0) {
                        // UNSIGNED_INTEGER
                        guiHandler.post(() -> {
                            tvLolanVal.setTextColor(Color.rgb(0,0,0));
                            tvLolanVal.append( dataItem.toString());
                        });
                    } else if (dataItem.getMajorType().getValue() == 2) {
                        // Byte String
                        // TODO DATA80
                        ByteString result = (ByteString) dataItem;
                        StringBuilder sb = new StringBuilder();
                        for (byte b : result.getBytes()){
                            sb.append(String.format("%02X", b));
                        }

                        // TODO: parse byte80 data
                        String res = data80Decode(result.getBytes());

                        guiHandler.post(() -> {
                            if (tvLolanVal != null) {
                                tvLolanVal.append(res);
                                tvLolanVal.setTextColor(Color.rgb(0,0,0));
                            }
                        });
                        decodedValue = "DATA80";
                    }  else {
                        guiHandler.post(() -> {
                            tvLolanVal.setTextColor(Color.rgb(0,0,0));
                            tvLolanVal.append(dataItem.toString());
                        });
                        decodedValue = dataItem.toString();
                    }
                });
            } catch (CborException e) {
                e.printStackTrace();
            }

            bais.close();
        } else {
            decodedValue = CBOR_ERROR;
            guiHandler.post(() -> {
                tvLolanVal.setText(CBOR_ERROR);
                tvLolanVal.setTextColor(Color.rgb(200,0,0));
            });
        }

        return decodedValue;
    }

    public static String decodeCborReadMulti(List<Byte> inputBytes) throws CborException, IOException {

        /*
        String attr1 = byteToBin(inputBytes.get(0));
        String attr2 = byteToBin(inputBytes.get(1));
        String binaryAttributes = attr2.concat(attr1);
        */

        byte[] inputBytesToVerify = new byte[inputBytes.size() - 1];
        for(int i=0; i<inputBytes.size() - 1; i++) {
            inputBytesToVerify[i] = inputBytes.get(i);
        }

        if(inputBytes.size() > 10 && verifyCRC(inputBytesToVerify)) {
            byte[] bytesToDecode = new byte[inputBytes.size() - 10];
            for (int i = 7, j = 0; i < inputBytes.size() - 3; i++, j++) {
                bytesToDecode[j] = inputBytes.get(i);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDecode);
            try {
                new CborDecoder(bais).decode(dataItem -> {
                    // MAP
                    if (dataItem.getMajorType().getValue() == 5) {
                        Map result = (Map) dataItem;
                        decodedValue = parseDataItemMap(result, new StringBuilder());
                    } else if (dataItem.getMajorType().getValue() == -1) {
                        // ERROR
                        decodedValue = CBOR_ERROR;
                    } else if (dataItem.getMajorType().getValue() == 7) {
                        // TYPE SPECIAL
                        decodedValue = "SPECIAL VALUE";
                    } else if (dataItem.getMajorType().getValue() == 0) {
                        // UNSIGNED_INTEGER
                        decodedValue = dataItem.toString();
                    } else if (dataItem.getMajorType().getValue() == 2) {
                        // Byte String
                        ByteString result = (ByteString) dataItem;
                        StringBuilder sb = new StringBuilder();
                        for (byte b : result.getBytes()){
                            sb.append(String.format("%02X", b));
                        }
                        decodedValue = data80Decode(result.getBytes());
                    }  else {
                        decodedValue = dataItem.toString();
                    }
                });
            } catch (CborException e) {
                e.printStackTrace();
            }
            bais.close();
        } else {
            decodedValue = CRC_ERROR;
        }
        return decodedValue;
    }

    public static String decodeCborWriteSingle(
            List<Byte> inputBytes,
            Handler guiHandler,
            TextView tvResponse,
            TextView tvLolanVal,
            String valueToSet,
            final int deviceAddress) throws CborException, IOException {

        /*
        String attr1 = byteToBin(inputBytes.get(0));
        String attr2 = byteToBin(inputBytes.get(1));
        String binaryAttributes = attr2.concat(attr1);
        */

        byte[] inputBytesToVerify = new byte[inputBytes.size() - 1];
        for(int i=0; i<inputBytes.size() - 1; i++)
            inputBytesToVerify[i] = inputBytes.get(i);

        if(inputBytes.size() > 10 && verifyCRC(inputBytesToVerify)) {

            /*
            String strPacketCounter = byteToBin(inputBytes.get(2));

            String srcAdress1 = byteToBin(inputBytes.get(3));
            String srcAdress2 = byteToBin(inputBytes.get(4));
            String strSrcAddress = srcAdress2.concat(srcAdress1);

            String dstAdress1 = byteToBin(inputBytes.get(5));
            String dstAdress2 = byteToBin(inputBytes.get(6));
            String dstSrcAddress = dstAdress2.concat(dstAdress1);

            int packetTypeInt = Integer.parseInt(binaryAttributes.substring(binaryAttributes.length() - 3), 2);
            final String packetType;
            switch (packetTypeInt) {
                case 0:
                    packetType = "802.15.4 BEACON";
                    break;
                case 1:
                    packetType = "802.15.4 DATA";
                    break;
                case 2:
                    packetType = "802.15.4 ACK";
                    break;
                case 3:
                    packetType = "802.15.4 MAC";
                    break;
                case 4:
                    packetType = "LoLaN INFORM";
                    break;
                case 5:
                    packetType = "LoLaN GET";
                    break;
                case 6:
                    packetType = "LoLaN SET";
                    break;
                case 7:
                    packetType = "LoLaN CONTROL";
                    break;
                default:
                    packetType = "NO INFO";
                    break;
            }

            */



            /**
             * Decoding only CBOR packet, without Slip packet overhead and CRC
             */
            byte[] bytesToDecode = new byte[inputBytes.size() - 10];
            for (int i = 7, j = 0; i < inputBytes.size() - 3; i++, j++)
                bytesToDecode[j] = inputBytes.get(i);

            ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDecode);
            try {
                new CborDecoder(bais).decode(dataItem -> {
                    tvResponse.setTextColor(Color.rgb(0,0,0));
                    tvLolanVal.setTextColor(Color.rgb(0,0,0));
                    if (dataItem.getMajorType().getValue() == -1) {
                        // ERROR
                        guiHandler.post(() -> tvResponse.setText("ERROR"));
                    } else if (dataItem.getMajorType().getValue() == 0) {
                        // UNSIGNED_INTEGER
                        guiHandler.post(() -> tvResponse.setText(dataItem.toString()));
                    } else  if (dataItem.getMajorType().getValue() == 7) {
                        // TYPE SPECIAL
                        Log.d(TAG, "onDataItem: RESPONSE: SPECIAL: " + dataItem.toString());
                    } else if (dataItem.getMajorType().getValue() == 5) {
                        // MAP
                        Map result = (Map) dataItem;
                        final String mapItemsToDisplay = parseDataItemMap(result, new StringBuilder());
                        guiHandler.post(() -> {
                            if(!tvResponse.getText().toString().contains("STATUS")){
                                tvResponse.setText("");
                            }
                            if(!tvLolanVal.getText().toString().contains("ERROR")
                                    || !tvLolanVal.getText().toString().contains("SET")) {
                                tvLolanVal.setText("");
                            }

                            if (mapItemsToDisplay.trim().contains("200")
                                    && !mapItemsToDisplay.trim().contains("47")) {
                                tvResponse.setText("STATUS: OK");
                                tvLolanVal.append(valueToSet);
                            }
                            if (mapItemsToDisplay.trim().contains("204")){
                                tvResponse.setText("STATUS: Variables not updated");
                                tvLolanVal.append(" ERROR 204");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("207")){
                                tvResponse.setText("STATUS: All variables updated\n");
                                tvLolanVal.append("SET: " + valueToSet + "\n");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }

                            if (mapItemsToDisplay.trim().contains("404")){
                                tvResponse.append("STATUS: Variable not found");
                                tvLolanVal.append("ERROR 404");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("405")){
                                tvResponse.append("STATUS: Variable is read-only\n");
                                tvLolanVal.setText("ERROR 405");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("470")){
                                tvResponse.append("STATUS: Errors occured");
                                tvLolanVal.setText("ERROR 470\n");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("471")){
                                tvResponse.append("STATUS: No variables were updated\n");
                                tvLolanVal.append("ERROR 471\n");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("472")){
                                tvResponse.append("STATUS: Variable type mismatch\n");
                                tvLolanVal.append("ERROR 472");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("473")){
                                tvResponse.setText("STATUS: Value over the range\n");
                                tvLolanVal.append("ERROR 473\n");
                                tvResponse.setTextColor(Color.rgb(200,0,0));
                            }

                        });
                        decodedValue = mapItemsToDisplay;
                    } else {
                        // UNICODE STRING
                        guiHandler.post(() -> {
                            tvResponse.setText("COMPLETED: " + dataItem.toString());
                            tvResponse.setTextColor(Color.rgb(0,0,0));
                        });
                        decodedValue = dataItem.toString();
                    }

                });
            } catch (CborException e) {
                e.printStackTrace();
            }

            bais.close();
        } else {
            // ERROR
            guiHandler.post(() -> {
                tvResponse.setText("ERROR");
                tvResponse.setTextColor(Color.rgb(200,0,0));
                tvLolanVal.setText(CRC_ERROR);
                tvLolanVal.setTextColor(Color.rgb(200,0,0));
            });
            decodedValue = CRC_ERROR;
        }

        return decodedValue;
    }
    public static HashMap<String, String> statusMap;
    public static String decodeCborWriteMulti(
            BluetoothGatt gatt,
            List<Byte> inputBytes,
            Handler guiHandler,
            TextView tvReply,
            TextView tvCurrentStatus,
            final String deviceName,
            final String deviceId) throws CborException, IOException {

        /*
        String attr1 = byteToBin(inputBytes.get(0));
        String attr2 = byteToBin(inputBytes.get(1));
        String binaryAttributes = attr2.concat(attr1);
        */

        byte[] inputBytesToVerify = new byte[inputBytes.size() - 1];
        for(int i=0; i<inputBytes.size() - 1; i++) {
            inputBytesToVerify[i] = inputBytes.get(i);
        }
        if(inputBytes.size() > 10 && verifyCRC(inputBytesToVerify)) {

            byte[] bytesToDecode = new byte[inputBytes.size() - 10];
            for (int i = 7, j = 0; i < inputBytes.size() - 3; i++, j++) {
                bytesToDecode[j] = inputBytes.get(i);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDecode);
            try {
                new CborDecoder(bais).decode(dataItem -> {
                    if (dataItem.getMajorType().getValue() == -1) {
                        // ERROR
                        guiHandler.post(() -> {
                            tvReply.setText("ERROR");
                            tvCurrentStatus.setText("ERROR");
                            tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            tvReply.setTextColor(Color.rgb(200,0,0));
                            decodedValue = "ERROR";

                        });
                    } else if (dataItem.getMajorType().getValue() == 0) {
                        // UNSIGNED_INTEGER
                        guiHandler.post(() -> {
                            tvReply.append(dataItem.toString());
                            tvCurrentStatus.setText(deviceName + " " + deviceId + "\nCOMPLETED");
                            tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                            tvReply.setTextColor(Color.rgb(0,0,0));
                            decodedValue = dataItem.toString();
                        });
                    } else  if (dataItem.getMajorType().getValue() == 7) {
                        // TYPE SPECIAL
                        guiHandler.post(() -> {
                            tvReply.append(dataItem.toString());
                            tvCurrentStatus.setText(deviceName + " " + deviceId + "\nCOMPLETED: " + dataItem.toString());
                            tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                            tvReply.setTextColor(Color.rgb(0,0,0));

                        });
                    } else if (dataItem.getMajorType().getValue() == 5) {
                        // MAP
                        Map result = (Map) dataItem;
                        final String mapItemsToDisplay = parseDataItemMap(result, new StringBuilder());
                        statusMap = parseDataItemMap(result);
                        guiHandler.post(() -> {
                            if(!tvReply.getText().toString().contains("STATUS")){
                                tvReply.setText("");
                            }
                            if(!tvCurrentStatus.getText().toString().contains("ERROR")
                                    || !tvCurrentStatus.getText().toString().contains("COMPLETED")) {
                                tvCurrentStatus.setText("");
                            }

                            if (mapItemsToDisplay.trim().contains("200")
                                    && !mapItemsToDisplay.trim().contains("470")) {
                                tvCurrentStatus.setText(deviceName + " " + deviceId + "\nCOMPLETED: 200 OK\n");
                                tvReply.setText(deviceName + " " + deviceId + "\nCOMPLETED" + "\n");
                                tvReply.setTextColor(Color.rgb(0,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("204")){
                                tvCurrentStatus.setText(deviceName + " " + deviceId + "\nERROR: 204 Variables not updated\n");
                                tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                                tvReply.setText(deviceName + " " + deviceId + " ERROR 204\n");
                                tvReply.setTextColor(Color.rgb(0,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("207")){
                                tvCurrentStatus.setText(deviceName + " " + deviceId + "\nCOMPLETED: 207 All variables updated\n");
                                tvReply.setText(deviceName + " " + deviceId + " \nCOMPLETED" + "\n");
                                tvReply.setTextColor(Color.rgb(0,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("404")){
                                tvCurrentStatus.setText(deviceName + " " + deviceId + "\nERROR: 404 Variable not found\n");
                                tvReply.setText(deviceName + " " + deviceId + " ERROR 404\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("405")){
                                tvReply.setText(deviceName + " " + deviceId + "\nSTATUS: 405 Variable is read-only\n");
                                tvCurrentStatus.setText(deviceName + " " + deviceId + " ERROR 405\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("470")){
                                tvReply.setText(deviceName + " " + deviceId + "\nSTATUS: 470 Errors occured\n");
                                tvCurrentStatus.setText(deviceName + " " + deviceId + " ERROR 470\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("471")){
                                tvReply.append(deviceName + " " + deviceId + "\nSTATUS: 471 No variables were updated\n");
                                tvCurrentStatus.setText(deviceName + " " + deviceId + "\n ERROR 471\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("472")){
                                tvReply.append(deviceName + " " + deviceId + "\nSTATUS: 472 Variable type mismatch\n");
                                tvCurrentStatus.setText(deviceName + " " + deviceId + " \nERROR 472\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                            if (mapItemsToDisplay.trim().contains("473")){
                                tvReply.append(deviceName + " " + deviceId + "\nSTATUS: 473 Value over the range\n");
                                tvCurrentStatus.setText(deviceName + " " + deviceId + " \nERROR 473\n");
                                tvReply.setTextColor(Color.rgb(200,0,0));
                                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
                            }
                        });
                        decodedValue = mapItemsToDisplay;
                    } else {
                        // UNICODE STRING
                        guiHandler.post(() -> {
                            tvReply.setText(dataItem.toString());
                            tvCurrentStatus.setText("COMPLETED: " + dataItem.toString());
                            tvReply.setTextColor(Color.rgb(0,0,0));
                            tvCurrentStatus.setTextColor(Color.rgb(0,0,0));
                        });
                        decodedValue = dataItem.toString();
                    }
                });
            } catch (CborException e) {
                e.printStackTrace();
            }
            bais.close();
        } else {
            // PACKET ERROR
            guiHandler.post(() -> {
                tvReply.setText(CRC_ERROR);
                tvCurrentStatus.setText(deviceName + " " + deviceId + "\n" + CBOR_ERROR);
                tvReply.setTextColor(Color.rgb(200,0,0));
                tvCurrentStatus.setTextColor(Color.rgb(200,0,0));
            });
            decodedValue = CRC_ERROR;
        }
        gatt.disconnect();
        return decodedValue;
    }


    private static String parseDataItemMap(DataItem dataItem, StringBuilder sb){
        Map result = (Map) dataItem;
        for(DataItem d : result.getValues()) {
            if (d.getMajorType().getValue() != 5) {
                sb.append( d.toString() + " ");
            } else {
                parseDataItemMap(d, sb);
            }
        }
        return sb.toString();
    }

    private static HashMap<String, String> parseDataItemMap(DataItem dataItem){
        Map result = (Map) dataItem;
        HashMap<String, String> pathStatusMap = new HashMap<>();

        for(DataItem key : result.getKeys()){
            if(result.get(key).getMajorType().getValue() != 5){
                Log.d(TAG, "parseDataItemMap: KEY: " + key.toString()+ " VALUE: " + result.get(key).toString());
                pathStatusMap.put(key.toString(), result.get(key).toString());
            } else if(result.get(key).getMajorType().getValue() == 5) {
                String k1 = key.toString();
                Map result2 = (Map) result.get(key);
                for(DataItem key2 : result2.getKeys()){
                    String k2 = key2.toString();
                    if(result2.get(key2).getMajorType().getValue() != 5){
                        Log.d(TAG, "parseDataItemMap: RESULT: [" + k1+"," + k2 +","+ "0] : " + result2.get(key2).toString());
                        pathStatusMap.put("["+k1+","+k2+","+"0]" , result2.get(key2).toString());
                    } else {
                        Map result3 = (Map) result2.get(key2);
                        for(DataItem key3 : result3.getKeys()) {
                            String k3 = key3.toString();
                            Log.d(TAG, "parseDataItemMap: RESULT: [" + k1+"," + k2+"," + k3 + "] : " + result3.get(key3));
                            pathStatusMap.put("["+k1+","+k2+","+ k3+"]", result3.get(key3).toString());
                        }
                    }
                }
            }
        }

        return pathStatusMap;
    }




    private static String generateHexSlipPacket(String hexStringData){
        int hexStringLength = hexStringData.length();
        StringBuilder slipPacketStringBuilder = new StringBuilder();
        for (int i = 0; i < hexStringLength; i += 2) {
            if(hexStringData.substring(i, i+2).toLowerCase().equals("c0")){
                slipPacketStringBuilder.append("DBDC");
            } else if (hexStringData.substring(i, i+2).toLowerCase().equals("db")){
                slipPacketStringBuilder.append("DBDD");
            } else {
                slipPacketStringBuilder.append(hexStringData.substring(i, i+2));
            }
        }
        slipPacketStringBuilder.append("C0");
        return slipPacketStringBuilder.toString();
    }


    private static String data80Decode(byte[] receivedDataBytes){
        /*
        StringBuilder dataDecoded = new StringBuilder();

        if(receivedDataBytes[0] == (byte) 0xFE && receivedDataBytes[1] == (byte) 0xCA) {
            dataDecoded.append("MAGIC WORD OK\n");

            String decodedType = byteToBin(receivedDataBytes[2]);

            int timestampType = Integer.parseInt(decodedType.substring(0,2), 2);
            switch (timestampType){
                case 0:
                    dataDecoded.append("no timestamp\n");
                    break;
                case 1:
                    dataDecoded.append("8-bit unsigned integer timestamp\n");
                    break;
                case 2:
                    dataDecoded.append("16-bit unsigned integer timestamp\n");
                    break;
                case 3:
                    dataDecoded.append("32-bit unsigned integer timestamp\n");
                    break;
                default:
                    dataDecoded.append("timestamp\n");
                    break;
            }

            int dataType = Integer.parseInt(decodedType.substring(2,6), 2);
            switch (dataType){
                case 0:
                    dataDecoded.append("8-bit unsigned integer\n");
                    break;
                case 1:
                    dataDecoded.append("8-bit signed integer\n");
                    break;
                case 2:
                    dataDecoded.append("16-bit unsigned integer\n");
                    break;
                case 3:
                    dataDecoded.append("16-bit signed integer\n");
                    break;
                case 4:
                    dataDecoded.append("24-bit unsigned integer\n");
                    break;
                case 5:
                    dataDecoded.append("24-bit signed integer\n");
                    break;
                case 6:
                    dataDecoded.append("32-bit unsigned integer\n");
                    break;
                case 7:
                    dataDecoded.append("32-bit signed integer\n");
                    break;
                case 8:
                    dataDecoded.append("64-bit unsigned integer\n");
                    break;
                case 9:
                    dataDecoded.append("64-bit signed integer\n");
                    break;
                default:
                    dataDecoded.append("reserved\n");
                    break;
            }
            int numOfValuesPerSet = Integer.parseInt(decodedType.substring(6,8), 2);
            switch (numOfValuesPerSet) {
                case 0:
                    dataDecoded.append("single value\n");
                    break;
                case 1:
                    dataDecoded.append("2 values per set\n");
                    break;
                case 2:
                    dataDecoded.append("3 values per set\n");
                    break;
                case 3:
                    dataDecoded.append("4 values per set\n");
                    break;
                default:
                    dataDecoded.append("number of values per set\n");
                    break;
            }

            int number = receivedDataBytes[3];
            // TIMESTAMP: VALUE1, VALUE2, VALUE3
            java.util.Map<Integer, ArrayList<Integer>> data;
            // 64-bit unsigned integer


        } else {
            dataDecoded.append("NO MAGIC WORD\n");
            return dataDecoded.toString();
        }
        */

        return "Data80";
    }


    public static List<Byte> slipEncodeData(BluetoothGattCharacteristic characteristic){
        List<Byte> outputEncodedByteList = new ArrayList<>();
        for(int i=0; i<characteristic.getValue().length; i++) {
            switch (characteristic.getValue()[i]){
                case SLIP_END_BYTE:
                    outputEncodedByteList.add(SLIP_ESC_BYTE);
                    outputEncodedByteList.add(SLIP_ESC_END_BYTE);
                    break;
                case SLIP_ESC_BYTE:
                    outputEncodedByteList.add(SLIP_ESC_BYTE);
                    outputEncodedByteList.add(SLIP_ESC_ESC_BYTE);
                    break;
                default:
                    outputEncodedByteList.add(characteristic.getValue()[i]);
                    break;
            }
        }
        outputEncodedByteList.add(SLIP_END_BYTE);
        return outputEncodedByteList;
    }

    public static List<Byte> slipDecodeData(List<Byte> bytesList){
        List<Byte> outputDecodedByteList = new ArrayList<>();
        for(int i=0; i<bytesList.size(); i++) {
            switch (bytesList.get(i)){
                case SLIP_ESC_END_BYTE:
                    // DC
                    if (i > 0) {
                        // DB
                        if (bytesList.get(i - 1) == SLIP_ESC_BYTE) {
                            outputDecodedByteList.set(i - 1, SLIP_END_BYTE);
                        } else {
                            outputDecodedByteList.add(bytesList.get(i));
                        }
                    } else {
                        outputDecodedByteList.add(bytesList.get(i));
                    }
                    break;
                case SLIP_ESC_ESC_BYTE:
                    //DD
                    if(i > 0) {
                        if (bytesList.get(i - 1) != SLIP_ESC_BYTE) {
                            outputDecodedByteList.add(bytesList.get(i));
                        }
                        // else DB DD => skip DD
                    } else {
                        outputDecodedByteList.add(bytesList.get(i));
                    }
                    break;
                default:
                    outputDecodedByteList.add(bytesList.get(i));
                    break;
            }
        }
        return outputDecodedByteList;
    }
}

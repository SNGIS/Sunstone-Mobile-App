package com.sunstone.utility;

import java.util.UUID;

public class BluetoothUtils {

    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHARACTERISTIC = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int DELAY_VERY_LOW = 100;
    public static final int DELAY_LOW = 200;
    public static final int DELAY_STANDARD = 500;
    public static final int DELAY_HIGH = 800;
    public static final int DELAY_VERY_HIGH = 1200;
    public static final int DELAY_SUPER_HIGH = 1600;

    public static final String SLIP_END = "C0";
    public static final String SLIP_ESC = "DB";
    public static final String SLIP_ESC_END = "DC";
    public static final String SLIP_ESC_ESC = "DD";

    public static final byte SLIP_END_BYTE = (byte) 0xC0;
    public static final byte SLIP_ESC_BYTE = (byte) 0xDB;
    public static final byte SLIP_ESC_END_BYTE = (byte) 0xDC;
    public static final byte SLIP_ESC_ESC_BYTE = (byte) 0xDD;

    public static final byte LOLAN_HEADER_INFORM = (byte) 0x02;
    public static final byte LOLAN_HEADER_GET = (byte) 0x05;
    public static final byte LOLAN_HEADER_SET = (byte) 0x06;
    public static final byte LOLAN_HEADER_ATTRIBUTES = (byte) 0x74;

    public static final int MANUFACTURER_ID = 2065;

    public static final String STR_DISCONNECTED = "DISCONNECTED";
    public static final String STR_CONNECTED = "CONNECTED ";
    public static final String STR_REFRESHING = "REFRESHING...";
    public static final String STR_TIMEOUT = "Timeout";


    public static final String STR_OK = "OK";
    public static final String STR_STOP = "STOP";
    public static final String STR_REFRESH = "REFRESH";
    public static final String STR_SET = "SET";

    public static final String VALUE_OVER_THE_RANGE = "Value over the range";

    public static final String COMPLETED = "COMPLETED";
    public static final String ERROR = "ERROR";
    public static final String CBOR_ERROR =  "PACKET ERROR OCCURED";
    public static final String CRC_ERROR =  "CRC ERROR OCCURED";

}

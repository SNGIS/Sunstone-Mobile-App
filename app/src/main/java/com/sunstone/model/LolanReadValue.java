package com.sunstone.model;

public class LolanReadValue {
    String deviceName;
    String deviceId;
    String readValue;

    public LolanReadValue(String deviceName, String deviceId, String readValue) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.readValue = readValue;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getReadValue() {
        return readValue;
    }

    public void setReadValue(String readValue) {
        this.readValue = readValue;
    }
}

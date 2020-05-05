package com.sunstone.model;

public class DfuDeviceStatus {
    String deviceName;
    String deviceId;
    String currentStatus;

    public DfuDeviceStatus(String deviceName, String deviceId, String currentStatus) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.currentStatus = currentStatus;
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

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}

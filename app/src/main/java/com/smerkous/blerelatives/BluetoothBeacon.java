package com.smerkous.blerelatives;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.UUID;

public class BluetoothBeacon {
    BluetoothDevice device;
    public int rssi;
    byte[] scanRecord;

    private String name;
    public int type;
    public long startTime = 0;
    int txPower;
    ArrayList<UUID> uuids;

    public void resetTime() {
        startTime = System.currentTimeMillis();
    }

    public long elapsedMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public String getName() {
        return device.getName();
    }

    public String getUnique() {
        return device.getAddress();
    }
}

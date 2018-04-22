package com.smerkous.greengrass.ble;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.UUID;

public class BluetoothBeacon {
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanRecord;
    public String name;
    public int type;
    public long startTime = 0;
    public int txPower;
    public ArrayList<UUID> uuids;

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

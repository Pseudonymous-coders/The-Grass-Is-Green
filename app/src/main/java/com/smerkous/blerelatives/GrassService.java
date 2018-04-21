package com.smerkous.blerelatives;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.smerkous.blerelatives.MainActivity.Log;

public class GrassService extends IntentService {
    private static boolean isScanning = false;
    private static HashMap<String, BluetoothBeacon> scannedDevices = new HashMap<>();
    private static Timer scanTimer;
    private static TimerTask scanTask;
    private static Handler handler = new Handler();
    private static BleDevicesScanner BLEScanner = null;

    public GrassService() {
        super("GrassService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log("Finished resetting the bluetooth adapter");
        resumeScanning();
        Log("Starting the scan timer");
        if(scanTimer != null) {
            scanTimer.cancel();
        }

        if(scanTask != null) {
            scanTask.cancel();
        }

        scanTimer = new Timer();
        scanTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log("DEVICES: " + Arrays.toString(scannedDevices.keySet().toArray()));
                        for (final String key : scannedDevices.keySet()) {
                            BluetoothBeacon b = scannedDevices.get(key);
                            if (b.elapsedMillis() > 2000) {
                                scannedDevices.remove(key);
                            }
                        }
                    }
                });
            }
        };
        scanTimer.schedule(scanTask, 0, 1000);
    }

    /*private void autoStartScan() {
        if(BleUtils.getBleStatus(this) == BleUtils.STATUS_BLE_ENABLED) {
            manager.disconnect();
            startScanning(null);
        }
    }*/

    public void resumeScanning() {
        Log("Resuming scan");
        if(!isScanning) {
            BLEScanner = null;
            isScanning = true;
            startScanning(null);
        }
    }

    public void stopScanning() {
        Log("Stopping scan");
        if(BLEScanner != null) {
            BLEScanner.stop();
            BLEScanner = null;
        }
        isScanning = false;
    }

    private void startScanning(final UUID[] services) {
        Log("Starting the scan");

        //Stop scanning before initiating a new scan
        stopScanning();

        //Get the bluetooth adapter
        BluetoothAdapter bleAdapter = BleUtils.getBluetoothAdapter(getApplicationContext());
        if(BleUtils.getBleStatus(this) != BleUtils.STATUS_BLE_ENABLED) {
            Log("The bluetooth device is not specified or permissions aren't available");
        } else {
            BLEScanner = new BleDevicesScanner(bleAdapter, services, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device.getName() != null) { //It must have a name for us to be able to scan it
                        if (scannedDevices == null) {
                            scannedDevices = new HashMap<>();
                        }

                        try {
                            //@TODO this is hacky way because the name could be changed (we need to look for service UUIDS in the future)
                            if (device.getName().contains("TheGrassIsGreen")) {
                                final BluetoothBeacon beacon = new BluetoothBeacon();
                                beacon.device = device;
                                beacon.rssi = rssi;
                                beacon.scanRecord = scanRecord;
                                beacon.resetTime();

                                //Check for scan duplicates
                                if (!scannedDevices.containsKey(beacon.getUnique())) {
                                    Log("Adding device: " + beacon.getUnique());
                                }

                                //Add or update the scanned devices
                                scannedDevices.put(beacon.getUnique(), beacon);
                            }
                        } catch(NullPointerException ignored) {}
                    }
                }
            }, new BleDevicesScanner.FinishedScanCallback() {
                @Override
                public void scanFinished() {
                    Log("Stopped scanning... starting again in 5 seconds");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scannedDevices.clear();
                            resumeScanning();
                        }
                    }, 5000); //Scan in another 5 seconds
                }
            });
            BLEScanner.start();
            Log("Started! Is Scanning " + BLEScanner.isScanning());
        }
    }
}

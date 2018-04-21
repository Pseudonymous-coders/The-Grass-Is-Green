package com.smerkous.blerelatives;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BleUtils.ResetBluetoothAdapterListener, BleManager.BleManagerListener {

    private static final int REQUEST_BLUETOOTH = 1;

    private static final int PERMISSION_REQUEST_LOCATION = 1;

    private static BleDevicesScanner BLEScanner = null;
    private static BleManager manager;
    private static boolean isScanning = false;

    public static void Log(String toLog) {
        Log.d("TheGrassIsGreen", toLog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null) {
            Log("Starting...");
            boolean isEnabled = manageBluetooth();
            boolean isLocationOn = manageLocation();

            if(isEnabled && isLocationOn) {
                BleUtils.resetBluetoothAdapter(this, this);
            }

            manager = BleManager.getInstance(this);
            onServicesDiscovered();
        }

        requestPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.setBleListener(this);
    }

    private boolean manageBluetooth() {
        boolean isEnabled = true;
        int errorId = 0;

        final int bleStatus = BleUtils.getBleStatus(getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                errorId = R.string.report_ble_not_available;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE:
                errorId = R.string.report_bluetooth_not_available;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_DISABLED:
                isEnabled = false;
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_BLUETOOTH);
                break;
        }

        if (errorId != 0) {
            //REPORT THE ERROR
            Log("There was a problem initializing BLE!");
        }
        Log("STATUS " + isEnabled);
        return isEnabled;
    }

    private boolean manageLocation() {
        boolean isReady = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int locMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch(Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            isReady = locMode != Settings.Secure.LOCATION_MODE_OFF;

            if(!isReady) {
                //REPORT THAT THE LOCATION SERVICES ARE
                Log("Failed to get location services");
            }
        }
        return isReady;
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log("RESULT");
        switch (reqCode) {
            case REQUEST_BLUETOOTH:
                if(resCode == Activity.RESULT_OK) {
                    resumeScanning();
                } else if(resCode == Activity.RESULT_CANCELED) {
                    //REPORT THE ERROR THAT THE USER HAS CANCELED BLUETOOTH
                    Log("The user declined Bluetooth permissions");
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        Log("Requesting permissions");
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log("Location permission granted");
                    autoStartScan();

                    //Update the UI
                } else {
                    //SHOW ERROR THAT SCANNING WAS NOT ALLOWED
                    Log("Scanning was not allowed");
                }
                break;
            }
            default:
                break;
        }
    }

    private void autoStartScan() {
        if(BleUtils.getBleStatus(this) == BleUtils.STATUS_BLE_ENABLED) {
            manager.disconnect();
            startScanning(null);
        }
    }

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
                    if(device.getName() != null)
                    Log("GOT DEVICE " + device.getName() + " " + Arrays.toString(device.getUuids()));
                }
            });
            BLEScanner.start();
            Log("Started! Is Scanning " + BLEScanner.isScanning());
        }
    }

    @Override
    public void resetBluetoothCompleted() {
        Log("Finished resetting the bluetooth adapter");
        resumeScanning();
    }


    @Override
    public void onConnected() {
        Log("CONNECTED");
    }

    @Override
    public void onConnecting() {
        Log("CONNECTING");
    }

    @Override
    public void onDisconnected() {
        Log("Disconnecting");
    }

    @Override
    public void onServicesDiscovered() {
        Log("Services discovered");
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        Log("RECIEVED " + new String(characteristic.getValue(), Charset.forName("UTF-8")).trim());
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }
}

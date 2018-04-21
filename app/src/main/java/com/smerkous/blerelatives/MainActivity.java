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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity implements BleUtils.ResetBluetoothAdapterListener {
    private static final int REQUEST_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_LOCATION = 1;

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

            //onServicesDiscovered();
        }

        requestPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        boolean isReady;
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
        return isReady;
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log("RESULT");
        switch (reqCode) {
            case REQUEST_BLUETOOTH:
                if(resCode == Activity.RESULT_OK) {
                    //resumeScanning();
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
                    //autoStartScan();

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

    @Override
    public void resetBluetoothCompleted() {
        Log("Starting service");
        startService(new Intent(this, GrassService.class));
    }
}

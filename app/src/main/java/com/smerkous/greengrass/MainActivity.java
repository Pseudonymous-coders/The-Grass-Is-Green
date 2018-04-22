package com.smerkous.greengrass;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.smerkous.greengrass.ble.BleUtils;
import com.smerkous.greengrass.services.GrassService;
import com.smerkous.greengrass.util.Dialogs;

import static com.smerkous.greengrass.services.GrassService.manageBluetooth;

public class MainActivity extends AppCompatActivity implements BleUtils.ResetBluetoothAdapterListener {
    public static final int REQUEST_BLUETOOTH = 1;
    public static final int PERMISSION_REQUEST_LOCATION = 1;
    public static boolean isBoot = false;
    private ConstraintLayout layout;

    public static void Log(String toLog) {
        Log.d("TheGrassIsGreen", toLog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null) {
            Log("Starting the main activity");
        } else {
            try {
                if (getIntent().getExtras().getBoolean("enable_ble", false)) {
                    Log("Requesting for bluetooth permissions...");
                    layout = findViewById(R.id.main_layout);
                    Glide.with(this)
                            .asDrawable()
                            .load(R.drawable.grass_meadow)
                            .apply(RequestOptions.centerCropTransform())
                            .into(new SimpleTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    layout.setBackground(resource);
                                }
                            });
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, REQUEST_BLUETOOTH);
                }
            } catch(NullPointerException ignored) {}

            try {
                isBoot = getIntent().getExtras().getBoolean("boot", false);
             } catch (NullPointerException ignored) {}
        }


        if(!isGrassServiceRunning()) {
            Log("The grass service is not running! Starting now...");
            startService(new Intent(getApplicationContext(), GrassService.class));
        }

        requestPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log("RESULT");
        switch (reqCode) {
            case REQUEST_BLUETOOTH:
                if(resCode == Activity.RESULT_OK) {
                    Log("Bluetooth enabled! Restarting service...");

                    if(isBoot) {
                        Log("Started at boot... going home");
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                    }

                    stopService(new Intent(getApplicationContext(), GrassService.class));
                    startService(new Intent(getApplicationContext(), GrassService.class));
                } else if(resCode == Activity.RESULT_CANCELED) {
                    Dialogs.ErrorDialog(this, "You are required to have bluetooth enabled", new Dialogs.ClickListener() {
                        @Override
                        public void onContinue() {
                            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBluetooth, REQUEST_BLUETOOTH);
                        }
                    });
                    Log("The user declined Bluetooth permissions");
                }
                break;
        }
    }

    private boolean isGrassServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GrassService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        Log("Requesting permissions");

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }

        try {
            if(Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED) == 0) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        } catch (Settings.SettingNotFoundException e) {
            Log("Accessibility Setting was not found!");
            e.printStackTrace();
        }

        manageBluetooth(getBaseContext(), this, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log("Location permission granted");
                } else {
                    Toast.makeText(getApplicationContext(), "Location will be required.", Toast.LENGTH_LONG).show();
                    Log("Scanning was not allowed");
                }
                requestPermissions();
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

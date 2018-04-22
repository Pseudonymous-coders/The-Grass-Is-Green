package com.smerkous.greengrass.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.accessibility.AccessibilityEvent;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.smerkous.greengrass.BlockAppActivity;
import com.smerkous.greengrass.MainActivity;
import com.smerkous.greengrass.R;
import com.smerkous.greengrass.apps.InstalledApps;
import com.smerkous.greengrass.ble.BleDevicesScanner;
import com.smerkous.greengrass.ble.BleUtils;
import com.smerkous.greengrass.ble.BluetoothBeacon;
import com.smerkous.greengrass.util.Dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.smerkous.greengrass.MainActivity.Log;
import static com.smerkous.greengrass.MainActivity.REQUEST_BLUETOOTH;

public class GrassService extends AccessibilityService implements BleUtils.ResetBluetoothAdapterListener {
    private static boolean isScanning = false;
    private static HashMap<String, BluetoothBeacon> scannedDevices = new HashMap<>();
    private static Timer scanTimer, bluetoothTimer;
    private static TimerTask scanTask, bluetoothTask;
    private static Handler handler = new Handler();
    private static BleDevicesScanner BLEScanner = null;
    private static HashMap<String, String> appWatch = new HashMap<>();
    private static BluetoothAdapter adapter;
    public static String user = "David";
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = event.getPackageName().toString();
        if(appWatch.containsKey(packageName)) {
            Log("Window focused on " + event.getPackageName());
            blockApp(packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log("Service interrupted");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED |
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.packageNames = new String[]{};
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        adapter = BleUtils.getBluetoothAdapter(getApplicationContext());

        //Start the checkbluetooth timer
        bluetoothTimer = new Timer();
        bluetoothTask = new TimerTask() {
            @Override
            public void run() {
                checkBLEStatus();
            }
        };
        bluetoothTimer.schedule(bluetoothTask, 0, 1000);

        //Start the bluetooth scanning
        startBLEScanner();

        //Create the package checking
        InstalledApps.init(getApplicationContext());

        //Log into firebase
        auth = FirebaseAuth.getInstance();

        Log("Signing in!");
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log("Signed in!");
                database = FirebaseFirestore.getInstance();
                database.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                        //.setHost("https://thegrassisgreen-40cb2.firebaseio.com/")
                        .setSslEnabled(true)
                        .setPersistenceEnabled(true)
                        .build());
                /*database.collection("green").document("apps").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot snapshot = task.getResult();
                        parseSnapShot(snapshot);
                    }
                });*/
                database.collection("green").document("apps").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        Log("Got app update!");
                        parseSnapShot(documentSnapshot);
                    }
                });
            }
        });
    }

    void parseSnapShot(DocumentSnapshot snapshot) {
        final Map<String, Object> data = snapshot.getData();
        if (data != null) {
            if(data.containsKey(user)) {
                final List<String> blockedApps = (List<String>) data.get(user);
                appWatch.clear();
                for(final String app : blockedApps) {
                    Log("Blocked app: " + app);
                    appWatch.put(app, user);
                }
            } else {
                Log("User is not in the database!");
            }
        }
    }

    void blockApp(final String packageName) {
        Intent block = new Intent(getApplicationContext(), BlockAppActivity.class);
        block.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
        block.putExtra("package", packageName);
        if(appWatch.containsKey(packageName)) {
            block.putExtra("ble", appWatch.get(packageName).contains("BLE"));
        } else {
            block.putExtra("ble", false);
        }
        startActivity(block);
    }

    void checkBLEStatus() {
        if (adapter != null && !adapter.isEnabled()) {
            Log("The bluetooth adapter was turned off");
            startBLEScanner();
            bluetoothTimer.cancel();
            Looper.prepare();
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothTask.cancel();
            bluetoothTimer = new Timer();
            bluetoothTask = new TimerTask() {
                @Override
                public void run() {
                    checkBLEStatus();
                }
            };
            bluetoothTimer.schedule(bluetoothTask, 5000, 5000);
        } else if(adapter != null && adapter.isEnabled() && (BLEScanner == null || (!BLEScanner.isScanning() && BLEScanner.getTimeSinceStop() > 6000))) {
            isScanning = false;
            scannedDevices.clear();
            BLEScanner = null;
            startBLEScanner();
        }
    }

    void startBLEScanner() {
        boolean isEnabled = manageBluetooth(getBaseContext(), getApplicationContext(), false);
        boolean isLocationOn = manageLocation();

        if(isEnabled && isLocationOn) {
            BleUtils.resetBluetoothAdapter(this, this);
        }
    }

    public static boolean manageBluetooth(final Context baseContext, final Object applicationContext, final boolean isMainActivity) {
        boolean isEnabled = true;
        int errorId = 0;

        final int bleStatus = BleUtils.getBleStatus(baseContext);
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

                if(isMainActivity) {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    ((AppCompatActivity) applicationContext).startActivityForResult(enableBluetooth, REQUEST_BLUETOOTH);
                } else {
                    Intent enableBluetoothIntent = new Intent((Context) applicationContext, MainActivity.class);
                    enableBluetoothIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    //Tell the main activity to enable bluetooth
                    enableBluetoothIntent.putExtra("enable_ble", true);

                    //Start the activity
                    ((Context) applicationContext).startActivity(enableBluetoothIntent);
                }
                break;
        }

        if (errorId != 0) {
            if(isMainActivity) {
                Dialogs.ErrorDialog((Context) applicationContext, ((Context) applicationContext).getString(errorId), new Dialogs.ClickListener() {
                    @Override
                    public void onContinue() {
                        Log("Exiting...");
                        System.exit(1);
                    }
                });
            }
            Log("There was a problem initializing BLE!");
        }
        Log("Bluetooth enabled: " + isEnabled);
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
            //REPORT THAT THE LOCATION SERVICES ARE NOT ENABLED
            Log("Failed to get location services");
        }
        return isReady;
    }

    @Override
    public void onDestroy() {
        Log("Destroying Grass Service");
        startBLEScanner();
    }

    /*
    void updateRunningPackages(final List<String> blockedPackages, final long lookBack) {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            final List<UsageStats> queryUsageStats;
            if (usageStatsManager != null) {
                queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, cal.getTimeInMillis() - lookBack, cal.getTimeInMillis() + 5000);
                for (final UsageStats usage : queryUsageStats) {
                    final String packageName = usage.getPackageName();
                    if(blockedPackages.contains(packageName)) {
                        Log("" + usage.getTotalTimeInForeground());
                    }
                    if (appWatch.containsKey(packageName)) {
                        BlockedApp app = appWatch.get(packageName);
                        if ((app.foregroundUsage + RUNNING_DIFF) < usage.getTotalTimeInForeground()) {
                            app.isOpen = true;
                            appWatch.put(packageName, app);
                        }
                    } else {
                        if ((cal.getTimeInMillis() - usage.getLastTimeUsed()) < lookBack && blockedPackages.contains(packageName)) {
                            appWatch.put(packageName, new BlockedApp(packageName, usage.getTotalTimeInForeground()));
                        }
                    }
                }
            }
        } catch (Throwable err) {
            err.printStackTrace();
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

    @Override
    public void resetBluetoothCompleted() {
        Log("Bluetooth reset complete");
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
                        //Log("DEVICES: " + Arrays.toString(scannedDevices.keySet().toArray()));
                        //Look for new bluetooth devices
                        for (final String key : scannedDevices.keySet()) {
                            BluetoothBeacon b = scannedDevices.get(key);
                            if (b.elapsedMillis() > 2000) {
                                scannedDevices.remove(key);
                            }
                        }

                        if(scannedDevices.keySet().size() > 0) {
                            appWatch.put("com.google.android.talk", "BLE");
                        } else {
                            if(appWatch.containsKey("com.google.android.talk")) {
                                try {
                                    if (appWatch.get("com.google.android.talk").contains("BLE")) {
                                        appWatch.remove("com.google.android.talk");
                                    }
                                } catch (NullPointerException ignored) {}
                            }
                        }

                        //Check for updated apps
                        if(InstalledApps.appsChanged()) {
                            Log("Apps have changed! Updating...");
                            if(database != null) {
                                Map<String, Object> installedApps = new HashMap<>();
                                installedApps.put(user, InstalledApps.getPackageNames(InstalledApps.getAppsByCategories()));
                                database.collection("green").document("installed").set(installedApps);
                            }
                        }

                        try {
                            //Log(getRootInActiveWindow().getPackageName().toString());
                            //Log("Checking...");
                            //Check to see if the current app is invalid
                            String currentViewablePackage = getRootInActiveWindow().getPackageName().toString();
                            if (appWatch.containsKey(currentViewablePackage)) {
                                blockApp(currentViewablePackage);
                            }
                        } catch(NullPointerException ignored) {}

                        //Log("APPS: " + Arrays.toString(appWatch.keySet().toArray()));
                    }
                });
            }
        };
        scanTimer.schedule(scanTask, 0, 500);
    }
}

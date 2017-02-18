/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AHKPKY&popup=n&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 * <p>
 * You may not use this file except in compliance with the license.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.pires.obd.enums.ObdProtocols;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.JsonObject;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Home extends AppCompatActivity implements LocationListener {

    public static final String DOESNOTEXIST = "doesNotExist";

    private static final int INITIAL_PERMISSIONS = 000;
    private static final int GPS_INTENT = 000;
    private static final int SETTINGS_INTENT = 001;
    private static final int BLUETOOTH_REQUEST = 002;

    static final int MIN_FREQUENCY_SEC = 5;
    static final int MAX_FREQUENCY_SEC = 60;
    static final int DEFAULT_FREQUENCY_SEC = 10;

    private static final int OBD_SCAN_DELAY_MS = 200;
    private static final int UPLOAD_DELAY_MS = 500;

    private static final int BLUETOOTH_CONNECTION_RETRY_DELAY_MS = 100;
    private static final int BLUETOOTH_CONNECTION_RETRY_INTERVAL_MS = 5000;
    private static final int MAX_RETRY = 10;

    private LocationManager locationManager;
    private String provider;
    private Location location = null;
    private boolean networkIntentNeeded = false;

    private String trip_id;

    private ProgressBar progressBar;
    private ActionBar supportActionBar;
    private Button changeNetwork;
    private Button changeFrequency;

    /**
     * ATTENTION: support for ELM 327 WiFi dongle is experimental. With Android, it seems not possible to
     * have a WiFi connection to the dongle and an internet connection simultaneously.
     */
    private final boolean bluetooth_mode = true;
    private final ObdBridgeBluetooth obdBridgeBluetooth = new ObdBridgeBluetooth(this);
    private final ObdBridgeWifi obdBridgeWifi = new ObdBridgeWifi(this); // experimental
    private final ObdBridge obdBridge = bluetooth_mode ? obdBridgeBluetooth : obdBridgeWifi;

    final IoTPlatformDevice iotpDevice = new IoTPlatformDevice(this);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> bluetoothConnectorHandle = null;
    private int retryCount = 0;

    private boolean initialized = false;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    void clean() {
        completeConnectingBluetoothDevice();
        scheduler.shutdown();
        iotpDevice.clean();
        obdBridge.clean();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setScaleX(0.5f);
        progressBar.setScaleY(0.5f);

        supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowCustomEnabled(true);
        supportActionBar.setCustomView(progressBar);

        changeNetwork = (Button) findViewById(R.id.changeNetwork);
        changeFrequency = (Button) findViewById(R.id.changeFrequency);

        obdBridge.initializeObdParameterList(this);

        try {
            checkForDisclaimer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void checkForDisclaimer() throws IOException {
        if (!wasDisclaimerShown(false)) {
            final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int choice) {
                    switch (choice) {
                        case DialogInterface.BUTTON_POSITIVE:
                            wasDisclaimerShown(true);
                            startApp();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast toast = Toast.makeText(Home.this, "Cannot use this application without agreeing to the disclaimer", Toast.LENGTH_SHORT);
                            toast.show();
                            Home.this.finishAffinity();
                            break;
                    }
                }
            };
            final InputStream is = getResources().getAssets().open("LICENSE");
            String line;
            final StringBuffer message = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    message.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) br.close();
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
            builder
                    .setTitle("Disclaimer")
                    .setMessage(message)
                    .setNegativeButton("Disagree", dialogClickListener)
                    .setPositiveButton("Agree", dialogClickListener)
                    .show();

        } else {
            startApp();
        }
    }

    private void startApp() {
        initialized = true;

        final String orgId = getPreference(SettingsFragment.ORGANIZATION_ID, IoTPlatformDevice.defaultOrganizationId);
        final String apiKey = getPreference(SettingsFragment.API_KEY, IoTPlatformDevice.defaultApiKey);
        final String apiToken = getPreference(SettingsFragment.API_TOKEN, IoTPlatformDevice.defaultApiToken);
        iotpDevice.changeOrganization(orgId, apiKey, apiToken);
        startApp2();
    }

    void restartApp(final String orgId, final String apiKey, final String apiToken) {
        // this may be called from a non UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (iotpDevice.getOrganizationId() != orgId) {
                    iotpDevice.changeOrganization(orgId, apiKey, apiToken);
                    startApp2();
                }
            }
        });
    }

    private void startApp2() {
        // this has to be called in the UI thread

        if (setLocationInformation() == false) {
            // GPS and/or Network is disabled
            return;
        }
        if (!iotpDevice.hasValidOrganization()) {
            // IoT Platform setting is missing
            startSettingsActivity();
            return;
        }
        if (!obdBridgeBluetooth.setupBluetooth()) {
            // when bluetooth is not available (e.g. Android Studio simulator)
            final boolean doNotRunSimulationWithoutBluetooth = false; // testing purpose
            if (doNotRunSimulationWithoutBluetooth) {
                // terminate the app
                Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth!", Toast.LENGTH_LONG).show();
                setChangeNetworkEnabled(false);
                setChangeFrequencyEnabled(false);
                showStatus("Bluetooth Failed");
            } else {
                // force to run in simulation mode for testing purpose
                Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth! Will be running in Simulation Mode", Toast.LENGTH_LONG).show();
                runSimulatedObdScan();
                showStatus("Simulated OBD Scan");
            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final String[] permissionsArray = getPermissionsNeeded();
                if (permissionsArray != null) {
                    requestPermissions(permissionsArray, INITIAL_PERMISSIONS);
                } else {
                    permissionsGranted();
                }

            } else {
                if (!wasWarningShown()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder
                            .setTitle("Warning")
                            .setMessage("This app requires permissions to your Locations, Bluetooth and Storage settings.\n\n" +
                                    "If you are running the application to your phone from Android Studio, you will not be able to allow these permissions.\n\n" +
                                    "If that is the case, please install the app through the provided APK file."
                            )
                            .setPositiveButton("Ok", null)
                            .show();
                }
                permissionsGranted();
            }
        }
    }

    private String[] getPermissionsNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Map<String, String> permissions = new HashMap<>();
            final ArrayList<String> permissionNeeded = new ArrayList<>();

            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                permissions.put("internet", Manifest.permission.INTERNET);

            if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
                permissions.put("networkState", Manifest.permission.ACCESS_NETWORK_STATE);

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.put("coarseLocation", Manifest.permission.ACCESS_COARSE_LOCATION);

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.put("fineLocation", Manifest.permission.ACCESS_FINE_LOCATION);

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
                permissions.put("bluetooth", Manifest.permission.BLUETOOTH_ADMIN);

            for (Map.Entry<String, String> entry : permissions.entrySet()) {
                permissionNeeded.add(entry.getValue());
            }
            if (permissionNeeded.size() > 0) {
                Object[] tempObjectArray = permissionNeeded.toArray();
                String[] permissionsArray = Arrays.copyOf(tempObjectArray, tempObjectArray.length, String[].class);

                return permissionsArray;

            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void startSettingsActivity() {
        final Intent intent = new Intent(this, AppSettingsActivity.class);
        intent.putExtra(SettingsFragment.ORGANIZATION_ID, iotpDevice.getOrganizationId());
        intent.putExtra(SettingsFragment.API_KEY, iotpDevice.getApiKey());
        intent.putExtra(SettingsFragment.API_TOKEN, iotpDevice.getApiToken());
        String device_id = "";
        try {
            device_id = obdBridge.getDeviceId(obdBridge.isSimulation(), getUUID());
        } catch (DeviceNotConnectedException e) {
            device_id = "";
        }
        intent.putExtra(SettingsFragment.DEVICE_ID, device_id);
        intent.putExtra(SettingsFragment.DEVICE_TOKEN, iotpDevice.getDeviceToken(device_id));
        intent.putExtra(SettingsFragment.BLUETOOTH_DEVICE_ADDRESS, obdBridgeBluetooth.getUserDeviceAddress());
        intent.putExtra(SettingsFragment.BLUETOOTH_DEVICE_NAME, obdBridgeBluetooth.getUserDeviceName());
        intent.putExtra(SettingsFragment.UPLOAD_FREQUENCY, "" + getUploadFrequencySec());
        intent.putExtra(SettingsFragment.OBD_TIMEOUT_MS, "" + getObdTimeoutMs());
        intent.putExtra(SettingsFragment.OBD_PROTOCOL, "" + getObdProtocol());

        startActivity(intent);
    }

    private void checkSettingsOnResume() {
        final String orgId = getPreference(SettingsFragment.ORGANIZATION_ID, IoTPlatformDevice.defaultOrganizationId);
        final String apiKey = getPreference(SettingsFragment.API_KEY, IoTPlatformDevice.defaultApiKey);
        final String apiToken = getPreference(SettingsFragment.API_TOKEN, IoTPlatformDevice.defaultApiToken);

        if (!iotpDevice.isCurrentOrganizationSameAs(orgId) || !iotpDevice.isConnected()) {
            restartApp(orgId, apiKey, apiToken);

        } else if (!obdBridge.isSimulation()) {
            final int obd_timeout_ms = Integer.parseInt(getPreference(SettingsFragment.OBD_TIMEOUT_MS, "" + ObdBridge.DEFAULT_OBD_TIMEOUT_MS));
            final ObdProtocols obd_protocol = ObdProtocols.valueOf(getPreference(SettingsFragment.OBD_PROTOCOL, ObdBridge.DEFAULT_OBD_PROTOCOL.name()));
            if (!obdBridge.isCurrentObdTimeoutSameAs(obd_timeout_ms) || !obdBridge.isCurrentObdProtocolSameAs(obd_protocol)) {
                runRealObdScan(obd_timeout_ms, obd_protocol);
            }
        }
    }

    private void showStatus(final String msg) {
        if (supportActionBar == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                supportActionBar.setTitle(msg);
            }
        });
    }

    private void setChangeNetworkEnabled(final boolean enableChangeNetwork) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeNetwork.setEnabled(enableChangeNetwork);
            }
        });
    }

    private void setChangeFrequencyEnabled(final boolean enableChangeFrequncy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeFrequency.setEnabled(enableChangeFrequncy);
            }
        });
    }

    private void showStatus(final String msg, final int progressBarVisibility) {
        if (progressBar == null || supportActionBar == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                supportActionBar.setTitle(msg);
                progressBar.setVisibility(progressBarVisibility);
            }
        });
    }

    private void showToastText(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Home.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionsMenu_1:
                startSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        iotpDevice.stopPublishing();
        iotpDevice.disconnectDevice();
        obdBridge.stopObdScan();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        switch (requestCode) {
            case INITIAL_PERMISSIONS:
                if (results[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted();
                } else {
                    Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, results);
        }
    }

    private void permissionsGranted() {
        System.out.println("PERMISSIONS GRANTED");
        showObdScanModeDialog();
    }

    private void showObdScanModeDialog() {
        // allows the user to select real OBD Scan mode or Simulation mode
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        alertDialog
                .setCancelable(false)
                .setTitle("Do you want to try out a simulated version?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        runSimulatedObdScan();
                    }
                })
                .setNegativeButton("No, I have a real OBDII Dongle", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        runRealObdScan();
                    }
                })
                .show();
    }

    private void runSimulatedObdScan() {
        setChangeNetworkEnabled(false);
        checkDeviceRegistry(true);
        setChangeFrequencyEnabled(true);
    }

    private void runRealObdScan() {
        final int obd_timeout_ms = getObdTimeoutMs();
        final ObdProtocols obd_protocol = getObdProtocol();
        runRealObdScan(obd_timeout_ms, obd_protocol);
    }

    private void runRealObdScan(final int obd_timeout_ms, final ObdProtocols obd_protocol) {
        obdBridge.stopObdScan();

        if (obdBridge instanceof ObdBridgeBluetooth) {
            runRealObdBluetoothScan(obd_timeout_ms, obd_protocol);
        } else if (obdBridge instanceof ObdBridgeWifi) {
            runRealObdWifiScan(obd_timeout_ms, obd_protocol);
        }
    }

    private void runRealObdWifiScan(final int obd_imeout_ms, final ObdProtocols obd_protocol) {
        final String address = "192.168.0.10";
        final int port = 35000;
        final boolean connected = obdBridgeWifi.connectSocket(address, port, obd_imeout_ms, obd_protocol);
        System.out.println("WI-FI CONNECTED " + connected);
        if (connected) {
            showStatus("Connected to " + address + ":" + port, View.GONE);
            startObdScan(false);
            //checkDeviceRegistry(false);
        } else {
            showStatus("Connection Failed for " + address + ":" + port, View.GONE);
        }
    }

    private void runRealObdBluetoothScan(final int obd_timeout_ms, final ObdProtocols obd_protocol) {
        if (!obdBridgeBluetooth.isBluetoothEnabled()) {
            final Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, BLUETOOTH_REQUEST);
            return;
        }
        final Set<BluetoothDevice> pairedDevicesSet = obdBridgeBluetooth.getPairedDeviceSet();

        // In case user clicks on Change Network, need to repopulate the devices list
        final ArrayList<String> deviceNames = new ArrayList<>();
        final ArrayList<String> deviceAddresses = new ArrayList<>();
        if (pairedDevicesSet != null && pairedDevicesSet.size() > 0) {
            for (BluetoothDevice device : pairedDevicesSet) {
                deviceNames.add(device.getName());
                deviceAddresses.add(device.getAddress());
            }
            final String preferredName = getPreference(SettingsFragment.BLUETOOTH_DEVICE_NAME, "obd").toLowerCase();
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
            final ArrayAdapter adapter = new ArrayAdapter(Home.this, android.R.layout.select_dialog_singlechoice, deviceNames.toArray(new String[deviceNames.size()]));
            int selectedDevice = -1;
            for (int i = 0; i < deviceNames.size(); i++) {
                if (deviceNames.get(i).toLowerCase().contains(preferredName)) {
                    selectedDevice = i;
                }
            }
            alertDialog
                    .setCancelable(false)
                    .setSingleChoiceItems(adapter, selectedDevice, null)
                    .setTitle("Please Choose the OBDII Bluetooth Device")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            final int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            final String deviceAddress = deviceAddresses.get(position);
                            final String deviceName = deviceNames.get(position);
                            startConnectingBluetoothDevice(deviceAddress, deviceName, obd_timeout_ms, obd_protocol);
                            setPreference(SettingsFragment.BLUETOOTH_DEVICE_NAME, deviceName);
                        }
                    })
                    .show();
        } else {
            Toast.makeText(getApplicationContext(), "Please pair with your OBDII device and restart the application!", Toast.LENGTH_SHORT).show();
        }
    }

    private synchronized void startConnectingBluetoothDevice(final String userDeviceAddress, final String userDeviceName, final int obd_timeout_ms, final ObdProtocols obd_protocol) {
        completeConnectingBluetoothDevice(); // clean up previous try

        retryCount = 0;
        Log.i("BT Connection Task", "STARTED");
        System.out.println("BT Connection Task: STARTED");
        showStatus("Connecting to \"" + userDeviceName + "\"", View.VISIBLE);
        bluetoothConnectorHandle = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (obdBridgeBluetooth.connectBluetoothSocket(userDeviceAddress, userDeviceName, obd_timeout_ms, obd_protocol)) {
                    showStatus("Connected to \"" + userDeviceName + "\"", View.GONE);
                    checkDeviceRegistry(false);
                    completeConnectingBluetoothDevice();
                } else if (++retryCount >= MAX_RETRY) {
                    showToastText("Unable to connect to the device, please make sure to choose the right network");
                    showStatus("Connection Failed", View.GONE);
                    completeConnectingBluetoothDevice();
                } else {
                    showStatus("Retry Connecting to \"" + userDeviceName + "\"", View.VISIBLE);
                }
            }
        }, BLUETOOTH_CONNECTION_RETRY_DELAY_MS, BLUETOOTH_CONNECTION_RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void completeConnectingBluetoothDevice() {
        if (bluetoothConnectorHandle != null) {
            bluetoothConnectorHandle.cancel(true);
            bluetoothConnectorHandle = null;
            Log.i("BT Connection Task", "ENDED");
            System.out.println("BT Connection Task: ENDED");
        }
    }

    private void checkDeviceRegistry(final boolean simulation) {
        setLocationInformation();

        try {
            showStatus("Checking Device Registration", View.VISIBLE);

            final String uuid = getUUID();
            final String device_id = obdBridge.getDeviceId(simulation, uuid);
            iotpDevice.checkDeviceRegistration(new IoTPlatformDevice.ResponseListener() {
                @Override
                protected void response(final int statusCode, final JSONArray result) {
                    try {
                        result.remove(result.length() - 1);
                        Log.d("Check Device Registry", result.toString(4));

                        final JSONObject deviceDefinition = result.length() > 0 ? result.getJSONObject(0) : null;
                        onDeviceRegistrationChecked(statusCode, deviceDefinition, simulation);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, device_id, uuid);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (DeviceNotConnectedException e) {
            e.printStackTrace();
        } catch (NoIoTPOrganizationException e) {
            e.printStackTrace();
        }
    }

    private void onDeviceRegistrationChecked(final int statusCode, final JSONObject deviceDefinition, final boolean simulation) {
        // run in UI thread
        switch (statusCode) {
            case 200: {
                Log.d("Check Device Registry", "***Already Registered***");
                showStatus("Device Already Registered", View.GONE);

                deviceRegistered(deviceDefinition, simulation);
                break;
            }
            case 404:
            case 405: {
                Log.d("Check Device Registry", "***Not Registered***");
                progressBar.setVisibility(View.GONE);

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
                alertDialog
                        .setCancelable(false)
                        .setTitle("Your Device is NOT Registered!")
                        .setMessage("In order to use this application, we need to register your device to the IBM IoT Platform")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {

                                registerDevice(simulation);
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Toast.makeText(Home.this, "Cannot continue without registering your device!", Toast.LENGTH_LONG).show();
                                Home.this.finishAffinity();
                            }
                        })
                        .show();
                break;
            }
            default: {
                Log.d("Failed to connect IoTP", "statusCode: " + statusCode);
                progressBar.setVisibility(View.GONE);

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
                alertDialog
                        .setCancelable(false)
                        .setTitle("Failed to connect to IBM IoT Platform")
                        .setMessage("Check orgId, apiKey and apiToken of your IBM IoT Platform. statusCode:" + statusCode)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                showStatus("Failed to connect to IBM IoT Platform");
                                deviceNotRegistered(simulation);
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Toast.makeText(Home.this, "Cannot continue without connecting to IBM IoT Platform!", Toast.LENGTH_LONG).show();
                                Home.this.finishAffinity();
                            }
                        })
                        .show();
                break;
            }
        }
    }

    private void registerDevice(final boolean simulation) {
        try {
            showStatus("Registering Your Device", View.VISIBLE);

            final String uuid = getUUID();
            final String device_id = obdBridge.getDeviceId(simulation, uuid);
            iotpDevice.requestDeviceRegistration(new IoTPlatformDevice.ResponseListener() {

                @Override
                protected void response(final int statusCode, final JSONArray result) {
                    try {
                        result.remove(result.length() - 1);
                        Log.d("Register Device", result.toString(4));

                        final JSONObject deviceDefinition = result.getJSONObject(0);
                        onDeviceRegistration(statusCode, deviceDefinition, simulation);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, device_id, uuid);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (DeviceNotConnectedException e) {
            e.printStackTrace();
        } catch (NoIoTPOrganizationException e) {
            e.printStackTrace();
        }
    }

    void storeSetting(final String prefKey, final String value) {
        PreferenceManager.getDefaultSharedPreferences(this).getString(prefKey, value);
    }

    String readSetting(final String prefKey, final String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(prefKey, defaultValue);
    }

    private void onDeviceRegistration(int statusCode, final JSONObject deviceDefinition, final boolean simulation) throws JSONException {
        // run in UI thread
        switch (statusCode) {
            case 201:
            case 202:

                final String authToken = deviceDefinition.getString("authToken");
                final String deviceId = deviceDefinition.getString("deviceId");
                iotpDevice.setDeviceToken(deviceId, authToken);

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
                final View authTokenAlert = getLayoutInflater().inflate(R.layout.activity_home_authtokenalert, null, false);
                final EditText authTokenField = (EditText) authTokenAlert.findViewById(R.id.authTokenField);
                authTokenField.setText(authToken);

                final Button copyToClipboard = (Button) authTokenAlert.findViewById(R.id.copyToClipboard);
                copyToClipboard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        final ClipData clipData = ClipData.newPlainText("authToken", authToken);
                        clipboardManager.setPrimaryClip(clipData);

                        Toast.makeText(Home.this, "Successfully copied to your Clipboard!", Toast.LENGTH_SHORT).show();
                    }
                });
                storeSetting(SettingsFragment.DEVICE_TOKEN, authToken);

                alertDialog.setView(authTokenAlert);
                alertDialog
                        .setCancelable(false)
                        .setTitle("Your Device is Now Registered!")
                        .setMessage("Please take note of this Autentication Token as you will need it in the future")
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                deviceRegistered(deviceDefinition, simulation);
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
        progressBar.setVisibility(View.GONE);
    }

    private void deviceRegistered(final JSONObject deviceDefinition, final boolean simulation) {
        // starts OBD scan and data transmission process here

        trip_id = createTripId();
        try {
            iotpDevice.createDeviceClient(deviceDefinition);
            iotpDevice.connectDevice();
            startObdScan(simulation);
            startPublishingProbeData();

        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deviceNotRegistered(final boolean simulation) {
        // starts OBD scan without data transmission
        try {
            // go settings for correct server connection
            // startSettingsActivity();

            // obd2 scan without registration
            startObdScan(simulation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private String createTripId() {
        String tid = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        tid += "-" + UUID.randomUUID();
        return tid;
    }

    private void startPublishingProbeData() {
        final int uploadIntervalMS = getUploadFrequencySec() * 1000;
        iotpDevice.startPublishing(new IoTPlatformDevice.ProbeDataGenerator() {
            @Override
            public JsonObject generateData() {
                if (location != null) {
                    return obdBridge.generateMqttEvent(location, trip_id);
                } else {
                    showStatus("Waiting to Find Location", View.VISIBLE);
                    return null;
                }
            }

            @Override
            public void notifyPostResult(final boolean success, final JsonObject event) {
                if (success) {
                    showStatus(obdBridge.isSimulation() ? "Simulated Data is Being Sent" : "Live Data is Being Sent", View.VISIBLE);
                    System.out.println("DATA SUCCESSFULLY POSTED......");
                    Log.d("Posted", event.toString());
                } else {
                    showStatus("Device Not Connected to IoT Platform", View.INVISIBLE);
                }
            }
        }, UPLOAD_DELAY_MS, uploadIntervalMS);
    }

    private void startObdScan() {
        startObdScan(obdBridge.isSimulation());
    }

    private void startObdScan(final boolean simulation) {
        final int scanIntervalMS = getUploadFrequencySec() * 1000;
        obdBridge.startObdScan(simulation, OBD_SCAN_DELAY_MS, scanIntervalMS);
    }

    public void changeFrequency(View view) {
        // called from UI panel
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
        final View changeFrequencyAlert = getLayoutInflater().inflate(R.layout.activity_home_changefrequency, null, false);

        final NumberPicker numberPicker = (NumberPicker) changeFrequencyAlert.findViewById(R.id.numberPicker);
        final int frequency_sec = getUploadFrequencySec();
        numberPicker.setMinValue(MIN_FREQUENCY_SEC);
        numberPicker.setMaxValue(MAX_FREQUENCY_SEC);
        numberPicker.setValue(frequency_sec);

        alertDialog.setView(changeFrequencyAlert);
        alertDialog
                .setCancelable(false)
                .setTitle("Change the Frequency of Data Being Sent (in Seconds)")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        final int value = numberPicker.getValue();
                        if (value != frequency_sec) {
                            setUploadFrequencySec(value);
                            startObdScan();
                            startPublishingProbeData();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void changeNetwork(View view) {
        // called from UI panel
        // do the following async as it may take time
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                obdBridge.stopObdScan();
            }
        }, 0, TimeUnit.MILLISECONDS);

        permissionsGranted();
    }

    public void endSession(View view) {
        // do the following async as it may take time
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                clean();
            }
        }, 0, TimeUnit.MILLISECONDS);

        Toast.makeText(Home.this, "Session Ended, application will close now!", Toast.LENGTH_LONG).show();
        Home.this.finishAffinity();
    }

    public void setUploadFrequencySec(final int sec) {
        setPreference(SettingsFragment.UPLOAD_FREQUENCY, "" + sec);
    }

    public int getUploadFrequencySec() {
        final String value = getPreference(SettingsFragment.UPLOAD_FREQUENCY, "" + DEFAULT_FREQUENCY_SEC);
        return Integer.parseInt(value);
    }

    public int getObdTimeoutMs() {
        final String value = getPreference(SettingsFragment.OBD_TIMEOUT_MS, "" + ObdBridge.DEFAULT_OBD_TIMEOUT_MS);
        return Integer.parseInt(value);
    }

    public ObdProtocols getObdProtocol() {
        final String value = getPreference(SettingsFragment.OBD_PROTOCOL, "" + ObdBridge.DEFAULT_OBD_PROTOCOL);
        return ObdProtocols.valueOf(value);
    }

    private boolean wasDisclaimerShown(boolean agreed) {
        final boolean disclaimerShownAndAgreed = getPreferenceBoolean("iota-starter-obdii-disclaimer", false);
        if (disclaimerShownAndAgreed) {
            return disclaimerShownAndAgreed;
        } else if (!disclaimerShownAndAgreed && agreed) {
            setPreferenceBoolean("iota-starter-obdii-disclaimer", true);
        }
        return false;
    }

    private boolean wasWarningShown() {
        final boolean warningShown = getPreferenceBoolean("iota-starter-obdii-warning-message", false);
        if (warningShown) {
            return warningShown;
        } else {
            setPreferenceBoolean("iota-starter-obdii-warning-message", true);
            return false;
        }
    }

    private String getUUID() {
        String uuidString = getPreference("iota-starter-obdii-uuid", DOESNOTEXIST);
        if (!DOESNOTEXIST.equals(uuidString)) {
            return uuidString;
        } else {
            uuidString = UUID.randomUUID().toString();
            setPreference("iota-starter-obdii-uuid", uuidString);
            return uuidString;
        }
    }

    @Override
    public void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 500, 1, this);

        if (initialized) {
            checkSettingsOnResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        setLocationInformation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public Location getLocation() {
        return location;
    }

    private boolean setLocationInformation() {
        // returns false if GPS and Network settings are needed
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && (networkInfo != null && networkInfo.isConnected())) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }

            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            final List<String> providers = locationManager.getProviders(true);
            Location finalLocation = null;

            for (String provider : providers) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }

                final Location lastKnown = locationManager.getLastKnownLocation(provider);

                if (lastKnown == null) {
                    continue;
                }
                if (finalLocation == null || (lastKnown.getAccuracy() < finalLocation.getAccuracy())) {
                    finalLocation = lastKnown;
                }
            }

            if (finalLocation == null) {
                Log.e("Location Data", "Not Working!");
            } else {
                Log.d("Location Data", finalLocation.getLatitude() + " " + finalLocation.getLongitude() + "");
                location = finalLocation;
            }

            return true;
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(getApplicationContext(), "Please turn on your GPS", Toast.LENGTH_LONG).show();

                final Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(gpsIntent, GPS_INTENT);

                if (networkInfo == null) {
                    networkIntentNeeded = true;
                }
                return false;
            } else {
                if (networkInfo == null) {
                    Toast.makeText(getApplicationContext(), "Please turn on Mobile Data or WIFI", Toast.LENGTH_LONG).show();

                    final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                    startActivityForResult(settingsIntent, SETTINGS_INTENT);
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_INTENT) {
            if (networkIntentNeeded) {
                Toast.makeText(getApplicationContext(), "Please connect to a network", Toast.LENGTH_LONG).show();
                final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                startActivityForResult(settingsIntent, SETTINGS_INTENT);

            } else {
                setLocationInformation();
                startApp2();
            }
        } else if (requestCode == SETTINGS_INTENT) {
            networkIntentNeeded = false;
            setLocationInformation();
            startApp2();

        } else if (requestCode == BLUETOOTH_REQUEST) {
            startApp2();
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Home Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    int getPreferenceInt(final String prefKey, final int defaultValue) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            return preferences.getInt(prefKey, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    boolean getPreferenceBoolean(final String prefKey, final boolean defaultValue) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            return preferences.getBoolean(prefKey, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    String getPreference(final String prefKey, final String defaultValue) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            return preferences.getString(prefKey, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    void setPreferenceInt(final String prefKey, final int value) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putInt(prefKey, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setPreferenceBoolean(final String prefKey, final boolean value) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putBoolean(prefKey, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setPreference(final String prefKey, final String value) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putString(prefKey, "" + value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

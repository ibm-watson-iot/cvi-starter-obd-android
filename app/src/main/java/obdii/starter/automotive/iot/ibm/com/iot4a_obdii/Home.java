/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AEGGZJ&popup=y&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
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
import android.content.pm.PackageManager;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Home extends AppCompatActivity implements LocationListener {

    private LocationManager locationManager;
    private Location location = null;
    private String provider;

    private final int INITIAL_PERMISSIONS = 000;

    private boolean permissionsGranted = false;

    private final int GPS_INTENT = 000;
    private final int SETTINGS_INTENT = 001;

    private boolean networkIntentNeeded = false;

    private String userDeviceAddress = null;

    private String trip_id;

    private ProgressBar progressBar;
    private ActionBar supportActionBar;
    private Button changeNetwork;
    private Button changeFrequency;

    private int uploadTimerDelay = 5000;
    private int uploadTimerPeriod = 15000;
    private Timer uploadTimer;

    private ObdBridge obdBridge = new ObdBridge();
    private IoTPlatformDevice iotpDevice = new IoTPlatformDevice();

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

        new API(getApplicationContext());

        try {
            checkForDisclaimer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkForDisclaimer() throws IOException {
        if (!API.disclaimerShown(false)) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int choice) {
                    switch (choice) {
                        case DialogInterface.BUTTON_POSITIVE:
                            API.disclaimerShown(true);

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
            StringBuffer message = new StringBuffer();
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

            AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
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

    public void startApp() {
        if (!obdBridge.setupBluetooth()) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth! Will be running on Simulation Mode", Toast.LENGTH_LONG).show();

            final boolean doNotRunSimulationWithoutBluetooth = false;
            if (doNotRunSimulationWithoutBluetooth) {
                // terminate the app
                changeNetwork.setEnabled(false);
                changeFrequency.setEnabled(false);
                showStatus("Bluetooth Failed");
            } else {
                // force to run in simulation mode for testing purpose
                runSimulatedObdScan();
                showStatus("Simulated OBD Scan");
            }

        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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

                    requestPermissions(permissionsArray, INITIAL_PERMISSIONS);
                } else {
                    permissionsGranted = true;

                    permissionsGranted();
                }
            } else {
                if (!API.warningShown()) {
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

    @Override
    protected void onDestroy() {
        stopPublishing();
        obdBridge.stopObdScanThread();
        iotpDevice.disconnectDevice();
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
        obdBridge.startObdScanThread();
        showObdScanModeDialog();
    }

    private void showObdScanModeDialog() {
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
        obdBridge.setSimulation(true);
        changeNetwork.setEnabled(false);
        checkDeviceRegistry();
        obdBridge.startObdScanThread();
        changeFrequency.setEnabled(true);
    }

    private void runRealObdScan() {
        obdBridge.setSimulation(false);
        if (!obdBridge.isBluetoothEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        } else {
            final Set<BluetoothDevice> pairedDevicesSet = obdBridge.getPairedDeviceSet();

            // In case user clicks on Change Network, need to repopulate the devices list
            final ArrayList<String> deviceNames = new ArrayList<>();
            final ArrayList<String> deviceAddresses = new ArrayList<>();

            if (pairedDevicesSet != null && pairedDevicesSet.size() > 0) {
                for (BluetoothDevice device : pairedDevicesSet) {
                    deviceNames.add(device.getName());
                    deviceAddresses.add(device.getAddress());
                }

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
                final ArrayAdapter adapter = new ArrayAdapter(Home.this, android.R.layout.select_dialog_singlechoice, deviceNames.toArray(new String[deviceNames.size()]));

                int selectedDevice = -1;
                for (int i = 0; i < deviceNames.size(); i++) {
                    if (deviceNames.get(i).toLowerCase().contains("obd")) {
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
                                userDeviceAddress = deviceAddresses.get(position);
                                final String userDeviceName = deviceNames.get(position);

                                showStatus("Connecting to \"" + userDeviceName + "\"", View.VISIBLE);

                                if (obdBridge.connectBluetoothSocket(userDeviceAddress)) {
                                    progressBar.setVisibility(View.GONE);
                                    checkDeviceRegistry();

                                } else {
                                    Toast.makeText(Home.this, "Unable to connect to the device, please make sure to choose the right network", Toast.LENGTH_LONG).show();
                                    showStatus("Connection Failed", View.GONE);
                                }
                            }
                        })
                        .show();
            } else {
                Toast.makeText(getApplicationContext(), "Please pair with your OBDII device and restart the application!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkDeviceRegistry() {
        getAccurateLocation();

        final String device_id = obdBridge.isSimulation() ? getSimulatedDeviceID() : getRealDeviceID();
        final String url = API.platformAPI + "/device/types/" + API.typeId + "/devices/" + device_id;
        try {
            showStatus("Checking Device Registration", View.VISIBLE);
            final API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                @Override
                public void postExecute(JSONArray result) throws JSONException {

                    final JSONObject serverResponse = result.getJSONObject(result.length() - 1);
                    final int statusCode = serverResponse.getInt("statusCode");

                    result.remove(result.length() - 1);

                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);

                    switch (statusCode) {
                        case 200:
                            Log.d("Check Device Registry", result.toString(4));
                            Log.d("Check Device Registry", "***Already Registered***");

                            showStatus("Device Already Registered", View.GONE);
                            iotpDevice.setDeviceDefinition(result.getJSONObject(0));
                            deviceRegistered();
                            break;
                        case 404:
                        case 405:
                            Log.d("Check Device Registry", "***Not Registered***");
                            progressBar.setVisibility(View.GONE);

                            alertDialog
                                    .setCancelable(false)
                                    .setTitle("Your Device is NOT Registered!")
                                    .setMessage("In order to use this application, we need to register your device to the IBM IoT Platform")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            registerDevice();
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
                        default:
                            Log.d("Failed to connect IoTP", "statusCode: " + statusCode);
                            progressBar.setVisibility(View.GONE);

                            alertDialog
                                    .setCancelable(false)
                                    .setTitle("Failed to connect to IBM IoT Platform")
                                    .setMessage("Check orgId, apiKey and apiToken of your IBM IoT Platform. statusCode:" + statusCode)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            showStatus("Failed to connect to IBM IoT Platform");
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
            });

            task.execute(url, "GET", null, null, API.credentialsBase64).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    public void registerDevice() {
        final String url = API.addDevices;

        try {
            showStatus("Registering Your Device", View.VISIBLE);
            final API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                @Override
                public void postExecute(final JSONArray result) throws JSONException {
                    Log.d("Register Device", result.toString(4));

                    final JSONObject serverResponse = result.getJSONObject(result.length() - 1);
                    final int statusCode = serverResponse.getInt("statusCode");

                    result.remove(result.length() - 1);

                    switch (statusCode) {
                        case 201:
                        case 202:
                            final String authToken = result.getJSONObject(0).getString("authToken");
                            final String deviceId = result.getJSONObject(0).getString("deviceId");
                            final String sharedPrefsKey = "iota-obdii-auth-" + deviceId;

                            if (!API.getStoredData(sharedPrefsKey).equals(authToken)) {
                                API.storeData(sharedPrefsKey, authToken);
                            }

                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
                            final View authTokenAlert = getLayoutInflater().inflate(R.layout.activity_home_authtokenalert, null, false);
                            final EditText authTokenField = (EditText) authTokenAlert.findViewById(R.id.authTokenField);
                            authTokenField.setText(authToken);

                            final Button copyToClipboard = (Button) authTokenAlert.findViewById(R.id.copyToClipboard);
                            copyToClipboard.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("authToken", authToken);
                                    clipboardManager.setPrimaryClip(clipData);

                                    Toast.makeText(Home.this, "Successfully copied to your Clipboard!", Toast.LENGTH_SHORT).show();
                                }
                            });

                            alertDialog.setView(authTokenAlert);
                            alertDialog
                                    .setCancelable(false)
                                    .setTitle("Your Device is Now Registered!")
                                    .setMessage("Please take note of this Autentication Token as you will need it in the future")
                                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int which) {
                                            try {
                                                iotpDevice.setDeviceDefinition(result.getJSONObject(0));
                                                deviceRegistered();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .show();
                            break;
                        default:
                            break;
                    }

                    progressBar.setVisibility(View.GONE);
                }
            });

            final JSONArray bodyArray = new JSONArray();
            final JSONObject bodyObject = new JSONObject();
            final String device_id = obdBridge.isSimulation() ? getSimulatedDeviceID() : getRealDeviceID();
            bodyObject
                    .put("typeId", API.typeId)
                    .put("deviceId", device_id);
            bodyArray
                    .put(bodyObject);

            task.execute(url, "POST", null, bodyArray.toString(), API.credentialsBase64).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getSimulatedDeviceID() {
        return API.getUUID();
    }

    private String getRealDeviceID() {
        return userDeviceAddress.replaceAll(":", "-");
    }

    public void deviceRegistered() {
        trip_id = createTripId();

        try {
            if (iotpDevice.createDeviceClient() != null) {
                iotpDevice.connectDevice();
                startPublishing();
            }
        } catch (MqttException e) {
            e.printStackTrace();
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


    private synchronized void startPublishing() {
        // stop existing uploadTimer
        stopPublishing();

        // start new uploadTimer
        uploadTimer = new Timer();
        uploadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    mqttPublish();
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, uploadTimerDelay, uploadTimerPeriod);
    }

    private synchronized void stopPublishing() {
        if (uploadTimer != null) {
            uploadTimer.cancel();
            uploadTimer = null;
        }
    }

    private void mqttPublish() throws MqttException {
        if (location != null) {
            final JsonObject event = obdBridge.generateMqttEvent(location, trip_id);
            final boolean success = iotpDevice.publishEvent(event);
            if (success) {
                showStatus(obdBridge.isSimulation() ? "Simulated Data is Being Sent" : "Live Data is Being Sent", View.VISIBLE);
                System.out.println("SUCCESSFULLY POSTED......");
                Log.d("Posted", event.toString());
            } else {
                showStatus("Device Not Connected to IoT Platform", View.INVISIBLE);
            }
        } else {
            showStatus("Waiting to Find Location", View.VISIBLE);
        }
    }


    public void changeFrequency(View view) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, R.style.AppCompatAlertDialogStyle);
        final View changeFrequencyAlert = getLayoutInflater().inflate(R.layout.activity_home_changefrequency, null, false);

        final NumberPicker numberPicker = (NumberPicker) changeFrequencyAlert.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(5);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(15);

        alertDialog.setView(changeFrequencyAlert);
        alertDialog
                .setCancelable(false)
                .setTitle("Change the Frequency of Data Being Sent (in Seconds)")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        int newFrequency = numberPicker.getValue() * 1000;

                        if (newFrequency != uploadTimerPeriod) {
                            uploadTimerPeriod = newFrequency;
                            startPublishing();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void changeNetwork(View view) {
        permissionsGranted();
    }

    public void endSession(View view) {
        Toast.makeText(Home.this, "Session Ended, application will close now!", Toast.LENGTH_LONG).show();
        Home.this.finishAffinity();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 500, 1, this);
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
        getAccurateLocation();
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

    private void getAccurateLocation() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && (networkInfo != null && networkInfo.isConnected())) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            final List<String> providers = locationManager.getProviders(true);
            Location finalLocation = null;

            for (String provider : providers) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
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
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(getApplicationContext(), "Please turn on your GPS", Toast.LENGTH_LONG).show();

                final Intent gpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(gpsIntent, GPS_INTENT);

                if (networkInfo == null) {
                    networkIntentNeeded = true;
                }
            } else {
                if (networkInfo == null) {
                    Toast.makeText(getApplicationContext(), "Please turn on Mobile Data or WIFI", Toast.LENGTH_LONG).show();

                    final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                    startActivityForResult(settingsIntent, SETTINGS_INTENT);
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GPS_INTENT) {
            if (networkIntentNeeded) {
                Toast.makeText(getApplicationContext(), "Please connect to a network", Toast.LENGTH_LONG).show();

                final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                startActivityForResult(settingsIntent, SETTINGS_INTENT);
            } else {
                getAccurateLocation();
            }
        } else if (requestCode == SETTINGS_INTENT) {
            networkIntentNeeded = false;

            getAccurateLocation();
        }
    }
}

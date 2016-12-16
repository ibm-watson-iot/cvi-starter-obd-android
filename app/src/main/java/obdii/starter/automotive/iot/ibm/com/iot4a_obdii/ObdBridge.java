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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.AdaptiveTimingCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.HeadersOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.ObdWarmstartCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static obdii.starter.automotive.iot.ibm.com.iot4a_obdii.API.DOESNOTEXIST;

/*
 * OBD Bridge
 */
public class ObdBridge {

    private static final UUID SPPUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BluetoothManager.class.getName();

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket socket = null;
    private boolean simulation = false;
    private List<ObdParameter> obdParameterList = null;

    private String userDeviceAddress = null;

    private static final int BLUETOOTH_SCAN_DELAY = 500;
    private static final int BLUETOOTH_SCAN_INTERVAL_MS = 1000;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> obdScannerHandle = null;

    void clean() {
        stopObdScan();
        closeBluetoothSocket();
        scheduler.shutdown();
    }

    public String getDeviceId(final boolean simulation) throws DeviceNotConnectedException {
        return simulation ? getSimulatedDeviceId() : getRealDeviceId();
    }

    private String getSimulatedDeviceId() {
        final String key = "simulated-device-id";
        String device_id = API.getStoredData(key);
        if (device_id == null || DOESNOTEXIST.equals(device_id)) {
            device_id = API.getUUID();
            API.storeData(key, device_id);
        }
        return device_id;
    }

    private String getRealDeviceId() throws DeviceNotConnectedException {
        if (userDeviceAddress == null) {
            throw new DeviceNotConnectedException();
        }
        final String key = "device-id-" + userDeviceAddress.replaceAll(":", "-");
        String device_id = API.getStoredData(key);
        if (device_id == null || DOESNOTEXIST.equals(device_id)) {
            device_id = UUID.randomUUID().toString();
            API.storeData(key, device_id);
        }
        return device_id;
    }

    public boolean setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public Set<BluetoothDevice> getPairedDeviceSet() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getBondedDevices();
        } else {
            return null;
        }
    }


    public synchronized boolean connectBluetoothSocket(final String userDeviceAddress) {
        if (this.userDeviceAddress == userDeviceAddress && socket != null && socket.isConnected()) {
            return true;
        }
        closeBluetoothSocket();
        this.userDeviceAddress = userDeviceAddress;

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice device = btAdapter.getRemoteDevice(userDeviceAddress);
        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            socket = device.createRfcommSocketToServiceRecord(ObdBridge.SPPUUID);
        } catch (Exception e) {
            Log.e("Bluetooth Connection", "Socket couldn't be created");
            e.printStackTrace();
        }
        try {
            socket.connect();
            socketConnected();
            return true;

        } catch (IOException e) {
            Log.e("Bluetooth Connection", e.getMessage());
            try {
                Log.i("Bluetooth Connection", "Using fallback method");
                socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                socket.connect();
                socketConnected();
                return true;
            } catch (IOException e2) {
                Log.e("Bluetooth Connection", "Couldn't establish connection");
                return false;
            } catch (Exception e2) {
                e2.printStackTrace();
                Log.e("Bluetooth Connection", "Couldn't establish connection");
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e("BT Connection Error: ", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e("BT Connection Error: ", e.getMessage());
            return false;
        }
    }

    private void socketConnected() throws IOException, InterruptedException {
        Log.i("Bluetooth Connection", "CONNECTED - " + socket.isConnected());
        initializeOBD2Device(socket);
    }

    private void initializeOBD2Device(final BluetoothSocket socket) throws IOException, InterruptedException {
        runObdCommandIgnoreException(socket, new ObdRawCommand("ATD")); // Set all to defaults
        runObdCommandIgnoreException(socket, new ObdWarmstartCommand()); // reset
        runObdCommandIgnoreException(socket, new ObdResetCommand());
        runObdCommandIgnoreException(socket, new EchoOffCommand());
        runObdCommandIgnoreException(socket, new LineFeedOffCommand());
        runObdCommandIgnoreException(socket, new HeadersOffCommand());
        runObdCommandIgnoreException(socket, new TimeoutCommand(125));
        runObdCommandIgnoreException(socket, new AdaptiveTimingCommand(1));
        runObdCommandIgnoreException(socket, new SelectProtocolCommand(ObdProtocols.AUTO));
    }

    private void runObdCommandIgnoreException(final BluetoothSocket socket, final ObdCommand cmd) {
        try {
            runObdCommand(socket, cmd);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runObdCommand(final BluetoothSocket socket, final ObdCommand cmd) throws IOException, InterruptedException {
        final InputStream ins = socket.getInputStream();
        final OutputStream outs = socket.getOutputStream();
        cmd.run(ins, outs);
    }

    public synchronized void closeBluetoothSocket() {
        if (socket != null) {
            try {
                if (socket.isConnected()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
            this.userDeviceAddress = null;
        }
    }

    public boolean isSimulation() {
        return simulation;
    }

    public void initializeObdParameterList(final AppCompatActivity activity) {
        obdParameterList = ObdParameters.getObdParameterList(activity);
    }

    @NonNull
    public JsonObject generateMqttEvent(final Location location, final String trip_id) {
        if (obdParameterList == null) {
            // parameter list has to be set
        }
        final JsonObject event = new JsonObject();
        final JsonObject data = new JsonObject();
        event.add("d", data);
        data.addProperty("trip_id", trip_id);

        final JsonObject props = new JsonObject();

        for (ObdParameter obdParameter : obdParameterList) {
            obdParameter.setJsonProp(obdParameter.isBaseProp() ? data : props);
        }
        data.add("props", props);
        return event;
    }

    public synchronized void startObdScan(final boolean simulation) {
        stopObdScan();

        ObdBridge.this.simulation = simulation;
        Log.i("Obd Scan Thread", "STARTED");
        System.out.println("Obd Scan Thread: STARTED");
        obdScannerHandle = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (simulation || (socket != null && socket.isConnected())) {
                    for (ObdParameter obdParam : obdParameterList) {
                        obdParam.showScannedValue(socket, simulation);
                    }
                }
            }
        }, BLUETOOTH_SCAN_DELAY, BLUETOOTH_SCAN_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopObdScan() {
        if (obdScannerHandle != null) {
            obdScannerHandle.cancel(true);
            obdScannerHandle = null;
            Log.i("Obd Scan Thread", "ENDED");
            System.out.println("Obd Scan Thread: ENDED");
        }
    }
}

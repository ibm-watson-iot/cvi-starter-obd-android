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
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * OBD Bridge
 */
public class ObdBridge {

    private static final int OBD_REFRESH_INTERVAL_MS = 1000;
    private static final UUID SPPUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BluetoothManager.class.getName();

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket socket = null;
    private boolean socketConnected = false;
    private Thread obdScanThread = null;
    private boolean simulation = false;
    private List<ObdParameter> obdParameterList = null;

    @Override
    protected void finalize() throws Throwable {
        stopObdScanThread();
        closeBluetoothSocket();
        super.finalize();
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

    public synchronized void closeBluetoothSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    public synchronized boolean connectBluetoothSocket(final String userDeviceAddress) {
        closeBluetoothSocket();

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
            initializeOBD2Device(socket);
            Log.i("Bluetooth Connection", "CONNECTED");
            socketConnected = true;
            return true;

        } catch (IOException e) {
            Log.e("Bluetooth Connection", e.getMessage());
            try {
                Log.i("Bluetooth Connection", "Using fallback method");

                socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                socket.connect();
                initializeOBD2Device(socket);
                Log.i("Bluetooth Connection", "CONNECTED");
                socketConnected = true;
                return true;

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

    private void initializeOBD2Device(final BluetoothSocket socket) throws IOException, InterruptedException {
        //runObdCommand(socket, new ObdRawCommand("ATD")); // Set all to defaults
        runObdCommand(socket, new ObdWarmstartCommand()); // reset
        //runObdCommand(socket, new ObdResetCommand());
        runObdCommand(socket, new EchoOffCommand());
        runObdCommand(socket, new LineFeedOffCommand());
        runObdCommand(socket, new HeadersOffCommand());
        runObdCommand(socket, new TimeoutCommand(125));
        runObdCommand(socket, new AdaptiveTimingCommand(1));
        runObdCommand(socket, new SelectProtocolCommand(ObdProtocols.AUTO));
    }

    private void runObdCommand(final BluetoothSocket socket, final ObdCommand cmd) throws IOException, InterruptedException {
        final InputStream ins = socket.getInputStream();
        final OutputStream outs = socket.getOutputStream();
        //cmd.setResponseTimeDelay((long)10);
        cmd.run(ins, outs);
    }

    public void setSimulation(final boolean simulation) {
        this.simulation = simulation;
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
        //data.addProperty("lat", location.getLatitude());
        //data.addProperty("lng", location.getLongitude());
        data.addProperty("trip_id", trip_id);

        final JsonObject props = new JsonObject();

        for (ObdParameter obdParameter : obdParameterList) {
            obdParameter.setJsonProp(obdParameter.isBaseProp() ? data : props);
        }
        data.add("props", props);
        return event;
    }

    public synchronized void startObdScanThread() {
        if (obdScanThread != null) {
            return;
        }
        obdScanThread = new Thread() {
            @Override
            public void run() {
                try {
                    Log.i("Obd Scan Thread", "STARTED");
                    System.out.println("Obd Scan Thread: STARTED");
                    while (!isInterrupted()) {
                        Thread.sleep(OBD_REFRESH_INTERVAL_MS);
                        if (simulation || socketConnected) {
                            for (ObdParameter obdParam : obdParameterList) {
                                obdParam.showScannedValue(socket, simulation);
                            }
                        }
                    }
                } catch (InterruptedException e) {

                } finally {
                    Log.i("Obd Scan Thread", "ENDED");
                    System.out.println("Obd Scan Thread: ENDED");
                }
            }
        };
        obdScanThread.start();
    }

    public synchronized void stopObdScanThread() {
        if (obdScanThread != null) {
            obdScanThread.interrupt();
            obdScanThread = null;
        }
    }
}

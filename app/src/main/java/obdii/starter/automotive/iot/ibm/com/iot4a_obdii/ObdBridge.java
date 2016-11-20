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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.gson.JsonObject;

import java.io.IOException;
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

    public boolean connectBluetoothSocket(final String userDeviceAddress) {
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
            Log.i("Bluetooth Connection", "CONNECTED");
            socketConnected = true;
            return true;

        } catch (IOException e) {
            Log.e("Bluetooth Connection", e.getMessage());
            try {
                Log.i("Bluetooth Connection", "Using fallback method");

                socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                socket.connect();

                Log.i("Bluetooth Connection", "CONNECTED");

                new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                socketConnected = true;
                return true;

            } catch (Exception e2) {
                e2.printStackTrace();
                Log.e("Bluetooth Connection", "Couldn't establish connection");
                return false;
            }
        }
    }

    public void setSimulation(final boolean simulation) {
        this.simulation = simulation;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public void setObdParameterList(final AppCompatActivity activity) {
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
        data.addProperty("lat", location.getLatitude());
        data.addProperty("lng", location.getLongitude());
        data.addProperty("trip_id", trip_id);

        final JsonObject props = new JsonObject();
        data.add("props", props);

        for (ObdParameter obdParameter : obdParameterList) {
            obdParameter.setJsonProp(props);
        }
        return event;
    }

    public synchronized void startObdScanThread(final Activity activity) {
        if (obdScanThread != null) {
            return;
        }
        obdScanThread = new Thread() {
            @Override
            public void run() {
                Log.i("Obd Scan Thread", "STARTED");
                System.out.println("Obd Scan Thread: STARTED");
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(OBD_REFRESH_INTERVAL_MS);
                        for (ObdParameter obdParam : obdParameterList) {
                            obdParam.showScannedValue(socket, simulation, activity);
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        obdScanThread.start();
    }

    public synchronized void stopObdScanThread() {
        if (obdScanThread != null) {
            System.out.println("Obd Scan Thread: INTERRUPTED");
            Log.i("Obd Scan Thread", "INTERRUPTED");
            obdScanThread.interrupt();
            obdScanThread = null;
        }
    }
}

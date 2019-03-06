/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AHKPKY&popup=n&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 * <p>
 * You may not use this file except in compliance with the license.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.Home;

/*
 * OBD Bridge via Bluetooth
 */
public class ObdBridgeBluetooth extends ObdBridge {

    private static final UUID SPPUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BluetoothManager.class.getName();

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket socket = null;

    private String userDeviceAddress = null;
    private String userDeviceName = null;

    public ObdBridgeBluetooth(final Home home) {
        super(home);
    }

    @Override
    public void clean() {
        super.clean();
        closeBluetoothSocket();
    }

    @Override
    protected String getDeviceUniqueKey() {
        return userDeviceAddress;
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

    public synchronized boolean connectBluetoothSocket(final String userDeviceAddress, final String userDeviceName, final int timeout_ms, final ObdProtocols obd_protocol) {
        if (this.userDeviceAddress == userDeviceAddress && socket != null && socket.isConnected()) {
            return true;
        }
        closeBluetoothSocket();
        this.userDeviceAddress = userDeviceAddress;
        this.userDeviceName = userDeviceName;

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice device = btAdapter.getRemoteDevice(userDeviceAddress);
        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            socket = device.createRfcommSocketToServiceRecord(ObdBridgeBluetooth.SPPUUID);
        } catch (Exception e) {
            Log.e("Bluetooth Connection", "Socket couldn't be created");
            e.printStackTrace();
            return false;
        }
        try {
            socket.connect();
            socketConnected(timeout_ms, obd_protocol);
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("BT Connection failed:", e.getMessage());
            return false;
        }
    }

    public synchronized void closeBluetoothSocket() {
        final OutputStream outs = getOutputStream();
        if (outs != null) {
            try {
                outs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final InputStream ins = getInputStream();
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    @Override
    public boolean isConnected() {
        return (socket != null && socket.isConnected());
    }

    @Override
    public void disconnect() {
        this.closeBluetoothSocket();
    }

    @Override
    protected OutputStream getOutputStream() {
        if (socket != null) {
            try {
                return socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected InputStream getInputStream() {
        if (socket != null) {
            try {
                return socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public String getUserDeviceAddress() {
        return userDeviceAddress;
    }

    public String getUserDeviceName() {
        return userDeviceName;
    }
}

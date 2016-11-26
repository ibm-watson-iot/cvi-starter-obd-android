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

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/*
 IoT Platform Device Client
 */

public class IoTPlatformDevice {

    static interface ProbeDatatGenerator {
        public JsonObject generateData();

        public void nofityPostResult(boolean success, JsonObject event);
    }

    private DeviceClient deviceClient = null;
    private JSONObject currentDevice;

    private int uploadTimerDelay = 5000;
    private int uploadTimerPeriod = 15000;
    private Timer uploadTimer;


    public void setDeviceDefinition(JSONObject deviceDefinition) {
        currentDevice = deviceDefinition;
    }

    public String getDeviceToken(final String deviceId) {
        final String sharedPrefsKey = "iota-obdii-auth-" + deviceId;
        return API.getStoredData(sharedPrefsKey);
    }

    public boolean hasDeviceToken(final String deviceId) {
        return !API.DOESNOTEXIST.equals(getDeviceToken(deviceId));
    }

    public void setDeviceToken(final String deviceId, final String authToken) {
        final String sharedPrefsKey = "iota-obdii-auth-" + deviceId;
        if (!API.getStoredData(sharedPrefsKey).equals(authToken)) {
            API.storeData(sharedPrefsKey, authToken);
        }
    }

    public synchronized DeviceClient createDeviceClient() throws Exception {
        if (deviceClient != null) {
            return deviceClient;
        }
        final Properties options = new Properties();
        options.setProperty("org", API.orgId);
        options.setProperty("type", API.typeId);
        final String deviceId = currentDevice.getString("deviceId");
        options.setProperty("id", deviceId);
        options.setProperty("auth-method", "token");
        final String token = getDeviceToken(deviceId);
        options.setProperty("auth-token", token);

        deviceClient = new DeviceClient(options);
        return deviceClient;
    }

    public void connectDevice() throws MqttException {
        if (deviceClient != null && !deviceClient.isConnected()) {
            deviceClient.connect();
        }
    }

    public void disconnectDevice() {
        if (deviceClient != null && deviceClient.isConnected()) {
            deviceClient.disconnect();
        }
        deviceClient = null;
    }


    public int getUploadTimerPeriod() {
        return uploadTimerPeriod;
    }

    public void setUploadTimerPeriod(final int value) {
        uploadTimerPeriod = value;
    }

    public synchronized void startPublishing(final ProbeDatatGenerator eventGenerator) {
        // stop existing uploadTimer
        stopPublishing();

        // start new uploadTimer
        uploadTimer = new Timer();
        uploadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    final JsonObject event = eventGenerator.generateData();
                    if (event != null) {
                        eventGenerator.nofityPostResult(publishEvent(event), event);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, uploadTimerDelay, uploadTimerPeriod);
    }

    public synchronized void stopPublishing() {
        if (uploadTimer != null) {
            uploadTimer.cancel();
            uploadTimer = null;
        }
    }

    private boolean publishEvent(final JsonObject event) throws MqttException {
        // Normally, the connection is kept alive, but it is closed when interval is long. Reconnect in this case.
        connectDevice();

        if (deviceClient != null) {
            deviceClient.publishEvent("status", event, 0);
            return true;
        } else {
            return false;
        }
    }
}

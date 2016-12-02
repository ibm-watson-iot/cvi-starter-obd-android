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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
 IoT Platform Device Client
 */

public class IoTPlatformDevice {

    public static final String MQTT_FREQENCY = "mqtt-freqency";
    public static final int MIN_FREQUENCY_SEC = 5;
    public static final int MAX_FREQUENCY_SEC = 60;
    public static final int DEFAULT_FREQUENCY_SEC = 10;
    public static final int DEFAULT_UPLOAD_DELAY_SEC = 5;

    public static int getMqttFrequencySec() {
        final String freq_str = API.getStoredData(MQTT_FREQENCY);
        return API.DOESNOTEXIST.equals(freq_str) ? DEFAULT_FREQUENCY_SEC : Integer.parseInt(freq_str);
    }

    public static void setMqttFrequencySec(int sec) {
        API.storeData(MQTT_FREQENCY, "" + sec);
    }

    static interface ProbeDataGenerator {
        public JsonObject generateData();

        public void notifyPostResult(boolean success, JsonObject event);
    }

    private DeviceClient deviceClient = null;
    private JSONObject currentDevice;

    private int uploadDelay = DEFAULT_UPLOAD_DELAY_SEC * 1000;
    private int uploadInterval = DEFAULT_FREQUENCY_SEC * 1000;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> uploadHandler = null;

    @Override
    protected void finalize() throws Throwable {
        stopPublishing();
        scheduler.shutdown();
        super.finalize();
    }

    public void setDeviceDefinition(JSONObject deviceDefinition) {
        currentDevice = deviceDefinition;
        if (deviceClient != null) {
            disconnectDevice();
            deviceClient = null;
        }
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
        if (currentDevice == null) {
            throw new NoDeviceDefinitionException();
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
        System.out.println("IOTP DEVICE CLIENT CREATED: " + options.toString());
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
        return uploadInterval;
    }

    public void setUploadTimerPeriod(final int value) {
        uploadInterval = value;
    }

    public synchronized void startPublishing(final ProbeDataGenerator eventGenerator) {
        stopPublishing();

        uploadDelay = DEFAULT_UPLOAD_DELAY_SEC * 1000;
        uploadInterval = getMqttFrequencySec() * 1000;
        uploadHandler = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    final JsonObject event = eventGenerator.generateData();
                    if (event != null) {
                        eventGenerator.notifyPostResult(publishEvent(event), event);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, uploadDelay, uploadInterval, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopPublishing() {
        if (uploadHandler != null) {
            uploadHandler.cancel(true);
            uploadHandler = null;
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

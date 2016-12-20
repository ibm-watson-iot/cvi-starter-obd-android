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

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
 IoT Platform Device Client
 */

public class IoTPlatformDevice {


    // Platform parameters
    static final String defaultOrganizationId = "kibb33";
    static final String defaultApiKey = "a-kibb33-rkhexfo7ml";
    static final String defaultApiToken = "lDfjTThkWv*@Ea_!4d";

    private static final String typeId = "OBDII";

    @NonNull
    private static final String getIoTPAPIURL(final String organizationId) {
        return "https://" + organizationId + ".internetofthings.ibmcloud.com/api/v0002";
    }

    @NonNull
    private static final String getIoTPAddDevicesEndPoint(final String organizationId) {
        return getIoTPAPIURL(organizationId) + "/bulk/devices/add";
    }

    @NonNull
    private static String getIoTPGetDeviceEndpoint(final String organizationId, final String device_id) {
        return getIoTPAPIURL(organizationId) + "/device/types/" + typeId + "/devices/" + device_id;
    }

    static interface ProbeDataGenerator {
        public JsonObject generateData();

        public void notifyPostResult(boolean success, JsonObject event);
    }

    public static abstract class ResponseListener implements API.doRequest.TaskListener {

        @Override
        public void postExecute(JSONArray result) throws JSONException {
            final JSONObject serverResponse = result.getJSONObject(result.length() - 1);
            final int statusCode = serverResponse.getInt("statusCode");
            response(statusCode, result);
        }

        protected abstract void response(int statusCode, JSONArray result);

    }

    private String organizationId = null;
    private String apiKey = null;
    private String apiToken = null;

    private DeviceClient deviceClient = null;
    private JSONObject currentDevice;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> uploadHandler = null;

    void clean() {
        stopPublishing();
        scheduler.shutdown();
    }


    public String getDeviceToken(final String deviceId) {
        final String sharedPrefsKey = "iota-obdii-auth-" + deviceId;
        final String deviceToken = API.getStoredData(sharedPrefsKey);
        return API.DOESNOTEXIST.equals(deviceToken) ? "" : deviceToken;
    }

    public boolean hasDeviceToken(final String deviceId) {
        return !"".equals(getDeviceToken(deviceId));
    }

    public void setDeviceToken(final String deviceId, final String authToken) {
        final String sharedPrefsKey = "iota-obdii-auth-" + deviceId;
        if (!API.getStoredData(sharedPrefsKey).equals(authToken)) {
            API.storeData(sharedPrefsKey, authToken);
        }
    }

    private String getCredentialsBase64() {
        return API.getCredentialsBase64(apiKey, apiToken);
    }

    public boolean hasValidOrganization() {
        return organizationId != null && !"".equals(organizationId);
    }

    public synchronized boolean createDeviceClient(final JSONObject deviceDefinition) throws Exception {
        if (deviceDefinition != null) {
            if (deviceClient != null) {
                disconnectDevice();
            }
            currentDevice = deviceDefinition;
        }
        if (deviceClient != null) {
            return true;
        }
        if (currentDevice == null) {
            throw new NoDeviceDefinitionException();
        }
        if (!hasValidOrganization()) {
            throw new NoIoTPOrganizationException();
        }

        final Properties options = new Properties();
        options.setProperty("org", organizationId);
        options.setProperty("type", typeId);
        final String deviceId = currentDevice.getString("deviceId");
        options.setProperty("id", deviceId);
        options.setProperty("auth-method", "token");
        final String token = getDeviceToken(deviceId);
        options.setProperty("auth-token", token);

        deviceClient = new DeviceClient(options);
        System.out.println("IOTP DEVICE CLIENT CREATED: " + options.toString());
        return true;
    }


    public void checkDeviceRegistration(final ResponseListener listener, final String device_id) throws InterruptedException, ExecutionException, NoIoTPOrganizationException {
        if (!hasValidOrganization()) {
            throw new NoIoTPOrganizationException();
        }
        final API.doRequest task = new API.doRequest(listener);
        final String url = getIoTPGetDeviceEndpoint(organizationId, device_id);
        task.execute(url, "GET", null, null, getCredentialsBase64()).get();
        System.out.println("CHECKING DEVICE REGISTRATION SUCCESSFULLY DONE......");
        Log.d("Got", url);
    }


    public void requestDeviceRegistration(final ResponseListener listener, final String device_id) throws InterruptedException, ExecutionException, NoIoTPOrganizationException {
        if (!hasValidOrganization()) {
            throw new NoIoTPOrganizationException();
        }
        final API.doRequest task = new API.doRequest(listener);
        try {
            final String url = getIoTPAddDevicesEndPoint(organizationId);
            final JSONArray bodyArray = new JSONArray();
            final JSONObject bodyObject = new JSONObject();
            bodyObject
                    .put("typeId", typeId)
                    .put("deviceId", device_id);
            bodyArray
                    .put(bodyObject);
            final String payload = bodyArray.toString();
            task.execute(url, "POST", null, payload, getCredentialsBase64()).get();

            System.out.println("REGISTER DEVICE REQUEST SUCCESSFULLY POSTED......");
            Log.d("Posted", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public synchronized void connectDevice() throws MqttException {
        if (deviceClient != null && !deviceClient.isConnected()) {
            deviceClient.connect();
        }
    }

    public synchronized void disconnectDevice() {
        if (deviceClient != null && deviceClient.isConnected()) {
            deviceClient.disconnect();
        }
        deviceClient = null;
    }

    public synchronized boolean isConnected() {
        return deviceClient != null && deviceClient.isConnected();
    }

    public synchronized void startPublishing(final ProbeDataGenerator eventGenerator, final int uploadDelayMS, final int uploadIntervalMS) {
        stopPublishing();

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
        }, uploadDelayMS, uploadIntervalMS, TimeUnit.MILLISECONDS);
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

    public final boolean isCurrentOrganizationSameAs(final String newId) {
        return organizationId != null && organizationId.equals(newId);
    }

    public void changeOrganization(final String newOrdId, final String newApiKey, final String newApiToken) {
        if (isCurrentOrganizationSameAs(newOrdId)) {
            return;
        }
        stopPublishing();
        disconnectDevice();

        this.organizationId = newOrdId;
        this.apiKey = newApiKey;
        this.apiToken = newApiToken;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiToken() {
        return apiToken;
    }
}

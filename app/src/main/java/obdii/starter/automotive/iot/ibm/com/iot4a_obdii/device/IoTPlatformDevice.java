/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AHKPKY&popup=n&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 * <p>
 * You may not use this file except in compliance with the license.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.DeviceNotConnectedException;
import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.NoAccessInfoException;

/*
 IoT Platform Device Client
 */

public class IoTPlatformDevice extends AbstractVehicleDevice {
    private DeviceClient deviceClient = null;

    IoTPlatformDevice(AccessInfo accessInfo){
        super(accessInfo);
    }

    @Override
    public void setAccessInfo(AccessInfo accessInfo) {
        String orgId = accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        if (isCurrentOrganizationSameAs(orgId)) {
            return;
        }
        stopPublishing();
        disconnect();
        this.accessInfo = accessInfo;
    }

    public void connect() throws Exception {
        if(deviceClient == null){
            createDeviceClient();
        }
        if (deviceClient != null && !deviceClient.isConnected()) {
            try {
                deviceClient.connect();
            } catch (MqttException e) {
                throw new DeviceNotConnectedException(e);
            }
        }
    }

    public void disconnect() {
        if (deviceClient != null && deviceClient.isConnected()) {
            deviceClient.disconnect();
        }
        deviceClient = null;
    }

    public boolean hasValidAccessInfo(){
        if(this.accessInfo == null){
            return false;
        }
        String orgId = this.accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        String typeId = this.accessInfo.get(AccessInfo.ParamName.VENDOR);
        String mo_id = this.accessInfo.get(AccessInfo.ParamName.MO_ID);
        String token = this.accessInfo.get(AccessInfo.ParamName.PASSWORD);
        return orgId != null && orgId != "" && typeId != null && typeId != "" && mo_id != null && mo_id != "" && token != null && token != "";
    }

    public synchronized boolean createDeviceClient() throws Exception {
        if (deviceClient != null) {
            return true;
        }
        if (!hasValidAccessInfo()) {
            throw new NoAccessInfoException("Access information to publish vehicle event is missing.");
        }
        disconnect();

        final Properties options = new Properties();
        options.setProperty("org", this.accessInfo.get(AccessInfo.ParamName.ENDPOINT));
        options.setProperty("type", this.accessInfo.get(AccessInfo.ParamName.VENDOR));
        options.setProperty("id", this.accessInfo.get(AccessInfo.ParamName.MO_ID));
        options.setProperty("auth-method", "token");
        options.setProperty("auth-token", this.accessInfo.get(AccessInfo.ParamName.PASSWORD));

        deviceClient = new DeviceClient(options);
        System.out.println("IOTP DEVICE CLIENT CREATED: " + options.toString());
        return true;
    }

    public synchronized boolean isConnected() {
        return deviceClient != null && deviceClient.isConnected();
    }

    @Override
    boolean publishEvent(final String probe) throws Exception {
        if(!isConnected()){
            connect();
        }

        if (deviceClient != null) {
            return deviceClient.publishEvent("carprobe", probe, "csv", 0);
        } else {
            return false;
        }
    }

    public final boolean isCurrentOrganizationSameAs(final String newId) {
        return accessInfo != null && accessInfo.get(AccessInfo.ParamName.ENDPOINT).equals(newId);
    }

    public void changeOrganization(final String orgId, final String typeId, final String deviceId, final String deviceToken) {


        setAccessInfo(new AccessInfo(orgId, typeId, deviceId, null, deviceToken));
    }

    public static AccessInfo createAccessInfo(String endpoint, String vendor, String mo_id, String user, String password){
        return new AccessInfo(endpoint, vendor, mo_id, user, password);
    }
}

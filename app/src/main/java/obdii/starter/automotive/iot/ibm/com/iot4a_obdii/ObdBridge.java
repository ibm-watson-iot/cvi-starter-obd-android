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

import android.location.Location;
import android.support.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static obdii.starter.automotive.iot.ibm.com.iot4a_obdii.Home.DOESNOTEXIST;

/*
 * abstract obd2 bridge for ELM 327 devices (common part for bluetooth, wifi, or usb type)
 */
public abstract class ObdBridge {

    static final int DEFAULT_OBD_TIMEOUT_MS = 500;
    static final int MIN_TIMEOUT_MS = 0;
    static final int MAX_TIMEOUT_MS = 1020;
    private int obd_timeout_ms = DEFAULT_OBD_TIMEOUT_MS;

    static final ObdProtocols DEFAULT_OBD_PROTOCOL = ObdProtocols.AUTO;
    private ObdProtocols obd_protocol = DEFAULT_OBD_PROTOCOL;

    public static CharSequence[] getOBDProtocols() {
        final ArrayList<CharSequence> retv = new ArrayList<CharSequence>();
        for (ObdProtocols protocol : ObdProtocols.values()) {
            retv.add(protocol.name());
        }
        return (CharSequence[]) retv.toArray(new CharSequence[0]);
    }

    private boolean simulation = true;
    private List<ObdParameter> obdParameterList = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> obdScannerHandle = null;

    protected final Home home;

    protected ObdBridge(Home home) {
        this.home = home;
    }

    public void clean() {
        stopObdScan();
        scheduler.shutdown();
    }

    public String getDeviceId(final boolean simulation, final String uuid) throws DeviceNotConnectedException {
        return simulation ? uuid : getRealDeviceId();
    }

    private String getRealDeviceId() throws DeviceNotConnectedException {
        if (getDeviceUniqueKey() == null) {
            throw new DeviceNotConnectedException();
        }
        final String key = "device-id-" + getDeviceUniqueKey().replaceAll(":", "-").replaceAll(".", "-");
        String device_id = home.getPreference(key, DOESNOTEXIST);
        if (device_id == null || DOESNOTEXIST.equals(device_id)) {
            device_id = UUID.randomUUID().toString();
            home.setPreference(key, device_id);
        }
        return device_id;
    }

    public synchronized void startObdScan(final boolean simulation, final int scanDelayMS, final int scanIntervalMS, final Runnable terminationHandler) {
        stopObdScan();

        this.simulation = simulation;
        Log.i("Obd Scan Thread", "STARTED");
        System.out.println("Obd Scan Thread: STARTED");
        obdScannerHandle = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (simulation || isConnected()) {
                        for (ObdParameter obdParam : obdParameterList) {
                            obdParam.showScannedValue(getInputStream(), getOutputStream(), simulation);
                        }
                    } else {
                        terminationHandler.run();
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    System.out.println("Cancelling OBD Scan Thread");
                    disconnect();
                    terminationHandler.run();
                }
            }
        }, scanDelayMS, scanIntervalMS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopObdScan() {
        if (obdScannerHandle != null) {
            obdScannerHandle.cancel(true);
            obdScannerHandle = null;
            Log.i("Obd Scan Thread", "ENDED");
            System.out.println("Obd Scan Thread: ENDED");
        }
    }

    protected void socketConnected(final int obd_timeout_ms, final ObdProtocols obd_protocol) throws InterruptedException {
        Log.i("OBD2 Device Connection", "CONNECTED - " + isConnected() + " timeout_ms=" + obd_timeout_ms + " protocol=" + obd_protocol);
        initializeOBD2Device(obd_timeout_ms, obd_protocol);
    }

    private void initializeOBD2Device(final int obd_timeout_ms, final ObdProtocols obd_protocol) throws InterruptedException {
        this.obd_timeout_ms = obd_timeout_ms;
        this.obd_protocol = obd_protocol;

        final InputStream ins = getInputStream();
        final OutputStream outs = getOutputStream();
        //runObdCommandIgnoreException(ins, outs, new ObdRawCommand("ATD")); // Set all to defaults
        runObdCommandIgnoreException(ins, outs, new ObdWarmstartCommand()); // reset
        runObdCommandIgnoreException(ins, outs, new ObdResetCommand());
        runObdCommandIgnoreException(ins, outs, new EchoOffCommand());
        runObdCommandIgnoreException(ins, outs, new LineFeedOffCommand());
        runObdCommandIgnoreException(ins, outs, new HeadersOffCommand());
        runObdCommandIgnoreException(ins, outs, new TimeoutCommand(obd_timeout_ms / 4)); // value should be 0-255 (0 - 1020 milli sec)
        runObdCommandIgnoreException(ins, outs, new AdaptiveTimingCommand(1));
        runObdCommandIgnoreException(ins, outs, new SelectProtocolCommand(obd_protocol));
    }

    private void runObdCommandIgnoreException(final InputStream ins, final OutputStream outs, final ObdCommand cmd) throws InterruptedException {
        // ignoring exceptions would be risky, but it may make the initialization going as much as possible
        if (ins == null || outs == null) {
            return;
        }
        try {
            cmd.run(ins, outs);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public boolean isSimulation() {
        return simulation;
    }

    public void initializeObdParameterList(final Home home) {
        obdParameterList = ObdParameters.getObdParameterList(home);
    }

    @NonNull
    public JsonObject generateEvent(final Location location, final String trip_id) {
        if (obdParameterList == null) {
            // parameter list has to be set
        }
        final JsonObject data = new JsonObject();
        data.addProperty("trip_id", trip_id);

        final JsonObject props = new JsonObject();

        for (ObdParameter obdParameter : obdParameterList) {
            obdParameter.setJsonProp(obdParameter.isBaseProp() ? data : props);
        }
        data.add("props", props);
        return data;
    }

    public boolean isCurrentObdTimeoutSameAs(final int obd_timeout_ms) {
        return this.obd_timeout_ms == obd_timeout_ms;
    }

    public boolean isCurrentObdProtocolSameAs(final ObdProtocols obd_protocol) {
        return this.obd_protocol == obd_protocol;
    }

    protected abstract String getDeviceUniqueKey();

    public abstract boolean isConnected();

    public void disconnect() {
    }


    protected abstract OutputStream getOutputStream();

    protected abstract InputStream getInputStream();


}

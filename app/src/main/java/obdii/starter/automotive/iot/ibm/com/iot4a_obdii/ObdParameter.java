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
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Process OBD parameter
 */

abstract public class ObdParameter {

    final private TextView textView;
    final private String label;
    final private ObdCommand obdCommand;

    public ObdParameter(final TextView textView, final String label, final ObdCommand obdCommand) {
        this.textView = textView;
        this.label = label;
        this.obdCommand = obdCommand;
    }

    public String getLabel() {
        return label;
    }

    public ObdCommand getObdCommand() {
        return obdCommand;
    }

    public void showScannedValue(final BluetoothSocket socket, final boolean simulation, final Activity activity) {
        if (simulation) {
            fetchValue(null, simulation);
            showText(getValueText(), activity);
        } else {
            String value = "";
            if (socket == null) {
                value = "Connection Error";
            } else {
                try {
                    final InputStream in = socket.getInputStream();
                    final OutputStream out = socket.getOutputStream();
                    obdCommand.run(in, out);
                    fetchValue(obdCommand, simulation);
                    value = getValueText();
                    Log.d(label, obdCommand.getFormattedResult());
                } catch (com.github.pires.obd.exceptions.UnableToConnectException e) {
                    // reach here when OBD device is not connected
                    value = "Connection Error";
                } catch (com.github.pires.obd.exceptions.NoDataException e) {
                    // reach here when this OBD parameter is not supported
                    value = "N/A";
                } catch (Exception e) {
                    value = "Error";
                }
            }
            showText(value, activity);
        }

    }

    private void showText(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    /*
    get actual OBD parameter value (obdCommand has already run at this call
     */
    abstract protected void fetchValue(ObdCommand obdCommand, boolean simulation);

    abstract protected void setJsonProp(JsonObject json);

    abstract protected String getValueText();
}

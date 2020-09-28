/**
 * Copyright 2016,2019 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.obd;

import android.location.Location;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.google.gson.JsonObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.Home;
import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.R;

/**
 * Obd Parameters
 */

public class ObdParameters {
    private static SecureRandom random = new SecureRandom();

    static String formatTemperature(double temperature) {
        double imperialUnit = temperature * 1.8f + 32;

        return String.format("%.1f%s", temperature, "C") + " (" + String.format("%.1f%s", imperialUnit, "F") + ")";
    }

    static String formatSpeed(double speed) {
        double imperialUnit = speed * 0.621371192f;

        return String.format("%.1f%s", speed, "km/h") + " (" + String.format("%.1f%s", imperialUnit, "mph") + ")";
    }

    @NonNull
    static public List<ObdParameter> getObdParameterList(final Home activity) {
        final List<ObdParameter> obdParameters = new ArrayList<ObdParameter>();

        final ObdParameter engineRPM = new ObdParameter((TextView) activity.findViewById(R.id.engineRPMValue), activity, "Engine RPM", new RPMCommand()) {
            private long engineRPM = Math.round(random.nextFloat() * 3000) + 600;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                if (simulation) {
                    engineRPM = Math.round(random.nextFloat() * 3000) + 600;
                } else {
                    final RPMCommand rpmCommand = (RPMCommand) obdCommand;
                    engineRPM = rpmCommand.getRPM();
                }
                valueText = engineRPM + "";
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("engineRPM", engineRPM);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(engineRPM);

        final ObdParameter speed = new ObdParameter((TextView) activity.findViewById(R.id.speedValue), activity, "Speed", new SpeedCommand()) {
            private final double max_speed = random.nextFloat() > 0.5 ? 130.0 : 90.0;
            private final double speed_increment = 6.3;
            private double speed = 30.0;
            private double delta = 0;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                if (simulation) {
                    final Location location = activity.getLocation();
                    if(location == null){
                        if (speed >= max_speed) {
                            delta = -1.0 * speed_increment;
                        } else if (speed <= 0.0) {
                            speed = 0.0;
                            delta = speed_increment;
                        } else {
                            final float randomFloat = random.nextFloat();
                            if (randomFloat < 0.4) {
                                delta = -1.0 * speed_increment;
                            } else if (randomFloat > 0.6) {
                                delta = speed_increment;
                            } else {
                                delta = 0;
                            }
                        }
                        speed += delta;
                        if (speed <= 0.0) {
                            speed = 0.01;
                        }
                    } else {
                        speed = location.getSpeed();
                    }
                } else {
                    final SpeedCommand speedCommand = (SpeedCommand) obdCommand;
                    speed = speedCommand.getMetricSpeed();
                }
                valueText = formatSpeed(speed);
            }

            @Override
            protected boolean isBaseProp() {
                return true;
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("speed", speed);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(speed);

        final ObdParameter engineOil = new ObdParameter((TextView) activity.findViewById(R.id.engineOilValue), activity, "Engine Oil", new OilTempCommand()) {
            private float engineOil = random.nextFloat() * 120 + 20;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                if (simulation) {
                    engineOil = random.nextFloat() * (140 - engineOil - 10) + engineOil - 10;

                } else {
                    final OilTempCommand oilTempCommand = (OilTempCommand) obdCommand;
                    engineOil = oilTempCommand.getTemperature();
                }
                valueText = formatTemperature(engineOil);
            }


            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("engineOilTemp", engineOil);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(engineOil);

        final ObdParameter engineCoolant = new ObdParameter((TextView) activity.findViewById(R.id.engineCoolantValue), activity, "Engine Coolant", new EngineCoolantTemperatureCommand()) {
            private float engineCoolant = random.nextFloat() * 120 + 20;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                if (simulation) {
                    engineCoolant = random.nextFloat() * (140 - engineCoolant - 10) + engineCoolant - 10;
                } else {
                    final EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = (EngineCoolantTemperatureCommand) obdCommand;
                    engineCoolant = engineCoolantTemperatureCommand.getTemperature();
                }
                valueText = formatTemperature(engineCoolant);
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("engineTemp", engineCoolant);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(engineCoolant);

        final ObdParameter fuelLevel = new ObdParameter((TextView) activity.findViewById(R.id.fuelLevelValue), activity, "Fuel Level", new FuelLevelCommand()) {
            private float fuelLevel = (float)Math.floor(random.nextFloat() * 95) + 5;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                if (simulation) {
                    if (--fuelLevel < 5) {
                        fuelLevel = 60;
                    }
                    valueText = Math.round(fuelLevel) + "%";
                } else {
                    final FuelLevelCommand fuelLevelCommand = (FuelLevelCommand) obdCommand;
                    fuelLevel = fuelLevelCommand.getFuelLevel();
                    valueText = Math.round(fuelLevel) + "%";
                }
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("fuel", fuelLevel);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(fuelLevel);

        final ObdParameter longitude = new ObdParameter((TextView) activity.findViewById(R.id.longitudeValue), activity, "Longitude", null) {
            private double longitudeValue = 0.0;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                final Location location = activity.getLocation();
                if (location != null) {
                    longitudeValue = location.getLongitude();
                }
                valueText = String.format("%1$.7f", longitudeValue);
            }

            @Override
            protected boolean isBaseProp() {
                return true;
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("lng", longitudeValue);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(longitude);

        final ObdParameter latitude = new ObdParameter((TextView) activity.findViewById(R.id.latitudeValue), activity, "Latitude", null) {
            private double latitude = 0.0;
            private String valueText;

            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                final Location location = activity.getLocation();
                if (location != null) {
                    latitude = location.getLatitude();
                }
                valueText = String.format("%1$.7f", latitude);
           }

            @Override
            protected boolean isBaseProp() {
                return true;
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("lat", latitude);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(latitude);

        final ObdParameter heading = new ObdParameter((TextView) activity.findViewById(R.id.headingValue), activity, "Heading", null) {
            private double heading = 0;
            private String valueText;
            private Location prevLocation;
            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                final Location location = activity.getLocation();
                if(prevLocation != null && !prevLocation.equals(location)){ // In case location is changed
                    heading = calcHeading(prevLocation, location);
                }
//                Bearing from location object may not represent vehicle heading for example in landscape mode
//                if(location != null){
//                    heading = location.getBearing();
//                }
                valueText = String.format("%1$.4f", heading);
                prevLocation = location;
            }

            @Override
            protected boolean isBaseProp() {
                return true;
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("heading", heading);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }

            private double calcHeading(Location p1, Location p2){
                // this will calculate bearing
                double p1lon = toRad(p1.getLongitude());
                double p1lat = toRad(p1.getLatitude());
                double p2lon = toRad(p2.getLongitude());
                double p2lat = toRad(p2.getLatitude());
                double y = Math.sin(p2lon-p1lon) * Math.cos(p2lat);
                double x = Math.cos(p1lat)*Math.sin(p2lat) - Math.sin(p1lat)*Math.cos(p2lat)*Math.cos(p2lon-p1lon);
                double brng = Math.atan2(y, x);
                return (toDegree(brng) + 360) % 360;
            }
            private double toRad(double d){
                return d * (Math.PI / 180);
            }
            private double toDegree(double d){
                return d * (180 / Math.PI);
            }
        };
        obdParameters.add(heading);

        final ObdParameter altitude = new ObdParameter(null, activity, "Altitude", null) {
            private double altitude = 0;
            private String valueText;
            @Override
            protected void fetchValue(ObdCommand obdCommand, boolean simulation) {
                final Location location = activity.getLocation();
                if(location != null){
                    altitude = location.getAltitude();
                }
                valueText = String.format("%1$.4f", altitude);
            }

            @Override
            protected boolean isBaseProp() {
                return true;
            }

            @Override
            protected void setJsonProp(JsonObject json) {
                json.addProperty("altitude", altitude);
            }

            @Override
            protected String getValueText() {
                return valueText;
            }
        };
        obdParameters.add(altitude);

        return obdParameters;
    }
}

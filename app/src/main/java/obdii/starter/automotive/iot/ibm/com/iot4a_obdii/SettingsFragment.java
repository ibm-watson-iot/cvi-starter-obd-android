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
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment {

    public static final String ORGANIZATION_ID = "organization_id";
    public static final String API_KEY = "api_key";
    public static final String API_TOKEN = "api_token";
    public static final String DEVICE_ID = "device_id";
    public static final String DEVICE_TOKEN = "device_token";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        initializeSettings();
    }

    public void initializeSettings() {
        prepareEditTextPreference(ORGANIZATION_ID, IoTPlatformDevice.defaultOrganizationId, true);
        prepareEditTextPreference(API_KEY, IoTPlatformDevice.defaultApiKey, true);
        prepareEditTextPreference(API_TOKEN, IoTPlatformDevice.defaultApiToken, true);

        String device_id = "";
        String device_token = "";
        if (Home.home != null) {
            final ObdBridge obdBridge = Home.home.obdBridge;
            final IoTPlatformDevice iotpDevice = Home.home.iotpDevice;
            try {
                device_id = obdBridge.getDeviceId(obdBridge.isSimulation());
            } catch (DeviceNotConnectedException e) {
            }
            device_token = iotpDevice.getDeviceToken(device_id);
        }
        setEditTextPreference(DEVICE_ID, device_id, false);
        setEditTextPreference(DEVICE_TOKEN, device_token, true);
    }

    private void prepareEditTextPreference(final String prefKey, final String defaultValue, final boolean enabled) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            final EditTextPreference editTextPref = (EditTextPreference) preference;
            editTextPref.setEnabled(enabled);
            if (editTextPref.getText() == null) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(prefKey, defaultValue);
                editTextPref.setText(defaultValue);
            }
            editTextPref.setSummary(editTextPref.getText());
            editTextPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
        }
    }

    private void setEditTextPreference(final String prefKey, final String value, final boolean enabled) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            final EditTextPreference editTextPref = (EditTextPreference) preference;
            editTextPref.setEnabled(enabled);
            editTextPref.setText(value);
            editTextPref.setSummary(editTextPref.getText());
            editTextPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
        }
    }

    private String getEditTextPreferenceValue(final String prefKey) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            final EditTextPreference editTextPref = (EditTextPreference) preference;
            final String value = editTextPref.getText();
            return value;
        } else {
            return null;
        }
    }

    public void completeSettings() {
        final String orgId = getEditTextPreferenceValue(ORGANIZATION_ID);
        final String apiKey = getEditTextPreferenceValue(API_KEY);
        final String apiToken = getEditTextPreferenceValue(API_TOKEN);
        //String device_id = getEditTextPreferenceValue(DEVICE_ID);

        if (!Home.home.iotpDevice.compareCurrentOrganization(orgId)) {
            Home.home.restartApp(orgId, apiKey, apiToken);
        }
    }
}

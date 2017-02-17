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

import android.content.Intent;
import android.content.SharedPreferences;
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
    public static final String BLUETOOTH_DEVICE_NAME = "bt_device_name";
    public static final String BLUETOOTH_DEVICE_ADDRESS = "bt_device_address";
    public static final String UPLOAD_FREQUENCY = "upload_frequency";

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
        final Intent intent = getActivity().getIntent();
        prepareEditTextPreference(ORGANIZATION_ID, intent.getStringExtra(ORGANIZATION_ID), IoTPlatformDevice.defaultOrganizationId, true);
        prepareEditTextPreference(API_KEY, intent.getStringExtra(API_KEY), IoTPlatformDevice.defaultApiKey, true);
        prepareEditTextPreference(API_TOKEN, intent.getStringExtra(API_TOKEN), IoTPlatformDevice.defaultApiToken, true);
        prepareEditTextPreference(DEVICE_ID, intent.getStringExtra(DEVICE_ID), "", false);
        prepareEditTextPreference(DEVICE_TOKEN, intent.getStringExtra(DEVICE_TOKEN), "", true);
        prepareEditTextPreference(BLUETOOTH_DEVICE_NAME, intent.getStringExtra(BLUETOOTH_DEVICE_NAME), "", false);
        prepareEditTextPreference(BLUETOOTH_DEVICE_ADDRESS, intent.getStringExtra(BLUETOOTH_DEVICE_ADDRESS), "", false);
        prepareEditTextPreference(UPLOAD_FREQUENCY, intent.getStringExtra(UPLOAD_FREQUENCY), "" + Home.DEFAULT_FREQUENCY_SEC, false);
    }

    private void prepareEditTextPreference(final String prefKey, final String currentValue, final String defaultValue, final boolean enabled) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            final EditTextPreference editTextPref = (EditTextPreference) preference;
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String storedValue = preferences.getString(prefKey, defaultValue);
            final String valueToSet = currentValue != null ? currentValue : storedValue;
            editTextPref.setText(valueToSet);
            editTextPref.setEnabled(enabled);

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

    void setPreferenceValue(final String prefKey, final String value) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setText(value);
        }
    }

    String getPreferenceValue(final String prefKey) {
        final Preference preference = findPreference(prefKey);
        if (preference instanceof EditTextPreference) {
            return ((EditTextPreference) preference).getText();
        } else {
            return null;
        }
    }
}

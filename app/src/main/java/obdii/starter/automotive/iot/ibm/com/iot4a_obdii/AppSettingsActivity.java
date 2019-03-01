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
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class AppSettingsActivity extends AppCompatActivity {

    private SettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        fragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("appServerChanged", isAppServerChanged());
        setResult(RESULT_OK, result);
        finish();
    }
    private boolean isAppServerChanged(){
        Intent intent = getIntent();
        String originalUrl = intent.getStringExtra(SettingsFragment.APP_SERVER_URL);
        String originalUser = intent.getStringExtra(SettingsFragment.APP_SERVER_USERNAME);
        String originalPass = intent.getStringExtra(SettingsFragment.APP_SERVER_PASSWORD);
        String newUrl = fragment.getPreferenceValue(SettingsFragment.APP_SERVER_URL);
        String newUser = fragment.getPreferenceValue(SettingsFragment.APP_SERVER_USERNAME);
        String newPass = fragment.getPreferenceValue(SettingsFragment.APP_SERVER_PASSWORD);
        return originalUrl == null || newUrl == null || !originalUrl.equals(newUrl)
            || (originalUser == null && newUser != null) || !(originalUser == null && newUser == null) || !originalUser.equals(newUser)
            || (originalPass == null && newPass != null) || !(originalPass == null && newPass == null) || !originalPass.equals(newPass);
    }
}

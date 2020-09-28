/**
 * Copyright 2019 IBM Corp. All Rights Reserved.
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

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.settings;


import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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

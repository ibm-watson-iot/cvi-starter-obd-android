/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
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

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class API {
    public static Context context;
    public static SharedPreferences sharedpreferences;

    public API(Context context){
        this.context = context;
    }

    public static String getUUID() {
        sharedpreferences = context.getSharedPreferences("obdii.starter.automotive.iot.ibm.com.API", Context.MODE_PRIVATE);

        String uuidString = sharedpreferences.getString("iota-starter-obdii-uuid", "no-iota-starter-obdii-uuid");

        if (uuidString != "no-iota-starter-obdii-uuid") {
            return uuidString;
        } else {
            uuidString = UUID.randomUUID().toString();

            sharedpreferences.edit().putString("iota-starter-obdii-uuid", uuidString).apply();

            return uuidString;
        }
    }
}

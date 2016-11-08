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
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class API {
    // Platform API URLs
    protected static final String orgId = "p375s9";
    protected static final String platformAPI = "https://" + orgId + ".internetofthings.ibmcloud.com/api/v0002";

    protected static final String apiKey = "a-p375s9-l4xsff5ftf";
    protected static final String apiToken = "pPKd5PLU0g-Zg1hJvt";
    protected static final String credentials = API.apiKey + ":" + API.apiToken;
    protected static final String credentialsBase64 = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT).replace("\n", "");

    protected static final String typeId = "OBDII";

    protected static final String DOESNOTEXIST = "doesNotExist";

    // Endpoints
    protected static final String addDevices = platformAPI + "/bulk/devices/add";

    public static Context context;
    public static SharedPreferences sharedpreferences;

    public API(Context context){
        this.context = context;
    }

    public static String getUUID() {
        sharedpreferences = context.getSharedPreferences("obdii.starter.automotive.iot.ibm.com.API", Context.MODE_PRIVATE);

        String uuidString = sharedpreferences.getString("iota-starter-obdii-uuid", DOESNOTEXIST);

        if (uuidString != DOESNOTEXIST) {
            return uuidString;
        } else {
            uuidString = UUID.randomUUID().toString();

            sharedpreferences.edit().putString("iota-starter-obdii-uuid", uuidString).apply();

            return uuidString;
        }
    }

    public static void storeData(String key, String value) {
        sharedpreferences = context.getSharedPreferences("obdii.starter.automotive.iot.ibm.com.API", Context.MODE_PRIVATE);
        sharedpreferences.edit().putString(key, value).apply();
    }

    public static String getStoredData(String key) {
        sharedpreferences = context.getSharedPreferences("obdii.starter.automotive.iot.ibm.com.API", Context.MODE_PRIVATE);
        return sharedpreferences.getString(key, DOESNOTEXIST);
    }

    public static boolean warningShown() {
        sharedpreferences = context.getSharedPreferences("obdii.starter.automotive.iot.ibm.com.API", Context.MODE_PRIVATE);

        boolean warningShown = sharedpreferences.getBoolean("iota-starter-obdii-warning-message", false);

        if (warningShown) {
            return warningShown;
        } else {
            sharedpreferences.edit().putBoolean("iota-starter-obdii-warning-message", true).apply();

            return false;
        }
    }

    public static class doRequest extends AsyncTask<String, Void, JSONArray> {

        public interface TaskListener {
            public void postExecute(JSONArray result) throws JSONException;
        }

        private final TaskListener taskListener;

        public doRequest(TaskListener listener) {
            this.taskListener = listener;
        }

        @Override
        protected JSONArray doInBackground(String... params) {
            /*      params[0] == url (String)
                    params[1] == request type (String e.g. "GET")
                    params[2] == parameters query (Uri converted to String)
                    params[3] == body (JSONObject converted to String)
                    params[4] == Basic Auth
            */

            int code = 0;

            try {
                URL url = new URL(params[0]);   // params[0] == URL - String
                String requestType = params[1]; // params[1] == Request Type - String e.g. "GET"
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                Log.i(requestType + " Request", params[0]);

                urlConnection.setRequestProperty("iota-starter-uuid", getUUID());
                Log.i("Using UUID", getUUID());

                urlConnection.setRequestMethod(requestType);

                if (requestType.equals("POST") || requestType.equals("PUT") || requestType.equals("GET")) {
                    if (!requestType.equals("GET")) {
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);
                    }

                    if (params.length > 2 && params[2] != null) { // params[2] == HTTP Parameters Query - String
                        String query = params[2];

                        OutputStream os = urlConnection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(query);
                        writer.flush();
                        writer.close();
                        os.close();

                        Log.i("Using Parameters", params[2]);
                    }

                    if (params.length > 4) {
                        urlConnection.setRequestProperty("Authorization", "Basic " + params[4]);

                        Log.i("Using Basic Auth", "");
                    }

                    if (params.length > 3 && params[3] != null) { // params[3] == HTTP Body - String
                        String httpBody = params[3];

                        urlConnection.setRequestProperty("Content-Type", "application/json");
                        urlConnection.setRequestProperty("Content-Length", httpBody.length() + "");

                        OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
                        wr.write(httpBody);
                        wr.flush();
                        wr.close();

                        Log.i("Using Body", httpBody);
                    }

                    urlConnection.connect();
                }

                try {
                    code = urlConnection.getResponseCode();

                    BufferedReader bufferedReader = null;
                    InputStream inputStream = null;

                    try {
                        inputStream = urlConnection.getInputStream();
                    } catch(IOException exception) {
                        inputStream = urlConnection.getErrorStream();
                    }

                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder stringBuilder = new StringBuilder();

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    bufferedReader.close();

                    try {
                        JSONArray result = new JSONArray(stringBuilder.toString());

                        JSONObject statusCode = new JSONObject();
                        statusCode.put("statusCode", code + "");

                        Log.d("Responded With", code + "");

                        result.put(statusCode);

                        return result;
                    } catch (JSONException ex) {
                        try {
                            JSONArray result = new JSONArray();

                            JSONObject object = new JSONObject(stringBuilder.toString());
                            result.put(object);

                            JSONObject statusCode = new JSONObject();
                            statusCode.put("statusCode", code);
                            Log.d("Responded With", code + "");

                            result.put(statusCode);

                            return result;
                        } catch (JSONException exc) {
                            JSONArray result = new JSONArray();

                            JSONObject object = new JSONObject();
                            object.put("result", stringBuilder.toString());

                            result.put(object);

                            JSONObject statusCode = new JSONObject();
                            statusCode.put("statusCode", code);
                            Log.d("Responded With", code + "");

                            result.put(statusCode);

                            return result;
                        }
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);

                JSONArray result = new JSONArray();

                JSONObject statusCode = new JSONObject();

                try {
                    statusCode.put("statusCode", code);
                    Log.d("Responded With", code + "");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                result.put(statusCode);

                return result;
            }
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            super.onPostExecute(result);

            if(this.taskListener != null) {
                try {
                    this.taskListener.postExecute(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

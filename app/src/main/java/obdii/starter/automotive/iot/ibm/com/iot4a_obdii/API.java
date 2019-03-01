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

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import android.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device.Protocol;

public class API {
    private static String defaultAppURL = "http://anfi.dhcp.hakozaki.ibm.com:6001";
    private static String defaultAppUser = "starter";
    private static String defaultAppPassword = "Starter4Iot";

    public static String connectedAppURL = defaultAppURL;
    public static String connectedAppUser = defaultAppUser;
    public static String connectedAppPassword = defaultAppPassword;

    public static AsyncTask getDeviceAccessInfo(String uuid, Protocol protocol, TaskListener listener){
        final API.doRequest request = new API.doRequest(listener);
        String url = connectedAppURL + "/user/device/" + uuid + "?protocol=" + protocol.name().toLowerCase();
        return request.execute(url, "GET", null, connectedAppUser, connectedAppPassword);
    }
    public static AsyncTask registerDevice(String uuid, Protocol protocol, TaskListener listener){
        final API.doRequest request = new API.doRequest(listener);
        String url = connectedAppURL + "/user/device/" + uuid + "?protocol=" + protocol.name().toLowerCase();
        return request.execute(url, "POST", null, connectedAppUser, connectedAppPassword);
    }
    public static AsyncTask checkMQTTAvailable(TaskListener listener){
        final API.doRequest request = new API.doRequest(listener);
        String url = connectedAppURL + "/user/capability/device";
        return request.execute(url, "GET", null, connectedAppUser, connectedAppPassword);
    }

    public static void useDefault(){
        connectedAppURL = defaultAppURL;
        connectedAppUser = defaultAppUser;
        connectedAppPassword = defaultAppPassword;
    }
    public static void doInitialize(String appUrl, String appUsername, String appPassword){
        connectedAppURL = appUrl;
        connectedAppUser = appUsername;
        connectedAppPassword = appPassword;
    }
    public static class doRequest extends AsyncTask<String, Void, Response> {
        private final TaskListener taskListener;

        public doRequest(final TaskListener listener) {
            this.taskListener = listener;
        }

        @Override
        protected Response doInBackground(String... params) {
            /*      params[0] == url (String)
                    params[1] == request method (String e.g. "GET")
                    params[2] == body (JsonObject converted to String)
                    params[3] == user
                    params[4] == password
            */

            int code = 0;

            try {
                URL url = new URL(params[0]);   // params[0] == URL - String
                String requestType = params[1]; // params[1] == Request Method - String e.g. "GET"
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                Log.i(requestType + " Request", params[0]);

                urlConnection.setRequestProperty("Accept", "application/json");

                urlConnection.setRequestMethod(requestType);

                // params[3] == user, params[4] == password
                if(params.length >= 5 && params[3] != null && params[3].length() > 0 && params[4] != null && params[4].length() > 0){
                    Log.i("Using Basic Auth", "");
                    urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((params[3]+":"+params[4]).getBytes("UTF-8"), Base64.NO_WRAP));
                }

                if (requestType.equals("POST") || requestType.equals("PUT") || requestType.equals("GET")) {
                    if (!requestType.equals("GET")) {
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);
                    }

                    if (params.length >= 3 && params[2] != null) { // params[3] == HTTP Body - String
                        String httpBody = params[2];

                        urlConnection.setRequestProperty("Accept", "application/json");
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
                    Log.d("Responded With", code + "");

                    BufferedReader bufferedReader = null;
                    InputStream inputStream = null;

                    try {
                        inputStream = urlConnection.getInputStream();
                    } catch (IOException exception) {
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
                        JsonObject result = new Gson().fromJson(stringBuilder.toString(), JsonObject.class);
                        Response response = new Response(code, result);

                        return response;
                    } catch (JsonSyntaxException ex) {
                        try {
                            JsonObject result = new Gson().fromJson(stringBuilder.toString(), JsonObject.class);
                            Response response = new Response(code, result);
                            return response;
                        } catch (JsonSyntaxException exc) {
                            JsonObject result = new JsonObject();
                            result.addProperty("result", stringBuilder.toString());

                            Response response = new Response(code, result);
                            return response;
                        }
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                Response response = new Response(code, null);
                return response;
            }
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);

            if (this.taskListener != null) {
                try {
                    this.taskListener.postExecute(response);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    static {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            TrustManager[] tm = { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
            } };
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, tm, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public interface TaskListener {
        void postExecute(Response result) throws JsonSyntaxException;
    }

    public static class Response{
        private int statusCode;
        private JsonObject body;

        public Response(int statusCode, JsonObject body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public JsonObject getBody() {
            return body;
        }
    }
}

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
import android.text.TextUtils;
import android.util.Base64;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device.Protocol;

public class API {
    private static String defaultAppURL = "https://iota-starter-server-fleetmgmt.mybluemix.net";
    private static String defaultAppUser = "starter";
    private static String defaultAppPassword = "Starter4Iot";

    public static String connectedAppURL = defaultAppURL;
    public static String connectedAppUser = defaultAppUser;
    public static String connectedAppPassword = defaultAppPassword;

    public static AsyncTask getDeviceAccessInfo(String uuid, Protocol protocol, TaskListener listener){
        String url = connectedAppURL + "/user/device/" + uuid + "?protocol=" + protocol.name().toLowerCase();
        doRequest request = new doRequest.Builder(url, "GET", listener)
                .credentials(connectedAppUser, connectedAppPassword)
                .build();
        return request.execute();
    }
    public static AsyncTask registerDevice(String uuid, Protocol protocol, TaskListener listener){
        String url = connectedAppURL + "/user/device/" + uuid;
        doRequest request = new doRequest.Builder(url, "POST", listener)
                .credentials(connectedAppUser, connectedAppPassword)
                .addQueryString("protocol", protocol.name().toLowerCase())
                .build();
        return request.execute();
    }
    public static AsyncTask checkMQTTAvailable(TaskListener listener){
        String url = connectedAppURL + "/user/capability/device";
        doRequest request = new doRequest.Builder(url, "GET", listener)
                .credentials(connectedAppUser, connectedAppPassword)
                .build();
        return request.execute();
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

    public static String getDefaultAppURL() {
        return defaultAppURL;
    }

    public static String getDefaultAppUser() {
        return defaultAppUser;
    }

    public static String getDefaultAppPassword() {
        return defaultAppPassword;
    }

    public static String getConnectedAppURL() {
        return connectedAppURL;
    }

    public static String getConnectedAppUser() {
        return connectedAppUser;
    }

    public static String getConnectedAppPassword() {
        return connectedAppPassword;
    }

    public static class doRequest extends AsyncTask<Void, Void, Response> {
        private final String strurl;
        private final String method;
        private final TaskListener taskListener;
        private String user;
        private String password;
        private final Map<String, String> headers = new HashMap<>();
        private String body;

        public static class Builder{
            private String strurl;
            private final String method;
            private final TaskListener listener;
            private String user;
            private String password;
            private final Map<String, String> headers = new HashMap<>();
            private final Map<String, String> qs = new HashMap<>();
            private String body;
            public Builder(String url, String method, TaskListener listener){
                this.strurl = url;
                this.method = method;
                this.listener = listener;
            }
            public Builder credentials(String user, String password){
                this.user = user;
                this.password = password;
                return this;
            }
            public Builder addHeader(String name, String value){
                if(name == null){
                    throw new IllegalArgumentException("Invalid header name: " + name);
                }
                headers.put(name, value);
                return this;
            }
            public Builder addQueryString(String name, String value){
                if(name == null){
                    throw new IllegalArgumentException("Invalid query string: null = " + value);
                }
                qs.put(name, value);
                return this;
            }
            public Builder body(String body){
                this.body = body;
                return this;
            }

            public doRequest build(){
                if(!qs.isEmpty()){
                    List<String> list = new ArrayList<>();
                    for(Map.Entry entry : qs.entrySet()){
                        list.add(entry.getKey() + "=" + entry.getValue());
                    }
                    strurl = strurl + (strurl.indexOf("?") > 0 ? "&" : "?") + TextUtils.join("&", list);
                }

                return new doRequest(this);
            }
        }
        private doRequest(Builder builder) {
            this.strurl = builder.strurl;
            this.method = builder.method;
            this.taskListener = builder.listener;
            this.user = builder.user;
            this.password = builder.password;
            this.headers.putAll(builder.headers);
            this.body = builder.body;
        }

        @Override
        protected Response doInBackground(Void... params){
            int code = 500;
            if(strurl == null || method == null){
                JsonObject message = new JsonObject();
                message.addProperty("error", "URL or method is not specified.");
                return new Response(code, message);
            }
            HttpURLConnection urlConnection = null;

            try {
                Log.i(method + " Request", strurl);
                URL url = new URL(strurl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);

                if(user != null && password != null){
                    Log.i("Using Basic Auth", String.format("user(%s)", user));
                    urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((user+":"+password).getBytes("UTF-8"), Base64.NO_WRAP));
                }

                urlConnection.setRequestProperty("Accept", "application/json");
                if(headers != null && !headers.isEmpty()){
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        urlConnection.setRequestProperty(header.getKey(), header.getValue());
                    }
                }
                if (method.equals("POST") || method.equals("PUT") || method.equals("GET")) {
                    if (!method.equals("GET")) {
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);
                    }

                    if (body != null) {
                        if(!headers.containsKey("Content-Type")){
                            urlConnection.setRequestProperty("Content-Type", "application/json");
                        }
                        urlConnection.setRequestProperty("Content-Length", body.length() + "");
                        Log.i("Using Body", body);
                        try(OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8")){
                            wr.write(body);
                            wr.flush();
                        }
                    }

                    urlConnection.connect();
                }

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
                    JsonObject result = new JsonObject();
                    result.addProperty("result", stringBuilder.toString());

                    Response response = new Response(code, result);
                    return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
                JsonObject result = new JsonObject();
                result.addProperty("result", "Unknown error");
                Response response = new Response(code, result);
                return response;
            }finally{
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);

            if (this.taskListener != null) {
                this.taskListener.postExecute(response);
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
        void postExecute(Response result);
    }

    public static class Response{
        private final int statusCode;
        private final JsonObject body;

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

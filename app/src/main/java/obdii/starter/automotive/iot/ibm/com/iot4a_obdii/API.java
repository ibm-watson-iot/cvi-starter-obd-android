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
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
                    ((HttpsURLConnection)urlConnection).setSSLSocketFactory(new SSLSocketFactory() {
                        private SSLSocketFactory def = SSLContext.getDefault().getSocketFactory();
                        @Override
                        public String[] getDefaultCipherSuites() {
                            return def.getDefaultCipherSuites();
                        }

                        @Override
                        public String[] getSupportedCipherSuites() {
                            return def.getSupportedCipherSuites();
                        }

                        @Override
                        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
                            return setTLSv12(def.createSocket(s, host, port, autoClose));
                        }

                        @Override
                        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                            return setTLSv12(def.createSocket(host, port));
                        }

                        @Override
                        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
                            return setTLSv12(def.createSocket(host, port, localHost, localPort));
                        }

                        @Override
                        public Socket createSocket(InetAddress host, int port) throws IOException {
                            return setTLSv12(def.createSocket(host, port));
                        }

                        @Override
                        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                            return setTLSv12(def.createSocket(address, port, localAddress, localPort));
                        }

                        private Socket setTLSv12(Socket socket){
                            if(socket != null && socket instanceof SSLSocket){
                                ((SSLSocket)socket).setEnabledProtocols(new String[]{"TLSv1.2"});
                            }
                            return socket;
                        }
                    });
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

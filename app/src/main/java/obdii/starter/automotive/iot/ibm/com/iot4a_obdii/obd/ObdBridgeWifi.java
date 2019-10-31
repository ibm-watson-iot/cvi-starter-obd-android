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

import android.os.AsyncTask;

import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.Home;

/*
 * OBD Bridge via Wifi (not completed)
 */
public class ObdBridgeWifi extends ObdBridge {

    private static final String deviceId = UUID.randomUUID().toString();
    private final Socket[] sockets = new Socket[1];


    public ObdBridgeWifi(final Home home) {
        super(home);
    }

    @Override
    public void clean() {
        super.clean();
        closeSocket();
    }

    @Override
    protected String getDeviceUniqueKey() {
        return deviceId;
    }

    private final Object taskLock = new Object();

    private void doSyncTask(final Runnable task) {
        synchronized (taskLock) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    synchronized (taskLock) {
                        try {
                            task.run();
                        } finally {
                            taskLock.notifyAll();
                        }
                    }
                    return null;
                }
            }.execute();
            try {
                taskLock.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean connectSocket(final String address, final int port, final int timeout_ms, final ObdProtocols obd_protocol) {
        doSyncTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sockets[0] != null && sockets[0].isConnected()) {
                        sockets[0].close();
                    }
                    sockets[0] = new Socket(address, port);
                    socketConnected(timeout_ms, obd_protocol);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return sockets[0] != null;
    }

    public synchronized void closeSocket() {
        if (sockets[0] != null) {
            doSyncTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (sockets[0] != null && sockets[0].isConnected()) {
                            sockets[0].close();
                        }
                        sockets[0] = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public boolean isConnected() {
        return (sockets[0] != null && sockets[0].isConnected());
    }

    @Override
    protected OutputStream getOutputStream() {
        // to be called by non UI thread
        if (sockets[0] != null) {
            try {
                return sockets[0].getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected InputStream getInputStream() {
        // to be called by non UI thread
        if (sockets[0] != null) {
            try {
                return sockets[0].getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
}

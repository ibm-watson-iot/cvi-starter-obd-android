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
package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.obd.EventDataGenerator;

public abstract class AbstractVehicleDevice implements IVehicleDevice {
    private static final int UPLOAD_DELAY_MS = 500;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> uploadHandler = null;

    protected AccessInfo accessInfo;

    AbstractVehicleDevice(AccessInfo accessInfo){
        this.accessInfo = accessInfo;
    }
    public static AccessInfo createAccessInfo(String endpoint, String tenant_id, String vendor, String mo_id, String user, String password){
        return new AccessInfo(endpoint, tenant_id, vendor, mo_id, user, password);
    }

    public synchronized void startPublishing(final EventDataGenerator eventGenerator, final int uploadIntervalMS, final NotificationHandler notificationHandler) {
        stopPublishing();

        uploadHandler = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                final String event = eventGenerator.generateData();
                if (event != null) {
                    try {
                        final boolean success = publishEvent(event, notificationHandler);
                        if (success) {
                            System.out.println("DATA SUCCESSFULLY POSTED......");
                            Log.d("SEND_CAR_PROBE", event.toString());
                            notificationHandler.notifyPostResult(success, null);
                        }else{
                            stopPublishing();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        stopPublishing();
                    }

                }

            }
        }, UPLOAD_DELAY_MS, uploadIntervalMS, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stopPublishing() {
        if (uploadHandler != null) {
            uploadHandler.cancel(true);
            uploadHandler = null;
        }
    }

    @Override
    public void setAccessInfo(AccessInfo accessInfo) {
        this.accessInfo = accessInfo;
    }

    @Override
    public AccessInfo getAccessInfo() {
        return this.accessInfo;
    }

    @Override
    public void clean() {
        stopPublishing();
        scheduler.shutdown();
    }

    abstract boolean publishEvent(String event, NotificationHandler notificationHandler);
}

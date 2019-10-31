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

public enum Protocol {
    HTTP(new EventFormat[]{EventFormat.JSON}){
        @Override
        public IVehicleDevice createDevice(AccessInfo accessInfo) {
            return new VdhHttpDevice(accessInfo);
        }
    },
    MQTT(new EventFormat[]{EventFormat.CSV}) {
        @Override
        public IVehicleDevice createDevice(AccessInfo accessInfo) {
            return new IoTPlatformDevice(accessInfo);
        }
    };

    private final EventFormat[] supportedEventFormat;
    private Protocol(EventFormat[] supportedEventFormat){
        if(supportedEventFormat == null || supportedEventFormat.length <= 0){
            throw new IllegalArgumentException();
        }
        this.supportedEventFormat = supportedEventFormat;
    };
    public abstract IVehicleDevice createDevice(AccessInfo accessInfo);
    public EventFormat[] getSupportedFormats(){
        return this.supportedEventFormat;
    };

    public String prefName(String prefName){
        return this.name() + "_" + prefName;
    }
    public EventFormat defaultFormat(){
        return this.supportedEventFormat[0];
    }
}

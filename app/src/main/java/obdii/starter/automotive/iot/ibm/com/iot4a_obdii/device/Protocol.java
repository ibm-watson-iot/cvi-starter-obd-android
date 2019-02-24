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

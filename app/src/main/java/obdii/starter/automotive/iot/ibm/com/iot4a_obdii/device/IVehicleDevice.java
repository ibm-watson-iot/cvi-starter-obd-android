package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import com.google.gson.JsonObject;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.DeviceNotConnectedException;

public interface IVehicleDevice {
    public void setAccessInfo(AccessInfo accessInfo);
    public AccessInfo getAccessInfo();
    public boolean hasValidAccessInfo();

    public void startPublishing(final EventDataGenerator eventGenerator, final int uploadIntervalMS, final NotificationHandler notificationHandler);
    public void stopPublishing();
    public void clean();
}

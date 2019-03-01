package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.API;

public interface NotificationHandler {
    void notifyPostResult(boolean success, Notification notification);
}

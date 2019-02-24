package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

public interface EventDataGenerator {
    String generateData();
    void notifyPostResult(boolean success, String event);
}

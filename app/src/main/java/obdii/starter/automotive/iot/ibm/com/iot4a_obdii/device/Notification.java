package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import java.util.Collections;
import java.util.Map;

public class Notification {
    enum Type{AffectedEvent, NotifyMessage, Error}
    private final Type type;
    private final String message;
    private final Map<String, String> params;
    public Notification(Type type, String message, Map<String, String> params){
        this.type = type;
        this.message = message;
        this.params = params;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
    }
}

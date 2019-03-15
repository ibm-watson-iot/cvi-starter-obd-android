package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import java.util.HashMap;
import java.util.Map;

public class AccessInfo {
    public enum ParamName {ENDPOINT, VENDOR, MO_ID, USERNAME, PASSWORD, TENANT_ID, USER_AGENT};

    private Map<ParamName, String> map = new HashMap<>();
    AccessInfo(String endpoint, String tenant_id, String vendor, String mo_id, String username, String password){
        map.put(ParamName.ENDPOINT, endpoint);
        map.put(ParamName.TENANT_ID, tenant_id);
        map.put(ParamName.VENDOR, vendor);
        map.put(ParamName.MO_ID, mo_id);
        map.put(ParamName.USERNAME, username);
        map.put(ParamName.PASSWORD, password);
    }
    public void put(ParamName key, String value){
        map.put(key, value);
    }

    public String get(ParamName key) {
        String value = map.get(key);
        if(value == null){
            value = "";
        }
        return value;
    }
}

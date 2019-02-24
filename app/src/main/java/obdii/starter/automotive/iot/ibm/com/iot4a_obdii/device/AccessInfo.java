package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import java.util.HashMap;
import java.util.Map;

public class AccessInfo {
    public enum ParamName implements IAccessInfoParamName {ENDPOINT, VENDOR, MO_ID, USERNAME, PASSWORD};

    private Map<IAccessInfoParamName, String> map = new HashMap<>();
    AccessInfo(String endpoint, String vendor, String mo_id, String username, String password){
        map.put(ParamName.ENDPOINT, endpoint);
        map.put(ParamName.VENDOR, vendor);
        map.put(ParamName.MO_ID, mo_id);
        map.put(ParamName.USERNAME, username);
        map.put(ParamName.PASSWORD, password);
    }

    public String get(IAccessInfoParamName key) {
        String value = map.get(key);
        if(value == null){
            value = "";
        }
        return value;
    }
}

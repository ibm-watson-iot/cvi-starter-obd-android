package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.API;

public class VdhHttpDevice extends AbstractVehicleDevice{
    VdhHttpDevice(AccessInfo accessInfo){
        super(accessInfo);
    }

    @Override
    boolean publishEvent(String event) throws Exception {
        final String op = "sync";
        API.doRequest request = new API.doRequest(new API.TaskListener() {
            @Override
            public void postExecute(API.Response result) throws JsonSyntaxException {
                if(op.equals("sync") && result.getStatusCode() == 200){
                    JsonObject body = result.getBody();
                    if(body != null){
                        Log.d("Publish Event", body.toString());
                    }
                }
            }
        });
        String url = accessInfo.get(AccessInfo.ParamName.ENDPOINT) + "?op=" + op;
        String user = accessInfo.get(AccessInfo.ParamName.USERNAME);
        String password = accessInfo.get(AccessInfo.ParamName.PASSWORD);
        request.execute(url, "POST", event, user, password);
        return true;
    }

    @Override
    public void setAccessInfo(AccessInfo accessInfo) {
        this.accessInfo = accessInfo;
    }

    @Override
    public boolean hasValidAccessInfo() {
        if(accessInfo == null){
            return false;
        }
        String endpoint = accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        String mo_id = accessInfo.get(AccessInfo.ParamName.MO_ID);
        return endpoint != null && endpoint != "" && mo_id != null && mo_id != "";
    }
}

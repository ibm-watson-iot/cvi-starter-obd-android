package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import android.bluetooth.BluetoothClass;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.API;

public class VdhHttpDevice extends AbstractVehicleDevice{
    private static String defaultVdhUserAgent = "IBM IoT Connected Vehicle Insights Client";

    VdhHttpDevice(AccessInfo accessInfo){
        super(accessInfo);
    }

    @Override
    boolean publishEvent(String event, final NotificationHandler notificationHandler) {
        final String op = "sync";
        String url = accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        String tenantId = accessInfo.get(AccessInfo.ParamName.TENANT_ID);
        String userAgent = accessInfo.get(AccessInfo.ParamName.USER_AGENT);
        if(userAgent == null){
            userAgent = defaultVdhUserAgent;
        }
        String user = accessInfo.get(AccessInfo.ParamName.USERNAME);
        String password = accessInfo.get(AccessInfo.ParamName.PASSWORD);
        API.doRequest request = new API.doRequest.Builder(url, "POST", new API.TaskListener() {
            @Override
            public void postExecute(API.Response result) {
                if(op.equals("sync") && result.getStatusCode() == 200){
                    JsonObject body = result.getBody();
                    if(body != null){
                        Log.d("Publish Event", body.toString());
                        notificationHandler.notifyPostResult(true, new Notification(Notification.Type.NotifyMessage, body.toString(), null));
                    }
                }else if(result.getStatusCode() == 500){
                    notificationHandler.notifyPostResult(false, new Notification(Notification.Type.Error, "Device cannot connect to Connected Vehicle Insights", null));
                }
            }
        }).credentials(user, password)
                .addHeader("User-Agent", userAgent)
                .addQueryString("op", op)
                .addQueryString("tenant_id", tenantId)
                .body(event).build();
        request.execute();
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

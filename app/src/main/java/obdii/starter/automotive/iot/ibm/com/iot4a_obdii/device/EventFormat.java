package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public enum EventFormat{
    CSV{
        @Override
        public String format(JsonObject data) {
            String timestamp = DATE_FORMAT.format(new Date());
            List<String> list = new ArrayList<>();
            list.add("SEND_CARPROBE");
            list.add("sync");
            list.add(data.get("mo_id").getAsString());
            list.add(data.get("lat").getAsString());
            list.add(data.get("lng").getAsString());
            list.add(data.get("altitude").getAsString()); // Altitude
            list.add(data.get("heading").getAsString());
            list.add(timestamp);
            list.add(data.get("trip_id").getAsString());
            list.add(data.get("speed").getAsString());
            list.add(""); // confidence
            list.add(""); // map_vendor_name
            list.add(""); // map_version
//                    JsonElement props = data.get("props");
//                    if(props != null && props.isJsonObject()){
//                        list.add(props.getAsJsonObject().get("fuelLevel").getAsString());
//                    }
            String csv = TextUtils.join(",", list);
            return csv;
        }
    },
    JSON {
        @Override
        public String format(JsonObject data) {
            String timestamp = DATE_FORMAT.format(new Date());
            data.addProperty("timestamp", timestamp);
            double latitude = data.get("lat").getAsDouble();
            data.remove("lat");
            data.addProperty("latitude", latitude);
            double longitude = data.get("lng").getAsDouble();
            data.remove("lng");
            data.addProperty("longitude", longitude);
            return data.toString();
        }
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    public abstract String format(JsonObject data);
}

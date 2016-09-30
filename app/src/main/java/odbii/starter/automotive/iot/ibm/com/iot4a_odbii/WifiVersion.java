package odbii.starter.automotive.iot.ibm.com.iot4a_odbii;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by iliadm on 2016-09-30.
 */
public class WifiVersion {
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);
//
//        int permissionGiven = 0;
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            permissionGiven = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);
//        }
//
//        if (permissionGiven != PackageManager.PERMISSION_GRANTED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE},
//                        WIFI_PERMISSIONS_CODE);
//            }
//            return;
//        } else {
//            Log.i("WIFI Permissions", "Already Granted.");
//            permissionsGranted();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case WIFI_PERMISSIONS_CODE:
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    permissionsGranted();
//                } else {
//                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
//                }
//                break;
//            default:
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//
//    public void permissionsGranted() {
//        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        if (wifiManager.isWifiEnabled()) {
//            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//            if (wifiInfo != null) {
//                NetworkInfo.DetailedState detailedState = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
//                if (detailedState == NetworkInfo.DetailedState.CONNECTED || detailedState == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
//                    Log.i("Network Info", wifiInfo.getSSID() + " " + wifiInfo.getIpAddress());
//
//                    if (wifiInfo.getSSID().equals("\"IBMVISITOR\"")) {
//                        Toast toast = Toast.makeText(getApplicationContext(), wifiInfo.getSSID(), Toast.LENGTH_SHORT);
//                        toast.show();
//                    } else {
//                        Toast toast = Toast.makeText(getApplicationContext(), "Please Connect to the \"WiFi_OBDII\" network!", Toast.LENGTH_SHORT);
//                        toast.show();
//                    }
//                }
//            }
//        } else {
//            Toast toast = Toast.makeText(getApplicationContext(), "Please Turn on your WIFI!", Toast.LENGTH_SHORT);
//            toast.show();
//        }
//    }
}

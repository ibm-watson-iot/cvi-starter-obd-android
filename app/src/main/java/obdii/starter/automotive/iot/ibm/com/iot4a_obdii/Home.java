package obdii.starter.automotive.iot.ibm.com.iot4a_obdii;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Home extends AppCompatActivity {
    final private int BT_PERMISSIONS_CODE = 000;

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothDevice userDevice;

    Set<BluetoothDevice> pairedDevicesSet;
    final ArrayList<String> deviceNames = new ArrayList<>();
    final ArrayList<String> deviceAdresses = new ArrayList<>();
    String userDeviceAddress;

    private static final String TAG = BluetoothManager.class.getName();

    TextView fuelLevelValue;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setScaleX(0.5f);
        progressBar.setScaleY(0.5f);

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(progressBar);

        fuelLevelValue = (TextView) findViewById(R.id.fuelLevelValue);

        new API(getApplicationContext());

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth!", Toast.LENGTH_SHORT).show();
        } else {
            int permissionGiven = 0;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                permissionGiven = checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN);
            }

            if (permissionGiven != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[] {Manifest.permission.BLUETOOTH_ADMIN},
                            BT_PERMISSIONS_CODE);
                }
                return;
            } else {
                Log.i("Bluetooth Permissions", "Already Granted.");
                permissionsGranted();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case BT_PERMISSIONS_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void permissionsGranted() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        } else {
            pairedDevicesSet = bluetoothAdapter.getBondedDevices();

            if (pairedDevicesSet.size() > 0) {
                for (BluetoothDevice device : pairedDevicesSet)
                {
                    deviceNames.add(device.getName());
                    deviceAdresses.add(device.getAddress());
                }

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, deviceNames.toArray(new String[deviceNames.size()]));

                int selectedDevice = -1;
                for (int i = 0; i < deviceNames.size(); i++) {
                    if (deviceNames.get(i).toLowerCase().contains("obd")) {
                        selectedDevice = i;
                    }
                }

                alertDialog
                           .setCancelable(false)
                           .setSingleChoiceItems(adapter, selectedDevice, null)
                           .setTitle("Please Choose the OBDII Bluetooth Device")
                           .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which)
                               {
                                   dialog.dismiss();
                                   int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                   userDeviceAddress = deviceAdresses.get(position);

//                                   connectSocket(userDeviceAddress);
                                   checkDeviceRegistry();
                               }
                           })
                           .show();
            } else {
                Toast.makeText(getApplicationContext(), "Please pair with your OBDII device and restart the application!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void checkDeviceRegistry() {
        String url = API.platformAPI + "/device/types/" + API.typeId + "/devices/" + userDeviceAddress.replaceAll(":", "-");

        getSupportActionBar().setTitle("Checking if Device is Registered");
        progressBar.setVisibility(View.VISIBLE);

        try {
            API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                @Override
                public void postExecute(JSONArray result) throws JSONException {
                    JSONObject serverResponse = result.getJSONObject(result.length() - 1);
                    int statusCode = serverResponse.getInt("statusCode");

                    result.remove(result.length() - 1);

                    switch (statusCode) {
                        case 200:
                            Log.d("Register Device", result.toString());
                            Log.d("Register Device", "Already Registered");

                            break;
                        case 404:
                            Log.d("Register Device", "Not Registered");
                            progressBar.setVisibility(View.GONE);

                            registerDevice();
                    }
                }
            });

            task.execute(url, "GET", null, null, API.credentialsBase64).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void registerDevice() {
        String url = API.addDevices;

        getSupportActionBar().setTitle("Registering Your Device");
        progressBar.setVisibility(View.VISIBLE);

        try {
            API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                @Override
                public void postExecute(JSONArray result) throws JSONException {
                    JSONObject serverResponse = result.getJSONObject(result.length() - 1);
                    int statusCode = serverResponse.getInt("statusCode");

                    result.remove(result.length() - 1);

                    Log.d("Register Device", result.toString());

                    progressBar.setVisibility(View.GONE);
                }
            });

            JSONObject bodyObject = new JSONObject();
            bodyObject
                    .put("typeId", API.typeId)
                    .put("deviceId", userDeviceAddress);

            task.execute(url, "POST", null, null, API.credentialsBase64).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void connectSocket(String userDeviceAddress) {
        final TextView fuelLevelValue = (TextView) findViewById(R.id.fuelLevelValue);
        final TextView speedValue = (TextView) findViewById(R.id.speedValue);
        final TextView engineCoolantValue = (TextView) findViewById(R.id.engineCoolantValue);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(userDeviceAddress);
        userDevice = device;

        UUID uuid = UUID.fromString(API.getUUID());

        BluetoothSocket socket = null;

        Log.d(TAG, "Starting Bluetooth connection..");

        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (Exception e) {
            Log.e("Bluetooth Connection", "Socket couldn't be created");
            e.printStackTrace();
        }

        try {
            socket.connect();
            Log.i("Bluetooth Connection", "CONNECTED");
        } catch (IOException e) {
            Log.e("Bluetooth Connection", e.getMessage());

            try {
                Log.i("Bluetooth Connection", "Using fallback method");

                socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device, 1);
                socket.connect();

                Log.i("Bluetooth Connection", "CONNECTED");
            }
            catch (Exception e2) {
                Log.e("Bluetooth Connection", "Couldn't establish connection");
            }
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                SpeedCommand speedCommand = new SpeedCommand();
                speedCommand.run(socket.getInputStream(), socket.getOutputStream());

                Log.d("Speed", speedCommand.getFormattedResult());
                speedValue.setText(speedCommand.getFormattedResult());

                FuelLevelCommand fuelLevelCommand = new FuelLevelCommand();
                fuelLevelCommand.run(socket.getInputStream(), socket.getOutputStream());

                Log.d("Fuel Level", fuelLevelCommand.getFormattedResult());
                fuelLevelValue.setText(fuelLevelCommand.getFormattedResult());

                EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = new EngineCoolantTemperatureCommand();
                engineCoolantTemperatureCommand.run(socket.getInputStream(), socket.getOutputStream());

                Log.d("Engine Coolant", engineCoolantTemperatureCommand.getFormattedResult());
                engineCoolantValue.setText(engineCoolantTemperatureCommand.getFormattedResult());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
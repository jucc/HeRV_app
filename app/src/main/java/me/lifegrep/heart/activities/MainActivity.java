package me.lifegrep.heart.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import me.lifegrep.heart.R;
import me.lifegrep.heart.adapters.BleServicesAdapter;
import me.lifegrep.heart.sensor.BleSensor;
import me.lifegrep.heart.sensor.BleSensors;
import me.lifegrep.heart.services.BleService;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    Switch heartSwitch;
    TextView heartbeat;

    private BluetoothAdapter blueAdapter;
    private BleService bleService;
    private BleServicesAdapter.OnServiceItemClickListener serviceListener;

    private BleSensor<?> heartRateSensor;
    private String deviceName;
    private String deviceAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        heartSwitch = (Switch) findViewById(R.id.sw_heart);
        heartbeat = (TextView) findViewById(R.id.tv_heartbeat);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            heartSwitch.setEnabled(false);
            heartbeat.setText(R.string.text_monitornobluetooth);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Listener registered for fab_start
    public void startActivity(View view) {
        Intent intent_activityselection = new Intent(this, ActivitySelectionActivity.class);
        startActivity(intent_activityselection);
    }


    // Listener registered for sw_heart toggle
    public void toggleHeartMonitor(View view) {
        if (heartSwitch.isChecked()) {

            // enable bluetooth if it is not already enabled

            int REQUEST_ENABLE_BT = 1;
            final BluetoothManager blueManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            blueAdapter = blueManager.getAdapter();
            if (blueAdapter == null) {
                heartbeat.setText(R.string.text_monitornobluetooth);
            } else {
                heartbeat.setText(R.string.text_monitorconnecting);
                if (!blueAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    //TODO check for the case where the user selects not to enable BT
                }
            }

            // find the device
            /**
             * TODO URG recreate the activity as stratActivityForResult and read the device
             Intent intent_scan = new Intent(this, DeviceScanActivity.class);
             startActivity(intent_scan);
             **/
            //            final Intent intent = new Intent(this, DeviceServicesActivity.class);
//            intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_NAME, this.deviceName);
//            intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_ADDRESS, this.deviceAddress);
//            startActivity(intent);
            this.deviceName = "Polar H7";
            this.deviceAddress = "00:22:D0:85:88:8E";

            final Intent bleServiceIntent = new Intent(this, BleService.class);
            bindService(bleServiceIntent, serviceConnection, BIND_AUTO_CREATE);
            Toast.makeText(this, "service connected", Toast.LENGTH_SHORT).show();

        } else {

            if (bleService != null) {
                //TODO encerrar o servico em vez de somente desconectar
                bleService.disconnect();
            }
            heartbeat.setText(R.string.text_monitoroff);
        }
    }


    /*
    ------------------------------------------------------------------------------------------
        BLUETOOTH LE SERVICE
        - connects to device
        - receives broadcasts to show HR on screen
    ------------------------------------------------------------------------------------------
    */

    // manage service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // handles events fired by the service.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                heartbeat.setText("Connecting...");
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                heartbeat.setText("Disconnected");
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                heartbeat.setText("Connected...");
                // Show all the supported services and characteristics on the user interface.
                enableHeartRateSensor();
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                heartbeat.setText(intent.getStringExtra(BleService.EXTRA_TEXT));
            }
        }
    };


    private boolean enableHeartRateSensor() {

        BleServicesAdapter gattServiceAdapter = new BleServicesAdapter(this, this.bleService.getSupportedGattServices());
        final BluetoothGattCharacteristic characteristic = gattServiceAdapter.getHeartRateCharacteristic();
        Log.d(TAG,"characteristic: " + characteristic);
        final BleSensor<?> sensor = BleSensors.getSensor(characteristic.getService().getUuid().toString());

        if (this.heartRateSensor != null)
            this.bleService.enableSensor(this.heartRateSensor, false);

        if (sensor == null) {
            this.bleService.readCharacteristic(characteristic);
            return true;
        }

        if (sensor == this.heartRateSensor)
            return true;

        this.heartRateSensor = sensor;
        this.bleService.enableSensor(sensor, true);

        return true;
    }
}

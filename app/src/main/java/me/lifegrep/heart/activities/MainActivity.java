package me.lifegrep.heart.activities;

import android.bluetooth.BluetoothAdapter;
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
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import me.lifegrep.heart.R;
import me.lifegrep.heart.adapters.BleServicesAdapter;
import me.lifegrep.heart.sensor.BleSensor;
import me.lifegrep.heart.sensor.BleSensors;
import me.lifegrep.heart.services.BleService;
import me.lifegrep.heart.services.BluetoothLeService;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    Switch heartSwitch;
    TextView heartbeat;
    EditText userID;

    private BluetoothAdapter blueAdapter;
    private BluetoothLeService blueService;
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
        userID = (EditText) findViewById(R.id.et_userID);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            heartSwitch.setEnabled(false);
            heartbeat.setText(R.string.text_monitornobluetooth);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Activity on resume");
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (blueService != null) {
            heartSwitch.setChecked(true);
            final boolean result = blueService.connect(deviceAddress);
            Log.i(TAG, "Connect request result=" + result);
        } else {
            heartSwitch.setChecked(false);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Activity on pause");
        unregisterReceiver(gattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity on destroy");
        if (serviceConnection != null) {
            Log.i(TAG, "Unbinding from service on activity destroy");
            unbindService(serviceConnection);
        }
        blueService = null;
        //    serviceConnection = null;
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
            turnOnMonitor();
        } else {
            turnOffMonitor();
            heartbeat.setText(R.string.text_monitoroff);
        }
    }

    /**
     * Start the heart monitor service running in background
     */
    private void turnOnMonitor() {

        // if there is no bluetooth adapter yet, get one
        if (this.blueAdapter == null) {
            this.blueAdapter = getBluetoothAdapter();
            if (this.blueAdapter == null) {
                heartbeat.setText(R.string.text_monitornobluetooth);
            } else {
                heartbeat.setText(R.string.text_monitorconnecting);
            }
        }
        // find the device
        /**
         * TODO URG recreate the activity as startActivityForResult and read the device
         Intent intent_scan = new Intent(this, DeviceScanActivity.class);
         startActivity(intent_scan);
         **/
        //            final Intent intent = new Intent(this, DeviceServicesActivity.class);
//            intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_NAME, this.deviceName);
//            intent.putExtra(DeviceServicesActivity.EXTRAS_DEVICE_ADDRESS, this.deviceAddress);
//            startActivity(intent);


        this.deviceName = "Polar H7";
        this.deviceAddress = "00:22:D0:85:88:8E";

        final Intent blueServiceIntent = new Intent(this, BluetoothLeService.class);
        // startService(blueServiceIntent);
        //TODO binding to the freakin' service makes it die when unbound. How to solve?????
        bindService(blueServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Activity bound");
    }

    /**
     * Stop the heart monitor service running in background
     */
    private void turnOffMonitor() {
        if (blueService != null) {
            Log.i(TAG, "Stopping service");
            unbindService(serviceConnection);
            blueService.stopSelf();
        }
    }

    /**
     * Get a bluetooth adapter and request the user to enable bluetooth if it is not yet enabled
     *
     * @return adapter
     */
    private BluetoothAdapter getBluetoothAdapter() {
        int REQUEST_ENABLE_BT = 1;
        final BluetoothManager blueManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter blueAdapter = blueManager.getAdapter();
        // enable bluetooth if it is not already enabled
        if (!blueAdapter.isEnabled()) {
            Log.i(TAG, "Requesting bluetooth to turn on");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //TODO check for the case where the user selects not to enable BT
        }
        return blueAdapter;
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
            Log.i(TAG, "Service Connected");
            blueService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!blueService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            blueService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "ServiceConnection onServiceConnected");
            blueService = null;
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // handles events fired by the service.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                heartbeat.setText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "Services discovered");
                BluetoothGattCharacteristic hr =
                        blueService.findHeartRateCharacteristic(blueService.getSupportedGattServices());
                blueService.setCharacteristicNotification(hr, true);
            }
            // blatantly ignore any other actions because the activity doesn't really care
        }
    };
}

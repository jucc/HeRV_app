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
import android.content.SharedPreferences;
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
import me.lifegrep.heart.services.BluetoothLeService;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    Switch heartSwitch;
    TextView heartbeat;
    EditText userID;

    private BluetoothAdapter blueAdapter;
    private BluetoothLeService blueService;

    private String deviceName;
    private String deviceAddress;

    private final int REQUEST_SCAN = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");

        setContentView(R.layout.activity_main);
        heartSwitch = (Switch) findViewById(R.id.sw_heart);
        heartbeat = (TextView) findViewById(R.id.tv_heartbeat);
        userID = (EditText) findViewById(R.id.et_userID);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Activity on resume");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            heartSwitch.setEnabled(false);
            heartbeat.setText(R.string.text_monitornobluetooth);
            return;
        }
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String savedAddress = preferences.getString("deviceAddress", null);
        if (savedAddress != null)
        {
            deviceAddress = savedAddress;
            Log.i(TAG, "Saved device address recovered: " + savedAddress);
        }

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        turnOnMonitor();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // TODO how to check that they are running to unbind/unregister??
        //unregisterReceiver(gattUpdateReceiver);
        //unbindService(serviceConnection);
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putString("deviceAddress", this.deviceAddress);
        Log.i(TAG, "Activity on pause. Saving preferences.");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity on destroy");
        // TODO how to check that they are running to unbind/unregister??
        unregisterReceiver(gattUpdateReceiver);
        if (blueService != null) {
            unbindService(serviceConnection);
            blueService = null;
        }
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

        //this.deviceName = "Polar H7";
        //this.deviceAddress = "00:22:D0:85:88:8E";
        if (this.deviceAddress == null) {
            // find the device
            Intent intent_scan = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(intent_scan, REQUEST_SCAN);
        } else {
            startBlueService();
        }
        // wait for activity result to proceed with device address
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG, "Activity resulted");
        // Make sure this is the scan result
        if (requestCode == REQUEST_SCAN) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                this.deviceAddress = data.getStringExtra("deviceAddr");
                Log.i(TAG, "User selected device with address " + this.deviceAddress);
                startBlueService();
            }
        }
    }

    private void startBlueService() {
        final Intent blueServiceIntent = new Intent(this, BluetoothLeService.class);
        //TODO can I figure out if the service is already started? Does it make a difference?
        startService(blueServiceIntent);
        bindService(blueServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.i(TAG, "Activity bound");

        heartSwitch.setChecked(true);
        heartSwitch.setEnabled(false);
    }

    /**
     * Stop the heart monitor service running in background
     */
    private void turnOffMonitor() {
        Log.i(TAG, "Stopping service");
        unregisterReceiver(gattUpdateReceiver);
        blueService.stopSelf();
        unbindService(serviceConnection);
        blueService = null;
        heartSwitch.setChecked(false);
    }

    /*
    ------------------------------------------------------------------------------------------
        BLUETOOTH LE SERVICE
        - connects to device
        - receives broadcasts to show HR on screen
    ------------------------------------------------------------------------------------------
    */

    /**
     * Get a bluetooth adapter and request the user to enable bluetooth if it is not yet enabled
     */
    private BluetoothAdapter getBluetoothAdapter() {

        final BluetoothManager blueManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter blueAdapter = blueManager.getAdapter();

        // enable bluetooth if it is not already enabled
        if (!blueAdapter.isEnabled()) {
            int REQUEST_ENABLE_BT = 1;
            Log.i(TAG, "Requesting bluetooth to turn on");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //TODO check for the case where the user selects not to enable BT
        }
        return blueAdapter;
    }


    // manage service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "ServiceConnection ON");
            blueService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!blueService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (!blueService.connect(deviceAddress)) {
                Log.e(TAG, "Unable to connect to selected device");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "ServiceConnection OFF");
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
                        blueService.findHRMCharacteristic(blueService.getSupportedGattServices());
                blueService.setCharacteristicNotification(hr, true);
            }
            // blatantly ignore any other actions because the activity doesn't really care
        }
    };
}

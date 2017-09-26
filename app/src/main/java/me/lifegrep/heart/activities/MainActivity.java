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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import me.lifegrep.heart.R;
import me.lifegrep.heart.model.DailyActivity;
import me.lifegrep.heart.model.Event;
import me.lifegrep.heart.services.BluetoothLeService;
import me.lifegrep.heart.services.ScratchWriter;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    Switch heartSwitch;
    TextView heartbeat;
    EditText userID;
    Spinner posture, dailyActivities;
    FloatingActionButton button_start, button_stop;
    ArrayAdapter<CharSequence> postureAdapter, sitAdapter, lieDownAdapter, standAdapter;

    private BluetoothAdapter blueAdapter;
    private BluetoothLeService blueService;
    private boolean serviceConnected;

    private String deviceAddress; // "00:22:D0:85:88:8E";

    private final int REQUEST_SCAN = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");

        setContentView(R.layout.activity_main);
        heartSwitch = (Switch) findViewById(R.id.sw_heart);
        heartSwitch.setEnabled(false);
        heartbeat = (TextView) findViewById(R.id.tv_heartbeat);
        posture = (Spinner) findViewById(R.id.sp_posture);
        dailyActivities = (Spinner) findViewById(R.id.sp_activity);
        button_start = (FloatingActionButton) findViewById(R.id.ab_start);
        button_stop = (FloatingActionButton) findViewById(R.id.ab_stop);
        userID = (EditText) findViewById(R.id.et_userID);

        postureAdapter = ArrayAdapter.createFromResource(this, R.array.postures, android.R.layout.simple_spinner_item);
        lieDownAdapter = ArrayAdapter.createFromResource(this, R.array.activities_lying, android.R.layout.simple_spinner_item);
        sitAdapter     = ArrayAdapter.createFromResource(this, R.array.activities_sit, android.R.layout.simple_spinner_item);
        standAdapter   = ArrayAdapter.createFromResource(this, R.array.activities_stand, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        postureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lieDownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        standAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        posture.setAdapter(postureAdapter);

        posture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch(posture.getSelectedItem().toString().toLowerCase()) {
                    case "sitting":
                        dailyActivities.setAdapter(sitAdapter);
                        break;
                    case  "lying down":
                        dailyActivities.setAdapter(lieDownAdapter);
                        break;
                    case "standing":
                        dailyActivities.setAdapter(standAdapter);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity starting");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            heartSwitch.setEnabled(false);
            heartbeat.setText(R.string.text_monitornobluetooth);
            return;
        }

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String savedAddress = preferences.getString("deviceAddress", null);
        //TODO shared preferences does not seem to be be kept on destroy, only on pause
        if (savedAddress != null)
        {
            deviceAddress = savedAddress;
            Log.i(TAG, "Saved device address recovered: " + savedAddress);
        }
        startMonitoringService();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Activity on pause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Activity stopping");
        unregisterReceiver(gattUpdateReceiver);
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putString("deviceAddress", this.deviceAddress);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity on destroy");
        // TODO how to check that they are running to unbind/unregister??
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


    // Listener registered for ab_start
    public void startActivity(View view) {
        DailyActivity activity = new DailyActivity(Event.TP_START,
                                                   this.dailyActivities.getSelectedItem().toString(),
                                                   this.posture.getSelectedItem().toString(),
                                                   new Date());
        ScratchWriter writer = new ScratchWriter(this, "activities.csv");
        writer.saveData(activity.toCSV());
        button_stop.setEnabled(true);
        button_stop.setClickable(true);
        button_start.setBackgroundColor(Color.GRAY);
        button_start.setEnabled(false);
        button_start.setClickable(false);
        Toast.makeText(this, "Started activity: " + this.dailyActivities.getSelectedItem().toString(),Toast.LENGTH_LONG );
    }

    // Listener registered for ab_stop
    public void stopActivity(View view) {
        //TODO save selected activity on destroy to use it here instead of blanks
        DailyActivity activity = new DailyActivity(Event.TP_STOP, "", "", new Date());
        ScratchWriter writer = new ScratchWriter(this, "activities.csv");
        writer.saveData(activity.toCSV());
        button_start.setEnabled(true);
        button_start.setClickable(true);
        button_stop.setBackgroundColor(Color.GRAY);
        button_stop.setEnabled(false);
        button_stop.setClickable(false);
        Toast.makeText(this, "Stopped activity",Toast.LENGTH_LONG );
    }


    // Listener registered for sw_heart toggle
    //TODO not working
    public void toggleHeartMonitor(View view) {
        if (heartSwitch.isChecked()) {
            startMonitoringService();
        } else {
            stopMonitoringService();
            heartbeat.setText(R.string.text_monitoroff);
        }
    }


    /**
     * Start the heart monitor service running in background
     */
    private void startMonitoringService() {

        // search for devices
        if (this.deviceAddress == null) {
            Intent intent_scan = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(intent_scan, REQUEST_SCAN);
            return;
            // when the activity returns the device address, this method will be called back
        }

        Log.i(TAG, "Starting service");
        final Intent blueServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(blueServiceIntent); // needed for Service not to die if activity unbinds
        bindService(blueServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Bound to service");
    }


    /**
     * Receives the selected device address result from the device scan activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SCAN) { // Make sure this is the scan result
            if (resultCode == RESULT_OK) { // Make sure the request was successful
                this.deviceAddress = data.getStringExtra("deviceAddr");
                Log.i(TAG, "User selected device with address " + this.deviceAddress);
                startMonitoringService();
            }
        }
    }


    private void stopMonitoringService() {
        Log.i(TAG, "Stopping service");
        unregisterReceiver(gattUpdateReceiver);
        unbindService(serviceConnection);
        Intent stopIntent = new Intent(this, BluetoothLeService.class);
        blueService.stopService(stopIntent);
        blueService = null;
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
            Log.i(TAG, "Service connected to activity");
            blueService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!blueService.connect(deviceAddress)) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                heartSwitch.setChecked(false);
                heartbeat.setText("Error in bluetooth initialization");
                return;
            }
            serviceConnected = true;
            heartSwitch.setChecked(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "Service disconnected!");
            serviceConnected = false;
            heartSwitch.setChecked(false);
            unregisterReceiver(gattUpdateReceiver);
            // maybe it was an accident? Let's try to get it back!
            startMonitoringService();
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
            // blatantly ignore any other actions - activity doesn't really care, service should
//            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                Log.i(TAG, "Services discovered");
                //TODO should move to inside BLE service? (On gatt connected)
//                BluetoothGattCharacteristic hr =
//                        blueService.findHRMCharacteristic(blueService.getSupportedGattServices());
//                blueService.setCharacteristicNotification(hr, true);
//            }

        }
    };
}

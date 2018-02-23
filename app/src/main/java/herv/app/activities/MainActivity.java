package herv.app.activities;

import android.bluetooth.BluetoothAdapter;
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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import herv.app.services.BluetoothLeService;
import herv.app.services.CloudFileWriter;
import herv.app.R;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ToggleButton heartToggle;
    private TextView heartbeat, pairedDevice, userText;
    private Button startScan, buttonSign;

    private BluetoothLeService blueService;
    private boolean serviceConnected;
    private CloudFileWriter cloud;
    private FirebaseAuth mAuth;

    private String deviceAddress; // "00:22:D0:85:88:8E";

    private final int REQUEST_SCAN = 1;
    private static final int RC_SIGN_IN = 42;


    //region lifecycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");

        setContentView(R.layout.activity_main);
        heartToggle = (ToggleButton) findViewById(R.id.tg_heart);
        heartToggle.setEnabled(false); //TODO make it work!
        heartbeat = (TextView) findViewById(R.id.tv_heartrate);
        startScan = (Button) findViewById(R.id.bt_pair);
        pairedDevice = (TextView) findViewById(R.id.tv_paired_device);

        userText = (TextView) findViewById(R.id.tv_user);
        buttonSign = (Button) findViewById(R.id.bt_signin);

        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity starting");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            this.heartToggle.setEnabled(false);
            this.startScan.setEnabled(false);
            this.pairedDevice.setText(R.string.text_monitornobluetooth);
            this.heartbeat.setText("");
            return;
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        this.deviceAddress = sharedPref.getString(getString(R.string.paired_device), "");
        if (this.deviceAddress != null && this.deviceAddress != "") {
            this.pairedDevice.setText(this.deviceAddress);
            startMonitoringService();
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        //FirebaseUser currentUser = mAuth.getCurrentUser();
        //signInAnonymously();
        updateLoginStatus();
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
        try {
            if (gattUpdateReceiver != null) {
                unregisterReceiver(gattUpdateReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Tried to unregister non existing receiver");
        }
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

    //endregion


    //region  service interaction
    // connects to service and receives broadcasts to show HR on screen


    // Listener registered for sw_heart toggle
    //TODO not working
    public void toggleHeartMonitor(View view) {
        if (heartToggle.isChecked()) {
            startMonitoringService();
        } else {
            stopMonitoringService();
            heartbeat.setText(R.string.text_monitoroff);
        }
    }


    /**
     * Allows the user to select a device to pair with the app
     **/
    public void scanDevices(View view) {
        Intent intent_scan = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(intent_scan, REQUEST_SCAN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        /**
         * Receives the selected device address result from the device scan activity
         */
        if (requestCode == REQUEST_SCAN) { // Make sure this is the scan result
            if (resultCode == RESULT_OK) { // Make sure the request was successful
                String address = data.getStringExtra("deviceAddr");
                Log.i(TAG, "User selected device with address " + address);
                this.saveDeviceAddress(address);
                this.startMonitoringService();
            }
        }

        /**
         * Returning from user login flow managed by FirebaseUI
         */
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                updateLoginStatus();
            } else {
                // Sign in failed, check response for error code
                // ...
            }
        }
    }

    protected void saveDeviceAddress(String address) {
        this.deviceAddress = address;
        this.pairedDevice.setText(address);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.paired_device), address);
        editor.commit();
    }


    /**
     * Starts the heart monitor service running in background
     */
    private void startMonitoringService() {
        if (this.deviceAddress == null || this.deviceAddress == "")
            return;
        Log.i(TAG, "Starting service");
        final Intent blueServiceIntent = new Intent(this, BluetoothLeService.class);
        blueServiceIntent.putExtra("address", this.deviceAddress);
        startService(blueServiceIntent); // needed for Service not to die if activity unbinds
        bindService(blueServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Bound to service");
    }


    //TODO figure out the order between disconnecting, unbinding and stopping the service
    private void stopMonitoringService() {
        Log.i(TAG, "Stopping service");
        unregisterReceiver(gattUpdateReceiver);
        blueService.disconnect();
        unbindService(serviceConnection);
        Intent stopIntent = new Intent(this, BluetoothLeService.class);
        blueService.stopService(stopIntent);
        blueService = null;
    }


    /**
     * Get a bluetooth adapter and request the user to enable bluetooth if it is not yet enabled
     * TODO can I start an activity from inside the service? If so, move this to the service, the activity should not be responsible for BT setting
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
            serviceConnected = true;
            heartToggle.setChecked(true);
        }


        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "Service disconnected from activity");
            serviceConnected = false;
            heartToggle.setChecked(false);
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

            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                heartbeat.setText("Disconnected");
            }
            // blatantly ignore any other events - activity doesn't really care, service does
        }
    };

    //endregion


    //region firebase methods


    // https://firebase.google.com/docs/auth/android/firebaseui
    public void signin() {

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());

        // Create and launch sign-in intent
        Intent intentSign = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        startActivityForResult(intentSign, RC_SIGN_IN);
    }


    public void signout() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        updateLoginStatus();
                    }
                });
    }

    public void sign(View view) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            signin();
        } else {
            signout();
        }
    }

    public void updateLoginStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            buttonSign.setText(R.string.common_signin_button_text);
            userText.setText(R.string.no_user);
        } else {
            buttonSign.setText(R.string.signout);
            userText.setText(user.getDisplayName() + "\n" + user.getUid());
        }
    }

    public void sendToCloud(View view) {
        int user = 0;
        cloud = new CloudFileWriter();
        int files = cloud.uploadFiles(user);
        cloud = null;
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInAnonymously:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}

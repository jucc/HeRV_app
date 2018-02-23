package herv.app.activities;

import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.firebase.ui.auth.IdpResponse;

import herv.app.R;
import herv.app.services.BluetoothLeService;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HeartbeatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HeartbeatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeartbeatFragment extends Fragment {

    private ToggleButton heartToggle;
    private TextView heartbeat, pairedDevice;
    private Button startScan;

    private BluetoothLeService blueService;
    private OnFragmentInteractionListener mListener;

    private boolean serviceConnected;
    private String deviceAddress; // "00:22:D0:85:88:8E";

    private final int REQUEST_SCAN = 42;
    private final static String TAG = MainActivity.class.getSimpleName();


    public HeartbeatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment HeartbeatFragment.
     */
    public static HeartbeatFragment newInstance(String param1, String param2) {
        HeartbeatFragment fragment = new HeartbeatFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_heartbeat, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        heartToggle = (ToggleButton) getActivity().findViewById(R.id.tg_heart);
        heartToggle.setEnabled(false); //TODO make it work!
        heartbeat = (TextView) getActivity().findViewById(R.id.tv_heartrate);
        startScan = (Button) getActivity().findViewById(R.id.bt_pair);
        pairedDevice = (TextView) getActivity().findViewById(R.id.tv_paired_device);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            this.heartToggle.setEnabled(false);
            this.startScan.setEnabled(false);
            this.pairedDevice.setText(R.string.text_monitornobluetooth);
            this.heartbeat.setText("");
            return;
        }

        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { scanDevices(v); }});

        heartToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleHeartMonitor(v); }});
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        this.deviceAddress = sharedPref.getString(getString(R.string.paired_device), "");
        if (this.deviceAddress != null && this.deviceAddress != "") {
            this.pairedDevice.setText(this.deviceAddress);
            startMonitoringService();
            getActivity().registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (gattUpdateReceiver != null) {
                getActivity().unregisterReceiver(gattUpdateReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Tried to unregister non existing receiver");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        // TODO how to check that they are running to unbind/unregister??
        if (blueService != null) {
            getActivity().unbindService(serviceConnection);
            blueService = null;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }


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
        Intent intent_scan = new Intent(getActivity(), DeviceScanActivity.class);
        startActivityForResult(intent_scan, REQUEST_SCAN);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        /**
         * Receives the selected device address result from the device scan activity
         */
        if (requestCode == REQUEST_SCAN) { // Make sure this is the scan result
            if (resultCode == Activity.RESULT_OK) { // Make sure the request was successful
                String address = data.getStringExtra("deviceAddr");
                Log.i(TAG, "User selected device with address " + address);
                this.saveDeviceAddress(address);
                this.startMonitoringService();
            }
        }
    }

    protected void saveDeviceAddress(String address) {
        this.deviceAddress = address;
        this.pairedDevice.setText(address);
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
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
        final Intent blueServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
        blueServiceIntent.putExtra("address", this.deviceAddress);
        getActivity().startService(blueServiceIntent); // needed for Service not to die if activity unbinds
        getActivity().bindService(blueServiceIntent, serviceConnection, Activity.BIND_AUTO_CREATE);
        Log.i(TAG, "Bound to service");
    }


    //TODO figure out the order between disconnecting, unbinding and stopping the service
    private void stopMonitoringService() {
        Log.i(TAG, "Stopping service");
        getActivity().unregisterReceiver(gattUpdateReceiver);
        blueService.disconnect();
        getActivity().unbindService(serviceConnection);
        Intent stopIntent = new Intent(getActivity(), BluetoothLeService.class);
        blueService.stopService(stopIntent);
        blueService = null;
    }


    /**
     * Get a bluetooth adapter and request the user to enable bluetooth if it is not yet enabled
     * TODO can I start an activity from inside the service? If so, move this to the service, the activity should not be responsible for BT setting
     */
    private BluetoothAdapter getBluetoothAdapter() {

        final BluetoothManager blueManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
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
            getActivity().unregisterReceiver(gattUpdateReceiver);
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
}

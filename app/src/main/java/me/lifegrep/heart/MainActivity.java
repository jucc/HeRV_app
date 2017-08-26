package me.lifegrep.heart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    Switch heartSwitch;
    TextView heartbeat;
    private BluetoothAdapter blueAdapter;

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

    /**
     * Listener for fab_start
     */
    public void startActivity(View view) {
        String text = "Starting";
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

        Intent intent_scan = new Intent(this, ActivitySelectionActivity.class);
        startActivity(intent_scan);
    }

    /**
     * Listener for sw_heart
     */
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
                }
            }

            // find the device

            Intent intent_scan = new Intent(this, DeviceScanActivity.class);
            startActivity(intent_scan);

        } else {
            heartbeat.setText(R.string.text_monitoroff);
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
}

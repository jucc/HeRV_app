package herv.app.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import herv.app.R;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity starting");
    }
}

package herv.app.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import herv.app.R;

public class MainActivity extends AppCompatActivity
                          implements ChooseSessionTypeFragment.OnSessionTypeSelectedListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fg_session_container) != null) {

            if (savedInstanceState != null) {
                return;
            }
            ChooseSessionTypeFragment sessionFragment = new ChooseSessionTypeFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fg_session_container, sessionFragment).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity starting");
    }

    public void onFragmentInteraction(String type) {
        switch (type){
            case "daily": {
                RecordSessionFragment newFragment = new RecordSessionFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fg_session_container, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            }
            case "training": {
                Fragment newFragment = new RecordTrainingFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fg_session_container, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            }
            default: {
                // just stay where it is
            }
        }
    }

}

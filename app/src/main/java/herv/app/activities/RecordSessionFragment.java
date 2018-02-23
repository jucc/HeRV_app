package herv.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;

import herv.app.R;
import herv.app.model.DailyActivity;
import herv.app.model.Event;
import herv.app.services.ScratchFileWriter;

/**
 * Fragment used for input of user activity sessions
 * TODO separation of concerns, should not be responsible for writing the files
 */
public class RecordSessionFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private RadioGroup radioPosture;
    private Spinner dailyActivities;
    private FloatingActionButton buttonStart, buttonStop;
    private ArrayAdapter<CharSequence> categoriesAdapter;
    private TextView sessionText;

    private static SimpleDateFormat formatActivityFilename = new SimpleDateFormat("yyMMdd");


    //region lifecycle

    public RecordSessionFragment() {
        // Required empty public constructor
    }

    public static RecordSessionFragment newInstance(String param1, String param2) {
        RecordSessionFragment fragment = new RecordSessionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record_session, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        dailyActivities = (Spinner)  getActivity().findViewById(R.id.sp_activity);
        radioPosture = (RadioGroup)  getActivity().findViewById(R.id.rg_posture);
        buttonStart = (FloatingActionButton)  getActivity().findViewById(R.id.ab_start);
        buttonStop = (FloatingActionButton)  getActivity().findViewById(R.id.ab_stop);
        sessionText = (TextView) getActivity().findViewById(R.id.tv_session);

        categoriesAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.activity_categories_descriptors,
                android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        dailyActivities.setAdapter(categoriesAdapter);


        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        boolean sessionStatus = sharedPref.getBoolean(getString(R.string.save_session_started), false);
        setSessionStatusView(sessionStatus);

        int selectedActivityID = sharedPref.getInt(getString(R.string.save_activity), -1);
        int selectedPostureID = sharedPref.getInt(getString(R.string.save_posture), -1);
        if (selectedActivityID != -1) {
            this.dailyActivities.setSelection(selectedActivityID);
        }
        if (selectedPostureID != -1) {
            this.radioPosture.check(selectedPostureID);
        }

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(v);
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopActivity(v);
            }
        });
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this fragment to allow
     * an interaction in this fragment to be communicated
     * http://developer.android.com/training/basics/fragments/communicating.html
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    //endregion

    //region session/activity recording

    public void setSessionStatusView(boolean started) {

        if (started) {
            buttonStop.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.INVISIBLE);
            sessionText.setText(getString(R.string.session_started));
        } else {
            buttonStop.setVisibility(View.INVISIBLE);
            buttonStart.setVisibility(View.VISIBLE);
            sessionText.setText(getString(R.string.session_stopped));
        }
    }


    private void persistStartedSession(int activityID, int postureID) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.save_session_started), true);
        editor.putInt(getString(R.string.save_activity), activityID);
        editor.putInt(getString(R.string.save_posture), postureID);
        editor.commit();
    }


    public void startActivity (View view){
        // get selected activity from spinner
        int selActivityID = dailyActivities.getSelectedItemPosition();
        String selActivity = getResources().getStringArray(R.array.activity_categories_names)[selActivityID];

        // get selected posture from radio button
        String selPosture = "";
        int selPostureID = radioPosture.getCheckedRadioButtonId();
        switch (selPostureID) {
            case R.id.rb_liedown:
                selPosture = "lie";
                break;
            case R.id.rb_sit:
                selPosture = "sit";
                break;
            case R.id.rb_stand:
                selPosture = "stand";
                break;
        }

        DailyActivity activity = new DailyActivity(Event.TP_START, selActivity, selPosture, new Date());
        saveActivity(activity);

        persistStartedSession(selActivityID, selPostureID);
        setSessionStatusView(true);
        Toast.makeText(getActivity(), "Started: " + this.dailyActivities.getSelectedItem().toString(), Toast.LENGTH_LONG);
    }

    // Listener registered for ab_stop
    public void stopActivity(View view) {
        //TODO save date to session instead
        DailyActivity activity = new DailyActivity(Event.TP_STOP, "", "", new Date());
        saveActivity(activity);

        setSessionStatusView(false);
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.save_session_started), false);
        editor.commit();

        Toast.makeText(getActivity(), "Finished activity",Toast.LENGTH_LONG );
    }


    private void saveActivity(DailyActivity activity) {
        String dt = formatActivityFilename.format(new Date());
        String user = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StringBuilder filename = new StringBuilder();
        filename.append("act");
        filename.append(dt);
        //filename.append("_");
        //filename.append(user);
        filename.append(".csv");
        ScratchFileWriter writer = new ScratchFileWriter(getActivity(), filename.toString());
        writer.saveData(activity.toCSV());
    }

    //endregion
}
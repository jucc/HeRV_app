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
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import herv.app.R;
import herv.app.model.DailyActivity;
import herv.app.model.Event;
import herv.app.services.ScratchFileWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Fragment used for input of user activity sessions
 * TODO separation of concerns, should not be responsible for writing the files
 */
public class RecordTrainingFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private FloatingActionButton buttonStart, buttonStop;
    private TextView sessionText, textview1, textview2, textview3, textview4;

    private static SimpleDateFormat formatActivityFilename = new SimpleDateFormat("yyMMdd");


    //region lifecycle

    public RecordTrainingFragment() {
        // Required empty public constructor
    }

    public static RecordTrainingFragment newInstance(String param1, String param2) {
        RecordTrainingFragment fragment = new RecordTrainingFragment();
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
        return inflater.inflate(R.layout.fragment_record_training, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sessionText = (TextView) getActivity().findViewById(R.id.tv_session);
        textview1   = (TextView) getActivity().findViewById(R.id.tv_1);
        textview2   = (TextView) getActivity().findViewById(R.id.tv_2);
        textview3   = (TextView) getActivity().findViewById(R.id.tv_3);
        textview4   = (TextView) getActivity().findViewById(R.id.tv_4);

        buttonStart = (FloatingActionButton) getActivity().findViewById(R.id.ab_start_train);
        buttonStop = (FloatingActionButton) getActivity().findViewById(R.id.ab_cancel_train);

        switchViewNextStage();

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchViewNextStage(); }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cancel();}
        });
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

    //region training session flow management

    public void switchViewNextStage() {

        //TODO restructure this later. I have completely run out of time and know not what I am writing now
        // Also, allow customization of number of repetitions of focus/breathe stages

        Integer stage = getActivity().getPreferences(Context.MODE_PRIVATE)
                                     .getInt(getString(R.string.training_stage), 0);

        switch (stage) {
            case 0: { //description
                startBaseline();
                break;
            }
            case 1: { //baseline
                startFocus();
                break;
            }
            case 2: { //focus 1
                startBreathe();
                break;
            }
            case 3: { //breathe 1
                startFocus();
                break;
            }
            case 4: { //focus 2
                startBreathe();
                break;
            }
            case 5: { //breathe 2
                startDescription();
                break;
            }
        };
    }

    private void persistStartedSession(int stage) {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                     .edit().putInt(getString(R.string.training_stage), stage).commit();
    }


    private void startDescription(){

        buttonStop.setVisibility(View.INVISIBLE);
        buttonStart.setVisibility(View.VISIBLE);
        sessionText.setText(getString(R.string.session_stopped));

        persistStartedSession(0);
    }


    public void startBaseline (){
//
//        DailyActivity activity = new DailyActivity(Event.TP_START, selActivity, selPosture, new Date());
//        saveActivity(activity);
//
//        buttonStop.setVisibility(View.INVISIBLE);
//        buttonStart.setVisibility(View.VISIBLE);
//        sessionText.setText(getString(R.string.session_stopped));
//
//        persistStartedSession(selActivityID, selPostureID);
//        switchViewNextStage(true);
        Toast.makeText(getActivity(), "Started baseline", Toast.LENGTH_LONG);
    }

    public void startFocus (){
        Toast.makeText(getActivity(), "Started focus", Toast.LENGTH_LONG);
    }

    public void startBreathe (){
        Toast.makeText(getActivity(), "Started breathe", Toast.LENGTH_LONG);
    }

    private void cancel() {
        Toast.makeText(getActivity(), "Cancel", Toast.LENGTH_LONG);
    }

    //endregion

    // region record TODO create reusable methods to share with RecordSession

    // Listener registered for ab_stop
    public void stopActivity(View view) {
        //TODO save date to session instead
        DailyActivity activity = new DailyActivity(Event.TP_STOP, "", "", new Date());
        saveActivity(activity);

        switchViewNextStage();
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
        filename.append(".csv");
        ScratchFileWriter writer = new ScratchFileWriter(getActivity(), filename.toString());
        writer.saveData(activity.toCSV());
    }

    //endregion
}
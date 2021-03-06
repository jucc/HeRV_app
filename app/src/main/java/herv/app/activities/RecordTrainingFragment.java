package herv.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Fragment used for input of user activity sessions
 * TODO separation of concerns, should not be responsible for writing the files
 */
public class RecordTrainingFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private FloatingActionButton buttonStart, buttonStop;
    private TextView sessionText, textview1, textview2, textview3, textview4, tvTime, tvCount;

    private static SimpleDateFormat formatActivityFilename = new SimpleDateFormat("yyMMdd");
    private final static String TAG = MainActivity.class.getSimpleName();


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
        tvTime   = (TextView) getActivity().findViewById(R.id.tv_time);
        tvCount   = (TextView) getActivity().findViewById(R.id.tv_countdown);


        buttonStart = (FloatingActionButton) getActivity().findViewById(R.id.ab_start_train);
        buttonStop = (FloatingActionButton) getActivity().findViewById(R.id.ab_cancel_train);

        //TODO user may have accidentaly rotated screen during session. Should check stage?
        setStageView(0);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startStage(); }
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

    //region training session flow management //TODO restructure this later.
    // I have completely run out of time and know not what I am writing now
    // Also, allow customization of number of repetitions of focus/breathe stages

    public void setStageView(Integer stage) {

        persistStartedSession(stage);

        switch (stage) {
            case 0: { //description
                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.VISIBLE);
                tvTime.setVisibility(View.INVISIBLE);
                tvCount.setVisibility(View.INVISIBLE);
                sessionText.setText(getString(R.string.tr_description));
                textview1.setText(getString(R.string.tr_description_1));
                textview2.setText("");
                textview3.setText(getString(R.string.tr_description_3));
                textview4.setText("");
                break;
            }
            case 1: { //baseline
                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.INVISIBLE);
                tvTime.setVisibility(View.VISIBLE);
                tvCount.setVisibility(View.VISIBLE);
                sessionText.setText(getString(R.string.tr_baseline));
                textview1.setText(getString(R.string.tr_baseline_1));
                textview2.setText("");
                textview3.setText(getString(R.string.tr_baseline_3));
                textview4.setText(getString(R.string.tr_baseline_4));
                break;
            }
            case 2:
            case 4:
                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.VISIBLE);
                tvTime.setVisibility(View.INVISIBLE);
                tvCount.setVisibility(View.INVISIBLE);
                sessionText.setText(getString(R.string.tr_focus));
                textview1.setText(getString(R.string.tr_focus_pre_1));
                textview2.setText("");
                textview3.setText(getString(R.string.tr_focus_pre_3));
                textview4.setText("");
                break;
            case 3:
            case 5: { //breathe
                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.VISIBLE);
                tvTime.setVisibility(View.INVISIBLE);
                tvCount.setVisibility(View.INVISIBLE);
                sessionText.setText(getString(R.string.tr_breathe));
                textview1.setText(getString(R.string.tr_breathe_1));
                textview2.setText(getString(R.string.tr_breathe_2));
                textview3.setText(getString(R.string.tr_breathe_3));
                textview4.setText("");
                break;
            }
            case 6: {
                setStageView(0);
                break;
            }
            default: {
                Log.w(TAG, "Set stage view case statement with invalid option");
            }
        };
    }

    public void startStage() {

        Integer stage = getActivity().getPreferences(Context.MODE_PRIVATE)
                .getInt(getString(R.string.training_stage), 0);

        switch (stage) {
            case 0: { //description
                startBaseline();
                break;
            }
            case 2:
            case 4: { //focus
                startFocus(stage);
                break;
            }
            case 3:
            case 5: { //breathe 1
                startBreathe(stage);
                break;
            }
            default: {
                Log.w(TAG, "Start stage view case statement with invalid option");
            }
        };
    }

    private void persistStartedSession(int stage) {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                     .edit().putInt(getString(R.string.training_stage), stage).commit();
    }


    public void startBaseline (){

        setStageView(1);
        saveActivity(activity_st(getString(R.string.act_baseline)));
        startCountdown(80, 2);
    }

    public void startFocus (final Integer stage){
        Integer decrement = stage == 2? 7 : 13;
        Integer countStart = new Random().nextInt(3000) + 3000;
        buttonStop.setVisibility(View.INVISIBLE);
        buttonStart.setVisibility(View.INVISIBLE);
        tvTime.setVisibility(View.VISIBLE);
        tvCount.setVisibility(View.VISIBLE);
        textview1.setText(getString(R.string.tr_focus_1));
        textview2.setText("" + countStart);
        textview3.setText(getString(R.string.tr_focus_3));
        textview4.setText("" + decrement);
        saveActivity(activity_st(getString(R.string.act_focus)));
        startCountdown(80, stage+1);
    }

    public void startBreathe (Integer stage){
        buttonStop.setVisibility(View.INVISIBLE);
        buttonStart.setVisibility(View.INVISIBLE);
        tvTime.setVisibility(View.VISIBLE);
        tvCount.setVisibility(View.VISIBLE);
        saveActivity(activity_st(getString(R.string.act_breathe)));
        startCountdown(80, stage+1);
    }

    private void cancel() {
        Toast.makeText(getActivity(), "Cancel", Toast.LENGTH_LONG);
    }

    private void startCountdown(int seconds, final int nextStage) {
        new CountDownTimer(seconds * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                tvCount.setText(" " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                Toast.makeText(getActivity(), "Finished baseline", Toast.LENGTH_LONG);
                stopActivity();
                setStageView(nextStage);
            }
        }.start();
    }

    //endregion

    // region record TODO create reusable methods to share with RecordSession

    // Listener registered for ab_stop
    public void stopActivity() {
        //TODO save date to session instead
        DailyActivity activity = new DailyActivity(Event.TP_STOP, "", "", new Date());
        saveActivity(activity);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.save_session_started), false);
        editor.commit();

        Toast.makeText(getActivity(), "Finished activity",Toast.LENGTH_LONG );
    }

    private DailyActivity activity_st(String activity_name) {
        return new DailyActivity(0, Event.TP_START, activity_name, "sit", new Date());
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
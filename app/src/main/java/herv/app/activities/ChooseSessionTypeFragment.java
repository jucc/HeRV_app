package herv.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import herv.app.R;

/**
 * {@link Fragment} to select a session type to start (training or daily activity)
 * Activities that contain this fragment must implement the
 * {@link ChooseSessionTypeFragment.OnSessionTypeSelectedListener} interface
 * to handle interaction events.
 */
public class ChooseSessionTypeFragment extends Fragment {

    private OnSessionTypeSelectedListener mListener;

    private TextView instructionText;
    private Button buttonDaily;
    private Button buttonTraining;

    public ChooseSessionTypeFragment() {
        // Required empty public constructor
    }

    public static ChooseSessionTypeFragment newInstance(String param1, String param2) {
        ChooseSessionTypeFragment fragment = new ChooseSessionTypeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_session_type, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        buttonDaily = (Button) getActivity().findViewById(R.id.bt_daily);
        buttonTraining = (Button) getActivity().findViewById(R.id.bt_training);

        buttonDaily.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mListener.onFragmentInteraction("daily");
            }
        });

        buttonTraining.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mListener.onFragmentInteraction("training");
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSessionTypeSelectedListener) {
            mListener = (OnSessionTypeSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSessionTypeSelectedListener");
            // This is just so weird! Why do we want to throw it on runtime instead of compile time?
            // But it is described here:
            // https://developer.android.com/guide/components/fragments#EventCallbacks
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
    public interface OnSessionTypeSelectedListener {
        void onFragmentInteraction(String type);
    }
}

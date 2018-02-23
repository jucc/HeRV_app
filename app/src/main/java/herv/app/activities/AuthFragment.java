package herv.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import herv.app.R;
import herv.app.services.CloudFileWriter;

/**
 * Fragment for adding login/logout button and showing currently logged user
 */
public class AuthFragment extends Fragment {

    private TextView userText;
    private Button buttonSign;

    private CloudFileWriter cloud;
    private FirebaseAuth mAuth;

    private OnFragmentInteractionListener mListener;

    private static final int RC_SIGN_IN = 42;
    private final static String TAG = MainActivity.class.getSimpleName();


    public AuthFragment() {
        // Required empty public constructor
    }

    public static AuthFragment newInstance(String param1, String param2) {
        AuthFragment fragment = new AuthFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        userText = (TextView) getActivity().findViewById(R.id.tv_user);
        buttonSign = (Button) getActivity().findViewById(R.id.bt_signin);
        mAuth = FirebaseAuth.getInstance();

        buttonSign.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sign(v); }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) signInAnonymously();
        updateLoginStatus();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // make sure its returning from user login flow managed by FirebaseUI
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == Activity.RESULT_OK) {
                updateLoginStatus();
            } else {
                // Sign in failed, check response for error code
                // ...
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this fragment to allow
     * an interaction in this fragment to be communicated
     * http://developer.android.com/training/basics/fragments/communicating.html
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    //region authentication

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
        AuthUI.getInstance().signOut(getActivity()).addOnCompleteListener(new OnCompleteListener<Void>() {
            public void onComplete(@NonNull Task<Void> task) {
                updateLoginStatus();
            }
        });
    }

    public void sign(View view) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {signin();} else {signout();}
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
        mAuth.signInAnonymously().addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInAnonymously:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                }
            }
        });
    }

    //endregion
}

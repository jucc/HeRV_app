package herv.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private TextView userText;
    private Button buttonSign;

    private CloudFileWriter cloud;
    private FirebaseAuth mAuth;

    private static final int RC_SIGN_IN = 42;


    //region lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");

        setContentView(R.layout.activity_main);

        userText = (TextView) findViewById(R.id.tv_user);
        buttonSign = (Button) findViewById(R.id.bt_signin);
        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity starting");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) signInAnonymously();
        updateLoginStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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

    //endregion

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

    //endregion

}

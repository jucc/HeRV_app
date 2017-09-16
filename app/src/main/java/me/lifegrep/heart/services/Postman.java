package me.lifegrep.heart.services;

/**
 * Created with IntelliJ IDEA.
 * User: ju
 * Date: 3/30/13
 * Time: 7:00 PM


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

//TODO redistribute responsibility between postman and activity to allow services (no UI, no toast)
/**
 * Separate task to post request to remote server

public class Postman extends AsyncTask<Void, Void, Void> {

    private final String TAG = "Lifegrep";

    String address = "http://lifegrep.org/save";
    Event letter = null;
    Context sender= null;

    HttpResponse response;
    String msg = "";

    public void send(Event event, String host, Context context) {
        this.letter = event;
        if (host != null) {
            this.address = host;
        }
        this.sender = context;
        this.execute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.d(TAG, "Called Postman on server " + address);
        Log.d(TAG, "to send event " + letter.toString());
        try {
            if (isInternetOK())
                postRequest();
            else
                msg = "Unable to save due to lack of internet connection";
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            msg = "Unable to save due to internal error";
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void s) {
        // if there was an internal error or network connection problem, msg will be set already
        if ("".equals(msg))
            msg = isResponseOK()? "Saved!" : "Unable to save due to server error";
        Toast.makeText(sender, msg, Toast.LENGTH_LONG).show();
    }

    private void postRequest() throws IOException {
        HttpPost httpost = new HttpPost(this.address);
        httpost.setHeader("Accept", "application/json");
        httpost.setHeader("Content-type", "application/json");
        httpost.setEntity(new StringEntity(this.letter.toString()));
        response = new DefaultHttpClient().execute(httpost);
    }

    private boolean isInternetOK() {
        ConnectivityManager connection = (ConnectivityManager) sender.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = connection.getActiveNetworkInfo();
        return (network != null && network.isConnected());
    }

    private boolean isResponseOK() {
        return
                response != null &&
                        response.getStatusLine() != null &&
                        response.getStatusLine().getStatusCode() == 200;
    }
}
*/
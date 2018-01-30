package herv.app.services;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class CloudFileWriter {

    private final static String TAG = CloudFileWriter.class.getSimpleName();
    FirebaseStorage storage = FirebaseStorage.getInstance();

    public int uploadFiles (int userID) {

        String remotePath = "raw/" + userID + "/";

        String dirname = Environment.getExternalStorageDirectory().getPath() + "/HeRV/";
        File dir = new File(dirname);
        if(!dir.exists() || !dir.isDirectory())
        {
            Log.w(TAG, "Trying to read non existent directory");
            return 0;
        }
        File files[] = dir.listFiles();
        if (files.length == 0) {
            Log.i(TAG, "Empty directory");
            return 0;
        }

        StorageReference storageRef = storage.getReference();
        for( File file : files) {
            String fname = file.getAbsolutePath();
            Log.i(TAG, "Preparing to send file: " + fname);
            StorageReference ref =  storageRef.child(remotePath + file.getName());
            Uri uri = Uri.fromFile(file);
            ref.putFile(uri);
        }
        return files.length;
    }

}

/**
 * Lifegrep scratch writer
 * Heartbeats and daily events are written in real time to a json scratch file in internal storage.
 * After sometime, these events/heartbeats are uploaded to a server and the scratch cleaned.
 */
package herv.app.services;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;


public class ScratchWriter {

    Context context;
    String filename, dirname;

    public ScratchWriter(Context context, String filename) {
        this.context = context;
        //this.filename = context.getFilesDir().getPath() + "/" + filename;
        this.dirname = "HeRV";
        this.filename = Environment.getExternalStorageDirectory().getPath() + "/" + dirname + "/" + filename;
        checkFolder();
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void checkFolder() {
        String folder_main = "HeRV";
        File f = new File(Environment.getExternalStorageDirectory(), folder_main);
        if(!f.exists())
        {
            f.mkdir();
        }
    }

    public void saveData(String data) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            //TODO URGENT open file only once and close at the end
            fw = new FileWriter(filename, true);
            bw = new BufferedWriter(fw);
            bw.newLine();
            bw.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public String getData() {
        try {
            File f = new File(filename);
            FileInputStream inStream = new FileInputStream(f);
            int size = inStream.available();
            byte[] buffer = new byte[size];
            inStream.read(buffer);
            inStream.close();
            return new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void eraseContents() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(filename);
            fw.write("");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

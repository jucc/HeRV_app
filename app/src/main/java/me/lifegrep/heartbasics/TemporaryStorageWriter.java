package me.lifegrep.heartbasics;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;


public class TemporaryStorageWriter {

    Context context;
    String filename;

    public TemporaryStorageWriter(Context context, String filename) {
        this.context = context;
        this.filename = context.getFilesDir().getPath() + "/" + filename;
    }


    public void saveData(String data) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(filename, true);
            bw = new BufferedWriter(fw);
            bw.write(data + "\n");
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

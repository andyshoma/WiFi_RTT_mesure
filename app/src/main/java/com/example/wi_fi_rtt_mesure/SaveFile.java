package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ContentHandler;

public class SaveFile {

    private final static String TAG = "SaveFile.java";
    private final static String RTT_DIR = "WifiRtt";
    private final static int CREATE_DOCUMENT_REQUEST = 43;

    private String path;
    private String filename;
    private Context context;

    private Activity activity;

    private File dir;

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public SaveFile(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
//        verifyStoragePermissions(activity);
//        dir = getPrivateRttStorageDir(RTT_DIR);
//        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
    }

    public void write(String str) {
        try {
            FileWriter file = new FileWriter(dir.getAbsoluteFile() + filename);
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            pw.println(str);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getPrivateRttStorageDir(String dirname) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), dirname);
        if (!file.mkdirs()) {
            Log.d(TAG, "Directory not created");
        }
        return file;
    }

    public void createNewFile(String title) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, title);
        activity.startActivityForResult(intent, CREATE_DOCUMENT_REQUEST);
    }


    private static void verifyStoragePermissions(Activity activity) {
        int readPermission = ContextCompat.checkSelfPermission(activity, mPermissions[0]);
        int writePermission = ContextCompat.checkSelfPermission(activity, mPermissions[1]);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }
}

package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ContentHandler;

public class SaveFile {
    private String path;
    private String filename;
    private Context context;

    public SaveFile(String path, Context context){
        this.path = path;
        this.context = context;
    }

    public void write(String str) {
        try {
            FileWriter file = new FileWriter(path + filename, true);
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            pw.println(str);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setFilename(String filename){
        this.filename = filename;
        Toast toast = Toast.makeText(context, "fileを作成!", Toast.LENGTH_LONG);
        toast.show();
    }
}

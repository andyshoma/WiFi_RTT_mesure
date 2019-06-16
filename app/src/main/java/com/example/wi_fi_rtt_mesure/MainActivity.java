package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Context context;
    private Handler handler;
    private boolean flag = false;
    private final static int measureTime = 30000;
    private final static int interval = 1000;
    private Timer measureTimer;
    private WifiManager wifiManager;

    private SaveFile saveFile;
    private String filename = "ranging.csv";
    private String path;

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView aware_check = findViewById(R.id.aware_check);
        TextView rtt_check = findViewById(R.id.rtt_check);
        TextView aware_avail = findViewById(R.id.aware_avail);
        TextView rtt_avail = findViewById(R.id.rtt_avail);
        TextView result_distance = findViewById(R.id.result_distance);
        TextView counter = findViewById(R.id.counter);
        Button reset = findViewById(R.id.reset);
        Button check_button = findViewById(R.id.check_permission);
        Button pub_button = findViewById(R.id.publish);
        Button sub_button = findViewById(R.id.subscribe);
        Button rtt_button = findViewById(R.id.wifi_rtt);

        context = getApplicationContext();

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        saveFile = new SaveFile(path, context);
        verifyStoragePermissions(this);
        File dir = new File(path);
        dir.mkdir();

        wifiManager = new WifiManager(context, handler);

        wifiManager.check(aware_check, rtt_check);
        wifiManager.available(aware_avail, rtt_avail);
        wifiManager.getTextView(result_distance, counter);

        check_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.check_permission();
            }
        });

        pub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.publisher();
            }
        });

        sub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.subscriber();
            }
        });

        rtt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                saveFile.setFilename(filename);
                saveFile.write("distanceMm,distanceStdDevMm,rssi,timestamp");
                CreateDialog();
            }
        });

        reset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                wifiManager.resetCounter();
            }
        });
    }

    public void CreateDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("now collecting")
                .setMessage(measureTime + "秒待ってください...\n" + path);

        alertDialogBuilder.setCancelable(true);
        final AlertDialog Dialog = alertDialogBuilder.create();
        Dialog.show();

        if(measureTimer == null)
            measureTimer = new Timer();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                measureStop();
                Dialog.dismiss();
            }
        }, measureTime);

        measureStart();
    }

    private void measureStart(){
        measureTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                wifiManager.connectRtt(saveFile);
            }
        }, 0, interval);
    }

    private void measureStop(){
        if(measureTimer != null){
            measureTimer.cancel();
            measureTimer = null;
        }
    }

    //storage管理のためのメソッド
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

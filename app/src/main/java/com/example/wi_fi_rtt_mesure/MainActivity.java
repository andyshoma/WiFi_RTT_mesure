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
import android.view.ViewDebug;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Context context;
    private Handler handler;
    private boolean flag = false;
    private final static int measureTime = 180000;
    private final static int interval = 2000;
    private Timer measureTimer;
    private WifiManager wifiManager;

    private boolean calOrDis;
    private SaveFile saveFile;
    private String filename = "testdata.csv";
    private String filename_cal = "calibration.csv";
    private String path;
    private String ID;

    public TextView result_distance;
    private TextView counter;
    private ArrayAdapter adapter;

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //xmlのデータを選択
        TextView aware_check = findViewById(R.id.aware_check);
        TextView rtt_check = findViewById(R.id.rtt_check);
        TextView aware_avail = findViewById(R.id.aware_avail);
        TextView rtt_avail = findViewById(R.id.rtt_avail);
        TextView id_view = findViewById(R.id.ID);
        result_distance = findViewById(R.id.result_distance);
        counter = findViewById(R.id.counter);
        Button reset = findViewById(R.id.reset);
        Button cal_button = findViewById(R.id.calibration);
        Button pub_button = findViewById(R.id.publish);
        Button sub_button = findViewById(R.id.subscribe);
        Button rtt_button = findViewById(R.id.wifi_rtt);
        Spinner peerSpinner = findViewById(R.id.peerSpinner);

        context = getApplicationContext();

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        saveFile = new SaveFile(path, context);
        verifyStoragePermissions(this);
        File dir = new File(path);
        dir.mkdir();

        wifiManager = new WifiManager(context, handler, peerSpinner);

        wifiManager.check(aware_check, rtt_check);
        wifiManager.available(aware_avail, rtt_avail);
        wifiManager.getTextView(result_distance, counter);

        id_view.setText("ID：" + wifiManager.getID());

        cal_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calOrDis = true;
                saveFile.setFilename(filename_cal);
                saveFile.write("distanceMm,distanceStdDevMm,rssi,timestamp");
                CreateDialog();
            }
        });

        pub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.publisher();
                //wifiManager.subscriber();
            }
        });

        sub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.subscriber();
                //wifiManager.publisher();
            }
        });

        rtt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                calOrDis = false;
                saveFile.setFilename(filename);
                saveFile.write("ID,distanceMm,distanceStdDevMm,rssi,timestamp");
                CreateDialog();
            }
        });

        reset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                wifiManager.resetCounter();
                Toast.makeText(context, "reset now", Toast.LENGTH_SHORT).show();
            }
        });

        peerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                ID = (String)spinner.getSelectedItem();
                System.out.println(ID);
                filename_cal = "calibration_" + ID + ".csv";
                wifiManager.selectID(ID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void CreateDialog(){
        if(calOrDis == true){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                    .setTitle("calibrating now")
                    .setMessage(measureTime/1000 + "秒待ってください...\n" + path);
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
        }else{
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                    .setTitle("now collecting")
                    .setMessage(measureTime/1000 + "秒待ってください...\n" + path);
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
        }

        measureStart();
    }

    /*private void measureStart(){
        measureTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(calOrDis == true)
                    wifiManager.getCalibrationdData(saveFile);
                else
                    wifiManager.connectRtt(saveFile);
            }
        }, 0, interval);
    }*/

    private void measureStart(){
        measureTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(calOrDis == true)
                    wifiManager.getCalibrationdData(saveFile);
                else
                    wifiManager.connectRtt(saveFile);
            }
        }, 0, interval);
    }

    private void measureStop(){
        if(measureTimer != null){
            measureTimer.cancel();
            measureTimer = null;
            counter.setText(wifiManager.count_success + "/" + wifiManager.countTry + "回");
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

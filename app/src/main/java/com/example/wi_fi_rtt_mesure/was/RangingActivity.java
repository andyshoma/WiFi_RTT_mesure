package com.example.wi_fi_rtt_mesure.was;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.SaveFile;

import java.util.Timer;
import java.util.TimerTask;

public class RangingActivity extends AppCompatActivity {

    Context context;
    private Handler handler;
    private boolean flag = false;
    private final static int measureTime = 300000;
    private final static int interval = 5000;
    private Timer measureTimer;
    private WifiManager wifiManager;

    private SaveFile saveFile;
    private boolean calOrDis;
    private String path;
    private TextView counter;

    private String mode;

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);

        context = getApplicationContext();

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        saveFile = new SaveFile(path, context);
        verifyStoragePermissions(this);

        Intent intent = this.getIntent();

        String deviceID = intent.getStringExtra("DEVICEID");
        TextView deviceID_text = findViewById(R.id.deviceId);
        deviceID_text.setText(deviceID);

        mode = intent.getStringExtra("AWARE");
        TextView mode_text = findViewById(R.id.mode);
        mode_text.setText(mode);

        TextView result_distance = findViewById(R.id.result_distance);

        final EditText file_text = findViewById(R.id.filename);

        Button back_button = findViewById(R.id.back);
        Button done_button = findViewById(R.id.done);
        Button start_button = findViewById(R.id.start);
        Button stop_button = findViewById(R.id.stop);

        Spinner peerSpinner = findViewById(R.id.peerSpinner);

        wifiManager = new WifiManager(context, handler, deviceID, peerSpinner);

        //distanceの表示のためのTextViewの追加
        wifiManager.setText(result_distance);

        /***
         * backボタンを押した時
         *      awaresessionを閉じてmainに戻る
         */
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.close();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        /***
         * doneボタンを押した時
         *      filenameをセットする
         */
        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(file_text.getText() != null) {
                    /*
                    モード選択結果の反映
                    publishもしくはsubscribe
                    */
                    if(mode.equals("publish")){
                        wifiManager.publisher();
                    }else if(mode.equals("subscribe")){
                        wifiManager.subscriber();
                    }
                    String filename = file_text.getText().toString();
                    saveFile.setFilename(filename);
                    saveFile.write("distanceMm,distanceStdDevMm,rssi,timestamp");
                }
            }
        });

        /***
         * startボタンを押した時
         *      rangingをstartさせる
         */
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.checkRanging()) {
                    wifiManager.makeRequest();
                    CreateDialog();
                }
            }
        });

        /***
         * stopボタンを押した時
         *      rangingをstop(一時停止)させる
         */
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        /***
         * spinnerの登録
         *      通信相手の端末番号の追加用
         */
        peerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                String ID = (String)spinner.getSelectedItem();
                wifiManager.selectID(ID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void CreateDialog(){

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

        measureStart();

    }

    private void measureStart(){
        measureTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                wifiManager.ranging(saveFile);
            }
        }, 0, interval);
    }

    private void measureStop(){
        if(measureTimer != null){
            measureTimer.cancel();
            measureTimer = null;
            //counter.setText(wifiManager.count_success + "/" + wifiManager.countTry + "回");
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

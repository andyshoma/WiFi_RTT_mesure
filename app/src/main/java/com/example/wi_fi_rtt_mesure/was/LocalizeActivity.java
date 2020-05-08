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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.SaveFile;

import java.util.Timer;
import java.util.TimerTask;

public class LocalizeActivity extends AppCompatActivity {

    public Context context;
    public WifiManager wifiManager;
    public SaveFile saveFile;

    public String deviceID;
    public String path;
    private Handler handler;
    private final static int measureTime = 60000;
    private final static int interval = 10000;
    private Timer measureTimer;

    private Integer rp_num;


    public EditText file_edit;
    public Spinner peerSpinner;
    public TextView counter;
    public String mode;
    public EditText rp_edit;

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localize);

        context = getApplicationContext();

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        saveFile = new SaveFile(path, context);
        verifyStoragePermissions(this);

        Intent intent = this.getIntent();
        deviceID = intent.getStringExtra("DEVICEID");
        TextView deviceID_text = findViewById(R.id.deviceId);
        deviceID_text.setText(deviceID);

        mode = intent.getStringExtra("AWARE");
        TextView mode_text = findViewById(R.id.mode);
        mode_text.setText(mode);

        Button back_button = findViewById(R.id.back);
        Button done_button = findViewById(R.id.done);
        Button start_button = findViewById(R.id.start);
        Button stop_button = findViewById(R.id.stop);

        counter = findViewById(R.id.counter);

        file_edit = findViewById(R.id.filename);

        TextView range_text = findViewById(R.id.result_distance);

        peerSpinner = findViewById(R.id.peerSpinner);

        rp_edit = findViewById(R.id.rp_num);

        wifiManager = new WifiManager(context, handler, deviceID, peerSpinner);

        //distanceの表示のためのTextViewの追加
        wifiManager.setText(range_text);

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(file_edit.getText() != null) {
                    /*
                    モード選択結果の反映
                    publishもしくはsubscribe
                    */
                    if(mode.equals("publish")){
                        wifiManager.publisher();
                    }else if(mode.equals("subscribe")){
                        wifiManager.subscriber();
                    }
                    String filename = file_edit.getText().toString();
                    saveFile.setFilename(filename);
                    saveFile.write("distanceMm,distanceStdDevMm,rssi,timestamp,deviceID");
                }
            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rp_num != null){
                    saveFile.write(String.valueOf(rp_num));
                    if (wifiManager.checkRanging()) {
                        CreateDialog();
                    }
                }else{
                    Toast.makeText(context, "観測番号が入力されていません", Toast.LENGTH_SHORT).show();
                }

            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        rp_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try{
                    rp_num = Integer.parseInt(s.toString());
                }catch (NumberFormatException e){
                    Toast.makeText(context, "数字を入力してください", Toast.LENGTH_SHORT).show();
                }
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

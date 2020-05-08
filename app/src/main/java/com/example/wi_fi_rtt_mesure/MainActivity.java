package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.sta.StaActivity;
import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;

import java.io.File;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WifiRttApplication application;
    private Context context;
    private Handler handler;

    private TextView aware_check;
    private TextView aware_avail;
    private TextView rtt_check;
    private TextView rtt_avail;

    private EditText deviceID;

    private AwareManager awareManager;
    private RttManager rttManager;

//    Context context;
//    private Handler handler;
//    private boolean flag = false;
//    private final static int measureTime = 180000;
//    private final static int interval = 2000;
//    private Timer measureTimer;
//    private WifiManager wifiManager;
//
//    private boolean calOrDis;
//    private SaveFile saveFile;
//    private String filename = "testdata.csv";
//    private String filename_cal = "calibration.csv";
//    private String path;
//    private String ID;
//    private String aware;
//
//    public TextView result_distance;
//    private TextView counter;
//    private ArrayAdapter adapter;


    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = (WifiRttApplication)getApplication();
        context = getApplicationContext();

        aware_check = findViewById(R.id.aware_check);
        aware_avail = findViewById(R.id.aware_avail);

        rtt_check = findViewById(R.id.rtt_check);
        rtt_avail = findViewById(R.id.rtt_avail);

        Button ap_button = findViewById(R.id.ap);
        ap_button.setOnClickListener(this);

        Button sta_button = findViewById(R.id.sta);
        sta_button.setOnClickListener(this);

        deviceID = findViewById(R.id.deviceID);

        mkdir();

        awareManager = application.getAwareManager();
        rttManager = application.getRttManager();

        awareCheck();
        rttCheck();

//        //xmlのデータを選択
//        TextView aware_check = findViewById(R.id.aware_check);
//        TextView rtt_check = findViewById(R.id.rtt_check);
//        TextView aware_avail = findViewById(R.id.aware_avail);
//        TextView rtt_avail = findViewById(R.id.rtt_avail);
//
//        Button pub_button = findViewById(R.id.publish);
//        Button sub_button = findViewById(R.id.subscribe);
//        Button ranging_button = findViewById(R.id.ranging);
//        Button localize_button = findViewById(R.id.localize);
//
//        final EditText deviceID = findViewById(R.id.deviceID);
//
//        context = getApplicationContext();
//
//        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
//        File dir = new File(path);
//        dir.mkdir();
//
//        wifiManager = new WifiManager(context, handler);
//
//        wifiManager.check(aware_check, rtt_check);
//        wifiManager.available(aware_avail, rtt_avail);
//
//        pub_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                aware = "publish";
//            }
//        });
//
//        sub_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                aware = "subscribe";
//            }
//        });
//
//        ranging_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v){
//                Intent intent = new Intent(context, RangingActivity.class);
//                if(deviceID.getText() != null) {
//                    String str = deviceID.getText().toString();
//                    intent.putExtra("DEVICEID", str);
//                }
//                if(aware != null){
//                    wifiManager.close();
//                    intent.putExtra("AWARE", aware);
//                    startActivity(intent);
//                }
//            }
//        });
//
//        localize_button.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v){
//                Intent intent = new Intent(context, LocalizeActivity.class);
//
//                if(deviceID.getText() != null) {
//                    // 端末番号が入力されている場合
//                    String str = deviceID.getText().toString();
//                    intent.putExtra("DEVICEID", str);
//                }
//                if(aware != null){
//                    // Wi-Fi Aware設定がされている場合
//                    wifiManager.close();
//                    intent.putExtra("AWARE", aware);
//                    startActivity(intent);
//                }
//            }
//        });
    }

    @Override
    public void onClick(View view) {
        String id = deviceID.getText().toString();

        if (id.equals("")){
            Toast.makeText(context, "端末番号を入力してください", Toast.LENGTH_SHORT).show();
            return;
        } else if (id.length() != 6) {
            Toast.makeText(context, "端末番号が正しくありません", Toast.LENGTH_SHORT).show();
            return;
        }

        if (view.getId() == R.id.ap) {
            // 基地局ボタン（publish）が押された場合
            onCallAp(id);

        }else if (view.getId() == R.id.sta) {
            // 移動端末ボタン（subscribe）が押された場合
            onCallSta(id);

        }
    }

    @Override
    public void onBackPressed() {
        // 戻るボタンを無効化
    }

    /**
     * 基地局ボタンが押されたときに呼び出されるメソッド
     * @param id
     */
    private void onCallAp(String id) {
        Intent intent = new Intent(context, ApActivity.class);
        intent.putExtra("DEVICE_ID", id);
        startActivity(intent);
    }

    /**
     * 移動端末ボタンが押された時に呼び出されるメソッド
     * @param id
     */
    private void onCallSta(String id) {
        Intent intent = new Intent(context, StaActivity.class);
        intent.putExtra("DEVICE_ID", id);
        startActivity(intent);
    }

    /**
     * ディレクトリの作成
     */
    private void mkdir() {
        String dirpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        File dir = new File(dirpath);
        dir.mkdir();
    }

    /**
     * Wi-Fi Awareに関するチェック
     */
    private void awareCheck() {
        if (awareManager.canUsed()) {
            aware_check.setText("True");
        } else {
            aware_check.setText("False");
        }

        if (awareManager.isAvailable()) {
            aware_avail.setText("True");
        } else {
            aware_avail.setText("False");
        }
    }

    /**
     * WI-Fi RTTに関するチェック
     */
    private void rttCheck() {
        if (rttManager.canUse()) {
            rtt_check.setText("True");
        } else {
            rtt_check.setText("False");
        }

        if (rttManager.isAvailable()) {
            rtt_avail.setText("True");
        } else {
            rtt_avail.setText("False");
        }
    }

}

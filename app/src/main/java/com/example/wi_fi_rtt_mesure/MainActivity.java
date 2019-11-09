package com.example.wi_fi_rtt_mesure;

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
import android.view.ViewDebug;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private String aware;

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

        Button pub_button = findViewById(R.id.publish);
        Button sub_button = findViewById(R.id.subscribe);
        Button ranging_button = findViewById(R.id.ranging);
        Button localize_button = findViewById(R.id.localize);

        final EditText deviceID = findViewById(R.id.deviceID);

        context = getApplicationContext();

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/wifirtt/";
        File dir = new File(path);
        dir.mkdir();

        wifiManager = new WifiManager(context, handler);

        wifiManager.check(aware_check, rtt_check);
        wifiManager.available(aware_avail, rtt_avail);

        pub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aware = "publish";
            }
        });

        sub_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aware = "subscribe";
            }
        });

        ranging_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(context, RangingActivity.class);
                if(deviceID.getText() != null) {
                    String str = deviceID.getText().toString();
                    intent.putExtra("DEVICEID", str);
                }
                if(aware != null){
                    wifiManager.close();
                    intent.putExtra("AWARE", aware);
                    startActivity(intent);
                }
            }
        });

        localize_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                wifiManager.close();
                Intent intent = new Intent(context, LocalizeActivity.class);
                startActivity(intent);
            }
        });
    }
}

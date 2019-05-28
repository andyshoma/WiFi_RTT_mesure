package com.example.wi_fi_rtt_mesure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Context context;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView aware_check = findViewById(R.id.aware_check);
        TextView rtt_check = findViewById(R.id.rtt_check);
        TextView aware_avail = findViewById(R.id.aware_avail);
        TextView rtt_avail = findViewById(R.id.rtt_avail);
        Button check_button = findViewById(R.id.check_permission);
        Button pub_button = findViewById(R.id.publish);
        Button sub_button = findViewById(R.id.subscribe);
        Button rtt_button = findViewById(R.id.wifi_rtt);

        context = getApplicationContext();

        final WifiManager wifiManager = new WifiManager(context, handler);

        wifiManager.check(aware_check, rtt_check);
        wifiManager.available(aware_avail, rtt_avail);

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
                wifiManager.connectRtt();
            }
        });
    }
}

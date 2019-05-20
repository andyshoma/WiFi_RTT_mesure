package com.example.wi_fi_rtt_mesure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.rtt.WifiRttManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    Context context;
    WifiAwareManager wifiAwareManager;
    WifiRttManager wifiRttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)){
            System.out.println("Android Wifi supports Wi-Fi Aware : True");
        }else{
            System.out.println("Android Wifi supports Wi-Fi Aware : False");
        }

        wifiAwareManager = (WifiAwareManager)context.getSystemService(Context.WIFI_AWARE_SERVICE);
        wifiRttManager = (WifiRttManager)context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);

        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wifiAwareManager.isAvailable()) {
                    System.out.println("Wi-Fi Aware is available : True");
                } else {
                    System.out.println("Wi-Fi Aware is available : False");
                }

                if (wifiRttManager.isAvailable()) {
            …
                } else {
            …
                }
            }
        };
        context.registerReceiver(myReceiver, filter);

        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)){
            System.out.println("Android Wifi supports RTT : True");
        }else{
            System.out.println("Android Wifi supports RTT : False");
        }

    }
}

package com.example.wi_fi_rtt_mesure.sta;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.aware.PeerHandle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.WifiRttApplication;
import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;

import java.util.List;

public class StaActivity extends AppCompatActivity{

    private Context context;
    private WifiRttApplication application;

    private AwareManager awareManager;
    private RttManager rttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sta);

        context = getApplicationContext();
        application = (WifiRttApplication)getApplication();

        Intent intent = this.getIntent();
        String deviceID = intent.getStringExtra("DEVICE_ID");

        ConnectFragment fragment = ConnectFragment.newInstance(deviceID);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, fragment);
        transaction.commit();

        awareManager = application.getAwareManager();
        rttManager = application.getRttManager();
    }

    @Override
    public void onBackPressed() {
        // Androidの戻るボタンが押された時
        awareManager.close();
        rttManager.delete();
        finish();
    }

}
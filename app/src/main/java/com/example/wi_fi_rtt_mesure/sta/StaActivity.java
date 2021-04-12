package com.example.wi_fi_rtt_mesure.sta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.aware.PeerHandle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

    private final static int CREATE_DOCUMENT_REQUEST = 43;
    private final static String TAG = "MainActivity.java";

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == CREATE_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData.getData() != null) {

                final int takeFlags = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Uri uri = resultData.getData();
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                application.setUri(uri);
                Log.d(TAG, uri.toString());

                Toast.makeText(context, "ファイルを作成しました", Toast.LENGTH_SHORT).show();

            }
        }
    }

}
package com.example.wi_fi_rtt_mesure;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.wifi.AwareManager;

public class ApActivity extends AppCompatActivity implements View.OnClickListener{

    private Context context;
    private WifiRttApplication application;
    private Handler handler;

    private AwareManager awareManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ap);

        context = getApplicationContext();
        application = (WifiRttApplication)getApplication();

        Intent intent = this.getIntent();
        String deviceID = intent.getStringExtra("DEVICE_ID");

        TextView title = findViewById(R.id.deviceID);
        title.setText(deviceID);

        Button pub_button = findViewById(R.id.publish);
        pub_button.setOnClickListener(this);
        Button close_button = findViewById(R.id.close);
        close_button.setOnClickListener(this);
        Button reconnect_button = findViewById(R.id.reconnect);
        reconnect_button.setOnClickListener(this);

        awareManager = application.getAwareManager();
        awareManager.connect();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.publish) {
            // publishボタンが押された時
            awareManager.publish();
            Toast.makeText(context, "publish開始しました", Toast.LENGTH_SHORT).show();
        }else if (view.getId() == R.id.close) {
            //closeボタンが押された時
            awareManager.close();
        }else if (view.getId() == R.id.reconnect) {
            // reconnectボタンが押された時
            awareManager.reconnect();
        }
    }

    @Override
    public void onBackPressed() {
        // Androidの戻るボタンが押された時
        awareManager.close();
        finish();
    }
}

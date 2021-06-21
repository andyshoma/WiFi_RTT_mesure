package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.Magnetic.MagneticActivity;
import com.example.wi_fi_rtt_mesure.sta.StaActivity;
import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int CREATE_DOCUMENT_REQUEST = 43;
    private final static String TAG = "MainActivity.java";

    private final static String TRUE = "True";
    private final static String FALSE = "False";

    private WifiRttApplication application;
    private Context context;
    private Handler handler;

    private TextView support_check;
    private TextView wifi_avail;

    private EditText deviceID;

    private AwareManager awareManager;
    private RttManager rttManager;

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

        support_check = findViewById(R.id.support_check);
        wifi_avail = findViewById(R.id.rtt_avail);

        Button ap_button = findViewById(R.id.ap);
        ap_button.setOnClickListener(this);

        Button sta_button = findViewById(R.id.sta);
        sta_button.setOnClickListener(this);

        Button mag_button = findViewById(R.id.magnetic);
        mag_button.setOnClickListener(this);

        deviceID = findViewById(R.id.deviceID);

        awareManager = application.getAwareManager();
        rttManager = application.getRttManager();

        supportCheck();
    }

    @Override
    public void onClick(View view) {

        String id = deviceID.getText().toString();
        if (id.equals("")){
            Toast.makeText(context, "端末番号を入力してください", Toast.LENGTH_SHORT).show();
            return;
        } else if (id.length() != 6) {
            Toast.makeText(context, "6桁の数字を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        awareManager.setDeviceId(id);

        if (view.getId() == R.id.ap) {
            // 基地局ボタン（publish）が押された時
            onCallAp(id);

        } else if (view.getId() == R.id.sta) {
            // 移動端末ボタン（subscribe）が押された時
            onCallSta(id);

        } else if (view.getId() == R.id.magnetic) {
            // 地磁気ボタンが押された時
            onCallMagnetic(id);

        }
    }

    @Override
    public void onBackPressed() {
        // 戻るボタンを無効化
    }

    /**
     * 基地局ボタンが押されたときに呼び出されるメソッド
     * @param id 端末番号
     */
    private void onCallAp(String id) {
        Intent intent = new Intent(context, ApActivity.class);
        intent.putExtra("DEVICE_ID", id);
        startActivity(intent);
    }

    /**
     * 移動端末ボタンが押された時に呼び出されるメソッド
     * @param id 端末番号
     */
    private void onCallSta(String id) {
        Intent intent = new Intent(context, StaActivity.class);
        intent.putExtra("DEVICE_ID", id);
        startActivity(intent);
    }

    /**
     * 地磁気ボタンが押された時に呼び出されるメソッド
     */
    private void onCallMagnetic(String id) {
        Intent intent = new Intent(context, MagneticActivity.class);
        intent.putExtra("DEVICE_ID", id);
        startActivity(intent);
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

    /**
     * Wi-Fi AwareとWi-Fi RTTに関するチェック
     */
    private void supportCheck() {

        if (awareManager.canUsed() && rttManager.canUse()) {
            support_check.setText(TRUE);
        } else {
            support_check.setText(FALSE);
        }

        if (awareManager.isAvailable() && rttManager.isAvailable()) {
            wifi_avail.setText(TRUE);
        } else {
            wifi_avail.setText(FALSE);
        }

    }

}

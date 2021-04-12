package com.example.wi_fi_rtt_mesure.Magnetic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.MapsActivity;
import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.WifiRttApplication;

import java.io.FileOutputStream;
import java.io.IOException;

public class MagneticActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener  {

    private WifiRttApplication application;
    private Context context;
    private Activity activity;

    private String deviceID;
    private int mapFloar = 5;

    private int dataID = 0;

    private TextView XaxisText;
    private TextView YaxisText;
    private TextView ZaxisText;

    private TextView magXaxisText;
    private TextView magYaxisText;
    private TextView magZaxisText;

    private Button cc1fButton;
    private Button cc5fButton;

    private boolean save_flag = false;

    private SensorManager sensorManager;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic);

        application = (WifiRttApplication)getApplication();
        context = getApplicationContext();
        activity = this;

        Intent intent = this.getIntent();
        deviceID = intent.getStringExtra("DEVICE_ID");

        XaxisText = findViewById(R.id.yaw);
        YaxisText = findViewById(R.id.pitch);
        ZaxisText = findViewById(R.id.roll);

        magXaxisText = findViewById(R.id.mag_yaw);
        magYaxisText = findViewById(R.id.mag_pitch);
        magZaxisText = findViewById(R.id.mag_roll);

        cc1fButton = findViewById(R.id.cc1f);
        cc1fButton.setOnClickListener(this);

        cc5fButton = findViewById(R.id.cc5f);
        cc5fButton.setOnClickListener(this);

        Button selectButton = findViewById(R.id.point_select);
        selectButton.setOnClickListener(this);

        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(this);

        Button stopButton = findViewById(R.id.stop);
        stopButton.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }

        timestamp = event.timestamp;

        if (accelerometerReading != null && magnetometerReading != null) {
            updateOrientationAngles();
        }
    }

    /**
     * センサの値が変わった時のセンサの出力
     */
    public void updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        double[] orientation = new double[3];

        orientation[0] = orientationAngles[0] * 180.0 / Math.PI;
        orientation[1] = orientationAngles[1] * 180.0 / Math.PI;
        orientation[2] = orientationAngles[2] * 180.0 / Math.PI;

        XaxisText.setText(String.valueOf(orientation[0]));
        YaxisText.setText(String.valueOf(orientation[1]));
        ZaxisText.setText(String.valueOf(orientation[2]));

        magXaxisText.setText(String.valueOf(magnetometerReading[0]));
        magYaxisText.setText(String.valueOf(magnetometerReading[1]));
        magZaxisText.setText(String.valueOf(magnetometerReading[2]));

        if (save_flag) {
            String text = String.valueOf(dataID) + ',' + orientation[0] + ',' + orientation[1] + ','
                    + orientation[2] + ',' + magnetometerReading[0] + ','
                    + magnetometerReading[1] + ',' + magnetometerReading[2] + ','
                    + timestamp + '\n';
            save(application.getUri(), text);
            dataID++;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cc1f) {
            onCallCC1F();

        } else if (view.getId() == R.id.cc5f) {
            onCallCC5F();

        } else if (view.getId() == R.id.point_select) {
            onCallSelect();

        } else if (view.getId() == R.id.start) {
            onCallStart();

        } else if (view.getId() == R.id.stop) {
            onCallStop();

        }
    }

    /**
     * ファイルへの書き込み
     * @param uri URI
     * @param text 書き込む内容
     */
    private void save(Uri uri, String text) {
        try {
            ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "wa");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(text.getBytes());
            pfd.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void onCallCC1F() {
        if (mapFloar != 1) {
            mapFloar = 1;
            cc1fButton.setBackgroundColor(Color.rgb(0, 134, 171));
            cc5fButton.setBackgroundColor(Color.rgb(151, 211, 227));
        }
    }

    private void onCallCC5F() {
        if (mapFloar != 5) {
            mapFloar = 5;
            cc5fButton.setBackgroundColor(Color.rgb(0, 134, 171));
            cc1fButton.setBackgroundColor(Color.rgb(151, 211, 227));
        }
    }

    /**
     * point selectボタンが押されたとき
     */
    private void onCallSelect() {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.putExtra("map", mapFloar);
        startActivity(intent);
    }

    /**
     * startボタンが押されたとき
     */
    private void onCallStart() {
        Toast.makeText(context, "ファイルへの出力を開始します", Toast.LENGTH_SHORT).show();
        String text = "id" + ',' + "yaw" + ',' + "pitch" + ',' + "roll" + ',' +
                "magX" + ',' + "magY" + ',' + "magZ" + "timestamp" + '\n';
        save(application.getUri(), text);
        save_flag = true;
    }

    /**
     * stopボタンが押されたとき
     */
    private void onCallStop() {
        save_flag = false;
        Toast.makeText(context, "ファイルへの出力を終了しました", Toast.LENGTH_SHORT).show();
    }

}

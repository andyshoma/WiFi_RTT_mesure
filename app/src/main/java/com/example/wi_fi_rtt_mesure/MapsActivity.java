package com.example.wi_fi_rtt_mesure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, SensorEventListener {

    private WifiRttApplication application;
    private Context context;
    private Activity activity;

    private int dataID = 0;

    private GoogleMap mMap;

    private final static double CC_LATITUDE = 34.97977501;
    private final static double CC_LONGITUDE = 135.96373915;
    private final static int DEFAULT_CAMERA_ZOOM_CC = 19;

    private final static float CC_MAP_ANCHOR_BEARING = 3;
    private final static double CC_MAP_ANHCOR_LATITUDE = 34.979389;
    private final static double CC_MAP_ANHCOR_LONGITUDE = 135.963716;
    private final static float CC_MAP_ANHCOR_WIDTH = 101.385f;
    private final static float CC_MAP_ANHCOR_HEIGHT = 43.795f;

    private GroundOverlay overlay;

    private int floar;

    private boolean save_flag = false;

    private SensorManager sensorManager;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float timestamp;

    private LatLng location = new LatLng(0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        application = (WifiRttApplication)getApplication();
        context = getApplicationContext();
        activity = this;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Intent intent = this.getIntent();
        floar = intent.getIntExtra("map", 5);

        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(this);

        Button stopButton = findViewById(R.id.stop);
        stopButton.setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

        if (save_flag) {
            String text = String.valueOf(dataID) + ',' + orientation[0] + ',' + orientation[1] + ','
                    + orientation[2] + ',' + magnetometerReading[0] + ','
                    + magnetometerReading[1] + ',' + magnetometerReading[2] + ','
                    + location.latitude + ',' + location.longitude + ','
                    + timestamp + '\n';
            save(application.getUri(), text);
            dataID++;
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start) {
            onCallStart();

        } else if (view.getId() == R.id.stop) {
            onCallStop();

        }
    }

    private void onCallStart() {

        if (application.getUri() != null) {
            application.getUri();
            Toast.makeText(context, "ファイルへの出力を開始します", Toast.LENGTH_SHORT).show();
            String text = "id" + ',' + "yaw" + ',' + "pitch" + ',' + "roll" + ',' +
                    "magX" + ',' + "magY" + ',' + "magZ" + ',' + "latitude" + ',' + "longitude" + ',' + "timestamp" + '\n';
            save(application.getUri(), text);
            save_flag = true;
        }else {
            Toast.makeText(context, "保存するファイルを作成してください", Toast.LENGTH_SHORT).show();
        }
    }

    private void onCallStop() {
        save_flag = false;
        Toast.makeText(context, "ファイルへの出力を終了しました", Toast.LENGTH_SHORT).show();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        if (floar == 1) {
            setCC1FMap();
        } else if (floar == 5) {
            setCC5FMap();
        } else {
            setCC5FMap();
        }

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                location = new LatLng(latLng.latitude, latLng.longitude);
                googleMap.addMarker(new MarkerOptions().position(location));
            }
        });

        // Add a marker in Sydney and move the camera
    }

    private void setCC5FMap() {
        GroundOverlayOptions options = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.floormap_cc5f_uc))
                .bearing(CC_MAP_ANCHOR_BEARING)
                .position(new LatLng(CC_MAP_ANHCOR_LATITUDE, CC_MAP_ANHCOR_LONGITUDE),
                        CC_MAP_ANHCOR_WIDTH, CC_MAP_ANHCOR_HEIGHT)
                .anchor(0, 1);
        if (overlay != null) {
            overlay.remove();
        }
        overlay = mMap.addGroundOverlay(options);
        setCameraPosition(CC_LATITUDE, CC_LONGITUDE, DEFAULT_CAMERA_ZOOM_CC);
    }

    private void setCC1FMap() {
        GroundOverlayOptions options = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.floormap_cc1f))
                .bearing(CC_MAP_ANCHOR_BEARING)
                .position(new LatLng(CC_MAP_ANHCOR_LATITUDE, CC_MAP_ANHCOR_LONGITUDE),
                        CC_MAP_ANHCOR_WIDTH, CC_MAP_ANHCOR_HEIGHT)
                .anchor(0, 1);
        if (overlay != null) {
            overlay.remove();
        }
        overlay = mMap.addGroundOverlay(options);
        setCameraPosition(CC_LATITUDE, CC_LONGITUDE, DEFAULT_CAMERA_ZOOM_CC);
    }

    private void setCameraPosition(double latitude, double longitude, float zoom) {
        CameraPosition nowCamera = mMap.getCameraPosition();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition(
                        new LatLng(latitude, longitude),
                        zoom,
                        nowCamera.tilt,
                        nowCamera.bearing
                ))
        );
    }
}

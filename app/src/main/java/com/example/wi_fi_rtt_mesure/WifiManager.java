package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;

import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class WifiManager {

    public Context context;
    private final WifiAwareManager wifiAwareManager;
    private final WifiRttManager wifiRttManager;
    private WifiAwareSession awareSession;
    private Handler mHandler;
    private final Executor executor;
    public static final String AWARE_SERVICE_NAME = "Aware";
    private SubscribeDiscoverySession subsession;
    private PublishDiscoverySession pubsession;

    private List<RangingResult> rangingResults;
    private TextView result_distance;
    private TextView counter;
    private int count_success = 0;

    //補正に使う一次関数の傾き(a)と切片(b)
    private double a = 0.85;
    private double b = -1354;

    public PeerHandle peerHandle = null;

    public WifiManager(Context context, Handler handler) {
        this.context = context;
        wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);
        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mHandler = handler;
        executor = context.getMainExecutor();

        wifiAwareManager.attach(new myAttachCallback(), mHandler);
    }

    class myAttachCallback extends AttachCallback {

        public void onAttachFailed() {
            super.onAttachFailed();
            Toast.makeText(context, "onAttachFailed", Toast.LENGTH_SHORT).show();
        }

        public void onAttached(WifiAwareSession session) {
            super.onAttached(session);
            Toast.makeText(context, "onAttach", Toast.LENGTH_SHORT).show();
            awareSession = session;
        }
    }

    public void publisher(){
        PublishConfig config = new PublishConfig.Builder()
                .setServiceName(AWARE_SERVICE_NAME)
                .build();

        if(awareSession != null) {
            awareSession.publish(config, new DiscoverySessionCallback() {
                @Override
                public void onPublishStarted(PublishDiscoverySession session) {
                    super.onPublishStarted(session);
                    Toast.makeText(context, "Service published! Waiting for Subscriber", Toast.LENGTH_SHORT).show();
                    WifiManager.this.pubsession = session;
                }
                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    super.onMessageReceived(peerHandle, message);
                    Toast.makeText(context, "Message Received" + message, Toast.LENGTH_SHORT).show();
                    WifiManager.this.peerHandle = peerHandle;
                }
            }, null);
        }
    }

    public void subscriber(){
        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName(AWARE_SERVICE_NAME)
                .build();

        if(awareSession != null) {
            awareSession.subscribe(config, new DiscoverySessionCallback() {
                @Override
                public void onSubscribeStarted(SubscribeDiscoverySession session) {
                    super.onSubscribeStarted(session);
                    WifiManager.this.subsession = session;
                    Toast.makeText(context, "Subscriber started", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                    Toast.makeText(context, "Service discoverd" + peerHandle.toString() + "\t sending message now", Toast.LENGTH_SHORT).show();
                    String msg = "hello";
                    subsession.sendMessage(peerHandle, 1, msg.getBytes());
                    //WifiManager.this.peerHandle = peerHandle;
                }
            }, null);

        }
    }

    public void check(TextView aware, TextView rtt) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            aware.setText("True");
        } else {
            aware.setText("False");
        }
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            rtt.setText("True");
        } else {
            rtt.setText("False");
        }
    }


    public void available(final TextView aware, final TextView rtt){
        if (wifiAwareManager.isAvailable()) {
            aware.setText("True");
        } else {
            aware.setText("False");
        }

        if (wifiRttManager.isAvailable()) {
            rtt.setText("True");
        } else {
            rtt.setText("False");
        }

        IntentFilter aware_filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        BroadcastReceiver aware_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wifiAwareManager.isAvailable()) {
                    aware.setText("True");
                } else {
                    aware.setText("False");
                }
            }
        };
        context.registerReceiver(aware_Receiver, aware_filter);

        IntentFilter rtt_filter = new IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
        BroadcastReceiver rtt_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wifiRttManager.isAvailable()) {
                    rtt.setText("True");
                } else {
                    rtt.setText("False");
                }
            }
        };
        context.registerReceiver(rtt_Receiver, rtt_filter);
    }

    public void connectRtt(final SaveFile saveFile) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("check", "ACCESS_FINE_LOCATION permission OK");
        } else {
            Log.d("check", "ACCESS_FINE_LOCATION permission NG");
            return;
        }

        RangingRequest.Builder builder = new RangingRequest.Builder();
        builder.addWifiAwarePeer(peerHandle);

        RangingRequest request = builder.build();

        wifiRttManager.startRanging(request, executor, new RangingResultCallback() {

            @Override
            public void onRangingFailure(int code) {
                Log.d("rtt_failure", "onRangingFailure" + code);
                //rangingResults = new ArrayList<RangingResult>();
            }

            @Override
            public void onRangingResults(List<RangingResult> results) {
                Log.v("rangingresults", "onRangingResults : " + results);
                rangingResults = results;
                if (rangingResults.get(0).getStatus() == RangingResult.STATUS_SUCCESS) {
                    //count_success++;
                    //Toast.makeText(context, "success!", Toast.LENGTH_SHORT).show();
                    result_distance.setText(rangingResults.get(0).getDistanceMm() + " Mm");
                    //counter.setText(count_success);
                    String str = rangingResults.get(0).getDistanceMm() + "," + rangingResults.get(0).getDistanceStdDevMm() + "," + rangingResults.get(0).getRssi() + "," + rangingResults.get(0).getRangingTimestampMillis();
                    saveFile.write(str);
                }else{
                    Toast.makeText(context, "failure", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void check_permission(){
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("check", "ACCESS_FINE_LOCATION permission OK");
        } else {
            Log.d("check", "ACCESS_FINE_LOCATION permission NG");
            return;
        }
        if(pubsession != null){
            pubsession.close();
            Toast.makeText(context, "pubsession closed", Toast.LENGTH_SHORT).show();
        }
        if(subsession != null){
            subsession.close();
            Toast.makeText(context, "subsession closed", Toast.LENGTH_SHORT).show();
        }
        //awareSession.close();
    }

    public void getTextView(TextView result_distance, TextView counter){
        this.result_distance = result_distance;
        this.counter = counter;
    }

    private int function(float x){
        double y;

        y = a*x + b;
        return (int)y;
    }

    public void resetCounter(){
        this.count_success = 0;
        awareSession.close();
    }
}

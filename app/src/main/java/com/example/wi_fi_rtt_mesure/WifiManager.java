package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.AttachCallback;
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
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.Executor;

public class WifiManager {

    public Context context;
    private final WifiAwareManager wifiAwareManager;
    private final WifiRttManager wifiRttManager;
    private final IntentFilter filter;
    private WifiAwareSession awareSession;
    private Handler mHandler;
    private final Executor executor;
    public static final String AWARE_SERVICE_NAME = "Aware Service";

    public PeerHandle peerHandle = null;

    public WifiManager(Context context, Handler handler) {
        this.context = context;
        wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);
        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        mHandler = handler;
        executor = context.getMainExecutor();

        checkAvailable();
    }

    public void publish() {
        wifiAwareManager.attach(new pubAttachCallback(), mHandler);
    }

    public void subscribe(){
        wifiAwareManager.attach(new subAttachCallback(), mHandler);
    }

    class pubAttachCallback extends AttachCallback {

        public void onAttachFailed() {
            Toast.makeText(context, "onAttachFailed", Toast.LENGTH_SHORT).show();
        }

        public void onAttached(WifiAwareSession session) {
            Toast.makeText(context, "onAttach", Toast.LENGTH_SHORT).show();

            awareSession = session;
            PublishConfig config = new PublishConfig.Builder()
                    .setServiceName(AWARE_SERVICE_NAME)
                    .build();
            awareSession.publish(config, new DiscoverySessionCallback() {
                @Override
                public void onPublishStarted(PublishDiscoverySession session) {
                    Toast.makeText(context, "onPublishStarted", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    WifiManager.this.peerHandle = peerHandle;
                    Toast.makeText(context, "onMessageReceived : " + message.toString(), Toast.LENGTH_SHORT).show();
                }
            }, null);
        }
    }

    class subAttachCallback extends AttachCallback {
        @Override
        public void onAttachFailed() {
            Toast.makeText(context, "onAttachFailed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAttached(WifiAwareSession session) {
            Toast.makeText(context, "onAttach", Toast.LENGTH_SHORT).show();

            awareSession = session;

            SubscribeConfig config = new SubscribeConfig.Builder()
                    .setServiceName(AWARE_SERVICE_NAME)
                    .build();

            awareSession.subscribe(config, new DiscoverySessionCallback() {

                @Override
                public void onSubscribeStarted(SubscribeDiscoverySession session) {
                    if (peerHandle != null) {
                        int messageId = 1;
                        session.sendMessage(peerHandle, messageId, "Test Message".getBytes());
                    }
                }

                @Override
                public void onServiceDiscovered(PeerHandle peerHandle,
                                                byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    WifiManager.this.peerHandle = peerHandle;
                }
            }, null);

        }
    }

    private void checkAvailable() {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            System.out.println("Android Wifi supports Wi-Fi Aware : True");
        } else {
            System.out.println("Android Wifi supports Wi-Fi Aware : False");
        }

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            System.out.println("Android Wifi supports RTT : True");
        } else {
            System.out.println("Android Wifi supports RTT : False");
        }

        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wifiAwareManager.isAvailable()) {
                    System.out.println("Wi-Fi Aware is available : True");
                } else {
                    System.out.println("Wi-Fi Aware is available : False");
                }

                if (wifiRttManager.isAvailable()) {
                    System.out.println("Wi-Fi RTT is available : True");
                } else {
                    System.out.println("Wi-Fi RTT is available : False");
                }
            }
        };
        context.registerReceiver(myReceiver, filter);
    }

    public void connectRtt() {
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

            }

            @Override
            public void onRangingResults(List<RangingResult> results) {

            }
        });
    }
}

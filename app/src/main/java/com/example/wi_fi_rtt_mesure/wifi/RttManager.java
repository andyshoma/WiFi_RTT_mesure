package com.example.wi_fi_rtt_mesure.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Handler;

public class RttManager {

    private Context context;
    private Handler mHandler;
    private Executor executor;

    private WifiRttManager wifiRttManager;
    private RangingRequest.Builder builder;

    private List<RangingResult> resultList;

    public RttManager(Context context, Handler handler) {
        this.context = context;
        mHandler = handler;
        executor = context.getMainExecutor();

        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        IntentFilter filter = new IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
        BroadcastReceiver mReceiver = new ChangeRttReceiver();
        context.registerReceiver(mReceiver, filter);

        builder = new RangingRequest.Builder();
    }

    /**
     * Wi-Fi RTTに対応したAPの追加
     * @param aps アクセスポイントのリスト
     */
    public void addAccessPoint(List<ScanResult> aps) {
        builder.addAccessPoints(aps);
    }

    /**
     * Wi-Fi RTTに対応したWi-Fi Aware peerの追加
     * @param peer PeerHandle
     */
    public void addPeer(PeerHandle peer) {
        builder.addWifiAwarePeer(peer);
    }

    /**
     * 距離測定
     */
    public void ranging() {

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("check", "ACCESS_FINE_LOCATION permission OK");
        } else {
            Log.d("check", "ACCESS_FINE_LOCATION permission NG");
            return;
        }

        RangingRequest request = builder.build();

        wifiRttManager.startRanging(request, executor, new RangingResultCallback() {
            @Override
            public void onRangingFailure(int code) {
                Log.d("rtt_failure", "onRangingFailure" + code);
            }

            @Override
            public void onRangingResults(@NonNull List<RangingResult> results) {
                Log.d("rangingresults", "onRangingResults : " + results);
                if (results.get(0).getStatus() == RangingResult.STATUS_SUCCESS) {
                    resultList = results;
                }
            }
        });
    }

    /**
     * 距離測定結果を取得
     * @return ranging結果のlist
     */
    public List<RangingResult> getResultList() {
        return resultList;
    }

    /**
     * Wi-Fi Awareが利用可能かどうかのチェック
     * @return 可能であればtrue
     */
    public Boolean canUse() {
        return wifiRttManager.isAvailable();
    }

    class ChangeRttReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (wifiRttManager.isAvailable()) {
                // wifi rttが使える場合
                System.out.println("OK");
            } else {
                // wifi rttが使えない場合
                System.out.println("No aware");
            }
        }
    }

}
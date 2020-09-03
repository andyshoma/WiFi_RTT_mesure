package com.example.wi_fi_rtt_mesure.wifi;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import android.os.Handler;

import com.example.wi_fi_rtt_mesure.view.RangingResultLayout;

public class RttManager {

    private static final String TAG = "RangingResult";

    private Context context;
    private Activity activity;
    private Executor executor;

    private WifiRttManager wifiRttManager;
    private RangingRequest.Builder builder;

    private RttRangingResultCallback rangingResultCallback;
    final Handler mRangeRequestDelayHandler = new Handler();

    private Handler rangingHandler;
    private Runnable rangingRunnable;

    private Map<String, String> partnerIdMap;
    private ArrayList<String> idList;
    private Uri uri;

    private int rangingRequestInterval = 100;
    private int rangingTime = 60000;
    private Boolean ranging = false;

    private Map<String, RangingResultLayout> rangingResultLayoutMap;

    public RttManager(Context context) {
        this.context = context;
        executor = context.getMainExecutor();

        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        IntentFilter filter = new IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
        BroadcastReceiver mReceiver = new ChangeRttReceiver();
        context.registerReceiver(mReceiver, filter);

        rangingResultCallback = new RttRangingResultCallback();
    }

    /**
     * Wi-Fi RTTに対応したAPの追加
     * @param aps アクセスポイントのリスト
     */
    public void addAccessPoint(@NonNull List<ScanResult> aps) {
        builder = new RangingRequest.Builder();
        builder.addAccessPoints(aps);
    }

    /**
     * Wi-Fi RTTに対応したWi-Fi Aware peerの追加
     * @param peers PeerHandle
     */
    public void addPeer(@NonNull List<PeerHandle> peers) {

        builder = new RangingRequest.Builder();
        for (PeerHandle peer:peers) {
            builder.addWifiAwarePeer(peer);
        }
    }

    /**
     * 登録している測距相手を消去
     */
    public void delete() {
        builder = null;
    }

    /**
     * 測距を開始
     */
    public void startRanging() {

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("check", "ACCESS_FINE_LOCATION permission OK");
        } else {
            Log.d("check", "ACCESS_FINE_LOCATION permission NG");
            return;
        }

        try {
            RangingRequest rangingRequest = builder.build();
            wifiRttManager.startRanging(rangingRequest, executor, rangingResultCallback);
            ranging = true;
        }catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(context, "通信相手が登録されていません", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 測距を停止
     */
    public void stopRanging() {
        ranging = false;
        if (rangingHandler != null) {
            rangingHandler.removeCallbacks(rangingRunnable);
        }
    }

    /**
     * 測距間隔を設定
     * @param rangingRequestInterval 測距間隔(mm)（デフォルトは1000mm）
     */
    public void setRangingResultInterval(int rangingRequestInterval) {
        this.rangingRequestInterval = rangingRequestInterval;
    }

    /**
     * 測距時間を設定
     * @param rangingTime 測距時間(mm)（デフォルトは60000mm）
     */
    public void setRangingTime(int rangingTime) {
        this.rangingTime = rangingTime;
    }

    /**
     * 設定した測距時間分のタイマーを開始
     */
    public void startTimer() {
        HandlerThread handlerThread = new HandlerThread("Timer");
        handlerThread.start();

        rangingRunnable = new Runnable() {
            @Override
            public void run() {
                ranging = false;
            }
        };

        rangingHandler = new Handler(handlerThread.getLooper());
        rangingHandler.postDelayed(rangingRunnable, rangingTime);
    }

    /**
     * Wi-Fi RTTが対応しているかどうか
     * @return 対応している場合はtrue
     */
    public Boolean canUse() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
    }

    /**
     * Wi-Fi RTTが使える状態かどうか
     * @return 使える場合はtrue
     */
    public Boolean isAvailable() {
        return wifiRttManager.isAvailable();
    }

    /**
     * Map型の変数に格納したRangingResultLayoutを登録する
     * @param rangingResultLayoutMap keyをMacアドレスもしくはPeerHandleにしたMap
     */
    public void setRangingResultLayoutMap(Map<String, RangingResultLayout> rangingResultLayoutMap) {
        this.rangingResultLayoutMap = rangingResultLayoutMap;
    }

    /**
     * ファイルに書き込むためのパラメータの設定
     * @param partnerIdMap PeerHandleに対応する端末番号のMap
     * @param idList 端末番号のリスト
     * @param uri 保存先のuri
     */
    public void setSaveParametor(Map<String, String> partnerIdMap, ArrayList<String> idList, Uri uri, Activity activity) {
        this.partnerIdMap = partnerIdMap;
        this.idList = idList;
        this.uri = uri;
        this.activity = activity;
    }

    /**
     * ファイルへの書き込み
     * @param text 書き込む内容
     */
    private void save(String text) {
        try {
            ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "wa");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(text.getBytes());
            fileOutputStream.close();
            pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wi-Fi RTTの権限が変化した時の手続き
     */
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

    /**
     * Wi-Fi RTTによる測距が開始した時に呼び出されるコールバッククラス
     */
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (ranging) {
                                startRanging();
                            } else {
                                Toast.makeText(context, "測距を停止しました", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, rangingRequestInterval);
        }

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> results) {
            Log.d(TAG, "onRangingResults(): " + results);

            Map<String, String> resultMap = new HashMap<>();

            for (RangingResult result : results) {
                if (result.getStatus() == RangingResult.STATUS_SUCCESS && result.getMacAddress() != null) {
                    RangingResultLayout layout = rangingResultLayoutMap.get(result.getMacAddress().toString());
                    layout.setRangeText(String.valueOf(result.getDistanceMm()));
                    layout.setRangeSdText(String.valueOf(result.getDistanceStdDevMm()));
                    layout.setRssiText(String.valueOf(result.getRssi()));
                    String data = String.valueOf(result.getDistanceMm()) + ',' + result.getDistanceStdDevMm() + ',' +
                            result.getRssi() + ',' + result.getRangingTimestampMillis() + ',';
                    resultMap.put(result.getMacAddress().toString(), data);

                } else if (result.getStatus() == RangingResult.STATUS_SUCCESS && result.getPeerHandle() != null) {
                    RangingResultLayout layout = rangingResultLayoutMap.get(result.getPeerHandle().toString());
                    layout.setRangeText(String.valueOf(result.getDistanceMm()));
                    layout.setRangeSdText(String.valueOf(result.getDistanceStdDevMm()));
                    layout.setRssiText(String.valueOf(result.getRssi()));
                    String data = String.valueOf(result.getDistanceMm()) + ',' + result.getDistanceStdDevMm() + ',' +
                            result.getRssi() + ',' + result.getRangingTimestampMillis() + ',';
                    resultMap.put(partnerIdMap.get(result.getPeerHandle().toString()), data);
                }else if (result.getStatus() == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC){
                    Log.d(TAG, "RangingResult failed (AP doesn't support IEEE802.11mc");

                }else {
                    String data = "Ranging Failure" + ",,,,";
                    if (result.getMacAddress() != null) {
                        resultMap.put(result.getMacAddress().toString(), data);
                    } else if(result.getPeerHandle() != null) {
                        resultMap.put(partnerIdMap.get(result.getPeerHandle().toString()), data);
                    }
                    Log.d(TAG, "RangingResult failed.");

                }
            }

            StringBuilder builder = new StringBuilder();
            for (String id : idList) {
                builder.append(resultMap.get(id));
            }
            String text = builder.toString().replaceAll(",$", "\n");
            save(text);

            queueNextRangingRequest();
        }
    }

}
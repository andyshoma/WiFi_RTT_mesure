package com.example.wi_fi_rtt_mesure.wifi;

import android.Manifest;
import android.app.Activity;
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
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwareManager {

    private final static String TAG = "AwareManager.java";
    private final static String CONNECT = "connect";

    private Context context;
    private Handler mhandler;

    private WifiAwareManager wifiAwareManager;
    private WifiAwareSession awareSession;

    private DiscoverySession msession;

    private List<PeerHandle> peers;
    private Map<String, String> peerDeviceMap;

    private String deviceId;


    public AwareManager(Context context, Handler handler){

        this.context = context;
        mhandler = handler;
        peerDeviceMap = new HashMap<>();

        wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);

        IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        BroadcastReceiver mReceiver = new ChangeAwareReceiver();
        context.registerReceiver(mReceiver, filter);
        wifiAwareManager.attach(new MyAttachCallback(), mhandler);

    }

    /**
     *  Wi-Fi Awareの公開（基地局）
     */
    public void publish() {
        PublishConfig config = new PublishConfig.Builder()
                .setServiceName("Aware")
                .build();

        if (awareSession != null) {
            awareSession.publish(config, new DiscoverySessionCallback(){
                @Override
                public void onPublishStarted(PublishDiscoverySession session) {
                    super.onPublishStarted(session);
                    // 公開を開始した時
                    msession = session;
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    super.onMessageReceived(peerHandle, message);
                    // メッセージを受信した時
                    msession.sendMessage(peerHandle, 2, deviceId.getBytes());
                    Toast.makeText(context, "Message Received" + new String(message), Toast.LENGTH_SHORT).show();
                }
            }, null);
        }
    }

    /**
     * 公開しているWi-Fi Awareへの登録（移動端末側）
     */
    public void subscribe() {
        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName("Aware")
                .build();

        awareSession.subscribe(config, new DiscoverySessionCallback() {
            @Override
            public void onSubscribeStarted(SubscribeDiscoverySession session) {
                super.onSubscribeStarted(session);
                // 登録開始時
                msession = session;
                if (peers == null) { peers = new ArrayList<>(); }
            }

            @Override
            public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                // 通信相手を見つけた時
                if (!peers.contains(peerHandle)) { peers.add(peerHandle);}
                msession.sendMessage(peerHandle, 1, CONNECT.getBytes());
                System.out.println(peers);
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                // メッセージを受信した時
                String device = new String(message);
                if (!peerDeviceMap.containsValue(device)) {
                    peerDeviceMap.put(peerHandle.toString(), device);
                    Toast.makeText(context, "Message Received" + device, Toast.LENGTH_SHORT).show();
                }
            }
        }, null);
    }

    /**
     * Wi-Fi Awareが対応しているかどうか
     * @return 対応している場合はtrue
     */
    public Boolean canUsed() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
    }

    /**
     * Wi-Fi Awareが使える状態かどうか
     * @return 使える場合はtrue
     */
    public Boolean isAvailable() {
        return wifiAwareManager.isAvailable();
    }

    /**
     * Wi-Fi Awareにアタッチ
     */
    public void connect() {
        if (awareSession == null) {
            wifiAwareManager.attach(new MyAttachCallback(), mhandler);
        }
    }

    /**
     * Wi-Fi Awareセッションを閉じる
     */
    public void close() {
        if (awareSession != null) {
            awareSession.close();
            awareSession = null;
            peers = null;
            peerDeviceMap.clear();
            Toast.makeText(context, "sessionを閉じました", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(context, "sessionが開かれていません", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Wi-Fi Awareに再度アタッチ
     */
    public void reconnect() {
        if (awareSession == null) {
            wifiAwareManager.attach(new MyAttachCallback(), mhandler);
        }else {
            awareSession.close();
            awareSession = null;
            peers = null;
            wifiAwareManager.attach(new MyAttachCallback(), mhandler);
        }
    }

    /**
     * peerhandleのリストを取得
     * @return peerhandleのリスト
     */
    public List<PeerHandle> getPeers() {
        return peers;
    }

    /**
     * 自端末の端末番号の設定
     * @param deviceId 自端末の端末番号
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * PeerHandleから通信相手の端末番号を検索し取得
     * @param peer 登録されている相手のpeerhandleの文字列
     * @return peerhandleに紐づけられている端末番号を返す。存在しない場合はnull
     */
    public String getPeerDeviceId(String peer) {
        if (peerDeviceMap.containsKey(peer)) {
            return peerDeviceMap.get(peer);
        } else {
            Log.d(TAG, "peerhandleが登録されていません");
            return null;
        }
    }

    /**
     *  ここでは以下の処理を行う
     *  ・Wi-Fi Awareハードウェアをオンにする。
     *  ・Wi-Fi Awareクラスタに参加、またはWi-Fi Awareクラスタを形成する。
     *  ・一意の名前空間をもつWi-Fi Awareセッションを作成する。これは、その中で作成される全ての検出セッションのコンテナとして機能します。
     */
    class MyAttachCallback extends AttachCallback {

        @Override
        public void onAttachFailed() {
            super.onAttachFailed();
            Toast.makeText(context, "onAttachFailed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAttached(WifiAwareSession session) {
            super.onAttached(session);
            awareSession = session;
            Toast.makeText(context, "onAttach", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     *  Wi-Fi Awareが利用できるかどうか変更されるたびに呼び出される
     */
    class ChangeAwareReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (wifiAwareManager.isAvailable()) {
                // wifi awareが使える場合
                System.out.println("OK");
            } else {
                // wifi awareが使えない場合
                System.out.println("No aware");
            }
        }
    }

}

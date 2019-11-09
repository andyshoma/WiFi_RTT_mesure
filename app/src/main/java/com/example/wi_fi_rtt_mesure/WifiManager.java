package com.example.wi_fi_rtt_mesure;

import android.Manifest;
import android.app.Dialog;
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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

public class WifiManager {

    public static final String SERIAL_NAME = "wifi";

    public Context context;
    private final WifiAwareManager wifiAwareManager;
    private final WifiRttManager wifiRttManager;
    private WifiAwareSession awareSession;
    private Handler mHandler;
    private final Executor executor;
    public static final String AWARE_SERVICE_NAME = "Aware";
    private SubscribeDiscoverySession subsession;
    private PublishDiscoverySession pubsession;
    private RangingRequest.Builder builder;
    private RangingRequest request;

    private List<RangingResult> rangingResults;
    public List<String> stringList = new ArrayList<String>();
    private TextView result_distance;
    private TextView counter;
    private Spinner spinner;
    private ArrayAdapter adapter;
    private ArrayList<String> idList;
    private Map<PeerHandle, String> peerToId;
    private Map<String, PeerHandle> idPeerHandle;
    private Map<String, Integer> idOffset;
    private String selectId;
    private Integer ID;
    private String deviceID;
    private final static int DELAYTIME = 500;

    private Integer sum = 0;
    private Integer trueDistance = 1000;
    public int count_success = 0;
    public int countTry = 0;

    public PeerHandle peerHandle = null;
    public ArrayList<PeerHandle> peerHandles;

    /***
     * RangingActivityを呼び出す時のコンストラクタ
     * @param context
     * @param handler
     * @param deviceID 端末自身のユニークなID
     * @param spinner
     */
    public WifiManager(Context context, Handler handler, String deviceID, Spinner spinner) {
        this.context = context;
        wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);
        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mHandler = handler;
        executor = context.getMainExecutor();

        this.spinner = spinner;
        adapter = new ArrayAdapter<PeerHandle>(context, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        this.deviceID = deviceID;
        idList = new ArrayList<>();
        idPeerHandle = new HashMap();
        peerToId = new HashMap<>();
        peerHandles = new ArrayList<>();
        wifiAwareManager.attach(new myAttachCallback(), mHandler);
    }

    /***
     * MainActivityを呼び出す時のコンストラクタ
     * @param context
     * @param handler
     */
    public WifiManager(Context context, Handler handler) {
        this.context = context;
        wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);
        wifiRttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mHandler = handler;
        executor = context.getMainExecutor();

        wifiAwareManager.attach(new myAttachCallback(), mHandler);

        builder = new RangingRequest.Builder();
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

    /***
     *publishを行う
     */
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
                    String messageID = new String(message);
                    if(idList == null || idList.indexOf(messageID) == -1){
                        idList.add(messageID);
                        peerHandles.add(peerHandle);
                        idPeerHandle.put(messageID, peerHandle);
                        peerToId.put(peerHandle, messageID);
                        adapter.add(messageID);
                        adapter.notifyDataSetChanged();
                    }
                }
            }, null);
        }
    }

    /***
     *subscribeを行う
     */
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
                    subsession.sendMessage(peerHandle, 1, deviceID.getBytes());
                    WifiManager.this.peerHandle = peerHandle;
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

        countTry++;
        for (PeerHandle peer : peerHandles) {
            builder.addWifiAwarePeer(peer);
        }
        request = builder.build();

        wifiRttManager.startRanging(request, executor, new RangingResultCallback() {

            @Override
            public void onRangingFailure(int code) {
                Log.d("rtt_failure", "onRangingFailure" + code);
                saveFile.write("rangingFailure");
            }

            @Override
            public void onRangingResults(List<RangingResult> results) {
                Log.v("rangingresults", "onRangingResults : " + results);

                for (RangingResult result : results) {
                    if (result.getStatus() == RangingResult.STATUS_SUCCESS) {
                        /*count_success++;
                        result_distance.setText(result.getDistanceMm() + " Mm");
                        String str = peerToId.get(result.getPeerHandle()) + "," + result.getDistanceMm() + "," + result.getDistanceStdDevMm() + "," + result.getRssi() + "," + result.getRangingTimestampMillis();
                        saveFile.write(str);*/
                    } else {
                        saveFile.write("STATUS_FAILURE");
                    }
                }
            }
        });

            /*try{
                Thread.sleep(2000);
            }catch (InterruptedException e){

            }*/
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

    public boolean checkRanging(){
        if (peerHandle != null){
            return true;
        }else{
            Toast toast = Toast.makeText(context, "通信相手がいません", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
    }

    /***
     * １対１の測距時に用いるためのrangingメソッド
     * @param saveFile データの保存先
     */
    public void ranging(final SaveFile saveFile){
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("check", "ACCESS_FINE_LOCATION permission OK");
        } else {
            Log.d("check", "ACCESS_FINE_LOCATION permission NG");
            return;
        }

        wifiRttManager.startRanging(request, executor, new RangingResultCallback() {

            @Override
            public void onRangingFailure(int code) {
                Log.d("rtt_failure", "onRangingFailure" + code);
            }

            @Override
            public void onRangingResults(List<RangingResult> results) {
                Log.v("rangingresults", "onRangingResults : " + results);
                if (results.get(0).getStatus() == RangingResult.STATUS_SUCCESS) {
                    //count_success++;
                    result_distance.setText(results.get(0).getDistanceMm() + " ");
                    //sum += results.get(0).getDistanceMm();
                    String str = results.get(0).getDistanceMm() + "," + results.get(0).getDistanceStdDevMm() + "," + results.get(0).getRssi() + "," + results.get(0).getRangingTimestampMillis();
                    saveFile.write(str);
                }
            }
        });
    }

    public void setCalibration(){
        if(idOffset == null){
            idOffset = new HashMap<>();
        }

        idOffset.put(selectId, sum/count_success-trueDistance);
    }

    public void getTextView(TextView result_distance, TextView counter){
        this.result_distance = result_distance;
        this.counter = counter;
    }

    public String getID(){
        return String.valueOf(ID);
    }

    public void resetCounter(){
        this.count_success = 0;
        awareSession.close();
    }

    public void makeRequest(){
        builder = new RangingRequest.Builder();

        if(peerHandle != null){
            Log.d("debug", peerHandle.toString());
            //RangingRequest.Builder builder = new RangingRequest.Builder();
            builder.addWifiAwarePeer(peerHandle);
        }else{
            return;
        }
        request = builder.build();
    }

    public void setText(TextView result_distance){
        this.result_distance = result_distance;
    }

    public void selectID(String select){
        selectId = select;
        peerHandle = idPeerHandle.get(select);
        System.out.println(peerHandle);
    }

    public void close() {
        if (awareSession != null) {
            awareSession.close();
            Toast toast = Toast.makeText(context, "close", Toast.LENGTH_LONG);
            toast.show();
        }
    }
}

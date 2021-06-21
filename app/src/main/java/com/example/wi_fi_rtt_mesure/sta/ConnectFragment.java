package com.example.wi_fi_rtt_mesure.sta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.rtt.RangingRequest;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.WifiRttApplication;
import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ConnectFragment extends Fragment implements View.OnClickListener{

    private final static String TAG = "ConnectFragment";
    private final static String DEVICE_ID = "device_id";

    private Context context;
    private WifiRttApplication application;

    private Spinner parterSpinner;
    private ArrayAdapter adapter;
    private int peerOrAp = 0;

    private boolean mLocationPermissionApproved = false;

    private List<ScanResult> accessPointsSupportFtm;
    private List<PeerHandle> peers;

    private AwareManager awareManager;
    private RttManager rttManager;
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;

    private String deviceID;
    private int partnerNum = 0;

    @CheckResult
    public static ConnectFragment newInstance(String id) {
        ConnectFragment fragment = new ConnectFragment();

        Bundle args = new Bundle();
        args.putString(DEVICE_ID, id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (WifiRttApplication)getActivity().getApplication();
        context = getActivity().getApplicationContext();

        awareManager = application.getAwareManager();
        rttManager = application.getRttManager();
        awareManager.connect();

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();

        Bundle args = getArguments();
        if (args != null) {
            deviceID = args.getString(DEVICE_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        System.out.println("hello");

        mLocationPermissionApproved = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(wifiScanReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_connect, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.subscribe).setOnClickListener(this);
        view.findViewById(R.id.close).setOnClickListener(this);
        view.findViewById(R.id.reconnect).setOnClickListener(this);
        view.findViewById(R.id.scan_wifi).setOnClickListener(this);
        view.findViewById(R.id.add).setOnClickListener(this);
        view.findViewById(R.id.ranging).setOnClickListener(this);

        parterSpinner = view.findViewById(R.id.partner_spinner);
        adapter = new ArrayAdapter<PeerHandle>(context, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        parterSpinner.setAdapter(adapter);

        parterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                String item = (String)spinner.getSelectedItem();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        TextView id = (TextView)view.findViewById(R.id.deviceID);
        id.setText(deviceID);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.subscribe) {
            // subscribeボタンが押された時
            onCallSubscribe();

        }else if (view.getId() == R.id.close) {
            // closeボタンが押された時
            onCallClose();

        }else if (view.getId() == R.id.reconnect) {
            // reconnectボタンが押された時
            onCallReconnect();

        }else if (view.getId() == R.id.scan_wifi) {
            // wifiボタンが押された時
            onCallWifi();

        }else if (view.getId() == R.id.add) {
            // addボタンが押された時
            onCallAdd();

        }else if (view.getId() == R.id.ranging) {
            // rangingボタンが押された時
            onCallRanging();

        }
    }

    /**
     * subscribeボタンが押された時に呼び出された時の手続き
     */
    private void onCallSubscribe() {
        awareManager.subscribe();
        Toast.makeText(context, "端末通信開始しました", Toast.LENGTH_SHORT).show();
    }

    /**
     * closeボタンが押された時に呼び出された時の手続き
     */
    private void onCallClose() {
        awareManager.close();
        rttManager.delete();
    }

    /**
     * reconnectボタンが押された時に呼び出された時の手続き
     */
    private void onCallReconnect() {
        awareManager.reconnect();
        rttManager.delete();
    }

    /**
     * wifiボタンが押された時に呼び出された時の手続き
     */
    private void onCallWifi() {
        if (mLocationPermissionApproved) {
            Toast.makeText(context, "探索中", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        } else {
            Log.d(TAG, "Permissions not allowed");
        }
    }

    /**
     * addボタンが押された時に呼び出された時の手続き
     */
    private void onCallAdd() {
        peers = awareManager.getPeers();
        if (peers != null) {
            for (PeerHandle peer : peers) {
                adapter.add(awareManager.getPeerDeviceId(peer.toString()));
            }
            peerOrAp = 0;
            rttManager.addPeer(peers);
            Toast.makeText(context, "追加しました", Toast.LENGTH_SHORT).show();
        } else if (accessPointsSupportFtm != null) {
            for (ScanResult ap : accessPointsSupportFtm) {
                adapter.add(ap.BSSID);
            }
            peerOrAp = 1;
            rttManager.addAccessPoint(accessPointsSupportFtm);
            Toast.makeText(context, "追加しました", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, "相手が見つかりません", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * rangingボタンが押された時の手続き
     */
    private void onCallRanging() {
        ArrayList<String> partners;

        if (peers != null) {
            partners = toStringsfromPeers(peers);
        } else if (accessPointsSupportFtm != null) {
            partners = toStringsfromScans(accessPointsSupportFtm);
        } else {
            Toast.makeText(context, "通信相手が追加されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

        if (fragmentManager != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container, RangingFragment.newInstance(partners));
            fragmentTransaction.commit();
        }
    }

    /**
     * 通信相手端末のPeerHandleをString型に変換
     * @param peers PeerHandleのリスト
     * @return String型に変換したPeerHandleのリスト
     */
    private ArrayList<String> toStringsfromPeers(List<PeerHandle> peers) {
        ArrayList<String> peerStrings = new ArrayList<>();

        for (PeerHandle peer : peers) { peerStrings.add(peer.toString()); }

        return peerStrings;
    }

    /**
     * スキャンした基地局のBSSID(MacAddress)をString型に変換
     * @param scanResults 基地局のスキャン結果のリスト
     * @return String型に変換した基地局のBSSIDのリスト
     */
    private ArrayList<String> toStringsfromScans(List<ScanResult> scanResults) {
        ArrayList<String> scanStrings = new ArrayList<>();

        for (ScanResult result : scanResults) { scanStrings.add(result.BSSID); }

        return scanStrings;
    }

    /**
     * Wi-Fiのスキャン結果を取得した時に呼び出されるクラス
     */
    private class WifiScanReceiver extends BroadcastReceiver {

        private List<ScanResult> findFtmSupportAccessPoints(@NonNull List<ScanResult> originalList) {
            List<ScanResult> newList = new ArrayList<>();

            for (ScanResult scanResult : originalList) {

                if (scanResult.is80211mcResponder()) {
                    newList.add(scanResult);
                }

                if (newList.size() >= RangingRequest.getMaxPeers()) {
                    break;
                }
            }
            return newList;
        }

        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = wifiManager.getScanResults();

            if (scanResults != null) {

                if (mLocationPermissionApproved) {
                    accessPointsSupportFtm = findFtmSupportAccessPoints(scanResults);
                    Toast.makeText(context, "探索終了", Toast.LENGTH_SHORT).show();

                } else {
                    Log.d(TAG, "Permissions not allowed");
                }
            }
        }
    }

}

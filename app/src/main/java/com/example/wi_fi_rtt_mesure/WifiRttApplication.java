package com.example.wi_fi_rtt_mesure;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;

import java.util.Optional;

public class WifiRttApplication extends Application {

    private AwareManager awareManager;
    private RttManager rttManager;

    private Uri uri;
    private String deviceID;

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = this;
        Handler handler = new Handler();

        awareManager = new AwareManager(context, handler);
        rttManager = new RttManager(context);

    }

    public AwareManager getAwareManager() {
        return awareManager;
    }

    public RttManager getRttManager() {
        return rttManager;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

}

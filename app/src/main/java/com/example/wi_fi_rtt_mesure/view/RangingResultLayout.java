package com.example.wi_fi_rtt_mesure.view;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.ArrayList;

public class RangingResultLayout extends LinearLayout {

    private Context context;

    private LinearLayout linearLayout;

    private ArrayList<TextView> textViews;

    private TextView rangeText;
    private TextView rangeSdText;
    private TextView rssiText;

    public RangingResultLayout(Context context, String partner) {
        super(context);
        this.context = context;
        this.setOrientation(LinearLayout.VERTICAL);

        setDeviceId(partner);
        rangeText = textView("0");
        addResultLayout("Range（m）", rangeText);
        rangeSdText = textView("0");
        addResultLayout("RangeSD（m）", rangeSdText);
        rssiText = textView("0");
        addResultLayout("RSSI（dBm）", rssiText);
    }


    public void setRangeText(String result) {
        rangeText.setText(result);
    }

    public void setRangeSdText(String result) {
        rangeSdText.setText(result);
    }

    public void setRssiText(String result) {
        rssiText.setText(result);
    }

    /**
     * layoutに通信相手のdeviceIDを設定
     * @param partner 通信相手のdeviceIDの文字列
     */
    private void setDeviceId(String partner) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Space space1 = new Space(context);
        LinearLayout.LayoutParams sp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp1.weight = 1;
        linearLayout.addView(space1, sp1);

        TextView textView = textView(partner);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.weight = 20;
        linearLayout.addView(textView, tp);

        Space space2 = new Space(context);
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp2.weight = 1;
        linearLayout.addView(space2, sp2);

        this.addView(linearLayout, lp);
    }

    /**
     * layoutに測定結果を表示するメソッド
     * @param name 測定結果の名前
     * @param dataText 測定結果
     */
    private void addResultLayout(String name, TextView dataText) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Space space1 = new Space(context);
        LinearLayout.LayoutParams sp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp1.weight = 1;
        linearLayout.addView(space1, sp1);

        TextView textView1 = textView(name);
        LinearLayout.LayoutParams tp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp1.weight = 50;
        linearLayout.addView(textView1, tp1);

        Space space2 = new Space(context);
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp1.weight = 5;
        linearLayout.addView(space2, sp2);

        LinearLayout.LayoutParams tp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        dataText.setGravity(Gravity.LEFT);
        tp2.weight = 10;
        linearLayout.addView(dataText, tp2);

        Space space3 = new Space(context);
        LinearLayout.LayoutParams sp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp3.weight = 1;
        linearLayout.addView(space3, sp3);

        this.addView(linearLayout, lp);
    }

    /**
     * textをセットするメソッド
     * @param text textviewにセットするtext
     * @return textview
     */
    private TextView textView(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(20);
        return textView;
    }

}

package com.example.wi_fi_rtt_mesure.sta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wi_fi_rtt_mesure.R;
import com.example.wi_fi_rtt_mesure.WifiRttApplication;
import com.example.wi_fi_rtt_mesure.view.RangingResultLayout;
import com.example.wi_fi_rtt_mesure.wifi.AwareManager;
import com.example.wi_fi_rtt_mesure.wifi.RttManager;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangingFragment extends Fragment implements View.OnClickListener {

    private final static String PARTNER = "partner";
    private final static String TAG = "RangingFragment.java";
    private final static int CREATE_DOCUMENT_REQUEST = 43;

    private final static int DEFAULT_INTERVAL = 100;
    private final static int DEFAULT_RANGING_TIME = 60000;

    private WifiRttApplication application;
    private Context context;
    private Activity activity;

    private ArrayList<String> partners;
    private Map<String, String> partnerIdMap;
    private ArrayList<String> idList;

    private AwareManager awareManager;
    private RttManager rttManager;

    private Spinner directionSpinner;

    private EditText distanceText;
    private EditText rangingIntervalText;
    private EditText rangingTimeText;

    private String direction = "";
    private String header;
    private int distance = 0;
    private int backnumber = -1;

    private String deviceId;
    private Uri uri;

    public static RangingFragment newInstance(ArrayList<String> partners) {
        RangingFragment fragment = new RangingFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PARTNER, partners);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        application = (WifiRttApplication)getActivity().getApplication();
        context = getActivity().getApplicationContext();
        activity = getActivity();

        awareManager = application.getAwareManager();
        rttManager = application.getRttManager();

        deviceId = awareManager.getDeviceId();

        Bundle args = getArguments();
        if (args != null) {
            partners = args.getStringArrayList(PARTNER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_ranging, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        directionSpinner = view.findViewById(R.id.direction);
        ArrayAdapter directionAdapter = ArrayAdapter.createFromResource(context,
                R.array.direction, android.R.layout.simple_spinner_item);
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directionSpinner.setAdapter(directionAdapter);

        directionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                direction = spinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        distanceText = view.findViewById(R.id.true_distance);
        rangingIntervalText = view.findViewById(R.id.ranging_interval);
        rangingTimeText = view.findViewById(R.id.ranging_time);

        view.findViewById(R.id.plus).setOnClickListener(this);
        view.findViewById(R.id.minus).setOnClickListener(this);
        view.findViewById(R.id.start).setOnClickListener(this);
        view.findViewById(R.id.stop).setOnClickListener(this);
        view.findViewById(R.id.make_file).setOnClickListener(this);

        ScrollView scrollView = view.findViewById(R.id.result_view);
        ScrollView.LayoutParams params = new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, params);

        partnerIdMap = new HashMap<>();
        idList = new ArrayList<>();
        Map<String, RangingResultLayout> rangingResultLayoutMap = new HashMap<>();

        rttManager.setRangingResultLayoutMap(rangingResultLayoutMap);

        StringBuilder builder = new StringBuilder();
        for (String partner : partners) {
            String id = selectId(partner);
            partnerIdMap.put(partner, id);
            idList.add(id);
            builder.append(id + ",,,,");
            RangingResultLayout rangingResultLayout = new RangingResultLayout(context, id);
            rangingResultLayoutMap.put(partner, rangingResultLayout);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            linearLayout.addView(rangingResultLayout, rp);
        }
        header = builder.toString().replaceAll(",$", "\n");
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.plus) {
            // plusボタンが押された時
            distance++;
            if (distance == 0) {
                distanceText.setText("", TextView.BufferType.NORMAL);
            }else{
                distanceText.setText(String.valueOf(distance), TextView.BufferType.NORMAL);
            }

        }else if (view.getId() == R.id.minus) {
            // minusボタンが押された時
            if (distance > 0) {
                distance--;
            }
            if (distance <= 0) {
                distanceText.setText("", TextView.BufferType.NORMAL);
            } else {
                distanceText.setText(String.valueOf(distance), TextView.BufferType.NORMAL);
            }
        }else if (view.getId() == R.id.make_file) {
            // make fileボタンが押された時
            createNewFile();
        }else if (view.getId() == R.id.start) {
            // ranging startボタンが押された時
            onCallStart();

        }else if (view.getId() == R.id.stop) {
            // ranging stopボタンが押された時
            rttManager.stopRanging();
        }
    }

    /**
     * ranging startボタンが押された時に呼び出されるメソッド
     */
    private void onCallStart() {
        String interval = rangingIntervalText.getText().toString();
        String time = rangingTimeText.getText().toString();

        if (!interval.equals("")) {
            rttManager.setRangingResultInterval(Integer.parseInt(interval));
        } else {
            rttManager.setRangingResultInterval(DEFAULT_INTERVAL);
        }

        if (!time.equals("")) {
            rttManager.setRangingTime(Integer.parseInt(time));
        } else {
            rttManager.setRangingTime(DEFAULT_RANGING_TIME);
        }

        rttManager.startTimer();
        rttManager.startRanging();
    }

    /**
     * listviewに表示させるIDの選択
     * @param partner 通信相手
     * @return 端末間通信の場合は端末番号を，基地局通信の場合はBSSIDを返す
     */
    private String selectId(String partner) {
        String id = awareManager.getPeerDeviceId(partner);
        if (id == null) {
            return partner;
        } else {
            return id;
        }
    }

    private void createNewFile() {
        String filename = deviceId;

        if (distance != 0) {
            filename = filename + '_' + distance + 'm';
        }

        if (!direction.equals("")) {
            filename = filename + '_' + direction;
        }

        filename = filename + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST);

    }

    private void save(Uri uri, String text) {
        try {
            ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "wa");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(text.getBytes());
            fileOutputStream.close();
            pfd.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == CREATE_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData.getData() != null) {

                final int takeFlags = getActivity().getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Uri uri = resultData.getData();
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                application.setUri(uri);
                Log.d(TAG, uri.toString());

                Toast.makeText(context, "ファイルを作成しました", Toast.LENGTH_SHORT).show();

                rttManager.setSaveParametor(partnerIdMap, idList, uri, getActivity());

                save(uri, header);
            }
        }
    }
}

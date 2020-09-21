package com.chentong.serviceexample;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TestServiceOne extends Service {
    private final String TAG = "TestServiceOne";
    private int count = 0;
    private boolean quit = false;
    private WifiManager wifiManager;
    private HashMap<String, BSSIDInfo> scanInfo;
    private HashMap<String, BSSIDInfo> beaconInfo;
    private BroadcastReceiver receiver;
    private int cumulative_scans = 0;
    private int NUM_SCANS = 2;
    private boolean answer = false;

    private MyBinder myBinder = new MyBinder();

    //定义onBinder方法所返回的对象
    public class MyBinder extends Binder {
        public int getCount() {
            return count;
        }
    }

    //必须实现的方法
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind run");
        return myBinder;
    }

    //Service被创建时调用
    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate run");
        super.onCreate();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!quit) {
//                    if (answer) {
//                        scanWifi();
//                        try {
//                            Thread.sleep(60000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        checkAlarm("http://192.168.12.3:5000/home", "Chen");
//                        try {
//                            Thread.sleep(5000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand ......");
        scanWifi();
        return super.onStartCommand(intent, flags, startId);
    }

    //Service断开连接时回调
    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind run!");
        return true;
    }

    //Service被销毁时调用
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy run");
        this.quit = true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.e(TAG, "onRebind run!");
    }

    public void checkAlarm(String url, final String userName) {
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("username", userName);
        Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(Tags.SERVER, "Failed received response!");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String message = response.body().string();
                Log.d(Tags.SERVER, "Successfully received response! " + message);
                answer = message.equals("true");
            }
        });
    }

    public void scanWifi() {
        wifiManager = (WifiManager)
                getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;
        if (!wifiManager.isWifiEnabled()) {
            Log.e(TAG, "Please Turn on Wifi!");
//            Toast.makeText(getApplication(), "Please Turn on Wifi", Toast.LENGTH_LONG).show();
        } else {
            receiver = getReceiver(wifiManager);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            getApplication().registerReceiver(receiver, intentFilter);
            boolean success = wifiManager.startScan();
            if (!success) {
                Log.e(TAG, "Wifi Scan Started failed!");
            } else {
                Log.e(TAG, "Wifi Scan Started successfully!");
            }

        }
    }

    private BroadcastReceiver getReceiver(final WifiManager wifiManager) {
        return new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    List<ScanResult> results = wifiManager.getScanResults();
                    Log.i(Tags.SCAN, "Wifi Scan finished");
//                    Toast.makeText(getApplication(), "Wifi Scan finished", Toast.LENGTH_LONG).show();
                    boolean scanning = scanSuccess(results);
                    if (!scanning) {
                        getApplicationContext().unregisterReceiver(receiver);
                        Log.i(Tags.SCAN, "Scan End");
//                        try {
//                            Log.i(Tags.SCAN, "Stop for 30 seconds");
//                            Thread.currentThread();
//                            Thread.sleep(30000);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        scanWifi();
                    } else {
                        Log.i(Tags.SCAN, "Continue Scan");
                    }
//                    getApplicationContext().unregisterReceiver(receiver);
//                    System.out.println("End");
                } else {
                    // scan failure handling
                    List<ScanResult> results = wifiManager.getScanResults();
                    System.out.println(results);
                    Log.e(Tags.SCAN, "Wifi Scan finished with failed!");
//                    Toast.makeText(getApplication(), "Wifi Scan finished with failed", Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private boolean scanSuccess(List<ScanResult> scanResults) {
        // Scan was successful. Connect to server, and update.
        // Build the JSON object from the scanResults:
        buildHaspMap(scanResults);
        ++cumulative_scans;
        Log.i(Tags.SCAN, "The wifi scan was completed " + cumulative_scans + "/" + NUM_SCANS);
        int percentage = (int) Math.round(((double) cumulative_scans / (double) NUM_SCANS) * 100);
        // Do we send the message?
        if (cumulative_scans >= NUM_SCANS) {
            JSONObject jObject = buildJSONObject(scanInfo, beaconInfo);
            Log.i(Tags.WIFI, "All wifi scans were successful.");
            JSONObject sendObject = new JSONObject();
            try {
                sendObject.put("Time", System.currentTimeMillis());
                sendObject.put("ID", "123456");
                sendObject.put("Scans", jObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Server server = new Server();
            server.execute(sendObject);
            server.setOnAsyncResponse(new AsyncResponse() {
                @Override
                public void onDataReceivedSuccess(String result) {
                    Log.i(Tags.SERVER, "Response received" + result);
                }

                @Override
                public void onDataReceivedFailed() {
                    Log.i(Tags.SERVER, "Response receive failed");
                }
            });
            Log.i(Tags.SCAN, "All Data being sent: " + sendObject.toString());
            cumulative_scans = 0;
            scanInfo = new HashMap<>();
            beaconInfo = new HashMap<>();
            return false;
        }
        if (cumulative_scans == NUM_SCANS - 1) {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
            Log.i(TAG, "Next scan will send a message to the server.");
        }

//        result.append("Appended the new scan to the Scan Container.");

        return true;
    }

    private JSONObject buildJSONObject(HashMap<String, BSSIDInfo> InfoMap, HashMap<String, BSSIDInfo> InfoBeacon) {
        JSONArray dataArray = new JSONArray();
        for (String bssid : InfoMap.keySet()) {
            BSSIDInfo bssidinfo = InfoMap.get(bssid);
            try {
                JSONObject innerObject = new JSONObject();
                innerObject.put("BSSID", bssid);
                innerObject.put("RSSIs", new JSONArray(bssidinfo.getRSSILevels()));
                dataArray.put(innerObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (InfoBeacon != null) {
            for (String bssid : InfoBeacon.keySet()) {
                BSSIDInfo bssidinfo = InfoBeacon.get(bssid);
                try {
                    JSONObject innerObject = new JSONObject();
                    innerObject.put("BSSID", bssid);
                    innerObject.put("RSSIs", new JSONArray(bssidinfo.getRSSILevels()));
                    dataArray.put(innerObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.i(Tags.WIFI, "Hash map was built." + dataArray.toString());
        JSONObject payload = new JSONObject();
        try {
            payload.put("Scan_number", cumulative_scans);
            payload.put("data", dataArray);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return payload;
    }

    private void buildHaspMap(List<ScanResult> scanResults) {
        if (scanInfo == null) {
            scanInfo = new HashMap<>();
        }
        for (ScanResult sr : scanResults) {
            String BSSID = sr.BSSID;
            String SSID = sr.SSID;
            int RSSIlevel = sr.level;
            System.out.println(BSSID + ' ' + SSID);
            if (scanInfo.containsKey(BSSID)) {
                scanInfo.get(BSSID).addRSSI(RSSIlevel);
            } else {
                scanInfo.put(BSSID, new BSSIDInfo(SSID, RSSIlevel));
            }
        }
        for (String name : scanInfo.keySet()) {
            System.out.println(name + " " + scanInfo.get(name).getRSSILevels());
        }
    }

}

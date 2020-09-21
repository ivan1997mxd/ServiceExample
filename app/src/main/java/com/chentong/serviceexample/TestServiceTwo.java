package com.chentong.serviceexample;

import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.sample.libproximitybeacon.ProximityBeacon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;

public class TestServiceTwo extends Service{
    private final String TAG = "TestServiceTwo";
    private int count = 0;
    private boolean quit = false;
    private WifiManager wifiManager;
    private HashMap<String, BSSIDInfo> scanInfo;
    private HashMap<String, BSSIDInfo> beaconInfo;
    private BroadcastReceiver receiver;
    private int cumulative_scans = 0;
    private int beacon_scans = 1;
    private int NUM_SCANS = 2;
    private boolean mScanning = false;
    public static ProximityBeacon client;
    static ProgressDialog connDialog;
    private ArrayList<String> deviceList = new ArrayList<>();
    private MyBinder myBinder = new MyBinder();

    private BluetoothLeScanner mBLEScanner;
    private static final long SCAN_TIME = 10000;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand ......");
        scanBeacon();
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

//    public void checkAlarm(String url, final String userName) {
//        OkHttpClient client = new OkHttpClient();
//        FormBody.Builder formBuilder = new FormBody.Builder();
//        formBuilder.add("username", userName);
//        Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
//        Call call = client.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                Log.d(Tags.SERVER, "Failed received response!");
//            }
//
//            @Override
//            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                String message = response.body().string();
//                Log.d(Tags.SERVER, "Successfully received response! " + message);
//                answer = message.equals("true");
//            }
//        });
//    }


    private void scanBeacon() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("C2:3B:C4:50:F8:6B").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("DF:5A:33:2A:96:47").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("DB:4F:6E:19:A2:52").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("CF:27:BE:19:4E:31").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("CA:D9:80:DE:59:61").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("CE:F1:1F:F8:97:20").build());
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress("FC:B0:61:E1:7F:E2").build());
        ScanSettings.Builder builder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_POWER);
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
        builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        if (mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
            //设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）
            //设置为0以立即通知结果
            builder.setReportDelay(1L);
            Log.e(TAG, "get support");
        }
        ScanSettings mScanSettings = builder.build();
        //如果没打开蓝牙，不进行扫描操作，或请求打开蓝牙。
        if(!mBluetoothAdapter.isEnabled()) {
            return;
        }
        //处于未扫描的状态
        if (!mScanning){
            //android 5.0后
            //标记当前的为扫描状态
            mScanning = true;
            //获取5.0新添的扫描类
            if (mBLEScanner == null){
                //mBLEScanner是5.0新添加的扫描类，通过BluetoothAdapter实例获取。
                mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            //开始扫描
            //mScanSettings是ScanSettings实例，mScanCallback是ScanCallback实例，后面进行讲解。
            mBLEScanner.startScan(bleScanFilters,mScanSettings,mScanCallback);
        }

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.e(TAG, "get result");
        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            super.onBatchScanResults(results);
            boolean finish = BleSuccess(results);
            if (finish){
                Log.e(TAG, "Finished Scan");
            }else {
                Log.e(TAG, "Continue Scan");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan Failed");
        }
    };

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
//                Toast.makeText(getApplication(), "Wifi Scan Started failed!", Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Wifi Scan Started successfully!");
//                Toast.makeText(getApplication(), "Wifi Scan Started successfully!", Toast.LENGTH_LONG).show();
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

    private boolean BleSuccess(List<android.bluetooth.le.ScanResult> results){
        if (beacon_scans > NUM_SCANS){
            mBLEScanner.stopScan(mScanCallback);
            beacon_scans = 1;
            return true;
        }
        buildBeaconMap(results);
        Log.i(Tags.SCAN, "The Beacon scan was completed " + beacon_scans + "/" + NUM_SCANS);
        beacon_scans++;
        return false;
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


    private void buildBeaconMap(List<android.bluetooth.le.ScanResult> results) {
        if (beaconInfo == null) {
            beaconInfo = new HashMap<>();
        }
        for (android.bluetooth.le.ScanResult result: results){
            String BSSID = result.getDevice().getAddress();
            String SSID = result.getDevice().getName();
            int RSSIlevel = result.getRssi();
            if (beaconInfo.containsKey(BSSID)) {
                beaconInfo.get(BSSID).addRSSI(RSSIlevel);
            } else {
                beaconInfo.put(BSSID, new BSSIDInfo(SSID, RSSIlevel));
            }
        }
        for (String name : beaconInfo.keySet()) {
            System.out.println(name + " " + beaconInfo.get(name).getRSSILevels());
        }
    }

}

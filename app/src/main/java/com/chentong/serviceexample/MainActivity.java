package com.chentong.serviceexample;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WifiManager wifiManager;
    private static final int PERMISSION_REQUEST_LOCATION = 1;

    //保持所启动的Service的IBinder对象,同时定义一个ServiceConnection对象
    TestServiceOne.MyBinder myBinder;
//    TestServiceTwo.MyBinder myBinder2;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        //Activity与Service连接成功时回调该方法
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("Service Connected", "success!");
            myBinder = (TestServiceOne.MyBinder) service;
//            myBinder2 = (TestServiceTwo.MyBinder) service;
        }

        //Activity与Service断开连接时回调该方法
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("Service Disconnected", "error!");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnServiceStart = findViewById(R.id.main_btnServiceStart);
        Button btnServiceStop = findViewById(R.id.main_btnServiceStop);
        Button btnServiceStatus = findViewById(R.id.main_btnServiceStatus);
        btnServiceStart.setOnClickListener(this);
        btnServiceStop.setOnClickListener(this);
        btnServiceStatus.setOnClickListener(this);
        Intent intent = new Intent();
//        intent.setAction("com.chentong.serviceexample.TEST_SERVICE_ONE");
        intent.setAction("com.chentong.serviceexample.TEST_SERVICE_TWO");
        //Android 5.0之后，隐式调用是除了设置setAction()外，还需要设置setPackage();
        intent.setPackage("com.chentong.serviceexample");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_btnServiceStart: {
                showPermissionCheck();
                break;
            }
            case R.id.main_btnServiceStop: {
                //解除绑定
//                unbindService(serviceConnection);
                Intent intent2 = new Intent(this,TestServiceTwo.class);
                //停止服务
                stopService(intent2);
                break;
            }
            case R.id.main_btnServiceStatus: {
                int count = myBinder.getCount();
                Toast.makeText(MainActivity.this, "Service的count值：" + count, Toast.LENGTH_SHORT).show();
                break;
            }
            default:
                break;
        }
    }

    private void showPermissionCheck() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent1 = new Intent(this,TestServiceTwo.class);
            startService(intent1);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Intent intent1 = new Intent(this,TestServiceTwo.class);
                startService(intent1);
            } else {
            }
        }
    }
}
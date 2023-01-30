package com.linmu.collision_warning_system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.baidu.location.LocationClient;
import com.linmu.collision_warning_system.listener.LocationListener;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient = null;
    private LocationListener mLocationListener = null;


    // 创建并初始化handler
    private final Handler locationHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TextView coordinateTextView = (TextView) findViewById(R.id.location_coordinate);
            if(msg.what == 1000) {
                Bundle bundle = msg.getData();
                double latitude = bundle.getDouble("lat");
                double longitude = bundle.getDouble("lon");
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(5);
                nf.setRoundingMode(RoundingMode.UP);
                String coordinate = "( " + nf.format(longitude) + " , " + nf.format(latitude) + " )";
                coordinateTextView.setText(coordinate);
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置是否同意隐私合规政策，true表示用户同意，false表示用户不同意(需要在LocationClient实例化前调用)
        LocationClient.setAgreePrivacy(true);

        try {
            // 初始化定位客户端
            mLocationClient = new LocationClient(getApplicationContext());
            // 初始化定位监听器
            mLocationListener = new LocationListener(locationHandler);
            // 配置定位监听器
            mLocationClient.registerLocationListener(mLocationListener);
        } catch (Exception e) {
            Log.e("初始化定位客户端失败",e.toString());
        }

        mLocationClient.start();
    }

}
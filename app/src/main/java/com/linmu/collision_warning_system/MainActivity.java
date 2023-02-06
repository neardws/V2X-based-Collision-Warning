package com.linmu.collision_warning_system;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.linmu.collision_warning_system.services.LocationService;
import com.linmu.collision_warning_system.services.TraceService;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    private Context context;

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    private TraceService traceService;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        verifyStoragePermissions(this);

        // 是否同意隐私政策，默认为false
        SDKInitializer.setAgreePrivacy(context, true);

//        verifyStoragePermissions(this);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        try {
            // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
            SDKInitializer.initialize(context);
            //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
            //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
            SDKInitializer.setCoordType(CoordType.BD09LL);
        } catch (BaiduMapSDKException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);

        LocationService locationService = new LocationService(context,locationHandler);
        locationService.startLocation();


        traceService = new TraceService(context);
        traceService.start();

        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(Float.parseFloat("19"));
        mBaiduMap.setMapStatus(msu);
        mBaiduMap.setMyLocationEnabled(true);
        MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null);
        mBaiduMap.setMyLocationConfiguration(myLocationConfiguration);
    }
    @Override
    protected void onResume() {
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        super.onResume();
    }
    @Override
    protected void onPause() {
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        traceService.stop();
        mBaiduMap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }

    private final Handler locationHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TextView coordinateTextView = findViewById(R.id.location_coordinate);
            if (msg.what == 1000) {
                Bundle bundle = msg.getData();
                double latitude = bundle.getDouble("lat");
                double longitude = bundle.getDouble("lon");

                float radius = bundle.getFloat("radius");
                float direction = bundle.getFloat("direction");

                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(radius)
                        .direction(direction) // 此处设置开发者获取到的方向信息，顺时针0-360
                        .latitude(latitude)
                        .longitude(longitude).build();

                mBaiduMap.setMyLocationData(locData);

                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(5);
                nf.setRoundingMode(RoundingMode.UP);
                String coordinate = "( " + nf.format(longitude) + " , " + nf.format(latitude) + " )";
                coordinateTextView.setText(coordinate);
            }
        }
    };

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
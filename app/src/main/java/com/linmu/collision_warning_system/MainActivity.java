package com.linmu.collision_warning_system;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.linmu.collision_warning_system.fragment.MapFragment;
import com.linmu.collision_warning_system.services.LocationService;
import com.linmu.collision_warning_system.services.TraceService;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class MainActivity extends FragmentActivity {

    private Context context;

    private static final String sNormalFragmentTag = "map_fragment";
    private FragmentManager mFragmentManager;
    private MapFragment mMapFragment;

    private MapView mMapView;

    private BaiduMap baiduMap;

    private LocationService locationService;
    private TraceService traceService;


    //权限数组（申请定位）
    private final String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    //返回code
    private static final int ALL_FILE_REQUEST_CODE = 101;
    private static final int OPEN_SET_REQUEST_CODE = 100;
    //调用此方法判断是否拥有权限



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        initPermissions();

        setContentView(R.layout.activity_main);

        initMapFragment();



        locationService = new LocationService(context,new LocationListener());
        locationService.startLocation();

//        traceService = new TraceService(context);
//        traceService.start();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mMapView = mMapFragment.getMapView();
        baiduMap = mMapFragment.getBaiduMap();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        traceService.stop();
        locationService.stopLocation();
    }

    /**
     * 初始化地图
     */
    private void initMapFragment() {
        mFragmentManager = getSupportFragmentManager();

        BaiduMapOptions baiduMapOptions = new BaiduMapOptions();
        baiduMapOptions.zoomControlsEnabled(false);
        mMapFragment = MapFragment.newInstance(baiduMapOptions);

        mFragmentManager.beginTransaction()
                .add(R.id.mapLayout
                        , mMapFragment
                        , sNormalFragmentTag)
                .commit();
    }


    public class LocationListener extends BDAbstractLocationListener {
        // 定位接收函数
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            // 这里的BDLocation为接收到的定位结果信息类

            // 当 MapView 被销毁后，停止处理
            if(mMapView == null) {
                Log.e("mMapView", "onReceiveLocation: no MapView" );
                return;
            }

            // 纬度信息
            double latitude = bdLocation.getLatitude();
            // 经度信息
            double longitude = bdLocation.getLongitude();
            // 定位精度
            float radius = bdLocation.getRadius();
            // 速度
//            float speed = bdLocation.getSpeed();
            // 经纬度坐标类型
//            String coordinateType = bdLocation.getCoorType();
            // 定位类型或定位错误返回码
//            int errorCode = bdLocation.getLocType();
            // 方向
            float direction = bdLocation.getDirection();

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(radius)
                    .direction(direction) // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(latitude)
                    .longitude(longitude).build();

            baiduMap = mMapView.getMap();

            baiduMap.setMyLocationData(locData);

            // 更新经纬度文本
            TextView coordinateTextView = findViewById(R.id.location_coordinate);
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(5);
            nf.setRoundingMode(RoundingMode.UP);
            String coordinate = "( " + nf.format(longitude) + " , " + nf.format(latitude) + " )";
            coordinateTextView.setText(coordinate);
        }
    }

    /**
     * 初始化权限
     * 检查是否拥有权限列表内的权限，若无则申请
     */
    private void initPermissions() {

        //检查是否已经有权限
        if (!Environment.isExternalStorageManager()) {
            //跳转新页面申请权限
            ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                //申请权限结果
                if (result.getResultCode() == ALL_FILE_REQUEST_CODE) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(MainActivity.this, "访问所有文件权限申请成功", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intentActivityResultLauncher.launch(intent);
        }

        if (lacksPermission(permissions)) {//判断是否拥有权限
            //请求权限，第二参数权限String数据，第三个参数是请求码便于在onRequestPermissionsResult 方法中根据code进行判断
            ActivityCompat.requestPermissions(this, permissions, OPEN_SET_REQUEST_CODE);
        }

    }

    /**
     * 判断是否缺少权限
     * 如果返回true表示缺少权限
     */
    public boolean lacksPermission(String[] permissions) {
        for (String permission : permissions) {
            //判断是否缺少权限，true=缺少权限
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    /**
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //响应Code
        if (requestCode == OPEN_SET_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "未拥有相应权限", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }
}
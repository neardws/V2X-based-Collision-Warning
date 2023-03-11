package com.linmu.collision_warning_system;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;
import com.linmu.collision_warning_system.fragment.CarInfoFragment;
import com.linmu.collision_warning_system.fragment.CommunicationFragment;
import com.linmu.collision_warning_system.fragment.MapFragment;
import com.linmu.collision_warning_system.fragment.MyFragmentAdapter;
import com.linmu.collision_warning_system.services.CommunicationService;
import com.linmu.collision_warning_system.services.NcsLocationService;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MainActivity extends FragmentActivity {
    private static final Car car = new Car("demoCar");

    private Context context;
    // 碎片标签
    private static final String sNormalFragmentTag = "map_fragment";
    private MapFragment mMapFragment;
    private CarInfoFragment mCarInfoFragment;
    private CommunicationFragment mCommunicationFragment;

    private MapView mMapView;
    private BaiduMap mBaiduMap;

    private boolean firstLocation = true;

    private Polyline mPolyline;

    private final BitmapDescriptor mGreenTexture = BitmapDescriptorFactory.fromAsset("Icon_road_green_arrow.png");


    private ConcurrentHashMap<String,Car> carHashMap;

    private CommunicationService communicationService;

    private NcsLocationService ncsLocationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取应用上下文
        context = getApplicationContext();
        // 检查并动态申请权限
        initPermissions();
        setContentView(R.layout.activity_main);

        // 初始化 pageview
        initPager();
        // 初始化地图碎片
        initMapFragment();

        // 创建通信服务
        communicationService = new CommunicationService(context);

        mCommunicationFragment.setCommunicationService(communicationService);

        // 初始化车辆hashMap
        carHashMap = new ConcurrentHashMap<>();
        // 初始化 NCS 定位服务
        ncsLocationService = new NcsLocationService(communicationService);
        ncsLocationService.checkNcsState();
        ncsLocationService.loginNcs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView = mMapFragment.getMapView();
        mBaiduMap = mMapFragment.getBaiduMap();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        communicationService.stopCommunication();
        ncsLocationService.logoutNcs();
        if (null != mGreenTexture) {
            mGreenTexture.recycle();
        }
    }

    /**
     * 初始化ViewPager2
     */
    private void initPager() {
        //Fragment
        List<Fragment> list = new ArrayList<>();
        list.add(CarInfoFragment.newInstance());
        list.add(CommunicationFragment.newInstance());
        //TODO 这里必须创建完后获取，具体原因待测试
        mCarInfoFragment = (CarInfoFragment) list.get(0);
        mCommunicationFragment = (CommunicationFragment) list.get(1);

        MyFragmentAdapter myFragmentAdapter = new MyFragmentAdapter(getSupportFragmentManager(),getLifecycle(),list);
        ViewPager2 viewPager = findViewById(R.id.viewpage2);
        viewPager.setAdapter(myFragmentAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                Log.d("positionOffset", ""+positionOffset);
                Log.d("position", ""+position);
            }
        });
    }

    /**
     * 初始化地图
     */
    private void initMapFragment() {
        FragmentManager mFragmentManager = getSupportFragmentManager();

        BaiduMapOptions baiduMapOptions = new BaiduMapOptions();
        baiduMapOptions.zoomControlsEnabled(false);
        // 动态创建地图碎片
        mMapFragment = MapFragment.newInstance(baiduMapOptions);

        mFragmentManager.beginTransaction()
                .add(R.id.mapLayout
                        , mMapFragment
                        , sNormalFragmentTag)
                .commit();
    }

    /**
     * 定位消息监听器
     */
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

            // 若是第一次定位，则进行初始化，并抛弃定位数据。
            if(firstLocation) {
                carHashMap.put(car.getCarId(),car);
                initPolyLine();
                firstLocation = false;
                return;
            }
            ConcurrentLinkedDeque<LatLng> latLonDeque = car.getDeque();
            if(latLonDeque == null) {
                Log.e("classEmpty", "onReceiveLocation: 位置队列为null");
                return;
            }
            // 当储存位置数量达到10个后，弹出最早的位置。
            if(latLonDeque.size() >= 10) {
                latLonDeque.pollLast();
            }

            // 纬度信息
            double latitude = bdLocation.getLatitude();
            // 经度信息
            double longitude = bdLocation.getLongitude();
            // 定位精度
            float radius = bdLocation.getRadius();
            // 速度
            float speed = bdLocation.getSpeed();

            // 定位类型或定位错误返回码
//            int errorCode = bdLocation.getLocType();
            // 方向
            float direction = bdLocation.getDirection();

            // 添加新位置进入队列
            LatLng newPosition = new LatLng(latitude,longitude);
            latLonDeque.addFirst(newPosition);
            car.setDeque(latLonDeque);
            car.setLatLng(newPosition);
            car.setSpeed(speed);
            car.setDirection(direction);

            mCarInfoFragment.addValueToSnakeView(speed);

            //更新绘制
            drawUpdatePolyLine();
            // 更新地图显示
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(radius)
                    .direction(direction) // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(latitude)
                    .longitude(longitude).build();
            mBaiduMap = mMapView.getMap();
            mBaiduMap.setMyLocationData(locData);

        }
    }

    /**
     * 初始化路径纹理
     */
    private void initPolyLine() {
        // 初始化需要至少两个点数据
        List<LatLng> polylineList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            polylineList.add(car.getLatLng());
        }
        // 绘制纹理PolyLine
        PolylineOptions polylineOptions =
                new PolylineOptions().points(polylineList)
                        .width(20)
                        .customTexture(mGreenTexture)
                        .dottedLine(true)
                        .zIndex(3);
        mPolyline = (Polyline) mBaiduMap.addOverlay(polylineOptions);
    }
    /**
     * 更新&绘制路径
     */
    private void drawUpdatePolyLine() {
        List<LatLng> polylineList = new ArrayList<>(car.getDeque());
        Collections.reverse(polylineList);
        mPolyline.setPoints(polylineList);
    }

    /***************************************************************************************
     * 以下部分为申请权限
     ***************************************************************************************
     */

    //权限数组（申请定位）
    private final String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    private static final int OPEN_SET_REQUEST_CODE = 100;

    /**
     * 初始化权限
     * 检查是否拥有权限列表内的权限，若无则申请
     */
    private void initPermissions() {
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
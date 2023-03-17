package com.linmu.collision_warning_system;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.baidu.mapapi.map.BaiduMapOptions;
import com.linmu.collision_warning_system.fragment.CarInfoFragment;
import com.linmu.collision_warning_system.fragment.CommunicationFragment;
import com.linmu.collision_warning_system.fragment.MapFragment;
import com.linmu.collision_warning_system.fragment.MyFragmentAdapter;
import com.linmu.collision_warning_system.fragment.OfflineMapFragment;
import com.linmu.collision_warning_system.services.CommunicationService;
import com.linmu.collision_warning_system.services.NcsLocationService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private Context context;
    private CommunicationService communicationService;
    private NcsLocationService ncsLocationService;
    private FragmentManager fragmentManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取应用上下文
        context = getApplicationContext();
        // 检查并动态申请权限
        initPermissions();
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();

        // 初始化 pageview
        initPager();
        // 初始化地图碎片
        initMapFragment();

        // 创建通信服务
        CommunicationService.initConfig(context,fragmentManager);
        communicationService = CommunicationService.getInstance();

        // 初始化 NCS 定位服务
        ncsLocationService = NcsLocationService.getInstance();

        // TODO 添加循环尝试登录
        ncsLocationService.checkNcsState();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }

    /**
     * 初始化ViewPager2
     */
    private void initPager() {
        //Fragment
        List<Fragment> list = new ArrayList<>();
        list.add(OfflineMapFragment.newInstance());
        list.add(CommunicationFragment.newInstance());
        list.add(CarInfoFragment.newInstance());

        CommunicationFragment communicationFragment = (CommunicationFragment) list.get(1);
        communicationFragment.initCommunication();

        MyFragmentAdapter myFragmentAdapter = new MyFragmentAdapter(fragmentManager,getLifecycle(),list);
        ViewPager2 viewPager = findViewById(R.id.viewpage2);
        viewPager.setAdapter(myFragmentAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        });
        viewPager.setCurrentItem(1);
    }

    /**
     * 初始化地图
     */
    private void initMapFragment() {
        FragmentManager mFragmentManager = getSupportFragmentManager();

        BaiduMapOptions baiduMapOptions = new BaiduMapOptions();
        baiduMapOptions.zoomControlsEnabled(false);
        // 动态创建地图碎片
        MapFragment mMapFragment = MapFragment.newInstance(baiduMapOptions);
        final String sNormalFragmentTag = "map_fragment";
        mFragmentManager.beginTransaction()
                .add(R.id.mapLayout
                        , mMapFragment
                        , sNormalFragmentTag)
                .commit();
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
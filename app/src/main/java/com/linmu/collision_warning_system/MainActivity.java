package com.linmu.collision_warning_system;

import android.Manifest;
import android.app.Dialog;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * @version V1.0
 * @name MainActivity
 * @author linmu
 * @description 主活动
 * @date 2023-04-06 16:15
*/
public class MainActivity extends FragmentActivity {
    /** 上下文 **/
    private Context context;
    /** 碎片管理对象 **/
    private FragmentManager fragmentManager;
    /** 通讯服务对象 **/
    private CommunicationService communicationService;
    /** NCS服务对象 **/
    private NcsLocationService ncsLocationService;

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
        CommunicationService.initConfig(fragmentManager);
        communicationService = CommunicationService.getInstance();

        // 初始化 NCS 定位服务
        ncsLocationService = NcsLocationService.getInstance();

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
        ncsLocationService.logoutNcs();
        communicationService.stopCommunication();
    }

    /**
     * @name 初始化 viewPage 页面
     * @description 创建碎片页并添加到 viewPage 中，配置 viewPage。
     * @date 2023-04-06 16:17
     */
    private void initPager() {
        //Fragment
        List<Fragment> list = new ArrayList<>();
        list.add(OfflineMapFragment.newInstance());
        list.add(CommunicationFragment.newInstance());
        list.add(CarInfoFragment.newInstance());

        fragmentManager.setFragmentResultListener("warning",this, this::doHandleWarning);

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
     * @name 处理警告信号
     * @description 弹出警告 dialog
     * @param requestKey 请求键
     * @param result 消息体
     * @date 2023-04-06 16:19
     */
    private void doHandleWarning(String requestKey, @NonNull Bundle result) {
//        int type = result.getInt("warning");
        Dialog dialog = new Dialog(this, R.style.warning_dialog_style);
        dialog.show();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                dialog.dismiss();
                timer.cancel();
            }
        },2000);
    }

    /**
     * @name 初始化地图碎片
     * @description 配置初始化选项，然后动态创建地图碎片。
     * @date 2023-04-06 16:32
     */
    private void initMapFragment() {
        BaiduMapOptions baiduMapOptions = new BaiduMapOptions();
        baiduMapOptions.zoomControlsEnabled(false);
        // 动态创建地图碎片
        MapFragment mMapFragment = MapFragment.newInstance(baiduMapOptions);
        final String sNormalFragmentTag = "map_fragment";
        fragmentManager.beginTransaction()
                .add(R.id.mapLayout
                        , mMapFragment
                        , sNormalFragmentTag)
                .commit();
    }

    /*
    ========================================================================================
    ==================================申 请 权 限=============================================
    ========================================================================================
    */
    /** 请求编码 **/
    private static final int OPEN_SET_REQUEST_CODE = 100;
    /**
     * @name 初始化权限
     * @description 检查是否拥有权限列表内的权限，若无则申请
     * @date 2023-04-06 16:30
     */
    private void initPermissions() {
        //权限数组
        final String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };
        if (lacksPermission(permissions)) {//判断是否拥有权限
            //请求权限，第二参数权限String数据，第三个参数是请求码便于在onRequestPermissionsResult 方法中根据code进行判断
            ActivityCompat.requestPermissions(this, permissions, OPEN_SET_REQUEST_CODE);
        }
    }

    /**
     * @name 判断是否缺少权限
     * @description 如果返回true表示缺少权限
     * @param permissions 权限数组
     * @date 2023-04-06 16:28
     */
    public boolean lacksPermission(@NonNull String[] permissions) {
        for (String permission : permissions) {
            //判断是否缺少权限，true=缺少权限
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }
    /**
     * @name onRequestPermissionsResult
     * @description TODO
     * @param requestCode 请求编码
     * @param permissions 权限数组
     * @param grantResults 返回结果
     * @date 2023-04-06 16:29
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
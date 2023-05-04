package com.linmu.collision_warning_system;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.os.Vibrator;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 是否同意隐私政策，默认为false
        SDKInitializer.setAgreePrivacy(getApplicationContext(), true);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        try {
            // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
            SDKInitializer.initialize(getApplicationContext());
            //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
            //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
            SDKInitializer.setCoordType(CoordType.BD09LL);
        } catch (BaiduMapSDKException e) {
            e.printStackTrace();
        }
    }
}

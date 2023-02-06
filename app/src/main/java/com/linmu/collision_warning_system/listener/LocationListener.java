package com.linmu.collision_warning_system.listener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class LocationListener extends BDAbstractLocationListener {
    // 用于向主线程传递定位结果
    Handler locationHandler;
    // 构造函数
    public LocationListener(Handler handler) {
        locationHandler = handler;
    }

    // 定位接收函数
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        // 这里的BDLocation为接收到的定位结果信息类

        // 纬度信息
        double latitude = bdLocation.getLatitude();
        // 经度信息
        double longitude = bdLocation.getLongitude();
        // 定位精度
        float radius = bdLocation.getRadius();
        // 速度
        float speed = bdLocation.getSpeed();
        // 经纬度坐标类型
        String coordinateType = bdLocation.getCoorType();
        // 定位类型或定位错误返回码
        int errorCode = bdLocation.getLocType();
        // 方向
        float direction = bdLocation.getDirection();

        Message msg = new Message();
        msg.what = 1000;
        Bundle bundle = new Bundle();
        bundle.putDouble("lat",latitude);
        bundle.putDouble("lon",longitude);
        bundle.putFloat("radius",radius);
        bundle.putFloat("direction",direction);
        bundle.putFloat("speed",speed);
        bundle.putString("coordinateType",coordinateType);
        bundle.putInt("errorCode",errorCode);
        msg.setData(bundle);

        locationHandler.sendMessage(msg);
    }
}

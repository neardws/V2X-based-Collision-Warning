package com.linmu.collision_warning_system.services;

import android.util.Log;

import com.linmu.collision_warning_system.Entry.Car;

public class CarManageService {
    private static Car carSelf = null;
    public static boolean initCarSelf(String carId) {
        if(carSelf != null) {
            Log.e("initCarSelf", "自己车辆已存在!");
            return false;
        }
        CarManageService.carSelf = new Car(carId);
        // 纹理初始化需要至少两个点数据
        carSelf.addLatLatLngToDeque(carSelf.getLatLng());
        return true;
    }
    public static Car getCarSelf() {
        return carSelf;
    }
}

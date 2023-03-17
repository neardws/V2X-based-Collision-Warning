package com.linmu.collision_warning_system.services;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;

import java.util.concurrent.atomic.AtomicReference;

public class CarManageService {
    private static Car carSelf = null;
    public static boolean initCarSelf(String carId) {
        if(carSelf != null) {
            Log.e("initCarSelf", "自己车辆已存在!");
            return false;
        }
        Car newCar = new Car(carId);
        // 纹理初始化需要至少两个点数据
        newCar.addLatLatLngToDeque(newCar.getLatLng());
        CarManageService.carSelf = newCar;
        return true;
    }
    public static Car getCarSelf() {
        return carSelf;
    }
    public static void updateCarSelf(LatLng newLatlng, float newSpeed, float newDirection) {
        carSelf.setLatLng(newLatlng);
        carSelf.setSpeed(newSpeed);
        carSelf.setDirection(newDirection);
        carSelf.addLatLatLngToDeque(newLatlng);
    }
}
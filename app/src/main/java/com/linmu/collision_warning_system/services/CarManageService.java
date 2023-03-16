package com.linmu.collision_warning_system.services;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;

import java.util.concurrent.atomic.AtomicReference;

public class CarManageService {
    private static AtomicReference<Car> carSelfReference = null;
    public static boolean initCarSelf(String carId) {
        if(carSelfReference != null) {
            Log.e("initCarSelf", "自己车辆已存在!");
            return false;
        }
        Car carSelf = new Car(carId);
        // 纹理初始化需要至少两个点数据
        carSelf.addLatLatLngToDeque(carSelf.getLatLng());
        CarManageService.carSelfReference = new AtomicReference<>(carSelf);
        return true;
    }
    public static Car getCarSelf() {
        return carSelfReference.get();
    }
    public static void updateCarSelf(LatLng newLatlng, float newSpeed, float newDirection) {
        Car carSelf = getCarSelf();
        carSelf.setLatLng(newLatlng);
        carSelf.setSpeed(newSpeed);
        carSelf.setDirection(newDirection);
        carSelf.addLatLatLngToDeque(newLatlng);
        carSelfReference.set(carSelf);
    }
}

package com.linmu.collision_warning_system.services;

import androidx.annotation.NonNull;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CarManageService {
    private static CarManageService INSTANCE;
    private static String carSelfId;
    private static Car carSelf = null;
    public static void setCarSelfId(String carId) {
        if(carSelfId != null) {
            return;
        }
        carSelfId = carId;
    }
    public static Car getCarSelf() {
        return carSelf;
    }

    public static CarManageService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new CarManageService();
        }
        return INSTANCE;
    }

    private final ConcurrentHashMap<String,Car> carMap;

    public CarManageService() {
        carMap = new ConcurrentHashMap<>();
    }

    public List<Car> getCarList() {
        return new ArrayList<>(carMap.values());
    }

    public int getCarCount() {
        return carMap.size();
    }

    /**
     * 更新车辆生命周期
     */
    public void updateCarsLife() {
        for (Map.Entry<String, Car> item : carMap.entrySet()) {
            String obuId = item.getKey();
            Car car = item.getValue();
            car.updateLife();
            if (car.getLife() <= 0) {
                carMap.remove(obuId);
                continue;
            }
            carMap.put(obuId, car);
        }
    }

    public void addCarInfo(@NonNull String obuId, LatLng newLatlng, float newSpeed, float newDirection) {
        if(obuId.equals(carSelfId)) {
            addSelfCarInfo(obuId, newLatlng, newSpeed, newDirection);
        }
        else {
            addOthersCarInfo(obuId, newLatlng, newSpeed, newDirection);
        }
    }
    private void addSelfCarInfo(String obuId, LatLng newLatlng, float newSpeed, float newDirection) {
        if(carSelf == null) {
            carSelf = new Car(obuId, newLatlng, newSpeed, newDirection);
        }
        carSelf.addCarInfo(newLatlng, newSpeed, newDirection);
        carSelf.keepLife();
    }
    private void addOthersCarInfo(String obuId, LatLng newLatlng, float newSpeed, float newDirection) {
        Car car = carMap.get(obuId);
        if(car == null) {
            car = new Car(obuId, newLatlng, newSpeed, newDirection);
        }
        car.addCarInfo(newLatlng, newSpeed, newDirection);
        car.keepLife();
        carMap.put(obuId,car);
    }
}
package com.linmu.collision_warning_system.services;

import androidx.annotation.NonNull;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;
import com.linmu.collision_warning_system.Entry.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CarManageService {
    private static CarManageService INSTANCE;
    private static String thisCarId;
    private static Car thisCar = null;
    public static void setThisCarId(String carId) {
        if(thisCarId != null) {
            return;
        }
        thisCarId = carId;
    }
    public static Car getThisCar() {
        return thisCar;
    }

    public static CarManageService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new CarManageService();
        }
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, Car> carMap;
    private final ConcurrentHashMap<String, List<Coordinate>> predictListMap;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<LatLng>> latLngDequeMap;

    public CarManageService() {
        carMap = new ConcurrentHashMap<>();
        predictListMap = new ConcurrentHashMap<>();
        latLngDequeMap = new ConcurrentHashMap<>();
    }

    public List<Car> getCarList() {
        return new ArrayList<>(carMap.values());
    }

    public int getCarCount() {
        return carMap.size();
    }

    public List<Coordinate> getPredictList(String obuId) {
        List<Coordinate> list = predictListMap.get(obuId);
        if(list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void setPredictList(String obuId, List<Coordinate> predictList) {
        predictListMap.put(obuId,predictList);
    }

    public ConcurrentLinkedDeque<LatLng> getLatLngDeque(String obuId) {
        return latLngDequeMap.get(obuId);
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

    public void addCarInfo(@NonNull String obuId, LatLng newLatlng, double altitude, float newSpeed, float newDirection) {
        if(obuId.equals(thisCarId)) {
            addSelfCarInfo(obuId, altitude, newLatlng, newSpeed, newDirection);
        }
        else {
            addOthersCarInfo(obuId, altitude, newLatlng, newSpeed, newDirection);
        }
        ConcurrentLinkedDeque<LatLng> deque = latLngDequeMap.get(obuId);
        if(deque == null) {
            deque = new ConcurrentLinkedDeque<>();
            deque.addFirst(newLatlng);
        }
        if(deque.size() >= 50) {
            deque.pollLast();
        }
        deque.addFirst(newLatlng);
        latLngDequeMap.put(obuId,deque);
    }
    private void addSelfCarInfo(String obuId, double altitude, LatLng newLatlng, float newSpeed, float newDirection) {
        if(thisCar == null) {
            thisCar = new Car(obuId, altitude, newLatlng, newSpeed, newDirection);
        }
        thisCar.addCarInfo(newLatlng, altitude, newSpeed, newDirection);
        thisCar.keepLife();
        WarningService.getInstance().checkCollision();
    }
    private void addOthersCarInfo(String obuId, double altitude, LatLng newLatlng, float newSpeed, float newDirection) {
        Car car = carMap.get(obuId);
        if(car == null) {
            car = new Car(obuId, altitude, newLatlng, newSpeed, newDirection);
        }
        car.addCarInfo(newLatlng, altitude, newSpeed, newDirection);
        car.keepLife();
        carMap.put(obuId,car);
    }
}
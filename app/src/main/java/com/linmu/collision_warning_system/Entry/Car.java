package com.linmu.collision_warning_system.Entry;

import com.baidu.mapapi.model.LatLng;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Car {
    private final String carId;
    private int life;
    LatLng latLng;
    private float speed;
    private float direction;
    private final ConcurrentLinkedDeque<LatLng> latLngDeque;

    public Car(String carId, LatLng latLng, float speed, float direction) {
        this.carId = carId;
        this.life = 100;
        this.latLng = latLng;
        this.speed = speed;
        this.direction = direction;
        this.latLngDeque = new ConcurrentLinkedDeque<>();
        // 添加初始化数据
        this.latLngDeque.addFirst(this.latLng);
    }

    public String getCarId() {
        return carId;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public float getSpeed() {
        return speed;
    }

    public float getDirection() {
        return direction;
    }
    public int getLife() {
        return life;
    }
    public void keepLife() {
        this.life = 10;
    }
    public void updateLife() {
        this.life -= 1;
    }

    public ConcurrentLinkedDeque<LatLng> getLatLngDeque() {
        return latLngDeque;
    }

    public void addCarInfo(LatLng latLng, float speed, float direction) {
        this.latLng = latLng;
        this.speed = speed;
        this.direction = direction;
        if(this.latLngDeque.size() >= 10) {
            this.latLngDeque.pollLast();
        }
        this.latLngDeque.addFirst(latLng);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return carId.equals(car.carId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carId);
    }
}

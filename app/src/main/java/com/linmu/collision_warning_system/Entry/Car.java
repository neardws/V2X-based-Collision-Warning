package com.linmu.collision_warning_system.Entry;

import com.baidu.mapapi.model.LatLng;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Car {
    private final String carId;

    LatLng latLng;
    private float speed;
    private float direction;

    private final ConcurrentLinkedDeque<LatLng> latLngDeque;

    public Car(String carId) {
        this.carId = carId;
        this.speed = 0.0f;
        this.latLng = new LatLng(29.738706d, 106.808177d); // 重庆卓越工程师学院
        this.direction = 0.0f;
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

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getDirection() {
        return direction;
    }

    public void setDirection(float direction) {
        this.direction = direction;
    }

    public ConcurrentLinkedDeque<LatLng> getLatLngDeque() {
        return latLngDeque;
    }

    public void addLatLatLngToDeque(LatLng newLatLng) {
        if(this.latLngDeque.size() >= 10) {
            this.latLngDeque.pollLast();
        }
        this.latLngDeque.addFirst(newLatLng);
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

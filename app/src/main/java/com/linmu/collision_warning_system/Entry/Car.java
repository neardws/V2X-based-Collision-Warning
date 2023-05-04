package com.linmu.collision_warning_system.Entry;

import com.baidu.mapapi.model.LatLng;

import java.util.Objects;

public class Car {
    private final String carId;
    private int life;
    LatLng latLng;
    private double altitude;
    private float speed;
    private float speed_last;
    private float direction;
    private float direction_last;


    public Car(String carId, double altitude, LatLng latLng, float speed, float direction) {
        this.carId = carId;
        this.life = 100;
        this.latLng = latLng;
        this.altitude = altitude;
        this.speed = speed;
        this.speed_last = speed;
        this.direction = direction;
        this.direction_last = direction;
    }

    public String getCarId() {
        return carId;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public double getAltitude() {
        return altitude;
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

    public float getSpeed_last() {
        return speed_last;
    }

    public float getDirection_last() {
        return direction_last;
    }

    public void keepLife() {
        this.life = 100;
    }
    public void updateLife() {
        this.life -= 1;
    }



    public void addCarInfo(LatLng latLng, double altitude, float speed, float direction) {
        this.latLng = latLng;
        this.altitude = altitude;
        this.speed_last = this.speed;
        this.speed = speed;
        this.direction_last = this.direction;
        this.direction = direction;
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

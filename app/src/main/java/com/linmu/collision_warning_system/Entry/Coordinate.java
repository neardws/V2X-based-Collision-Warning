package com.linmu.collision_warning_system.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.mapapi.model.LatLng;

import java.util.Locale;

/**
 * @author linmu
 * @version V1.0
 * @name CenterCoordinate
 * @description 地心直角坐标系下的点
 * @date 2023-04-06 20:38
 **/
public class Coordinate {
    /** 扁率 **/
    private final static double f = 1/298.257223563;
    /** 地球赤道半径 **/
    private final static double r = 6378137;
    /** 偏心率 **/
    private final static double e = Math.sqrt(2*f-f*f);
    public final double x,y,z;

    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @name 通过经纬度创建地心直角坐标系下的坐标
     * @description 基于 WGS84下的经纬度和海拔来计算地心直角坐标系下的坐标。
     * @param latitude 纬度
     * @param longitude 经度
     * @param altitude 海拔
     * @return 地心直角坐标系下的坐标
     * @date 2023-04-09 17:02
     */
    @NonNull
    public static Coordinate createCoordinateFromBLH(double latitude, double longitude, double altitude) {
        double B = Math.toRadians(latitude);
        double L = Math.toRadians(longitude);
        double N = r/Math.sqrt(1-e*e*Math.sin(B)*Math.sin(B));
        double x = (N+altitude)*Math.cos(B)*Math.cos(L);
        double y = (N+altitude)*Math.cos(B)*Math.sin(L);
        double z = (N*(1-e*e)+altitude)*Math.sin(B);
        return new Coordinate(x,y,z);
    }
    public static LatLng xyz2BLH(@NonNull Coordinate point)
    {
        double x = point.x;
        double y = point.y;
        double z = point.z;
        double epsilon = 0.000000000000001;
        double r2d = 180 / Math.PI;


        double N;
        double curB = 0;
        double xy_sqrt = Math.sqrt(x * x + y * y);
        double calB = Math.atan2(z, xy_sqrt);

        int counter = 0;
        while (Math.abs(curB - calB) * r2d > epsilon  && counter < 25)
        {
            curB = calB;
            N = r / Math.sqrt(1 - e * e * Math.sin(curB) * Math.sin(curB));
            calB = Math.atan2(z + N * e * e * Math.sin(curB), xy_sqrt);
            counter++;
        }

        double L = Math.atan2(y, x) * r2d;
        double B = curB * r2d;
//        double H = z / Math.sin(curB) - N * (1 - e * e);
        return new LatLng(B,L);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.CHINA,"(%5f,%5f,%5f)",x,y,z);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof Coordinate)) return false;
        Coordinate o = (Coordinate) obj;
        return this.x == o.x && this.y == o.y && this.z == o.z;
    }
}

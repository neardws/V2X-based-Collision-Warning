package com.linmu.collision_warning_system.utils;

import androidx.annotation.NonNull;

import com.linmu.collision_warning_system.Entry.Car;
import com.linmu.collision_warning_system.Entry.Coordinate;
import com.linmu.collision_warning_system.Entry.Vector;

/**
 * @author linmu
 * @version V1.0
 * @name CarCoordinateUtil
 * @description 车辆直角坐标系相关服务
 * @date 2023-04-09 17:15
 **/
public class CoordinateUtil {
    /** 本车位置 **/
    private final Coordinate base;
    /** 用于加速计算的中间变量 **/
    private final double dxy,dxyz,z_power;

    public CoordinateUtil(@NonNull Car car) {
        this.base = Coordinate.createCoordinateFromBLH(car.getLatLng().latitude,car.getLatLng().longitude,car.getAltitude());
        dxy = (base.x * base.x) + (base.y * base.y);
        z_power = base.z * base.z;
        dxyz = dxy + z_power;
    }

    public Coordinate getBase() {
        return base;
    }

    /**
     * @name 计算相对坐标
     * @description 将本坐标相对于标准点
     * @param point 待计算坐标点
     * @return 相对坐标
     * @date 2023-04-09 17:02
     */
    public Coordinate relativeCoordinate(@NonNull Coordinate point) {
        return new Coordinate(point.x-base.x, point.y-base.y, point.z-base.z);
    }
    /**
     * @name countDistance
     * @description 计算两点距离
     * @param point1 第一个点
     * @param point2 第二个点
     * @return double 距离
     * @date 2023-04-17 16:31
     */
    public double countDistance(@NonNull Coordinate point1, @NonNull Coordinate point2) {
        double dis_x = point1.x - point2.x;
        double dis_y = point1.y - point2.y;
        double dis_z = point1.z - point2.z;
        return Math.sqrt(dis_x*dis_x + dis_y*dis_y + dis_z* dis_z);
    }

    /**
     * @name 转换方向
     * @description 将车辆方向由原来的标量，转化为车心直角坐标系三维向量。
     * @param direction 大地坐标系下的速度方向。
     * @return 车心直角坐标系下的车辆速度向量
     * @date 2023-04-09 17:02
     */
    public Vector convertDirection(double direction) {
        double dir = Math.toRadians(direction);
        double cos_dir = Math.cos(dir);
        double tempV = Math.sqrt( (1-cos_dir*cos_dir) / dxy );
        double tempU = Math.sqrt( (dxy * dxyz) / z_power );
        double dir_x, dir_y, dir_z;
        double temp_x_sub = -(base.x * z_power * tempU * cos_dir) / (dxy * dxyz);
        double temp_y_sub = -(base.y * z_power * tempU * cos_dir) / (dxy * dxyz);
        if(dir <= Math.PI) {
            dir_x = temp_x_sub - base.y * tempV;
            dir_y = temp_y_sub + base.x * tempV;
        }
        else {
            dir_x = temp_x_sub + base.y * tempV;
            dir_y = temp_y_sub - base.x * tempV;
        }
        dir_z = (base.z * tempU * cos_dir)/dxyz;
        return new Vector(dir_x,dir_y,dir_z);
    }
    /**
     * @name 计算位置
     * @description 根据速度及时长计算新的位置
     * @param speed 速度向量
     * @param time 时长
     * @return Coordinate 新的位置
     * @date 2023-04-13 16:20
     */
    public Coordinate move(@NonNull Coordinate coordinate, @NonNull Vector speed, @NonNull Vector acc, int time) {
        double x_add = coordinate.x + speed.x*time + 0.5*acc.x*time*time;
        double y_add = coordinate.y + speed.y*time + 0.5*acc.y*time*time;
        double z_add = coordinate.z + speed.z*time + 0.5*acc.z*time*time;
        return new Coordinate(x_add, y_add, z_add);
    }

}

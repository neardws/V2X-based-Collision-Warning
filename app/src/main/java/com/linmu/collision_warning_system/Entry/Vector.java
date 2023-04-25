package com.linmu.collision_warning_system.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author linmu
 * @version V1.0
 * @name Vector
 * @description 向量
 * @date 2023-04-17 18:06
 **/
public class Vector {
    public double x,y,z;

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @name 向量点乘
     * @description 通过速度方向向量和速率来计算速度向量
     * @param vector 向量
     * @param value 速率
     * @return Coordinate 速度向量
     * @date 2023-04-13 16:17
     */
    public static Vector dotMultiply(@NonNull Vector vector, double value) {
        double x_value = vector.x * value;
        double y_value = vector.y * value;
        double z_value = vector.z * value;
        return new Vector(x_value,y_value,z_value);
    }

    /**
     * @name sub
     * @description 向量减法
     * @param vector1 向量一
     * @param vector2 向量二
     * @return Vector
     * @date 2023-04-17 18:15
     */
    public static Vector sub(@NonNull Vector vector1, @NonNull Vector vector2) {
        return new Vector(vector1.x-vector2.x, vector1.y-vector2.y, vector1.z-vector2.z);
    }
    /**
     * @name add
     * @description 向量假发
     * @param vector1 向量一
     * @param vector2 向量二
     * @return Vector
     * @date 2023-04-19 17:33
     */
    public static Vector add(@NonNull Vector vector1, @NonNull Vector vector2) {
        return new Vector(vector1.x+vector2.x, vector1.y+vector2.y, vector1.z+vector2.z);
    }
    /**
     * @name abs
     * @description 计算向量绝对值
     * @param vector 向量
     * @return 绝对值
     * @date 2023-04-17 18:33
     */
    public static double abs(@NonNull Vector vector) {
        return Math.sqrt(vector.x*vector.x + vector.y*vector.y + vector.z*vector.z);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof Vector)) return false;
        Vector o = (Vector) obj;
        return this.x == o.x && this.y == o.y && this.z == o.z;
    }
}

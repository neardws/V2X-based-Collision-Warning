package com.linmu.collision_warning_system.services;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.linmu.collision_warning_system.Entry.Car;
import com.linmu.collision_warning_system.Entry.Coordinate;
import com.linmu.collision_warning_system.Entry.Vector;
import com.linmu.collision_warning_system.utils.CoordinateUtil;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version V1.0
 * @name WarningService
 * @author linmu
 * @description 碰撞预警服务
 * @date 2023-04-06 16:38
*/
public class WarningService {
    /** 类静态实例 **/
    private static WarningService INSTANCE;
    /**
     * @name 获取单例
     * @description 懒汉式获取警告服务的单例
     * @return WarningService 警告服务单例
     * @date 2023-04-06 16:35
     */
    public static WarningService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new WarningService();
        }
        return INSTANCE;
    }
    private final CarManageService carManageService;
    private final CommunicationService communicationService;
    private CoordinateUtil coordinateUtil;
    private double warningValue;
    private final HashMap<String,Long> warningCountMap;
    private int count;

    private WarningService() {
        carManageService = CarManageService.getInstance();
        communicationService = CommunicationService.getInstance();
        warningValue = 0.00004d;
        warningCountMap = new HashMap<>();
        count = 0;
    }

    public void checkCollision() {
        count += 1;
        if(count % 10 == 0) {
            Iterator<Map.Entry<String,Long>> iterator;
            for(iterator = warningCountMap.entrySet().iterator();iterator.hasNext();) {
                Map.Entry<String,Long> item = iterator.next();
                Long value = item.getValue();
                if(value > 1) {
                    item.setValue(value-1);
                }
                else {
                    iterator.remove();
                }
            }
            count = 0;
        }

        // 获取本车
        Car thisCar = CarManageService.getThisCar();
        // 创建本车参考系工具类
        coordinateUtil = new CoordinateUtil(thisCar);
        // 获取本车直角坐标系坐标
        Coordinate thisCarCoordinate = coordinateUtil.getBase();
        // 计算车辆速度方向向量
        Vector thisCarDirection = coordinateUtil.convertDirection(thisCar.getDirection());
        // 计算车辆上一次速度方向向量
        Vector thisCarDirectionLast = coordinateUtil.convertDirection(thisCar.getDirection_last());
        // 计算车辆速度向量
        Vector thisCarSpeed = Vector.dotMultiply(thisCarDirection, thisCar.getSpeed());
        // 计算车辆上一次速度向量
        Vector thisCarSpeedLast = Vector.dotMultiply(thisCarDirectionLast,thisCar.getSpeed_last());
        // 计算车辆加速度(两次间隔为0.1s)
        Vector thisCarAcc = Vector.dotMultiply(Vector.sub(thisCarSpeed,thisCarSpeedLast),10.0d);

        // 预测位置
        List<Coordinate> thisCarPredictList = predict(thisCarCoordinate,thisCarSpeed,thisCarAcc);
        carManageService.setPredictList(thisCar.getCarId(),thisCarPredictList);


        List<Double> distances = new ArrayList<>();
        List<Car> carList = carManageService.getCarList();
        for(Car otherCar:carList) {
            Coordinate otherCarCoordinate = Coordinate.createCoordinateFromBLH(otherCar.getLatLng().latitude,otherCar.getLatLng().longitude,otherCar.getAltitude());
            // 计算车辆速度方向向量
            Vector otherCarDirection = coordinateUtil.convertDirection(otherCar.getDirection());
            // 计算车辆上一次速度方向向量
            Vector otherCarDirectionLast = coordinateUtil.convertDirection(otherCar.getDirection_last());
            // 计算车辆速度向量
            Vector otherCarSpeed = Vector.dotMultiply(otherCarDirection, otherCar.getSpeed());
            // 计算车辆上一次速度向量
            Vector otherCarSpeedLast = Vector.dotMultiply(otherCarDirectionLast,otherCar.getSpeed_last());
            // 计算车辆加速度
            Vector otherCarAcc = Vector.dotMultiply(Vector.sub(otherCarSpeed,otherCarSpeedLast),10.0d);
            distances.add(coordinateUtil.countDistance(thisCarCoordinate,otherCarCoordinate));
            // 预测位置
            List<Coordinate> otherCarPredictList = predict(otherCarCoordinate,otherCarSpeed,otherCarAcc);
            carManageService.setPredictList(otherCar.getCarId(),otherCarPredictList);

            checkPredictCollision(thisCarPredictList,otherCarPredictList,otherCar.getCarId());
        }
        communicationService.passMessageToUI("predict",new Bundle());
        NumberFormat nf_dis = NumberFormat.getNumberInstance();
        nf_dis.setMaximumFractionDigits(2);
        nf_dis.setRoundingMode(RoundingMode.UP);
        Bundle logBundle = new Bundle();
        logBundle.putInt("type",3);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for(Double d:distances) {
            stringBuilder.append(nf_dis.format(d));
            stringBuilder.append(",");
        }
        stringBuilder.append("]");
        logBundle.putString("log",stringBuilder.toString());
        communicationService.passMessageToUI("NcsLog", logBundle);
    }
    /**
     * @name checkPredictCollision
     * @description 检查预测位置的碰撞
     * @param thisCarPredictList 本车预测列表
     * @param otherCarPredictList 他车预测列表
     * @date 2023-04-17 19:27
     */
    private void checkPredictCollision(@NonNull List<Coordinate> thisCarPredictList, @NonNull List<Coordinate> otherCarPredictList,String obuId) {
        for(int i=0;i<thisCarPredictList.size();i++) {
            Coordinate thisCarPredict = thisCarPredictList.get(i);
            Coordinate otherCarPredict = otherCarPredictList.get(i);

            if(carGaussianCdf(thisCarPredict,otherCarPredict,10.0d) >= warningValue) {
                Long warningCount = warningCountMap.get(obuId);
                if(warningCount == null) {
                    warningCount = 0L;
                }
                warningCount += 1;

                if(warningCount >= 3) {
                    warningCount = 0L;
                    int warningType;
                    if(i <= 2) {
                        warningType = 1;
                    }
                    else {
                        warningType = 2;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt("warningType",warningType);
                    communicationService.passMessageToUI("warning",bundle);
                }
                warningCountMap.put(obuId,warningCount);
                return;
            }
        }
    }
    /**  
     * @name 预测车辆位置
     * @description 预测车辆接下来1-6秒内的位置
     * @param coordinate 坐标位置
     * @param speed 速度向量
     * @param acc 加速度向量
     * @return List<Coordinate> 位置列表(1-6秒)
     * @date 2023-04-17 14:20   
     */
    @NonNull
    private List<Coordinate> predict(Coordinate coordinate, Vector speed, Vector acc) {
        Coordinate predictCoordinate;
        List<Coordinate> predictList = new ArrayList<>();
        for (int i = 1; i < 7; i++) {
            // 计算车辆预测位置
            predictCoordinate = coordinateUtil.move(coordinate, speed,acc, i);
            predictList.add(predictCoordinate);
        }
        return predictList;
    }

    /**
     * @name carGaussianCdf
     * @description 计算车辆高斯分布三维累积密度概率
     * @date 2023-04-19 18:25
     */
    public double carGaussianCdf(Coordinate thisCarCoordinate, Coordinate otherCarCoordinate, double sigma) {
        double width = 0.5d;
        double cdf_x = gaussianCdf(otherCarCoordinate.x-width, otherCarCoordinate.x+width, thisCarCoordinate.x, sigma);
        double cdf_y = gaussianCdf(otherCarCoordinate.y-width, otherCarCoordinate.y+width, thisCarCoordinate.y, sigma);
        double cdf_z = gaussianCdf(otherCarCoordinate.z-width, otherCarCoordinate.z+width, thisCarCoordinate.z, sigma);
        return cdf_x*cdf_y*cdf_z;
    }
    /**
     * @name gaussianCdf
     * @description 计算累积密度函数
     * @date 2023-04-19 18:57
     */
    public double gaussianCdf(double x1,double x2, double mu, double sigma) {
        // 创建一个均为2，标准差为3的正态分布
        NormalDistribution normal = new NormalDistribution(mu, sigma);
        // 计算从0到x的累积分布函数值
        double cdf1 = normal.cumulativeProbability(x1);
        double cdf2 = normal.cumulativeProbability(x2);
        return cdf2 - cdf1;
    }

    public double getWarningValue() {
        return warningValue;
    }

    public void setWarningValue(double warningValue) {
        this.warningValue = warningValue;
    }
}

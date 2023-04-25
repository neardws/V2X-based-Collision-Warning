package com.linmu.collision_warning_system.services;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.MessageType;
import com.linmu.collision_warning_system.utils.NcsTag;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**   
 * @version V1.0   
 * @name Ncs定位服务
 * @author linmu
 * @description 与NCS相关的服务，主要包括发起请求和消息处理两个部分。
 * @date 2023-04-05 14:02
*/
public class NcsLocationService {
    /** 类静态实例 **/
    private static NcsLocationService INSTANCE;
    /**
     * @name 获取NCS定位服务实例
     * @description 懒汉式获取类的单例
     * @return NcsLocationService 单例对象
     * @date 2023-04-05 14:04
     */
    public static NcsLocationService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new NcsLocationService();
        }
        return INSTANCE;
    }
    /** NCS 登录时注册的标识 **/
    private String unique = null;
    private final CarManageService carManageService;
    private final CommunicationService communicationService;
    /**
     * @description 私有构造函数
     */
    private NcsLocationService() {
        carManageService = CarManageService.getInstance();
        communicationService = CommunicationService.getInstance();
    }
    /*
    ====================================================================================
    ==================================发 起 请 求=========================================
    ====================================================================================
    */
    /**
     * @name checkNcsState
     * @description 发起查询ncs状态的请求
     * @date 2023-04-05 16:22
     */
    public void checkNcsState() {
        JSONObject askNcsState;
        try {
            askNcsState = new JSONObject();
            askNcsState.put("tag", NcsTag.Broadcast.getTag());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceive(MessageType.Once.getPort(), askNcsState);
    }
    /**
     * @name loginNcs
     * @description 发起ncs登录请求
     * @date 2023-04-05 16:25
     */
    public void loginNcs() {
        Log.w("MyLogTag", "loginNcs: 开始尝试登录!");
        JSONObject loginNcs = new JSONObject();
        try {
            loginNcs.put("tag",NcsTag.Login.getTag());
            JSONObject loginNcs_data = new JSONObject();
            loginNcs_data.put("ip", IpUtil.getIpAddress());
            loginNcs_data.put("port",50501);
            loginNcs.put("data",loginNcs_data);
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceive(MessageType.Once.getPort(), loginNcs);
    }
    public void sentAskNcsState() {
        JSONObject askNcsState;
        try {
            askNcsState = new JSONObject();
            askNcsState.put("tag", NcsTag.State.getTag());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceive(MessageType.Log.getPort(), askNcsState);
    }

    /**
     * @name keepNcsAlive
     * @description 发起ncs激活请求
     * @date 2023-04-05 16:25
     */
    public void keepNcsAlive() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        long period = Long.parseLong(PropertiesUtil.getValue("activate.period"));
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Log.i("MyLogTag", "keepNcsAlive: 激活NCS");
            JSONObject activateNcs = new JSONObject();
            try {
                activateNcs.put("tag",NcsTag.Activate.getTag());
                activateNcs.put("unique", unique);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            communicationService.sentAndReceive(MessageType.Once.getPort(), activateNcs);
        },0,period, TimeUnit.SECONDS);
    }
    /**
     * @name logoutNcs
     * @description 发起NCS注销请求
     * @date 2023-04-05 16:28
     */
    public void logoutNcs() {
        JSONObject logoutNcs;
        try {
            logoutNcs = new JSONObject();
            logoutNcs.put("tag",NcsTag.Logout.getTag());
            logoutNcs.put("ip", unique);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 使用随机端口发送
        communicationService.sentAndReceive(-1,logoutNcs);
    }

    /*
    ====================================================================================
    ==================================消 息 处 理=========================================
    ====================================================================================
    */

    /**
     * @name doHandleReceiveMessage
     * @description 处理接收到的消息
     * @param msg 接收到的消息体
     * @return boolean 处理结果
     * @date 2023-04-05 17:15
     */
    protected boolean doHandleReceiveMessage(@NonNull Message msg) {
        boolean handleRes;
        MessageType messageType = MessageType.getMessageType(msg.what);
        if(messageType == null) {
            return false;
        }
        switch (messageType) {
            case Once: {
                handleRes = this.doHandleOnceMessage(msg);
                return handleRes;
            }
            case Push: {
                handleRes = this.doHandlePushMessage(msg);
                return handleRes;
            }
            case Log: {
                handleRes = this.doHandleTestMessage(msg);
                return handleRes;
            }
        }
        return false;
    }
    /**
     * @name 处理单次接收的消息
     * @description 用于处理仅会接收一次的消息
     * @param msg 接收到的消息体
     * @return boolean 处理结果
     * @date 2023-04-05 14:07
     */
    public boolean doHandleOnceMessage(@NonNull Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;
        int tag;
        try {
            tag = jsonObject.getInt("tag");
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        NcsTag ncsTag = NcsTag.getTag(tag);
        if(ncsTag == null) {
            return false;
        }
        switch (ncsTag) {
            case BroadcastReturn: {
                handleBroadcastReturn(jsonObject);
                break;
            }
            case LoginReturn: {
                handleLoginReturn(jsonObject);
                break;
            }
            case ActivateReturn: {
                Log.i("MyLogTag", String.format("doHandleOnceMessage: %s",jsonObject));
            }
            default:
                return false;
        }
        return true;
    }
    /**
     * @name doHandleStateCheckRes
     * @description 处理
     * @param res Json数据体
     * @date 2023-04-05 17:25
     */
    private void handleBroadcastReturn(@NonNull JSONObject res) {
        String obuId;
        try {
            JSONObject data = res.getJSONObject("data");
            obuId = data.getString("vehicle_num");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        CarManageService.setThisCarId(obuId);
        Log.w("checkNcsState", String.format("广播寻址成功! OBU_id: %s",obuId));
        Log.w("checkNcsState", String.format("json: %s",res));

        // 进行NCS登录
        this.loginNcs();
    }
    /**
     * @name doHandleLoginRes
     * @description 处理接收到的登录反馈消息
     * @param res 待处理的登录反馈Json对象
     * @date 2023-04-06 12:54
     */
    private void handleLoginReturn(@NonNull JSONObject res) {
        int rsp;
        try {
            rsp = (int) res.get("rsp");
            unique = (String) res.get("unique");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String wordRsp = rsp == 0 ? "成功" : "失败";
        Log.w("MyLogTag", String.format("doHandleLoginRes: \n 登录: %s ", wordRsp));
        if(rsp != 0) {
            if(unique == null) {
                Log.w("MyLogTag", "doHandleLoginRes: 登录失败, 即将重新尝试登录。");
                this.loginNcs();
                return;
            }
            Log.w("MyLogTag", String.format("doHandleLoginRes: 在请求之前，已完成注册登录 unique: %s",unique));
        }
        // 通信服务开始接受消息
        communicationService.startReceivePushInfo();
        this.keepNcsAlive();
    }
    /**
     * @name doHandlePushMessage
     * @description 从消息中取出Json对象，并解析出tag，进而选择对应处理。
     * @param msg 待处理的推送消息
     * @return boolean 处理结果
     * @date 2023-04-06 12:56
     */
    private boolean doHandlePushMessage(@NonNull Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;

        // 发送消息给log页面显示
        Bundle logBundle = new Bundle();


        // 解析数据包
        int tag;
        try {
            tag = jsonObject.getInt("tag");
            if(tag == NcsTag.ThisCarInfo.getTag()) {
                handleThisCarInfo(jsonObject);
//                Log.i("MyLogTag", String.format("本车信息 : %d %s",System.currentTimeMillis(),jsonObject));
                logBundle.putInt("type",1);
                logBundle.putString("log",jsonObject.toString());
                communicationService.passMessageToUI("NcsLog", logBundle);
            }
            else if (tag == NcsTag.OtherCarInfo.getTag()){
                handleOtherCarInfo(jsonObject);
                logBundle.putInt("type",2);
                logBundle.putString("log",jsonObject.toString());
                communicationService.passMessageToUI("NcsLog", logBundle);
            } else if (tag == NcsTag.RsiINFO.getTag()) {
                doHandleRsiInfo(jsonObject.getJSONObject("data"));
            } else if (tag == NcsTag.StateInfo.getTag()) {
                // TODO 处理OBU状态信息
//                Log.w("MyLogTag", String.format("doHandleLocationMessage: OBU 状态信息 : \n %s", jsonObject));
            }
            else {
                Log.e("MyLogTag", String.format("doHandleReceiverMessage: 无法处理的tag \n tag : %d \n  : %s", tag, jsonObject));
                return false;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    /**
     * @name handleThisCarInfo
     * @description 提取并处理本车辆信息Json对象，通知页面更新。
     * @param jsonObject 待处理的本车消息
     * @date 2023-04-06 13:01
     */
    private void handleThisCarInfo(@NonNull JSONObject jsonObject) throws JSONException {
        JSONObject carData = jsonObject.getJSONObject("data");
        handleCarInfo(carData);

        Bundle ncsCarInfoUpdateSignal = new Bundle();
        ncsCarInfoUpdateSignal.putInt("type",1);
        communicationService.passMessageToUI("NcsLocationForMap",ncsCarInfoUpdateSignal);
        communicationService.passMessageToUI("NcsLocationForCarInfo",ncsCarInfoUpdateSignal);
//                Log.i("MyLogTag", String.format("doHandleReceiverMessage: \n tag : %d \n data : %s", tag, carData));
    }
    /**
     * @name handleOtherCarInfo
     * @description 循环提取并处理其他车辆信息Json对象，通知页面更新。
     * @param jsonObject 待处理的其他车辆信息
     * @date 2023-04-06 13:01
     */
    private void handleOtherCarInfo(@NonNull JSONObject jsonObject) throws JSONException {
        JSONArray carsData = jsonObject.getJSONArray("data");
        int length = carsData.length();
        for (int i = 0; i < length; i++) {
            JSONObject carData = carsData.getJSONObject(i);
            handleCarInfo(carData);
        }
        carManageService.updateCarsLife();
        Bundle ncsCarInfoUpdateSignal = new Bundle();
        ncsCarInfoUpdateSignal.putInt("type",2);
        communicationService.passMessageToUI("NcsLocationForMap",ncsCarInfoUpdateSignal);
//                Log.i("MyLogTag", String.format("doHandleReceiverMessage: \n tag : %d \n data : %s", tag, carsData));
    }
    /**
     * @name handleCarInfo
     * @description 从Json对象中解析并处理车辆信息
     * @param carData 待处理的车辆信息
     * @date 2023-04-06 13:06
     */
    private void handleCarInfo(@NonNull JSONObject carData) throws JSONException {
        // 解析 json 对象
        String obuId = carData.getString("device_id");
        double latitude = carData.getDouble("lat");
        double longitude = carData.getDouble("lon");
        double altitude = carData.getDouble("ele");
        double speed = carData.getDouble("spd");
        double direction = carData.getDouble("hea");
        // 坐标转换
        LatLng latLng = new LatLng(latitude,longitude);
        // 更新车辆信息
        carManageService.addCarInfo(obuId, latLng, altitude, (float)speed, (float)direction);
    }
    private void doHandleRsiInfo(@NonNull JSONObject rsiInfo) throws JSONException {
        int type = rsiInfo.getInt("type");
        if(type == 10409902) {
            Bundle bundle = new Bundle();
            bundle.putInt("warningType",1);
            communicationService.passMessageToUI("warning",bundle);
            Log.w("MyLogTag", "doHandleRsiInfo: 发出警告！！！");
        }
    }
    /**
     * @name doHandleTestMessage
     * @description 从消息中提取并处理测试数据，通知log页面更新。
     * @param msg 待处理的测试消息
     * @return boolean 处理结果
     * @date 2023-04-06 13:07
     */
    private boolean doHandleTestMessage(@NonNull Message msg) {
        JSONObject jsonObject = (JSONObject) msg.obj;
        Log.w("MyLogTag", String.format("doHandleTestMessage: %s",jsonObject));
        long time;
        try {
            JSONObject carData = jsonObject.getJSONObject("data");
            /* 用于测试时延 */
            time = carData.getLong("current_time");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 发送消息给log页面显示
        Bundle timeBundle = new Bundle();
        timeBundle.putLong("time",time);
        communicationService.passMessageToUI("NcsTime",timeBundle);
        return true;
    }

}

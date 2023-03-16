package com.linmu.collision_warning_system.services;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.fragment.app.FragmentManager;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.services.udp.UdpReceiver;
import com.linmu.collision_warning_system.services.udp.UdpSender;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通信服务类
 */
public class CommunicationService {
    private static final CommunicationService INSTANCE = new CommunicationService();
    public static CommunicationService getInstance() {
        return INSTANCE;
    }

    public static void initConfig(Context context, FragmentManager fragmentManager) {
        INSTANCE.fragmentManager = fragmentManager;

        Properties config = PropertiesUtil.getProperties(context,"CommunicationConfig.properties");
        int corePoolSize = Integer.parseInt(config.getProperty("send.corePoolSize"));
        int maxPoolSize = Integer.parseInt(config.getProperty("send.maxPoolSize"));
        long keepAliveTime = Long.parseLong(config.getProperty("send.keepAliveTime"));

        // 初始化线程池
        INSTANCE.mThreadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        INSTANCE.singlePort = Integer.parseInt(config.getProperty("singleSendAndReceive.port"));

        String receivePort = config.getProperty("receive.port");
        String buffSize = config.getProperty("BUFF_SIZE");
        INSTANCE.receiver = new UdpReceiver(receivePort, buffSize);

        String targetIp = config.getProperty("send.targetIp");
        String targetPort = config.getProperty("send.targetPort");
        INSTANCE.sender = new UdpSender(targetIp,targetPort);
        // 接受消息的 handler，具体处理放在 doHandleReceiveMessage
        Handler receiverHandler = new Handler(Looper.getMainLooper(), INSTANCE::doHandleReceiveMessage);
        INSTANCE.receiver.setHandler(receiverHandler);
    }
    private FragmentManager fragmentManager;
    private int singlePort;
    private UdpReceiver receiver;
    private UdpSender sender;
    private ThreadPoolExecutor mThreadPool;

    private CommunicationService() {}


    public void startReceive() {
        mThreadPool.execute(() -> {
            try {
                receiver.startReceive();
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sentAndReceive(JSONObject jsonObject){
        mThreadPool.execute(() -> {
            try {
                // 发送消息
                sender.send(singlePort,jsonObject);
                // 接收消息
                receiver.receiveOnce(singlePort);
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sentMessage(int port, JSONObject jsonObject) {
        mThreadPool.execute(() -> {
            try {
                sender.send(port,jsonObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void stopCommunication() {
        receiver.stopReceive();
    }

    private boolean doHandleReceiveMessage(Message msg) {
        boolean handleRes;
        switch (msg.what) {
            case 1111: {
                handleRes = NcsLocationService.getInstance().doHandleReceiveOnceMessage(msg);
                return handleRes;
            }
            case 2222: {
                handleRes = doHandleLocationMessage(msg);
                return handleRes;
            }
        }
        return false;
    }

    private boolean doHandleLocationMessage(Message msg) {
        JSONObject resJsonObject = (JSONObject) msg.obj;
        if(CarManageService.getCarSelf() == null) {
            Log.w("doHandleReceiveMessage", "本车还没有完成初始化! 拒绝处理接收消息!");
            return false;
        }
        Log.i("handleMessage", String.format("doHandleReceiverMessage: %s",resJsonObject.toString()));

        // 解析数据包
        int tag;
        JSONObject data;
        String obuId;
        double latitude,longitude,direction,speed;
        try {
            tag = resJsonObject.getInt("tag");
            if(tag == 2101) {
                data = resJsonObject.getJSONObject("data");
                obuId = data.getString("device_id");
                latitude = data.getDouble("lat");
                longitude = data.getDouble("lon");
                direction = data.getDouble("hea");
                speed = data.getDouble("spd");
            }
            else if (tag == 2102){
                // TODO 多车位置处理
                return true;
            }
            else {
                Log.e("doHandleReceiveMessage", "无法处理的tag");
                return false;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // 更新车辆信息
        CarManageService.updateCarSelf(new LatLng(latitude,longitude), (float) speed, (float) direction);

        Bundle logBundle = new Bundle();
        logBundle.putString("log",resJsonObject.toString());
        fragmentManager.setFragmentResult("NcsLog",logBundle);

        Bundle ncsCarInfoUpdateSignal = new Bundle();
        ncsCarInfoUpdateSignal.putString("obu_id",obuId);
        fragmentManager.setFragmentResult("NcsLocationForMap",ncsCarInfoUpdateSignal);
        fragmentManager.setFragmentResult("NcsLocationForCarInfo",ncsCarInfoUpdateSignal);

        return true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        receiver.stopReceive();
    }
}

package com.linmu.collision_warning_system.services;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.linmu.collision_warning_system.services.udp.UdpReceiver;
import com.linmu.collision_warning_system.services.udp.UdpSender;
import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通信服务类
 */
public class CommunicationService {
    private static CommunicationService INSTANCE;
    public static CommunicationService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new CommunicationService();
        }
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
        INSTANCE.singlePort = Integer.parseInt(config.getProperty("single.port"));
        INSTANCE.activatePort = Integer.parseInt(config.getProperty("activate.port"));
        INSTANCE.activatePeriod = Long.parseLong(config.getProperty("activate.period"));

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
    private int singlePort,activatePort;
    private long activatePeriod;
    private volatile boolean activateFlag;
    private UdpReceiver receiver;
    private UdpSender sender;
    private ThreadPoolExecutor mThreadPool;
    private final CarManageService carManageService;

    private CommunicationService() {
        carManageService = CarManageService.getInstance();
    }
    public void startReceive() {
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                receiver.startReceive();
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void sentAndReceiveTest(int port, JSONObject jsonObject,long waitTime){
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                Thread.sleep(waitTime);
                // 发送消息
                sender.send(port,jsonObject);
                // 接收消息
                receiver.receiveOnceTest(port);
            } catch (IOException | RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void sentAndReceive(JSONObject jsonObject,long waitTime){
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                Thread.sleep(waitTime);
                // 发送消息
                sender.send(singlePort,jsonObject);
                // 接收消息
                receiver.receiveOnce(singlePort);
            } catch (IOException | RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void sendMessage(int port, JSONObject jsonObject) {
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                sender.send(port,jsonObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void sendMessageConstantly(JSONObject jsonObject) {
        if(checkNetNotAvailable()) return;
        activateFlag = true;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!activateFlag) {
                    timer.cancel();
                }
                try {
                    // 发送消息
                    sender.send(activatePort,jsonObject);
                    // 接收消息
                    receiver.receiveOnce(activatePort);
                } catch (IOException | RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        },0,this.activatePeriod);
    }
    public void stopCommunication() {
        activateFlag = false;
        receiver.stopReceive();
    }

    private boolean doHandleReceiveMessage(@NonNull Message msg) {
        boolean handleRes;
        switch (msg.what) {
            case 1111: {
                handleRes = NcsLocationService.getInstance().doHandleReceiveOnceMessage(msg);
                return handleRes;
            }
            case 2222: {
                handleRes = this.doHandleLocationMessage(msg);
                return handleRes;
            }
            case 9999: {
                handleRes = this.doHandleTestMessage(msg);
                return handleRes;
            }
        }
        return false;
    }

    private boolean doHandleLocationMessage(@NonNull Message msg) {
        JSONObject resJsonObject = (JSONObject) msg.obj;

        // 发送消息给log页面显示
        Bundle logBundle = new Bundle();
        logBundle.putString("log",resJsonObject.toString());
        fragmentManager.setFragmentResult("NcsLog",logBundle);

        // 解析数据包
        int tag;
        try {
            tag = resJsonObject.getInt("tag");
            if(tag == 2101) {
                JSONObject carData = resJsonObject.getJSONObject("data");
                handleCarInfo(carData);

                Bundle ncsCarInfoUpdateSignal = new Bundle();
                ncsCarInfoUpdateSignal.putInt("type",1);
                fragmentManager.setFragmentResult("NcsLocationForMap",ncsCarInfoUpdateSignal);
                fragmentManager.setFragmentResult("NcsLocationForCarInfo",ncsCarInfoUpdateSignal);
//                Log.i("MyLogTag", String.format("doHandleReceiverMessage: \n tag : %d \n data : %s", tag, carData));
            }
            else if (tag == 2102){
                JSONArray carsData = resJsonObject.getJSONArray("data");
                int length = carsData.length();
                for (int i = 0; i < length; i++) {
                    JSONObject carData = carsData.getJSONObject(i);
                    handleCarInfo(carData);
                }
                carManageService.updateCarsLife();
                Bundle ncsCarInfoUpdateSignal = new Bundle();
                ncsCarInfoUpdateSignal.putInt("type",2);
                fragmentManager.setFragmentResult("NcsLocationForMap",ncsCarInfoUpdateSignal);
//                Log.i("MyLogTag", String.format("doHandleReceiverMessage: \n tag : %d \n data : %s", tag, carsData));
            }
            else if (tag == 2113) {
                // TODO 处理OBU状态信息
                Log.w("MyLogTag", String.format("doHandleLocationMessage: OBU 状态信息 : \n %s", resJsonObject));
            }
            else {
                Log.e("MyLogTag", String.format("doHandleReceiverMessage: 无法处理的tag \n tag : %d \n  : %s", tag, resJsonObject));
                return false;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    private void handleCarInfo(@NonNull JSONObject carData) throws JSONException {
        // 解析 json 对象
        String obuId = carData.getString("device_id");
        double latitude = carData.getDouble("lat");
        double longitude = carData.getDouble("lon");
        double speed = carData.getDouble("spd");
        double direction = carData.getDouble("hea");
        // 坐标转换
        LatLng latLng = new LatLng(latitude,longitude);
        CoordinateConverter coordinateConverter = new CoordinateConverter()
                .from(CoordinateConverter.CoordType.GPS)
                .coord(latLng);
        latLng = coordinateConverter.convert();
        // 更新车辆信息
        carManageService.addCarInfo(obuId, latLng, (float)speed, (float)direction);
    }
    private boolean checkNetNotAvailable() {
        if(IpUtil.getIpAddress() == null) {
            Log.e("MyLogTag", "sentAndReceive: 没有连接到网络！");
            return true;
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        receiver.stopReceive();
    }

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
        fragmentManager.setFragmentResult("NcsTime",timeBundle);
        return true;
    }
}

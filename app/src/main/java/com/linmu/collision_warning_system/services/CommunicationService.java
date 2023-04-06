package com.linmu.collision_warning_system.services;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.linmu.collision_warning_system.services.udp.UdpReceiver;
import com.linmu.collision_warning_system.services.udp.UdpSender;
import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

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

        String receivePort = config.getProperty("receive.port");
        String buffSize = config.getProperty("BUFF_SIZE");
        INSTANCE.receiver = new UdpReceiver(receivePort, buffSize);

        String targetIp = config.getProperty("send.targetIp");
        String targetPort = config.getProperty("send.targetPort");
        INSTANCE.sender = new UdpSender(targetIp,targetPort);
        // 接受消息的 handler，具体处理放在 doHandleReceiveMessage
        Handler receiverHandler = new Handler(Looper.getMainLooper(), NcsLocationService.getInstance()::doHandleReceiveMessage);
        INSTANCE.receiver.setHandler(receiverHandler);
    }
    private FragmentManager fragmentManager;
    private int singlePort;
    private UdpReceiver receiver;
    private UdpSender sender;
    private ThreadPoolExecutor mThreadPool;
    private CommunicationService() {}
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
    public void passMessageToUI(@NonNull String tag, Bundle bundle) {
        fragmentManager.setFragmentResult(tag,bundle);
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
    public void sentAndReceiveNcs(JSONObject jsonObject){
        if(checkNetNotAvailable()) return;
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

    private boolean checkNetNotAvailable() {
        if(IpUtil.getIpAddress() == null) {
            Log.e("MyLogTag", "sentAndReceive: 没有连接到网络！");
            return true;
        }
        return false;
    }

    public void stopCommunication() {
        receiver.stopReceive();
    }
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        receiver.stopReceive();
    }


}

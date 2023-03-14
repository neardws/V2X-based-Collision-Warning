package com.linmu.collision_warning_system.services;


import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

import com.linmu.collision_warning_system.services.udp.UdpReceiver;
import com.linmu.collision_warning_system.services.udp.UdpSender;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramSocket;
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
    public static void initConfig(Context context) {
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

        String receivePort = config.getProperty("receive.port");
        String buffSize = config.getProperty("BUFF_SIZE");
        INSTANCE.receiver = new UdpReceiver(receivePort, buffSize);

        String targetIp = config.getProperty("send.targetIp");
        String targetPort = config.getProperty("send.targetPort");
        INSTANCE.sender = new UdpSender(targetIp,targetPort);
    }

    private UdpReceiver receiver;
    private UdpSender sender;
    private ThreadPoolExecutor mThreadPool;

    private CommunicationService() {}


    public void setReceiverHandler(Handler receiverHandler) {
        receiver.setHandler(receiverHandler);
    }

    public void startReceive() {
        mThreadPool.execute(() -> {
            try {
                receiver.startReceive();
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sentAndReceive(int port, JSONObject jsonObject){
        mThreadPool.execute(() -> {
            try {
                sender.send(port,jsonObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DatagramSocket socket;
            try {
                socket = new DatagramSocket(port);
                receiver.receiveOnce(socket);
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
            socket.close();
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        receiver.stopReceive();
    }
}

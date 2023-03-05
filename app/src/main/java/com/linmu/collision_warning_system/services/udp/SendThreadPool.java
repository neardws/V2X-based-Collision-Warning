package com.linmu.collision_warning_system.services.udp;

import android.content.Context;

import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 发送udp消息的线程类
 */
public class SendThreadPool {

    private final String selfIp;
    private final String targetIp;
    private final int targetPort;
    private final int BUFF_SIZE;

    private final ThreadPoolExecutor mThreadPool;

    public SendThreadPool(Context context) {
        this.selfIp = IpUtil.getIpAddress();
        Properties config = PropertiesUtil.getProperties(context,"CommunicationConfig.properties");
        this.BUFF_SIZE = Integer.parseInt(config.getProperty("BUFF_SIZE"));
        targetIp = config.getProperty("send.targetIp");
        targetPort = Integer.parseInt(config.getProperty("send.targetPort"));
        int corePoolSize = Integer.parseInt(config.getProperty("send.corePoolSize"));
        int maxPoolSize = Integer.parseInt(config.getProperty("send.maxPoolSize"));
        long keepAliveTime = Long.parseLong(config.getProperty("send.keepAliveTime"));
        mThreadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void send(JSONObject jsonObject) {
        mThreadPool.execute(() -> {
            byte[] buff = new byte[BUFF_SIZE];//发送过来的数据的长度范围
            // 这里不添加参数，使用随机端口发送消息
            try(DatagramSocket sendSocket = new DatagramSocket()) {
                InetAddress targetAddress = InetAddress.getByName(targetIp);
                DatagramPacket outPacket = new DatagramPacket(buff,buff.length,targetAddress,targetPort);
                sendSocket.send(outPacket);
            } catch (SocketException|UnknownHostException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }
}

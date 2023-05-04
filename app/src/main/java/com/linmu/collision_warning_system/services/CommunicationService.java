package com.linmu.collision_warning_system.services;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.linmu.collision_warning_system.utils.udp.UdpReceiver;
import com.linmu.collision_warning_system.utils.udp.UdpSender;
import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @version V1.0
 * @name CommunicationService
 * @author linmu
 * @description 包含与OBU、UI页面通信的方法
 * @date 2023-04-06 13:28
*/
public class CommunicationService {
    /** 类静态实例 **/
    private static CommunicationService INSTANCE;
    /**
     * @name 获取NCS定位服务实例
     * @description 懒汉式获取类的单例
     * @return CommunicationService 通信服务单例
     * @date 2023-04-06 13:29
     */
    public static CommunicationService getInstance() {
        if(INSTANCE == null) {
            Log.e("MyLogTag", "getInstance: 通信服务还没有初始化");
        }
        return INSTANCE;
    }
    /**
     * @name 初始化通信服务配置
     * @description 从配置文件中读取配置并进行初始化
     * @param fragmentManager 碎片管理对象
     * @date 2023-04-06 13:33
     */
    public static void initConfig(FragmentManager fragmentManager) {
        INSTANCE = new CommunicationService();
        INSTANCE.fragmentManager = fragmentManager;

        int corePoolSize = Integer.parseInt(PropertiesUtil.getValue("send.corePoolSize"));
        int maxPoolSize = Integer.parseInt(PropertiesUtil.getValue("send.maxPoolSize"));
        long keepAliveTime = Long.parseLong(PropertiesUtil.getValue("send.keepAliveTime"));

        // 初始化线程池
        INSTANCE.mThreadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        INSTANCE.receiver = new UdpReceiver();

        String targetIp = PropertiesUtil.getValue("send.targetIp");
        String targetPort = PropertiesUtil.getValue("send.targetPort");
        INSTANCE.sender = new UdpSender(targetIp,targetPort);
        // 接受消息的 handler，具体处理放在 doHandleReceiveMessage
        Handler receiverHandler = new Handler(Looper.getMainLooper(), NcsService.getInstance()::doHandleReceiveMessage);
        INSTANCE.receiver.setHandler(receiverHandler);
    }
    /** 页面管理对象 **/
    private FragmentManager fragmentManager;
    /** UDP接收者 **/
    private UdpReceiver receiver;
    /** UDP发送者 **/
    private UdpSender sender;
    /** 线程池对象 **/
    private ThreadPoolExecutor mThreadPool;
    private CommunicationService() {}
    /**  
     * @name 将消息发送给UI页面
     * @description 通过碎片管理对象，发送消息给各个页面。
     * @param tag 消息的标签
     * @param bundle 消息体
     * @date 2023-04-06 13:41
     */ 
    public void passMessageToUI(@NonNull String tag, Bundle bundle) {
        fragmentManager.setFragmentResult(tag,bundle);
    }
    /**
     *@name 开始接收推送消息
     *@description 调用一个线程，开始接收推送的消息。
     *@date 2023-04-06 13:39
     */
    public void startReceivePushInfo() {
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                receiver.receivePushInfo();
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /**
     * @name 发送并接收消息
     * @description 从线程池获取线程发送消息后，接收反馈消息。
     * @param port 发送及接收的端口号
     * @param jsonObject 发送的Json对象
     * @date 2023-04-06 15:08
     */
    public void sentAndReceive(int port, JSONObject jsonObject){
        if(checkNetNotAvailable()) return;
        mThreadPool.execute(() -> {
            try {
                // 发送消息
                sender.send(port,jsonObject);
                // 接收消息
                receiver.receiveOnce(port);
            } catch (IOException | RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /**
     * @name testWifiDelay
     * @description 测试WIFI时延
     * @param port 端口
     * @param jsonObject 消息体
     * @date 2023-05-03 19:04
     */
    public void testWifiDelay(int port, JSONObject jsonObject) {
        mThreadPool.execute(() -> {
            try {
                // 发送消息
                long sendTime = sender.send(port,jsonObject);
                // 接收消息
                receiver.receiveTest(port,sendTime);
            } catch (IOException | RemoteException | JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /**
     * @name 检查网络是否断开
     * @description 通过尝试获取IP，来测试是否连接到网络。
     * @return boolean 网络是否断开
     * @date 2023-04-06 15:56
     */
    private boolean checkNetNotAvailable() {
        if(IpUtil.getIpAddress() == null) {
            Log.e("MyLogTag", "sentAndReceive: 没有连接到网络！");
            return true;
        }
        return false;
    }
    /**
     * @name stopCommunication
     * @description 停止通信
     * @date 2023-04-06 15:58
     */
    public void stopCommunication() {
        receiver.stopReceive();
        mThreadPool.shutdownNow();
    }
}

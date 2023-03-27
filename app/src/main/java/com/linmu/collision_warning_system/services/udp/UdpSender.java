package com.linmu.collision_warning_system.services.udp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * 发送udp消息的线程类
 */
public class UdpSender {

    private final InetAddress targetIp;
    private final int targetPort;

    public UdpSender(String targetIp, String targetPort) {

        // 将对方地址包装为InetAddress类
        try {
            this.targetIp = InetAddress.getByName(targetIp);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.targetPort = Integer.parseInt(targetPort);
    }

    public void send(int port, @NonNull JSONObject jsonObject) throws IOException {
        // 将数据转化为byte[]
        byte[] buff = jsonObject.toString().getBytes(StandardCharsets.US_ASCII);//发送过来的数据的长度范围
        // 组装数据包(数据，偏移量，数据长度，目标IP地址，目标端口号)
        DatagramPacket packet = new DatagramPacket(buff,0,buff.length,targetIp,targetPort);
        // 若 port 为-1则选择自动获取随机空闲端口进行发送
        DatagramSocket sendSocket = port == -1 ? new DatagramSocket() : new DatagramSocket(port);
        sendSocket.send(packet);
        sendSocket.close();
        Log.i("sendMessage",String.format("send:ip: %s port: %s json: %s",targetIp.getHostAddress(),port, jsonObject));
    }

}

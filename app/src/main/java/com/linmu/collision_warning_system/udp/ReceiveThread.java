package com.linmu.collision_warning_system.udp;

import android.content.Context;
import android.util.Log;

import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Properties;

/**
 * 接受udp消息的线程类
 */
public class ReceiveThread extends Thread {

    private final String ip;
    private final int port;

    private final int BUFF_SIZE;

    private boolean stop;

    public ReceiveThread(Context context) {
        this.ip = IpUtil.getIpAddress();

        Properties config = PropertiesUtil.getProperties(context,"CommunicationConfig.properties");
        this.port = Integer.parseInt(config.getProperty("receive.port"));
        this.BUFF_SIZE = Integer.parseInt(config.getProperty("BUFF_SIZE"));
        stop = false;
    }

    @Override
    public void run() {
        DatagramPacket receivePacket;
        try (DatagramSocket receiveSocket = new DatagramSocket(port)) {
            byte[] buff = new byte[BUFF_SIZE];//发送过来的数据的长度范围

            while (!stop){
                receivePacket = new DatagramPacket(buff, buff.length);

                receiveSocket.receive(receivePacket);

                String msg = new String(receivePacket.getData(),0,receivePacket.getLength());

                JSONObject jsonObject = new JSONObject(msg);

                Log.i("receiveMessage", String.format("run: ip: %s ,jsonObject: %s",ip,jsonObject));

            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void stopReceive(){
        this.stop = true;
    }
}

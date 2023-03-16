package com.linmu.collision_warning_system.services.udp;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 接受udp消息的线程类
 */
public class UdpReceiver {

    private final int port;

    private final int BUFF_SIZE;

    private boolean stop;

    private Messenger mServer = null;

    public UdpReceiver(String receivePort, String buffSize) {
        this.port = Integer.parseInt(receivePort);
        this.BUFF_SIZE = Integer.parseInt(buffSize);
        stop = false;
    }

    public void setHandler(Handler receiverHandler) {
        mServer = new Messenger(receiverHandler);
    }

    private JSONObject receive(DatagramSocket socket) throws IOException {
        JSONObject jsonObject;
        DatagramPacket receivePacket;
        byte[] buff = new byte[BUFF_SIZE];//发送过来的数据的长度范围


        receivePacket = new DatagramPacket(buff, buff.length);

        socket.receive(receivePacket);

        // 获取对方的IP地址
        InetAddress ipAddress = receivePacket.getAddress();
        String hostAddress = ipAddress.getHostAddress();

        // 获取到数据
        String msg = new String(receivePacket.getData(),0,receivePacket.getLength());

        // 将数据解析为 JSONObject
        try {
            jsonObject = new JSONObject(msg);
            jsonObject.put("senderIp",hostAddress);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        Log.i("MyLogTag", String.format("ReceiveThread: \n jsonObject: %s",jsonObject));

        return jsonObject;
    }

    public void receiveOnce(int port) throws RemoteException, IOException {
        // 创建通信
        DatagramSocket socket = new DatagramSocket(port);
        JSONObject jsonObject = receive(socket);
        socket.close();

        if(mServer == null) {
            Log.e("receiveOnce", "Message Server 还没有初始化");
            return;
        }
        // 将消息发送给主线程
        Message message = new Message();
        message.what = 1111;
        message.obj = jsonObject;
        mServer.send(message);
    }

    public void startReceive() throws IOException, RemoteException {
        DatagramSocket socket = new DatagramSocket(port);
        while (!stop) {
            JSONObject jsonObject;

            jsonObject = receive(socket);

            if(jsonObject == null) continue;

            if(mServer == null) {
                Log.e("startReceive", "Message Server 还没有初始化");
                continue;
            }

            // 创建消息并发送
            Message receivedMessage = Message.obtain();
            receivedMessage.what = 2222;
            receivedMessage.obj = jsonObject;
            mServer.send(receivedMessage);
        }
        socket.close();
    }

    public void stopReceive(){
        this.stop = true;
    }
}

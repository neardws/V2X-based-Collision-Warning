package com.linmu.collision_warning_system.services;


import android.content.Context;

import com.linmu.collision_warning_system.udp.ReceiveThread;
import com.linmu.collision_warning_system.udp.SendThreadPool;

import org.json.JSONObject;

/**
 * 通信服务类
 */
public class CommunicationService {

    ReceiveThread receiver;
    SendThreadPool sender;

    public CommunicationService(Context context) {
        receiver = new ReceiveThread(context);
        sender = new SendThreadPool(context);
    }

    public void startCommunication() {
        receiver.start();
    }

    public void sentMessage(JSONObject jsonObject) {
        sender.send(jsonObject);
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

package com.linmu.collision_warning_system.services;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.linmu.collision_warning_system.Application;
import com.linmu.collision_warning_system.utils.IpUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class NcsLocationService {

    private static NcsLocationService INSTANCE;
    public static NcsLocationService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new NcsLocationService();
        }
        return INSTANCE;
    }
    private String unique = null;
    private CommunicationService communicationService;
    private NcsLocationService() {}
    public boolean doHandleReceiveOnceMessage(@NonNull Message msg) {
        JSONObject res = (JSONObject) msg.obj;
        int tag;
        try {
            tag = (int) res.get("tag");
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        switch (tag) {
            case 1002: {
                doHandleStateCheckRes(res);
                break;
            }
            case 2002: {
                doHandleLoginRes(res);
                break;
            }
            default:
                return false;
        }
        return false;
    }

    public void checkNcsState() {
        JSONObject askNcsState;
        try {
            askNcsState = new JSONObject();
            askNcsState.put("tag",1001);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicationService = CommunicationService.getInstance();
        communicationService.sentAndReceiveNcs(askNcsState);
    }

    public void loginNcs() {
        Log.w("MyLogTag", "loginNcs: 开始尝试登录!");
        JSONObject loginNcs = new JSONObject();
        try {
            loginNcs.put("tag",2001);
            JSONObject loginNcs_data = new JSONObject();
            loginNcs_data.put("ip", IpUtil.getIpAddress());
            loginNcs_data.put("port",50501);
            loginNcs.put("data",loginNcs_data);
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceiveNcs(loginNcs);
    }

    public void keepNcsAlive() {
        JSONObject activateNcs = new JSONObject();
        try {
            activateNcs.put("tag",2006);
            activateNcs.put("unique", unique);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceiveNcs(activateNcs);
    }

    private void doHandleStateCheckRes(@NonNull JSONObject res) {
        String obuId;
        try {
            JSONObject data = res.getJSONObject("data");
            obuId = data.getString("vehicle_num");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        CarManageService.setCarSelfId(obuId);
        Log.w("checkNcsState", String.format("广播寻址成功! OBU_id: %s",obuId));
        Log.w("checkNcsState", String.format("json: %s",res));

        // 进行NCS登录
        this.loginNcs();
    }
    private void doHandleLoginRes(@NonNull JSONObject res) {
        int rsp;
        try {
            rsp = (int) res.get("rsp");
            unique = (String) res.get("unique");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String wordRsp = rsp == 0 ? "成功" : "失败";
        Log.w("MyLogTag", String.format("doHandleLoginRes: \n 登录: %s ", wordRsp));
        if(rsp != 0) {
            if(unique == null) {
                Log.w("MyLogTag", "doHandleLoginRes: 登录失败, 即将重新尝试登录。");
                this.loginNcs();
                return;
            }
            Log.w("MyLogTag", String.format("doHandleLoginRes: 在请求之前，已完成注册登录 unique: %s",unique));
        }
        // 通信服务开始接受消息
        communicationService.startReceive();

        WorkService.getInstance().keepNCSAlive(Application.getContext());
    }

    public void logoutNcs() {
        JSONObject logoutNcs;
        try {
            logoutNcs = new JSONObject();
            logoutNcs.put("tag",2003);
            logoutNcs.put("ip", unique);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 使用随机端口发送
        communicationService.sendMessage(-1,logoutNcs);
    }
}

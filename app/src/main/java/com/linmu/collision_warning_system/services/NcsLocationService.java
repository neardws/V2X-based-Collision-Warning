package com.linmu.collision_warning_system.services;

import android.os.Message;
import android.util.Log;

import com.linmu.collision_warning_system.utils.IpUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class NcsLocationService {

    private static final NcsLocationService INSTANCE = new NcsLocationService();
    public static void setCommunicationService(CommunicationService communicationService) {
        INSTANCE.communicationService = communicationService;
    }
    public static NcsLocationService getInstance() {
        return INSTANCE;
    }

    private String unique = null;
    private CommunicationService communicationService;

    private NcsLocationService() {}

    public boolean doHandleReceiveOnceMessage(Message msg) {
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
        communicationService.sentAndReceive(askNcsState);
    }

    public void loginNcs() {
        JSONObject loginNcs;
        try {
            loginNcs = new JSONObject();
            loginNcs.put("tag",2001);
            JSONObject loginNcs_data = new JSONObject();
            loginNcs_data.put("ip", IpUtil.getIpAddress());
            loginNcs_data.put("port",50501);
            loginNcs.put("data",loginNcs_data);
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceive(loginNcs);
    }
    private void doHandleStateCheckRes(JSONObject res) {
        String obuId;
        try {
            JSONObject data = res.getJSONObject("data");
            obuId = data.getString("vehicle_num");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        boolean initCarSelfRes = CarManageService.initCarSelf(obuId);
        String wordRsp = initCarSelfRes ? "车辆初始化成功" : "车辆初始化失败";
        Log.w("checkNcsState", String.format("广播寻址成功! %s OBU_id: %s",wordRsp,obuId));
        Log.w("checkNcsState", String.format("json: %s",res));

        // 进行NCS登录
        this.loginNcs();
    }
    private void doHandleLoginRes(JSONObject res) {
        int rsp;
        try {
            rsp = (int) res.get("rsp");
            unique = (String) res.get("unique");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String wordRsp = rsp == 0 ? "成功" : "失败";
        Log.w("NCS_login", String.format("startReceive:  登录: %s unique: %s",wordRsp,unique));
        // 通信服务开始接受消息
        communicationService.startReceive();
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
        communicationService.sentMessage(50501,logoutNcs);
    }
}

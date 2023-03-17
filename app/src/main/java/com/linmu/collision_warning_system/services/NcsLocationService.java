package com.linmu.collision_warning_system.services;

import android.os.Message;
import android.util.Log;

import com.linmu.collision_warning_system.utils.IpUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class NcsLocationService {

    private static final NcsLocationService INSTANCE = new NcsLocationService();
    public static NcsLocationService getInstance() {
        return INSTANCE;
    }
    private String unique = null;
    private int tryCheckNcsTimes, tryLoginNcsTimes;
    private NcsLocationService() {
        tryCheckNcsTimes = 0;
        tryLoginNcsTimes = 0;
    }
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
        if(tryCheckNcsTimes > 5) {
            Log.e("MyLogTag", "checkNcsState: 已尝试连接NCS 5次失败! 请检查网络连接情况!");
            return;
        }
        // 计算等待时间, 每失败一次多等待0.5s;
        long waitTime = tryCheckNcsTimes * 500L;
        CommunicationService.getInstance().sentAndReceive(askNcsState,waitTime);
    }

    public void loginNcs() {
        Log.w("MyLogTag", "loginNcs: 开始尝试登录!");
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
        if(tryLoginNcsTimes > 5) {
            Log.e("MyLogTag", "checkNcsState: 已尝试登录NCS 5次失败! 请检查情况!");
            return;
        }
        // 计算等待时间, 每失败一次多等待0.5s;
        long waitTime = tryCheckNcsTimes * 500L;
        CommunicationService.getInstance().sentAndReceive(loginNcs,waitTime);
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

        // 若车辆初始化失败则重新进行初始化
        if(!initCarSelfRes) {
            tryCheckNcsTimes += 1;
            checkNcsState();
            return;
        }
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
        Log.w("MyLogTag", String.format("doHandleLoginRes: \n 登录: %s ", wordRsp));
        if(rsp != 0) {
            if(unique == null) {
                Log.w("MyLogTag", "doHandleLoginRes: 登录失败, 即将重新尝试登录。");
                tryLoginNcsTimes += 1;
                this.loginNcs();
                return;
            }
            Log.w("MyLogTag", String.format("doHandleLoginRes: 在请求之前，已完成注册登录 unique: %s",unique));
        }
        // 通信服务开始接受消息
        CommunicationService.getInstance().startReceive();
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
        CommunicationService.getInstance().sentMessage(50502,logoutNcs);
    }
}

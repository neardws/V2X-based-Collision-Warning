package com.linmu.collision_warning_system.services;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.linmu.collision_warning_system.Application;
import com.linmu.collision_warning_system.utils.IpUtil;
import com.linmu.collision_warning_system.utils.NcsTag;

import org.json.JSONException;
import org.json.JSONObject;

/**   
 * @version V1.0   
 * @name Ncs定位服务
 * @author linmu
 * @description TODO
 * @date 2023-04-05 14:02
*/
public class NcsLocationService {
    /** 类静态实例 **/
    private static NcsLocationService INSTANCE;
    /**
     *@name 获取实例
     *@author linmu
     *@description 获取类的单例(懒汉式)
     *@return NcsLocationService INSTANCE
     *@date 2023-04-05 14:04
     */
    public static NcsLocationService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new NcsLocationService();
        }
        return INSTANCE;
    }
    /** NCS 登录时注册的标识 **/
    private String unique = null;
    /** 通信服务 **/
    private CommunicationService communicationService;
    private NcsLocationService() {}
    /**
     *@name 处理单次接收的消息
     *@author linmu
     *@description 用于处理仅会接收一次的消息
     *@param msg 接收到的消息体
     *@return boolean 处理结果
     *@date 2023-04-05 14:07
     */
    public boolean doHandleReceiveOnceMessage(@NonNull Message msg) {
        JSONObject res = (JSONObject) msg.obj;
        int tag;
        try {
            tag = res.getInt("tag");
        } catch (JSONException e) {
            logoutNcs();
            throw new RuntimeException(e);
        }
        NcsTag ncsTag = NcsTag.getTag(tag);
        if(ncsTag == null) {
            return false;
        }
        switch (ncsTag) {
            case BroadcastReturn: {
                doHandleStateCheckRes(res);
                break;
            }
            case LoginReturn: {
                doHandleLoginRes(res);
                break;
            }
            default:
                return false;
        }
        return true;
    }

    public void checkNcsState() {
        JSONObject askNcsState;
        try {
            askNcsState = new JSONObject();
            askNcsState.put("tag", NcsTag.Broadcast.getTag());
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
            loginNcs.put("tag",NcsTag.Login.getTag());
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
            activateNcs.put("tag",NcsTag.Activate.getTag());
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
            logoutNcs.put("tag",NcsTag.Logout.getTag());
            logoutNcs.put("ip", unique);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // 使用随机端口发送
        communicationService.sendMessage(-1,logoutNcs);
    }
}

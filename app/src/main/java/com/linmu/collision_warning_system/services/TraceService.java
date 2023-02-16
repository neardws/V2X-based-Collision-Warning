package com.linmu.collision_warning_system.services;

import android.content.Context;
import android.util.Log;

import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.PushMessage;

public class TraceService {

    // 轨迹服务ID
    private final long serviceId = 235692;
    // 设备标识
    private final String entityName = "myTrace";

    // 轨迹服务
    private Trace mTrace;
    // 轨迹服务客户端
    private LBSTraceClient mTraceClient;
    // 轨迹服务监听器
    private OnTraceListener mTraceListener;

    // 轨迹请求标识
    int tag = 1;
    // 初始化轨迹监听器
    OnTrackListener mTrackListener;

    public TraceService(Context context) {
        LBSTraceClient.setAgreePrivacy(context,true);
        // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
        final boolean isNeedObjectStorage = false;

        mTrace = new Trace(serviceId, entityName, isNeedObjectStorage);
        try {
            mTraceClient = new LBSTraceClient(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 定位周期(单位:秒)
        int gatherInterval = 5;
        // 打包回传周期(单位:秒)
        int packInterval = 10;
        // 设置定位和打包周期
        mTraceClient.setInterval(gatherInterval, packInterval);

        initTraceListener();
        initTraceRequestListener();

        mTraceClient.startTrace(mTrace,mTraceListener);
    }

    /**
     * 析构函数
     */
    protected void finalize() {
        mTraceClient.stopTrace(mTrace,mTraceListener);
    }


    /**
     * 初始化轨迹服务监听器
     */
    private void initTraceListener() {
        mTraceListener = new OnTraceListener() {
            @Override
            public void onBindServiceCallback(int status, String s) {}
            // 开启服务回调
            @Override
            public void onStartTraceCallback(int status, String message) {}
            // 停止服务回调
            @Override
            public void onStopTraceCallback(int status, String message) {}
            // 开启采集回调
            @Override
            public void onStartGatherCallback(int status, String message) {}
            // 停止采集回调
            @Override
            public void onStopGatherCallback(int status, String message) {}
            // 推送回调
            @Override
            public void onPushCallback(byte messageNo, PushMessage message) {}

            @Override
            public void onInitBOSCallback(int status, String message) {}
            @Override
            public void onTraceDataUploadCallBack(int status, String message, int i1, int i2) {}
        };
    }

    /**
     * 初始化轨迹请求监听器
     */
    private void initTraceRequestListener() {
        // 初始化轨迹监听器
        mTrackListener = new OnTrackListener() {
            // 历史轨迹回调
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {

            }
        };
    }

    public void start() {
        mTraceClient.startGather(mTraceListener);
    }

    public void stop() {
        mTraceClient.stopGather(mTraceListener);
    }

    public void requestTrace() {

        // 创建历史轨迹请求实例
        HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(tag, serviceId, entityName);

        // 设置轨迹查询起止时间
        // 开始时间(单位：秒)
        long startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60;
        // 结束时间(单位：秒)
        long endTime = System.currentTimeMillis() / 1000;
        // 设置开始时间
        historyTrackRequest.setStartTime(startTime);
        // 设置结束时间
        historyTrackRequest.setEndTime(endTime);

        // 查询历史轨迹
        mTraceClient.queryHistoryTrack(historyTrackRequest, mTrackListener);
    }

}

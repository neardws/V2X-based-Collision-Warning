package com.linmu.collision_warning_system.utils;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

/**
 * @author linmu
 * @version V1.0
 * @name NcsTag
 * @description NCS 的标签定义类
 * @date 2023-04-05 14:29
 **/
public enum NcsTag {
    Broadcast(1001,"NCS状态请求(广播)"),
    BroadcastReturn(1002,"OBU-NCS响应广播状态查询请求"),
    Login(2001,"注册登录请求"),
    LoginReturn(2002,"OBU-NCS响应注册登录请求"),
    Logout(2003,"注销请求"),
    LogoutReturn(2005,"OBU-NCS响应注销请求"),
    Activate(2005,"激活请求"),
    ActivateReturn(2006,"OBU-NCS响应激活请求"),
    ThisCarInfo(2101,"OBU-NCS推送本车信息"),
    OtherCarInfo(2102,"OBU-NCS推送附件其他车信息"),
    InfrastructureInfo(2103,"OBU-NCS推送附件基础设施信息"),
    EventInfo(2105,"OBU-NCS推送事件信息"),
    State(2112,"详细状态查询请求"),
    StateInfo(2113,"OBU-NCS响应|推送NCS详细状态信息");
    private final int tag;
    private final String description;
    NcsTag(int tag, String description) {
        this.tag = tag;
        this.description = description;
    }
    @Nullable
    @Contract(pure = true)
    public static NcsTag getTag(int tag) {
        for(NcsTag item:NcsTag.values()) {
            if(item.tag == tag) {
                return item;
            }
        }
        return null;
    }
    public int getTag() {
        return tag;
    }
    public String getDescription() {
        return description;
    }
}

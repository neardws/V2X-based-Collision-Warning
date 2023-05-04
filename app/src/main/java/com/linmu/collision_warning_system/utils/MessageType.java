package com.linmu.collision_warning_system.utils;

import androidx.annotation.Nullable;

/**
 * @version V1.0
 * @name MessageType
 * @author linmu
 * @description Android内部消息的枚举类型
 * @date 2023-04-05 15:20
*/
public enum MessageType {
    Once("receive.once.port","单次消息"),
    Push("receive.push.port","推送消息"),
    Log("log.port","测试消息");
    private final int port;
    private final String description;
    MessageType(String type, String description) {
        this.description = description;
        this.port =Integer.parseInt(PropertiesUtil.getValue(type));
    }
    @Nullable
    public static MessageType getMessageType(int port) {
        for(MessageType item:MessageType.values()) {
            if(item.port == port) {
                return item;
            }
        }
        return null;
    }
    public int getPort() {
        return port;
    }
    public String getDescription() {
        return description;
    }
}

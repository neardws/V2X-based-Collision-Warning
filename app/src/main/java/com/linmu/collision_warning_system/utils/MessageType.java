package com.linmu.collision_warning_system.utils;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

/**
 * @version V1.0
 * @name MessageType
 * @author linmu
 * @description Android内部消息的枚举类型
 * @date 2023-04-05 15:20
*/
public enum MessageType {
    Once(1111,"单次消息"),
    Push(2222,"推送消息"),
    Log(9999,"日志消息");
    private final int type;
    private final String description;
    MessageType(int type, String description) {
        this.type = type;
        this.description = description;
    }
    @Nullable
    @Contract(pure = true)
    public static MessageType getMessageType(int type) {
        for(MessageType item:MessageType.values()) {
            if(item.type == type) {
                return item;
            }
        }
        return null;
    }
    public int getType() {
        return type;
    }
    public String getDescription() {
        return description;
    }
}

package com.chatsever.common.enums;

/**
 * Loại tin nhắn truyền qua WebSocket / RabbitMQ.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.1.
 */
public enum MessageType {
    CHAT,
    PRIVATE,
    SYSTEM,
    ERROR,
    LIST,
    JOIN,
    LEAVE,
    PING,
    PONG
}

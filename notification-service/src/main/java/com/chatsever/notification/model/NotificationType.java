package com.chatsever.notification.model;

/** Loại notification */
public enum NotificationType {
    MENTION,        // @user cụ thể
    MENTION_ALL,    // @everyone
    MESSAGE,        // Tin nhắn thường (cho unread)
    DM              // Direct message
}

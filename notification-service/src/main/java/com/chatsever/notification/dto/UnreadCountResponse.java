package com.chatsever.notification.dto;

import java.util.Map;

/**
 * Response cho API unread count — map channelId → số tin chưa đọc.
 */
public class UnreadCountResponse {

    private String userId;
    private Map<Long, Long> unreadCounts;   // channelId → count

    public UnreadCountResponse() {}

    public UnreadCountResponse(String userId, Map<Long, Long> unreadCounts) {
        this.userId = userId;
        this.unreadCounts = unreadCounts;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<Long, Long> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<Long, Long> unreadCounts) { this.unreadCounts = unreadCounts; }
}

package com.chatsever.notification.dto;

/**
 * Request body cho API đánh dấu đã đọc.
 * POST /api/channels/{channelId}/ack
 */
public class AckRequest {

    private String userId;
    private Long lastMessageId;

    public AckRequest() {}

    public AckRequest(String userId, Long lastMessageId) {
        this.userId = userId;
        this.lastMessageId = lastMessageId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(Long lastMessageId) { this.lastMessageId = lastMessageId; }
}

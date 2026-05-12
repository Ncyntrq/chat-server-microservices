package com.chatsever.notification.dto;

import com.chatsever.notification.model.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO trả về cho client — thông tin notification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {

    private Long id;
    private String userId;
    private Long channelId;
    private Long serverId;
    private String sender;
    private NotificationType type;
    private String content;
    private boolean read;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(Long id, String userId, Long channelId, Long serverId,
                           String sender, NotificationType type, String content,
                           boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.channelId = channelId;
        this.serverId = serverId;
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
    }

    // --- Getter & Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

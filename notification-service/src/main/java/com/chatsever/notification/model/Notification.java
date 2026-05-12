package com.chatsever.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity bảng notifications — lưu thông báo cho user.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_unread", columnList = "userId, isRead"),
        @Index(name = "idx_channel", columnList = "channelId")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;          // người nhận notification

    private Long channelId;         // channel nguồn (nullable cho DM)
    private Long serverId;          // server nguồn (nullable cho DM)

    @Column(length = 100)
    private String sender;          // người gửi tin nhắn gốc

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(length = 500)
    private String content;         // preview nội dung tin nhắn

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Constructors ---

    public Notification() {}

    public Notification(String userId, Long channelId, Long serverId, String sender,
                        NotificationType type, String content) {
        this.userId = userId;
        this.channelId = channelId;
        this.serverId = serverId;
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
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

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

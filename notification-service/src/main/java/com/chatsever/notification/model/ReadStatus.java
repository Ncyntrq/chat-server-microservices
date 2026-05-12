package com.chatsever.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity bảng read_status — theo dõi tin nhắn đã đọc cho mỗi user/channel.
 */
@Entity
@Table(name = "read_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_channel", columnNames = {"userId", "channelId"})
})
public class ReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false)
    private Long channelId;

    @Column(nullable = false)
    private Long lastReadMsgId = 0L;    // ID tin nhắn cuối cùng đã đọc

    @Column(nullable = false)
    private LocalDateTime lastReadAt = LocalDateTime.now();

    // --- Constructors ---

    public ReadStatus() {}

    public ReadStatus(String userId, Long channelId, Long lastReadMsgId) {
        this.userId = userId;
        this.channelId = channelId;
        this.lastReadMsgId = lastReadMsgId;
        this.lastReadAt = LocalDateTime.now();
    }

    // --- Getter & Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public Long getLastReadMsgId() { return lastReadMsgId; }
    public void setLastReadMsgId(Long lastReadMsgId) { this.lastReadMsgId = lastReadMsgId; }

    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}

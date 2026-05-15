package com.chatsever.channel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity cho ghim tin nhắn (CH6, CH7).
 * Lưu mapping channel → messageId.
 */
@Entity
@Table(name = "pinned_messages",
       uniqueConstraints = @UniqueConstraint(columnNames = {"channelId", "messageId"}))
public class PinnedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long channelId;

    @Column(nullable = false)
    private Long messageId;

    private String pinnedBy; // username of who pinned it

    private LocalDateTime pinnedAt;

    public PinnedMessage() {}

    public PinnedMessage(Long channelId, Long messageId, String pinnedBy) {
        this.channelId = channelId;
        this.messageId = messageId;
        this.pinnedBy = pinnedBy;
        this.pinnedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getPinnedBy() { return pinnedBy; }
    public void setPinnedBy(String pinnedBy) { this.pinnedBy = pinnedBy; }
    public LocalDateTime getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(LocalDateTime pinnedAt) { this.pinnedAt = pinnedAt; }
}

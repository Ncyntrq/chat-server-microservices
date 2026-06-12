package com.chatsever.messaging.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_message_reactions_message_id", columnList = "message_id"),
    @Index(name = "idx_message_reactions_unique", columnList = "message_id, user_id, emoji", unique = true)
})
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String emoji;

    @Column(name = "reaction_count", nullable = false)
    private int count = 1;

    public MessageReaction() {}

    public MessageReaction(Long messageId, String userId, String emoji) {
        this.messageId = messageId;
        this.userId = userId;
        this.emoji = emoji;
        this.count = 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}

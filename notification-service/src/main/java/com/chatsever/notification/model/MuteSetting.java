package com.chatsever.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mute_settings", indexes = {
        @Index(name = "idx_mute_user", columnList = "userId, targetType, targetId")
})
public class MuteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 20)
    private String targetType; // CHANNEL, SERVER, USER

    @Column(nullable = false, length = 100)
    private String targetId;

    @Column(nullable = false)
    private boolean isMuted = true;

    @Column(nullable = false)
    private LocalDateTime mutedAt = LocalDateTime.now();

    public MuteSetting() {}

    public MuteSetting(String userId, String targetType, String targetId, boolean isMuted) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.isMuted = isMuted;
        this.mutedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }

    public LocalDateTime getMutedAt() { return mutedAt; }
    public void setMutedAt(LocalDateTime mutedAt) { this.mutedAt = mutedAt; }
}

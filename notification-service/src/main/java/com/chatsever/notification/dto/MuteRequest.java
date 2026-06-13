package com.chatsever.notification.dto;

public class MuteRequest {
    private String targetType; // CHANNEL, SERVER, USER
    private String targetId;
    private boolean muted;

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
}

package com.chatsever.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Su kien log gui qua RabbitMQ (Topic Exchange `chat.exchange`).
 * Producer: messaging-service. Consumer: log-service.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.1, doc/04_giao_thuc_truyen_thong.md § 4.6.
 *
 * eventType hop le: BROADCAST, PRIVATE, USER_LOGIN, USER_LOGOUT, USER_REGISTER.
 */
public class LogEntry {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String eventType;
    private String sender;
    private String receiver;
    private String content;

    public LogEntry() {}

    public LogEntry(LocalDateTime timestamp, String eventType, String sender, String receiver, String content) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

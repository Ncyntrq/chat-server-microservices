package com.chatsever.common.dto;

import com.chatsever.common.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Payload tin nhan qua WebSocket. JSON-serialized bang Jackson.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.1.
 *
 * KHONG co field token — JWT chi truyen qua query param khi WS handshake.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDTO {

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public MessageDTO() {}

    public MessageDTO(MessageType type, String sender, String receiver, String content, LocalDateTime timestamp) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

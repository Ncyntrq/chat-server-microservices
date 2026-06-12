package com.chatsever.messaging.controller;

import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class MessageController {
    
    private final MessageRepository messageRepository;
    private final com.chatsever.messaging.service.MessageService messageService;

    public MessageController(MessageRepository messageRepository, com.chatsever.messaging.service.MessageService messageService) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
    }
    
    @GetMapping("/{channelId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit) {
            
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages;
        
        if (before != null) {
            messages = messageRepository.findByChannelIdAndIdLessThanOrderByIdDesc(channelId, before, pageable);
        } else {
            messages = messageRepository.findByChannelIdOrderByIdDesc(channelId, pageable);
        }
        
        return ResponseEntity.ok(messages);
    }

    // Lấy nhiều tin nhắn theo IDs (dùng cho tính năng ghim tin nhắn)
    @GetMapping("/bulk")
    public ResponseEntity<List<ChatMessage>> getMessagesBulk(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(messageRepository.findAllById(ids));
    }

    // Tìm kiếm tin nhắn theo từ khóa
    @GetMapping("/search")
    public ResponseEntity<List<ChatMessage>> searchMessages(
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long serverId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> results;
        if (channelId != null) {
            results = messageRepository.searchByChannel(channelId, keyword, pageable);
        } else if (serverId != null) {
            results = messageRepository.searchByServer(serverId, keyword, pageable);
        } else {
            results = List.of();
        }
        return ResponseEntity.ok(results);
    }

    // Thêm Cảm xúc
    @PostMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        messageService.addReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }

    // Xóa Cảm xúc
    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        messageService.removeReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }
}

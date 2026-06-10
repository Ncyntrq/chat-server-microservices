package com.chatsever.messaging.controller;

import com.chatsever.messaging.entity.ChatMessage;
import com.chatsever.messaging.repository.MessageRepository;
import com.chatsever.messaging.service.MessageService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tìm kiếm tin nhắn theo từ khóa.
 *
 * GET /api/messages/search?q={kw}&scope={channel|private|all}&channelId=&targetUser=&limit=50
 * Header X-User-Id do gateway inject sau JwtAuthFilter.
 *
 * - scope=channel  → cần channelId
 * - scope=private  → cần targetUser (kết hợp X-User-Id)
 * - scope=all      → chỉ dùng X-User-Id
 */
@RestController
@RequestMapping("/api/messages/search")
public class MessageSearchController {

    private static final int MAX_LIMIT = 100;
    private static final int MIN_QUERY_LENGTH = 2;

    private final MessageRepository messageRepository;
    private final MessageService messageService;

    public MessageSearchController(MessageRepository messageRepository, MessageService messageService) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessage>> search(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String targetUser,
            @RequestParam(defaultValue = "50") int limit) {

        String keyword = q == null ? "" : q.trim();
        if (keyword.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_LIMIT));
        List<ChatMessage> results;

        switch (scope) {
            case "channel":
                if (channelId == null) return ResponseEntity.badRequest().build();
                // Chỉ trả kết quả nếu user là member của server sở hữu channel (không lộ dữ liệu)
                if (!messageService.canSearchChannel(channelId, userId)) {
                    return ResponseEntity.ok(List.of());
                }
                results = messageRepository.searchInChannel(channelId, keyword, pageable);
                break;
            case "private":
                if (targetUser == null || targetUser.isBlank()) return ResponseEntity.badRequest().build();
                results = messageRepository.searchInPrivate(userId, targetUser, keyword, pageable);
                break;
            default: // "all"
                results = messageRepository.searchAllForUser(userId, keyword, pageable);
                break;
        }

        return ResponseEntity.ok(results);
    }
}

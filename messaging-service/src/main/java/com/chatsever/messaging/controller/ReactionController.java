package com.chatsever.messaging.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chatsever.messaging.service.MessageService;

@RestController
@RequestMapping("/api/messages")
public class ReactionController {

    private final MessageService messageService;

    public ReactionController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        messageService.addReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        messageService.removeReaction(messageId, userId, emoji);
        return ResponseEntity.ok().build();
    }
}

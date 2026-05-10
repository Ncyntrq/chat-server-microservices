package com.chatsever.presence.controller;

import com.chatsever.presence.model.UserStatus;
import com.chatsever.presence.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/test")
    public String test() {
        return "Presence Service is ONLINE - Chào mừng bạn đến với phòng 8083!";
    }

    // Đã đổi sang @RequestHeader để nhận diện user từ Gateway
    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestHeader("X-User-Id") String userId) {
        presenceService.connect(userId);
        return ResponseEntity.ok(Map.of("message", "User connected", "status", "ONLINE"));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(@RequestHeader("X-User-Id") String userId) {
        presenceService.disconnect(userId);
        return ResponseEntity.ok(Map.of("message", "User disconnected", "status", "OFFLINE"));
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getStatus(@PathVariable String userId) {
        return ResponseEntity.ok(presenceService.getUserStatus(userId));
    }

    // THÊM MỚI (P5): API cho phép user tự đổi trạng thái (VD: Đang bận)
    @PutMapping("/status")
    public ResponseEntity<?> updateCustomStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UserStatus.Status status) {
        presenceService.updateCustomStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }
}
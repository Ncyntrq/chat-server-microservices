package com.chatsever.notification.controller;

import com.chatsever.notification.dto.AckRequest;
import com.chatsever.notification.dto.NotificationDTO;
import com.chatsever.notification.dto.UnreadCountResponse;
import com.chatsever.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API cho notification-service.
 * 4 endpoints chính + actuator health check.
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 1. Lấy danh sách notification.
     * GET /api/notifications?userId=xxx&unreadOnly=true
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestParam String userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        List<NotificationDTO> notifications = notificationService.getNotifications(userId, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 2. Đánh dấu đã đọc cho channel.
     * POST /api/channels/{channelId}/ack
     */
    @PostMapping("/channels/{channelId}/ack")
    public ResponseEntity<Void> acknowledgeChannel(
            @PathVariable Long channelId,
            @RequestBody AckRequest request) {
        notificationService.acknowledgeChannel(channelId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 3. Số tin chưa đọc mỗi channel.
     * GET /api/notifications/unread-count?userId=xxx
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@RequestParam String userId) {
        UnreadCountResponse response = notificationService.getUnreadCounts(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Đánh dấu 1 notification đã đọc.
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}

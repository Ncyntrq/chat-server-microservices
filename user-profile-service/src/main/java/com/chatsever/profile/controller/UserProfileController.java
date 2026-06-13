package com.chatsever.profile.controller;

import com.chatsever.profile.model.UserProfile;
import com.chatsever.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    // UP1 — Xem hồ sơ
    @GetMapping("/{username}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String username, @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        if (requesterId != null && !requesterId.equals(username)) {
            try {
                String friendUrl = System.getenv("FRIEND_URL") != null ? System.getenv("FRIEND_URL") : "http://localhost:8088";
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-User-Id", requesterId);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(friendUrl + "/api/friends/check-block?targetUsername=" + username, org.springframework.http.HttpMethod.GET, entity, Map.class);
                if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("blocked"))) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("message", "Hồ sơ không khả dụng."));
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    // UP2 — Cập nhật hồ sơ (displayName, bio, avatarUrl)
    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(@RequestHeader("X-User-Id") String username,
                                                     @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateProfile(username, payload.get("displayName"), payload.get("bio"), payload.get("avatarUrl")));
    }

    // UP3 — Upload avatar (multipart, max 2MB, JPEG/PNG)
    @PostMapping("/avatar")
    public ResponseEntity<UserProfile> uploadAvatar(@RequestHeader("X-User-Id") String username,
                                                    @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(username, file));
    }

    // UP4 — Đặt trạng thái tùy chỉnh
    @PutMapping("/status")
    public ResponseEntity<UserProfile> updateStatus(@RequestHeader("X-User-Id") String username,
                                                    @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateStatus(username, payload.get("status")));
    }

    // UP5 — Tìm kiếm user (loại trừ chính mình nếu có X-User-Id)
    @GetMapping("/search")
    public ResponseEntity<List<UserProfile>> searchUsers(
            @RequestParam("q") String keyword,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser) {
        return ResponseEntity.ok(profileService.searchUsers(keyword, currentUser));
    }
}
package com.chatsever.profile.controller;

import com.chatsever.profile.model.UserProfile;
import com.chatsever.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(@RequestHeader("username") String username,
                                                     @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateProfile(username, payload.get("displayName"), payload.get("bio")));
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserProfile> updateAvatar(@RequestHeader("username") String username,
                                                    @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateAvatar(username, payload.get("avatarUrl")));
    }

    @PutMapping("/status")
    public ResponseEntity<UserProfile> updateStatus(@RequestHeader("username") String username,
                                                    @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(profileService.updateStatus(username, payload.get("status")));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfile>> searchUsers(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(profileService.searchUsers(keyword));
    }
}
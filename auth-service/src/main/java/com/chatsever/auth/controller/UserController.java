package com.chatsever.auth.controller;

import com.chatsever.auth.dto.ChangePasswordRequest;
import com.chatsever.auth.dto.ChangeRoleRequest;
import com.chatsever.auth.dto.UpdateProfileRequest;
import com.chatsever.auth.dto.UserProfileResponse;
import com.chatsever.auth.model.User;
import com.chatsever.auth.repository.UserRepository;
import com.chatsever.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID: " + userId));

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable String userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<String> changePassword(
            @PathVariable String userId,
            @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(userId, request));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<String> updateRole(
            @PathVariable String userId,
            @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(userId, request));
    }
}
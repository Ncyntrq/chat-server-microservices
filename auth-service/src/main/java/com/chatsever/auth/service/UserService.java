package com.chatsever.auth.service;

import com.chatsever.auth.dto.ChangePasswordRequest;
import com.chatsever.auth.dto.ChangeRoleRequest;
import com.chatsever.auth.dto.UpdateProfileRequest;
import com.chatsever.auth.dto.UserProfileResponse;
import com.chatsever.auth.model.User;
import com.chatsever.auth.repository.UserRepository;
import com.chatsever.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setDisplayName(request.getDisplayName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBio(request.getBio());

        userRepository.save(user);

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();
    }

    public String changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!SecurityUtil.checkPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        user.setPasswordHash(SecurityUtil.hashPassword(request.getNewPassword()));
        userRepository.save(user);

        return "Đổi mật khẩu thành công!";
    }

    public String updateRole(String targetUserId, ChangeRoleRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setRole(request.getRole().toUpperCase());
        userRepository.save(user);

        return "Cập nhật quyền thành công! Người dùng này giờ là: " + user.getRole();
    }
}
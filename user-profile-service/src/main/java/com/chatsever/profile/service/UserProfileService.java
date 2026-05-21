package com.chatsever.profile.service;

import com.chatsever.profile.model.UserProfile;
import com.chatsever.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository repository;

    public UserProfile getProfile(String username) {
        return repository.findByUsername(username).orElseGet(() -> {
            UserProfile newProfile = UserProfile.builder()
                    .username(username)
                    .displayName(username)
                    .avatarUrl("https://default-avatar.com/user.png")
                    .build();
            return repository.save(newProfile);
        });
    }

    public UserProfile updateProfile(String username, String displayName, String bio) {
        UserProfile profile = getProfile(username);
        if (displayName != null) profile.setDisplayName(displayName);
        if (bio != null) profile.setBio(bio);
        return repository.save(profile);
    }

    public UserProfile updateAvatar(String username, String avatarUrl) {
        UserProfile profile = getProfile(username);
        profile.setAvatarUrl(avatarUrl);
        return repository.save(profile);
    }

    public UserProfile updateStatus(String username, String customStatus) {
        UserProfile profile = getProfile(username);
        profile.setCustomStatus(customStatus);
        return repository.save(profile);
    }

    public List<UserProfile> searchUsers(String keyword) {
        return repository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(keyword, keyword);
    }
}
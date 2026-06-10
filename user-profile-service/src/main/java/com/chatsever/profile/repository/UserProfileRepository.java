package com.chatsever.profile.repository;

import com.chatsever.profile.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUsername(String username);
    // Top 21: giới hạn ở DB (lấy dư 1 để bù khi loại trừ chính mình)
    List<UserProfile> findTop21ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(String username, String displayName);
}
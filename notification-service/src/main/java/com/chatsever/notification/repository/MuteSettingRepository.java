package com.chatsever.notification.repository;

import com.chatsever.notification.model.MuteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MuteSettingRepository extends JpaRepository<MuteSetting, Long> {
    Optional<MuteSetting> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    List<MuteSetting> findAllByUserIdAndIsMutedTrue(String userId);
}

package com.chatsever.notification.repository;

import com.chatsever.notification.model.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho bảng read_status.
 */
@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    /** Tìm read_status theo userId + channelId */
    Optional<ReadStatus> findByUserIdAndChannelId(String userId, Long channelId);
}

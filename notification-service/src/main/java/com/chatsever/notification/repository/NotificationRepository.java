package com.chatsever.notification.repository;

import com.chatsever.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho bảng notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Lấy notification theo userId, sắp xếp mới nhất trước */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Lấy notification chưa đọc theo userId */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    /** Đếm notification chưa đọc theo userId và channelId */
    long countByUserIdAndChannelIdAndIsReadFalse(String userId, Long channelId);

    /** Lấy notification chưa đọc theo userId, group theo channel */
    List<Notification> findByUserIdAndIsReadFalse(String userId);

    // NF13 — Pagination support
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);
}

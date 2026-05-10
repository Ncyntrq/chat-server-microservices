package com.chatsever.server.repository;

import com.chatsever.server.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    // Đã tối ưu truy vấn để không load toàn bộ RAM
    Optional<Server> findByInviteCode(String inviteCode);
}
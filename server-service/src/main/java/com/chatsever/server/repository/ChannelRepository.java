package com.chatsever.server.repository;

import com.chatsever.server.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByServerId(Long serverId);
    void deleteByServerId(Long serverId); // Dùng để dọn rác khi xóa server
}
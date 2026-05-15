package com.chatsever.channel.repository;

import com.chatsever.channel.model.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Long> {
    List<PinnedMessage> findByChannelIdOrderByPinnedAtDesc(Long channelId);
    Optional<PinnedMessage> findByChannelIdAndMessageId(Long channelId, Long messageId);
    void deleteByChannelIdAndMessageId(Long channelId, Long messageId);
}

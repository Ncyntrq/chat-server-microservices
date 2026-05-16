package com.chatsever.channel.service;

import com.chatsever.common.dto.ChannelDto;
import com.chatsever.channel.dto.ChannelRequest;
import com.chatsever.channel.model.PinnedMessage;

import java.util.List;

public interface ChannelService {
    ChannelDto createChannel(ChannelRequest request);
    List<ChannelDto> getChannelsByServerId(Long serverId);
    ChannelDto updateChannel(Long id, ChannelRequest request); // CH3
    void deleteChannel(Long id);
    void deleteChannelsByServerId(Long serverId);

    // CH6, CH7 — Pin messages
    PinnedMessage pinMessage(Long channelId, Long messageId, String pinnedBy);
    void unpinMessage(Long channelId, Long messageId);
    List<PinnedMessage> getPinnedMessages(Long channelId);
}

package com.chatsever.channel.dto;

import lombok.Data;

@Data
public class ChannelRequest {
    private String name;
    private Long serverId;
    private String type;
    // CH3 fields
    private String topic;
    private Integer slowmode;
    // CH5 field
    private String category;
}

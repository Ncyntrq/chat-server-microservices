package com.chatsever.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
}
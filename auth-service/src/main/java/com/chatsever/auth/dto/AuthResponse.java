package com.chatsever.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private String username;
}
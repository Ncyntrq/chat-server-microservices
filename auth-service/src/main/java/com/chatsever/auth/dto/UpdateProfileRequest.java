package com.chatsever.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    private String displayName;
    private String avatarUrl;
    private String bio;
}
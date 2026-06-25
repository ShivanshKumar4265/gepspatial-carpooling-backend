package com.carPooling.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private String email;
    private String fullName;
    private String roleType;
    private String message;
    private String accessToken;
    private String refreshToken;
}
package com.carPooling.backend.service;

import com.carPooling.backend.dto.GenricDTO;
import com.carPooling.backend.dto.request.*;
import com.carPooling.backend.dto.response.*;
import com.carPooling.backend.entity.RefreshToken;
import com.carPooling.backend.exception.custom_exception.InvalidTokenException;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface AuthService {
    LogInResponse login(LoginRequest req);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    CreatePasswordResponse createPassword(CreatePasswordRequest request);
}
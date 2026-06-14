package com.carPooling.backend.service;

import com.carPooling.backend.dto.request.LogoutRequest;
import com.carPooling.backend.dto.request.ProfileRequest;
import com.carPooling.backend.dto.request.UpdateProfileRequest;
import com.carPooling.backend.dto.response.UpdateProfileResponse;
import com.carPooling.backend.entity.RefreshToken;
import com.carPooling.backend.exception.custom_exception.InvalidTokenException;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface UserService {
    UpdateProfileResponse updateProfile(ProfileRequest req);
    void logout();
}

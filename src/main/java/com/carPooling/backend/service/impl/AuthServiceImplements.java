package com.carPooling.backend.service.impl;

import com.carPooling.backend.dto.request.*;
import com.carPooling.backend.dto.response.CreatePasswordResponse;
import com.carPooling.backend.dto.response.LogInResponse;
import com.carPooling.backend.dto.response.RefreshTokenResponse;
import com.carPooling.backend.dto.response.VehicleListResponse;
import com.carPooling.backend.entity.RefreshToken;
import com.carPooling.backend.entity.User;
import com.carPooling.backend.exception.custom_exception.InvalidRequestException;
import com.carPooling.backend.exception.custom_exception.InvalidTokenException;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import com.carPooling.backend.repository.RefreshTokenRepository;
import com.carPooling.backend.repository.UserRepository;
import com.carPooling.backend.security.JwtUtil;
import com.carPooling.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j                          // ← Lombok generates: private static final Logger log = ... Simple Logging Facade
@Service
@RequiredArgsConstructor
public class AuthServiceImplements implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public CreatePasswordResponse createPassword(CreatePasswordRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidRequestException("Password and confirm password do not match");
        }

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        User user;

        if (optionalUser.isEmpty()) {
            user = new User();
            user.setEmail(request.getEmail());
        } else {
            user = optionalUser.get();
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());

        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .user(user)
                .build();

        refreshTokenRepository.save(refreshToken);

        CreatePasswordResponse response = new CreatePasswordResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue);

        return response;
    }

    @Override
    @Transactional
    public LogInResponse login(LoginRequest req) {

        Optional<User> optionalUser = userRepository.findByEmail(req.getEmail());
        if (optionalUser.isEmpty()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = optionalUser.get();

        boolean isPasswordCorrect = passwordEncoder.matches(req.getPassword(), user.getPassword());

        if (!isPasswordCorrect) {
            throw new UnauthorizedException("Password is incorrect");
        }

        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());

        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder().token(refreshTokenValue).expiryDate(LocalDateTime.now().plusDays(7)).revoked(false).user(user).build();

        refreshTokenRepository.save(refreshToken);

        LogInResponse response = new LogInResponse();

        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue);

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setProfilePicture(user.getProfilePicture());
        response.setDob(user.getDob());
        response.setGender(user.getGender());
        response.setPhoneVerified(user.isPhoneVerified());
        response.setCollegeCompanyName(user.getCollegeCompanyName());
        response.setEmergencyContactName(user.getEmergencyContactName());
        response.setEmergencyContactNumber(user.getEmergencyContactNumber());
        response.setCity(user.getCity());

        List<VehicleListResponse> vehicles = user.getVehiclesList()
                .stream()
                .map(vehicle -> new VehicleListResponse(
                        vehicle.getId(),
                        vehicle.getVehicleNumber(),
                        vehicle.getVehicleType(),
                        vehicle.getVehicleModel(),
                        vehicle.getColor(),
                        vehicle.getTotalSeats()
                ))
                .toList();

//        response.setVehicles(vehicles);

        return response;
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        String refreshTokenString = request.getRefreshToken();

        // 1. Find in DB
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException(
                        "Unauthorized Access - try to login/register first"));

        // 2. Check revoked
        if (storedToken.isRevoked()) {
            throw new InvalidTokenException(
                    "Unauthorized Access - try to login/register first");
        }

        // 3. Check expiry from DB (not from JWT)
        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken); // cleanup expired token
            throw new InvalidTokenException(
                    "Session expired. Please log in again.");
        }

        // 4. Get user from stored token (no JWT parsing needed)
        User user = storedToken.getUser();

        // 5. Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // 6. Rotate refresh token — delete old, save new
        refreshTokenRepository.delete(storedToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(newRefreshToken)
                .expiryDate(LocalDateTime.now().plusSeconds(120)) // match JwtUtil value
                .revoked(false)
                .user(user)
                .build();

        refreshTokenRepository.save(refreshToken);

        // 7. Return response
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }
}
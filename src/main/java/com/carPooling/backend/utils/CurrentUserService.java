package com.carPooling.backend.utils;

import com.carPooling.backend.entity.User;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import com.carPooling.backend.repository.UserRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Unauthorized: User not found"));
    }
}
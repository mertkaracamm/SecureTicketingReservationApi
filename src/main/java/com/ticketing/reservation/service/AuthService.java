package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.*;
import com.ticketing.reservation.dto.response.Responses.AuthResponse;
import com.ticketing.reservation.dto.response.Responses.UserResponse;
import com.ticketing.reservation.exception.ConflictException;
import com.ticketing.reservation.repository.UserRepository;
import com.ticketing.reservation.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        String role = (request.role() != null && !request.role().isBlank())
                ? request.role().toUpperCase() : "CUSTOMER";

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(role)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        userRepository.updateLastLogin(user.getId(), Instant.now());

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRoles());
        return AuthResponse.of(access, refresh, 900_000L);
    }

    public AuthResponse refresh(RefreshRequest request) {
        Claims claims = jwtService.validateAndExtract(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRoles());
        return AuthResponse.of(access, refresh, 900_000L);
    }
}
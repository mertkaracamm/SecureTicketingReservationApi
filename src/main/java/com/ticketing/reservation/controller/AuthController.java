package com.ticketing.reservation.controller;

import com.ticketing.reservation.dto.request.LoginRequest;
import com.ticketing.reservation.dto.request.RefreshRequest;
import com.ticketing.reservation.dto.request.RegisterRequest;
import com.ticketing.reservation.dto.response.Responses.AuthResponse;
import com.ticketing.reservation.dto.response.Responses.UserResponse;
import com.ticketing.reservation.exception.RateLimitException;
import com.ticketing.reservation.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Register, login and token refresh")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens",
            description = "Rate limited: max 10 requests per minute")
    @RateLimiter(name = "loginRateLimit", fallbackMethod = "loginFallback")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    public AuthResponse loginFallback(LoginRequest request, RequestNotPermitted e) {
        throw new RateLimitException("Too many login attempts, please try again later");
    }
}
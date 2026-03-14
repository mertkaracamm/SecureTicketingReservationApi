package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.*;
import com.ticketing.reservation.exception.ConflictException;
import com.ticketing.reservation.repository.UserRepository;
import com.ticketing.reservation.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    private User existingUser;

    @BeforeEach
    void setup() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("existing@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .roles("CUSTOMER")
                .build();
    }

    @Test
    void register_newUser_shouldSucceed() {
        var request = new RegisterRequest("new@test.com", "Test@12345!", null);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("Test@12345!")).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = authService.register(request);

        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.roles()).isEqualTo("CUSTOMER");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void register_duplicateEmail_shouldThrowConflictException() {
        var request = new RegisterRequest("existing@test.com", "Test@12345!", null);
        given(userRepository.existsByEmail("existing@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_withOrganizerRole_shouldSetOrganizerRole() {
        var request = new RegisterRequest("org@test.com", "Test@12345!", "ORGANIZER");
        given(userRepository.existsByEmail("org@test.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hashed");
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = authService.register(request);

        assertThat(result.roles()).isEqualTo("ORGANIZER");
    }

    @Test
    void login_validCredentials_shouldReturnTokens() {
        var request = new LoginRequest("existing@test.com", "Test@12345!");
        given(authenticationManager.authenticate(any())).willReturn(
                new UsernamePasswordAuthenticationToken("existing@test.com", "Test@12345!"));
        given(userRepository.findByEmail("existing@test.com")).willReturn(Optional.of(existingUser));
        given(jwtService.generateAccessToken(any(), any(), any())).willReturn("access-token");
        given(jwtService.generateRefreshToken(any(), any(), any())).willReturn("refresh-token");

        var result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_badCredentials_shouldThrowException() {
        var request = new LoginRequest("existing@test.com", "WrongPass!");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_validToken_shouldReturnNewTokens() {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(existingUser.getId().toString());
        given(jwtService.validateAndExtract("valid-refresh-token")).willReturn(claims);
        given(jwtService.isRefreshToken(claims)).willReturn(true);
        given(userRepository.findById(existingUser.getId())).willReturn(Optional.of(existingUser));
        given(jwtService.generateAccessToken(any(), any(), any())).willReturn("new-access-token");
        given(jwtService.generateRefreshToken(any(), any(), any())).willReturn("new-refresh-token");

        var result = authService.refresh(new RefreshRequest("valid-refresh-token"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    void refresh_accessTokenInstead_shouldThrowIllegalArgumentException() {
        Claims claims = mock(Claims.class);
        given(jwtService.validateAndExtract("access-token")).willReturn(claims);
        given(jwtService.isRefreshToken(claims)).willReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("access-token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a refresh token");
    }
}
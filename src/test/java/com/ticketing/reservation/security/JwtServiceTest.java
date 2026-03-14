package com.ticketing.reservation.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        JwtProperties props = new JwtProperties();
        props.setSecret("TestSecretKeyForJWTSigningThatIsAtLeast512BitsLongForTestingPurposesOnlyDoNotUseInProd!!");
        props.setAccessTokenExpiryMs(900_000L);
        props.setRefreshTokenExpiryMs(604_800_000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAndValidateAccessToken_shouldSucceed() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "test@test.com", "CUSTOMER");

        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo("test@test.com");
        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
    }

    @Test
    void generateAndValidateRefreshToken_shouldSucceed() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId, "test@test.com", "CUSTOMER");

        Claims claims = jwtService.validateAndExtract(token);
        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isAccessToken(claims)).isFalse();
    }

    @Test
    void validateTamperedToken_shouldThrowException() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "test@test.com", "CUSTOMER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.validateAndExtract(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
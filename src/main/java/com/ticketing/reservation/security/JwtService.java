package com.ticketing.reservation.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

// Handles JWT generation and validation
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public String generateAccessToken(UUID userId, String email, String roles) {
        return buildToken(userId, email, roles, props.getAccessTokenExpiryMs(), "access");
    }

    public String generateRefreshToken(UUID userId, String email, String roles) {
        return buildToken(userId, email, roles, props.getRefreshTokenExpiryMs(), "refresh");
    }

    private String buildToken(UUID userId, String email, String roles, long expiryMs, String tokenType) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("email", email, "roles", roles, "type", tokenType))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
        		java.util.Base64.getEncoder().encodeToString(props.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
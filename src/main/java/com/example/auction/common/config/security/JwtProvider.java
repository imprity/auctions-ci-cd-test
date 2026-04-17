package com.example.auction.common.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.accessExpire}")
    private long accessTokenExpireTime;

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = secretKey.trim().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpireTime))
                .signWith(key)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpireTime))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public long getRemainingTtl(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, "ACCESS");
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, "REFRESH");
    }

    public String resolveToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token, String tokenType) {
        try {
            Claims claims = getClaims(token);
            return tokenType.equals(claims.get("tokenType", String.class));
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

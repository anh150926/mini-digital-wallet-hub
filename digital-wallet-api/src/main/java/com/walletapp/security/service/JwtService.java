package com.walletapp.security.service;

import com.walletapp.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${app.jwt.access-token-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    @Getter
    @Value("${app.jwt.refresh-token-expiration-seconds:604800}")
    private long refreshTokenExpirationSeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        // Check if hex-encoded or plain string
        if (jwtSecret.matches("^[0-9a-fA-F]+$") && jwtSecret.length() >= 64) {
            keyBytes = hexStringToByteArray(jwtSecret);
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone_number", user.getPhoneNumber());
        claims.put("role", user.getRole().name());
        claims.put("type", "ACCESS");
        return buildToken(claims, user.getId().toString(), accessTokenExpirationSeconds);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        return buildToken(claims, user.getId().toString(), refreshTokenExpirationSeconds);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationSeconds) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + expirationSeconds * 1000))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        String sub = extractClaim(token, Claims::getSubject);
        return UUID.fromString(sub);
    }

    public String extractPhoneNumber(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("phone_number", String.class);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("type", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}

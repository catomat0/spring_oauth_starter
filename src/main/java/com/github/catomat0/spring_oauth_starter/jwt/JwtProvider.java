package com.github.catomat0.spring_oauth_starter.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JwtProvider {

    static final String TYPE_ACCESS = "ACCESS";
    static final String TYPE_REFRESH = "REFRESH";

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private static final Set<String> RESERVED_CLAIMS = Set.of(
            CLAIM_TYPE, CLAIM_ROLE,
            "iat", "exp", "nbf", "iss", "sub", "aud", "jti"
    );

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtProvider(JwtProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.signingKey = buildSigningKey(properties.getSecretKey());
    }

    private static SecretKey buildSigningKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new JwtException(JwtErrorCode.SECRET_KEY_MISSING, "jwt.secret-key must be configured");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new JwtException(JwtErrorCode.SECRET_KEY_TOO_SHORT,
                    "jwt.secret-key must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256; got " + keyBytes.length + " bytes. "
                            + "Generate one with: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String userId, String role) {
        return generateAccessToken(userId, role, Collections.emptyMap());
    }

    public String generateAccessToken(String userId, String role, Map<String, String> extras) {
        Objects.requireNonNull(userId, "userId");
        return buildToken(userId, role, TYPE_ACCESS, extras, properties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(String userId) {
        Objects.requireNonNull(userId, "userId");
        return buildToken(userId, null, TYPE_REFRESH, Collections.emptyMap(),
                properties.getRefreshTokenExpiration());
    }

    private String buildToken(String userId, String role, String type,
                              Map<String, String> extras, long ttlMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        var builder = Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey);

        if (role != null && !role.isBlank()) {
            builder.claim(CLAIM_ROLE, role);
        }
        if (extras != null) {
            for (Map.Entry<String, String> e : extras.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                if (key == null || RESERVED_CLAIMS.contains(key)) continue;
                if (value == null || value.isBlank()) continue;
                builder.claim(key, value);
            }
        }
        return builder.compact();
    }

    public boolean validateAccess(String token) {
        return validate(token, TYPE_ACCESS);
    }

    public boolean validateRefresh(String token) {
        return validate(token, TYPE_REFRESH);
    }

    private boolean validate(String token, String expectedType) {
        if (token == null || token.isBlank()) return false;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public JwtPayload parseAccess(String token) {
        return doParse(token, TYPE_ACCESS);
    }

    public JwtPayload parseRefresh(String token) {
        return doParse(token, TYPE_REFRESH);
    }

    private JwtPayload doParse(String token, String expectedType) {
        Objects.requireNonNull(token, "token");
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException(JwtErrorCode.TOKEN_TYPE_MISMATCH,
                    "Token type mismatch (expected=" + expectedType + ", got=" + type + ")");
        }

        String userId = claims.getSubject();
        String role = claims.get(CLAIM_ROLE, String.class);
        Map<String, String> extras = new HashMap<>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (RESERVED_CLAIMS.contains(entry.getKey())) continue;
            if (entry.getValue() instanceof String s) {
                extras.put(entry.getKey(), s);
            }
        }
        return new JwtPayload(userId, role, type, extras);
    }
}

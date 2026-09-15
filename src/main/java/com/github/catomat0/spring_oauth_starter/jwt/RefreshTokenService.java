package com.github.catomat0.spring_oauth_starter.jwt;

import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties properties;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate, JwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void save(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(userId),
                refreshToken,
                properties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    /**
     * Redis에 저장된 refresh token과 일치하는지만 확인 (삭제하지 않음).
     */
    public boolean validate(String userId, String refreshToken) {
        String saved = get(userId);
        return timingSafeEquals(saved, refreshToken);
    }

    /**
     * Redis에서 refresh token을 원자적으로 조회+삭제(GETDEL) 후 일치 여부 반환.
     * refresh rotation 시 사용하면 이전 토큰 재사용 방지.
     * <p><b>요구사항:</b> Redis 6.2 이상 (GETDEL 지원).
     */
    public boolean validateAndConsume(String userId, String refreshToken) {
        String saved = redisTemplate.opsForValue().getAndDelete(key(userId));
        return timingSafeEquals(saved, refreshToken);
    }

    public void delete(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(String userId) {
        return properties.getRedisKeyPrefix() + userId;
    }

    private static boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}

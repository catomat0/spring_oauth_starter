package com.github.catomat0.spring_oauth_starter.oauth;

import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * OAuth state + PKCE code_verifier 발급/검증 (CSRF + code injection 방어).
 * <p>authorize 리다이렉트 전 {@link #issue(String)} → state 와 code_challenge 반환.
 * <p>콜백에서 {@link #validateAndConsume(String, String)} → code_verifier 반환 (token exchange 에 첨부).
 * <p>같은 state 는 두 번 쓸 수 없다 (원자적 소비).
 * <p>Redis 저장 값: {@code "{provider}|{codeVerifier}"} 형식.
 */
public class OAuthStateService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final String DELIMITER = "|";

    private final RedisTemplate<String, String> redisTemplate;
    private final OAuthProperties properties;

    public OAuthStateService(RedisTemplate<String, String> redisTemplate, OAuthProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * state 와 PKCE code_challenge 를 생성하고 Redis 에 저장 후 반환.
     * <p>반환된 값을 authorize URL 쿼리에 붙여야 한다:
     * {@code &state=... &code_challenge=... &code_challenge_method=S256}
     */
    public OAuthAuthorizeParams issue(String provider) {
        String state = randomUrlSafe(24);
        String codeVerifier = randomUrlSafe(32);
        String codeChallenge = sha256Base64Url(codeVerifier);

        redisTemplate.opsForValue().set(
                key(state),
                provider + DELIMITER + codeVerifier,
                properties.getStateTtlSeconds(),
                TimeUnit.SECONDS
        );
        return OAuthAuthorizeParams.of(state, codeChallenge);
    }

    /**
     * state 를 Redis 에서 원자적으로 조회+삭제(GETDEL) 후 provider 일치 여부 확인 → code_verifier 반환.
     * <p>false 대신 null 반환 시 CSRF 공격 or 만료 or 재사용 시도로 간주하고 요청 거부.
     * <p><b>요구사항:</b> Redis 6.2 이상.
     *
     * @return code_verifier (PKCE token exchange 에 사용) — 검증 실패 시 null
     */
    public String validateAndConsume(String state, String expectedProvider) {
        if (state == null || state.isBlank() || expectedProvider == null) return null;
        String saved = redisTemplate.opsForValue().getAndDelete(key(state));
        if (saved == null) return null;
        int sep = saved.indexOf(DELIMITER);
        if (sep < 0) return null;
        String storedProvider = saved.substring(0, sep);
        String codeVerifier = saved.substring(sep + 1);
        if (!timingSafeEquals(storedProvider, expectedProvider)) return null;
        return codeVerifier;
    }

    private String key(String state) {
        return properties.getStateRedisKeyPrefix() + state;
    }

    private static String randomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        RNG.nextBytes(buf);
        return BASE64_URL.encodeToString(buf);
    }

    private static String sha256Base64Url(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.US_ASCII));
            return BASE64_URL.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available (JDK is broken)", e);
        }
    }

    private static boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}

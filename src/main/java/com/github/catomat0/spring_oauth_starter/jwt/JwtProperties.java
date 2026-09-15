package com.github.catomat0.spring_oauth_starter.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpiration = 1_800_000L;
    private long refreshTokenExpiration = 1_209_600_000L;
    private String redisKeyPrefix = "RT:";

    @PostConstruct
    void validate() {
        if (secretKey == null || secretKey.isBlank()) return;
        if (accessTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.access-token-expiration must be positive (millis); got " + accessTokenExpiration);
        }
        if (refreshTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.refresh-token-expiration must be positive (millis); got " + refreshTokenExpiration);
        }
        if (refreshTokenExpiration < accessTokenExpiration) {
            throw new IllegalStateException(
                    "jwt.refresh-token-expiration (" + refreshTokenExpiration + ") must be >= "
                            + "jwt.access-token-expiration (" + accessTokenExpiration + ")");
        }
    }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getRedisKeyPrefix() { return redisKeyPrefix; }
    public void setRedisKeyPrefix(String redisKeyPrefix) { this.redisKeyPrefix = redisKeyPrefix; }

    @Override
    public String toString() {
        return "JwtProperties{"
                + "secretKey=" + (secretKey == null || secretKey.isEmpty() ? "[UNSET]" : "[MASKED]")
                + ", accessTokenExpiration=" + accessTokenExpiration
                + ", refreshTokenExpiration=" + refreshTokenExpiration
                + ", redisKeyPrefix='" + redisKeyPrefix + "'}";
    }
}

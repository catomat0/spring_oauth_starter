package com.github.catomat0.spring_oauth_starter.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpiration = 1_800_000L;
    private long refreshTokenExpiration = 1_209_600_000L;
    private String redisKeyPrefix = "RT:";
    private RefreshCookie refreshCookie = new RefreshCookie();

    @PostConstruct
    void validate() {
        if (secretKey == null || secretKey.isBlank()) return;
        if (accessTokenExpiration <= 0) {
            throw new JwtException(JwtErrorCode.EXPIRATION_INVALID,
                    "jwt.access-token-expiration must be positive (millis); got " + accessTokenExpiration);
        }
        if (refreshTokenExpiration <= 0) {
            throw new JwtException(JwtErrorCode.EXPIRATION_INVALID,
                    "jwt.refresh-token-expiration must be positive (millis); got " + refreshTokenExpiration);
        }
        if (refreshTokenExpiration < accessTokenExpiration) {
            throw new JwtException(JwtErrorCode.EXPIRATION_INVALID,
                    "jwt.refresh-token-expiration (" + refreshTokenExpiration + ") must be >= "
                            + "jwt.access-token-expiration (" + accessTokenExpiration + ")");
        }
        if ("None".equalsIgnoreCase(refreshCookie.getSameSite()) && !refreshCookie.isSecure()) {
            throw new JwtException(JwtErrorCode.REFRESH_COOKIE_INSECURE_SAMESITE,
                    "jwt.refresh-cookie.same-site=None requires secure=true "
                            + "(browsers drop the cookie otherwise). "
                            + "For HTTP dev environments, use same-site=Lax and secure=false.");
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

    public RefreshCookie getRefreshCookie() { return refreshCookie; }
    public void setRefreshCookie(RefreshCookie refreshCookie) { this.refreshCookie = refreshCookie; }

    @Override
    public String toString() {
        return "JwtProperties{"
                + "secretKey=" + (secretKey == null || secretKey.isEmpty() ? "[UNSET]" : "[MASKED]")
                + ", accessTokenExpiration=" + accessTokenExpiration
                + ", refreshTokenExpiration=" + refreshTokenExpiration
                + ", redisKeyPrefix='" + redisKeyPrefix + '\''
                + ", refreshCookie=" + refreshCookie + '}';
    }

    public static class RefreshCookie {
        private String name = "refresh_token";
        private String path = "/";
        private String domain;
        private boolean httpOnly = true;
        private boolean secure = true;
        private String sameSite = "None";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }

        public boolean isHttpOnly() { return httpOnly; }
        public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }

        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }

        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }

        @Override
        public String toString() {
            return "RefreshCookie{name='" + name + "', path='" + path + "', domain='" + domain
                    + "', httpOnly=" + httpOnly + ", secure=" + secure
                    + ", sameSite='" + sameSite + "'}";
        }
    }
}

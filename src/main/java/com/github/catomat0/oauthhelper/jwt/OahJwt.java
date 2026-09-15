package com.github.catomat0.oauthhelper.jwt;

/**
 * JWT 관련 서비스를 묶은 파사드.
 * <pre>{@code
 * public AuthController(OahJwt jwt) { this.jwt = jwt; }
 *
 * String access = jwt.provider().generateAccessToken(userId, role);
 * String refresh = jwt.provider().generateRefreshToken(userId);
 * jwt.refresh().save(userId, refresh);
 * jwt.cookie().write(response, refresh);
 * }</pre>
 * <p>Redis / servlet 종속이 없으면 이 파사드는 등록 안 됨 —
 * 그 경우 {@link OahJwtProvider} 만 직접 주입.
 */
public record OahJwt(
        OahJwtProvider provider,
        OahRefreshTokenService refresh,
        OahRefreshTokenCookieWriter cookie
) {}

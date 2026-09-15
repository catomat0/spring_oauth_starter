package com.github.catomat0.spring_oauth_starter.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Refresh token 을 HttpOnly 쿠키로 관리하는 헬퍼.
 * <p>{@link com.github.catomat0.spring_oauth_starter.signuptoken.SignupTokenCookieWriter} 와 대칭 구조.
 * <p>XSS 로 refresh 토큰 탈취 방어 — body 로 전달하지 말고 이 헬퍼로 쿠키 세팅 권장.
 */
public class RefreshTokenCookieWriter {

    private final JwtProperties properties;

    public RefreshTokenCookieWriter(JwtProperties properties) {
        this.properties = properties;
    }

    public void write(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, false).toString());
    }

    public String read(HttpServletRequest request) {
        return read(request, properties.getRefreshCookie().getName());
    }

    public String read(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", true).toString());
    }

    private ResponseCookie buildCookie(String value, boolean expire) {
        JwtProperties.RefreshCookie c = properties.getRefreshCookie();
        Duration maxAge = expire ? Duration.ZERO : Duration.ofMillis(properties.getRefreshTokenExpiration());

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(c.getName(), value)
                .httpOnly(c.isHttpOnly())
                .secure(c.isSecure())
                .sameSite(c.getSameSite())
                .path(c.getPath())
                .maxAge(maxAge);
        if (c.getDomain() != null && !c.getDomain().isBlank()) {
            builder.domain(c.getDomain());
        }
        return builder.build();
    }
}

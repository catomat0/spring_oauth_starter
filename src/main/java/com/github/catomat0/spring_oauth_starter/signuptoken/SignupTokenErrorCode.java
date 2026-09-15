package com.github.catomat0.spring_oauth_starter.signuptoken;

public enum SignupTokenErrorCode {
    /** signup-token.secret-key 가 설정되지 않음 (startup). */
    SECRET_KEY_MISSING,
    /** signup-token.secret-key 가 32byte 미만 (HS256 최소 요건 미달, startup). */
    SECRET_KEY_TOO_SHORT,
    /** 쿠키 설정 불일치 — same-site=None 인데 secure=false (브라우저가 쿠키 drop). */
    COOKIE_INSECURE_SAMESITE,
    /** expiration 값이 0 이하. */
    EXPIRATION_INVALID,
    /** 토큰 타입이 SIGNUP 이 아님 (다른 종류 JWT 를 signup 자리에 넣음). */
    TOKEN_TYPE_MISMATCH,
    /** signup-token.redis-key-prefix 가 jwt.redis-key-prefix 와 동일 (Redis 키 충돌 위험). */
    REDIS_PREFIX_COLLISION
}

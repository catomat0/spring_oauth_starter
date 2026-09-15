package com.github.catomat0.oauthhelper.jwt;

public enum OahJwtErrorCode {
    /** jwt.secret-key 가 설정되지 않음 (startup). */
    SECRET_KEY_MISSING,
    /** jwt.secret-key 가 32byte 미만 (HS256 최소 요건 미달, startup). */
    SECRET_KEY_TOO_SHORT,
    /** 토큰 파싱 시 type 클레임이 기대값과 다름 (예: access 자리에 refresh 넣음). */
    TOKEN_TYPE_MISMATCH,
    /** access/refresh expiration 값이 0 이하이거나 refresh < access (startup). */
    EXPIRATION_INVALID,
    /** jwt.refresh-cookie.same-site=None 인데 secure=false (브라우저가 쿠키 drop, startup). */
    REFRESH_COOKIE_INSECURE_SAMESITE
}

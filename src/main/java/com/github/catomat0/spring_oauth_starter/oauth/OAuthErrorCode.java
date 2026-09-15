package com.github.catomat0.spring_oauth_starter.oauth;

public enum OAuthErrorCode {
    /** 알 수 없는 provider 이름 (kakao/google 이외). */
    PROVIDER_UNKNOWN,
    /** provider config 미설정 (client-id 없음). */
    PROVIDER_NOT_CONFIGURED,
    /** provider config 의 token-uri / user-info-uri 가 http:// 로 시작. */
    INSECURE_URI,
    /** code -> access_token 교환 HTTP 호출 실패 (네트워크/4xx/5xx). */
    TOKEN_EXCHANGE_FAILED,
    /** code -> access_token 응답이 비어있음 (200 이지만 access_token null). */
    TOKEN_EXCHANGE_EMPTY,
    /** userinfo 호출 HTTP 실패. */
    USERINFO_FETCH_FAILED,
    /** userinfo 응답이 비어있음. */
    USERINFO_EMPTY,
    /** userinfo 응답에 email 이 없음. provider 콘솔에서 email scope/동의항목 필수 설정 필요. */
    EMAIL_MISSING,
    /** OAuth state 파라미터 검증 실패 (CSRF 공격 or 만료된 state). */
    STATE_INVALID,
    /** oauth.state-redis-key-prefix 가 다른 서비스의 Redis prefix 와 동일 (충돌 위험). */
    REDIS_PREFIX_COLLISION
}

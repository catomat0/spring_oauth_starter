# Changelog

이 프로젝트의 모든 주요 변경사항은 이 파일에 기록됩니다.
버전 형식은 [Semantic Versioning](https://semver.org/) 기준.

## [1.1.1] - Unreleased

### Changed
- **예외 타입 일관성 정리** — 남아있던 `IllegalStateException` 5곳을 라이브러리 전용 예외로 통일.
  - `JwtProperties.validate()` access/refresh expiration 검증 → `JwtException(EXPIRATION_INVALID)`
  - `JwtProperties.validate()` refresh-cookie same-site 검증 → `JwtException(REFRESH_COOKIE_INSECURE_SAMESITE)`
  - `OAuthProperties.validate()` state-ttl-seconds 검증 → `OAuthException(STATE_TTL_INVALID)`
  - `OAuthProperties.Provider.requireField()` → `OAuthException(PROVIDER_INCOMPLETE)`
  - `OAuthProperties.Provider.requireHttps()` → `OAuthException(INSECURE_URI)`

### Added
- **신규 에러 코드**
  - `JwtErrorCode`: `EXPIRATION_INVALID`, `REFRESH_COOKIE_INSECURE_SAMESITE`
  - `OAuthErrorCode`: `PROVIDER_INCOMPLETE`, `STATE_TTL_INVALID`
- README 배지 (Release / JavaDoc / License) + 상단 링크 라인.

## [1.1.0] - 2026-09-15

### Added
- **OAuth2 (Kakao/Google) 로그인** — code → access_token → userinfo 를 `OAuthLoginService.fetchUserInfo()` 한 번에.
- **JWT access/refresh 발급/검증** — `type=ACCESS/REFRESH` 클레임으로 강제 구분.
- **`RefreshTokenService`** — Redis `RT:{userId}`, `validateAndConsume` (GETDEL) 로 원자적 rotation.
- **CSRF `state` 파라미터 + PKCE** — `OAuthStateService.issue(provider)` 가 state + code_challenge 자동 생성/저장. `validateAndConsume` 이 code_verifier 반환.
- **`OAuthAuthorizeUrlBuilder`** — authorize URL 자동 조립 (state, code_challenge, scope 포함).
- **`JwtAuthenticationFilter`** — `Bearer` 헤더에서 access token 추출 → SecurityContext 세팅 (Spring Security 있을 때만 자동 등록).
- **`RefreshTokenCookieWriter`** — refresh token HttpOnly 쿠키 헬퍼 (SignupTokenCookieWriter 대칭).
- **Kakao/Google 엔드포인트 URI 디폴트** — `oauth.<provider>.token-uri` 등을 미기재해도 표준 URI 자동 적용.
- **RestClient 타임아웃 프로퍼티** — `oauth.rest-client.connect-timeout-ms` (기본 3000), `read-timeout-ms` (기본 5000).
- **에러 코드 도입** — `OAuthErrorCode` / `JwtErrorCode` / `SignupTokenErrorCode` enum. 예외의 `code()` 로 케이스별 분기.
- **Prefix 충돌 검증** — `ST:` / `RT:` / `OS:` 동일 시 startup fail-fast.
- **HTTPS 강제** — `oauth.<provider>.token-uri` / `user-info-uri` `http://` 시작 시 startup fail-fast.
- **Kakao email 필수 강제** — email 없으면 `OAuthException(EMAIL_MISSING)`. (기존 합성 email fallback 제거)
- **Refresh cookie same-site+secure 조합 검증** — startup fail-fast.

### Changed
- **라이센스 MIT → Apache 2.0**.
- 예외 타입 변경: `IllegalStateException` → `OAuthException` / `JwtException` / `SignupTokenException` (모두 `RuntimeException` 서브클래스라 대부분 사용자 영향 없음).
- 패키지 재구성: `com.github.catomat0.oauth_signup_token.*` → `com.github.catomat0.spring_oauth_starter.signuptoken.*` (별도 라이브러리 `oauth_signup_token` 은 그대로 유지).

### Security
- **PKCE (S256) 지원** — code injection 공격 방어.
- **state 파라미터 원자적 소비** — CSRF 재사용 방어.
- **Timing-safe compare** — state/token 비교 사이드채널 방어.
- **`.gitignore` 강화** — `gradle.properties` 실수 커밋 방지.

## [1.0.0] - 2026-09-15

### Added
- Signup token JWT + Redis + HttpOnly Cookie 초기 구현.
- Spring Boot AutoConfiguration.

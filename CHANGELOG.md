# Changelog

이 프로젝트의 모든 주요 변경사항은 이 파일에 기록됩니다.
버전 형식은 [Semantic Versioning](https://semver.org/) 기준.

## [2.1.2] - 2026-09-15

### Fixed
- **파사드 빈 (`OahOAuth` / `OahJwt` / `OahSignup`) 이 등록되지 않던 버그 수정**.
  Nested `@Configuration` 내부의 `@ConditionalOnBean` 이 outer `@AutoConfiguration` 클래스의 빈을 조건 평가 시점에 발견하지 못해 (Spring Boot 컨피그 파싱 타이밍 이슈) 세 파사드 빈이 모두 등록 실패했음. 3개 파일에서 방어 조건 제거 (`OAuthAutoConfiguration`, `JwtAutoConfiguration`, `SignupTokenAutoConfiguration`).
- 사용 방법 변경 없음. 이전에 파사드 주입에 의존한 사용자는 이제 정상 동작.

## [2.1.1] - 2026-09-15

### Changed
- **README 재구성**: JitPack 을 primary 설치 경로로 승격. 소비자 프로젝트가 개인/조직/외부 레포 어디서든 credential 없이 사용 가능.
  - JitPack 사용 예시 & CI/CD 스니펫 (GitHub Actions, Jenkins, Docker) 상단으로 이동
  - GitHub Packages 방식은 접힘 섹션으로 이동 (private 배포/캐시 최적화 필요 시)
- 시나리오별 비교표 추가 (JitPack vs GitHub Packages vs Maven Central)

## [2.1.0] - 2026-09-15

### Added
- **3개 도메인 파사드 record 추가** — 컨트롤러에서 개별 서비스 9개 주입 대신 파사드 3개 주입 가능. 기존 개별 빈도 그대로 등록됨 (non-breaking additive).
  - `OahOAuth(state, authorize, login)` — OAuth 3개
  - `OahJwt(provider, refresh, cookie)` — JWT 3개
  - `OahSignup(provider, service, cookie)` — Signup 3개
- 파사드는 내부 3개 빈이 모두 존재할 때만 자동 등록 (`@ConditionalOnBean`). 예: Redis 미설정 → `OahJwt` 등록 안 됨 → `OahJwtProvider` 개별 주입.

> ⚠️ **알려진 결함**: v2.1.0 / v2.1.1 에서는 위 파사드 3개가 실제로는 등록되지 않는 버그가 있습니다. **v2.1.2 이상 사용 권장.**

## [2.0.0] - 2026-09-15

### Changed (BREAKING)
- **프로젝트 리브랜딩**: `spring_oauth_starter` → **`oauth-helper`** (약칭 OAH)
  - GitHub repo: `catomat0/spring_oauth_starter` → `cattomato-libs/Oah`
  - Maven artifactId: `spring_oauth_starter` → `oauth-helper`
  - Java 패키지: `com.github.catomat0.spring_oauth_starter.*` → `com.github.catomat0.oauthhelper.*`
- **모든 public 클래스에 `Oah` prefix 추가** (외부 사용자가 임포트하는 리소스). 총 31개 클래스 rename.
  - OAuth 관련 (`OAuth*` → `Oah*`): `OAuthProvider`→`OahProvider`, `OAuthLoginService`→`OahLoginService`, `OAuthStateService`→`OahStateService`, `OAuthAuthorizeUrlBuilder`→`OahAuthorizeUrlBuilder`, `OAuthProperties`→`OahProperties`, `OAuthException`→`OahException`, `OAuthErrorCode`→`OahErrorCode`, `OAuthUserInfo`→`OahUserInfo`, `OAuthTokenClient`→`OahTokenClient`, `OAuthUserInfoClient`→`OahUserInfoClient`, `OAuthAuthorizeParams`→`OahAuthorizeParams`
  - DTO: `OAuthTokenResponse`→`OahTokenResponse`, `KakaoUserInfoResponse`→`OahKakaoUserInfoResponse`, `GoogleUserInfoResponse`→`OahGoogleUserInfoResponse`
  - JWT (`Jwt*`→`OahJwt*`, `RefreshToken*`→`OahRefreshToken*`): `JwtProvider`, `JwtProperties`, `JwtPayload`, `JwtException`, `JwtErrorCode`, `JwtAuthenticationFilter`, `RefreshTokenService`, `RefreshTokenCookieWriter`
  - Signup Token (`SignupToken*`→`OahSignupToken*`): 9개 클래스
- AutoConfiguration 내부 클래스는 rename 하지 않음 (사용자 직접 임포트 안 함) — 단 `SpringOAuthStarterAutoConfiguration` → `OauthHelperAutoConfiguration` 은 프로젝트명 변경 반영.

### Migration guide (v1.1.x → v2.0.0)
1. `build.gradle` 의존성 좌표 변경:
   ```gradle
   // before
   implementation 'com.github.cattomato-libs:spring_oauth_starter:1.1.1'
   // after
   implementation 'com.github.cattomato-libs:oauth-helper:2.0.0'
   ```
   Maven repo URL 도 갱신:
   ```gradle
   url = uri('https://maven.pkg.github.com/cattomato-libs/Oah')
   ```
2. 모든 import 문 갱신:
   ```java
   // before
   import com.github.catomat0.spring_oauth_starter.jwt.JwtProvider;
   // after
   import com.github.catomat0.oauthhelper.jwt.OahJwtProvider;
   ```
3. 클래스 타입 참조 갱신 (IDE 의 rename refactor 기능 활용). 위 rename 표 참고.

## [1.1.1] - 2026-09-15

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

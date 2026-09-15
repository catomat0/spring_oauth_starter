# spring_oauth_starter

Spring Boot용 소셜로그인 스타터. Kakao/Google OAuth2 로그인 + JWT (access/refresh) + 온보딩 signup token 을 자동 설정으로 제공합니다.

- **OAuth2 로그인** — Kakao/Google 콜백에서 code → access_token → userinfo 를 한 번에 처리
- **JWT** access/refresh 발급/검증 (JJWT 0.12.6, `type=ACCESS/REFRESH` 강제 구분)
- **Refresh token Redis 저장** — `RT:{userId}` 키, GETDEL 기반 원자적 rotation
- **Signup token** — 온보딩 이탈 시 재개 (JWT + Redis + HttpOnly 쿠키)
- **Spring Boot AutoConfiguration** — 빈 자동 등록, provider별 opt-in
- **필수 종속성 부재 시 명시적 WARN 로그** — 조용한 실패 방지

---

## 1. 설치 (GitHub Packages)

### 1-1. Consumer 세팅

**Step 1. GitHub PAT 발급**
[Settings → Developer settings → Personal access tokens (classic)](https://github.com/settings/tokens/new)
- Scope: `read:packages` (필수), `repo` (private repo인 경우)

**Step 2. `~/.gradle/gradle.properties`**
```properties
gpr.user=catomat0
gpr.token=ghp_xxxxxxxxxxxxxxxxxxxxx
```

**Step 3. 프로젝트 `build.gradle`**
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri('https://maven.pkg.github.com/catomat0/spring_oauth_starter')
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = project.findProperty('gpr.token') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    implementation 'com.github.catomat0:spring_oauth_starter:1.0.0'
}
```

**필수 런타임 의존성** (consumer 프로젝트에 이미 있어야 함):
- `spring-boot-starter-web`
- `spring-boot-starter-data-redis` (refresh token / signup token 저장용)
- Redis 서버 **6.2 이상** (`GETDEL` 사용)

---

## 2. Developer Console 세팅

### Kakao Developers
1. https://developers.kakao.com/ → 로그인 → **내 애플리케이션 → 애플리케이션 추가하기**
2. **앱 키** 탭에서 `REST API 키` 복사 → `KAKAO_CLIENT_ID`
3. **카카오 로그인 → 활성화** ON
4. **Redirect URI 등록**: `https://your-domain.com/api/auth/oauth2/kakao/callback` (개발용은 `http://localhost:8080/...`)
5. **동의항목** — 카카오계정(이메일) 필수 동의로 설정 (선택 시 email null 옴 → 라이브러리가 `kakao_{id}@kakao.user` 로 fallback)
6. **보안 → Client Secret**: 사용 상태 ON → 코드 발급 → `KAKAO_CLIENT_SECRET`

### Google Cloud Console
1. https://console.cloud.google.com/ → 프로젝트 생성/선택
2. **APIs & Services → OAuth consent screen** 생성 (External, scope: `email`, `profile`, `openid`)
3. **Credentials → CREATE CREDENTIALS → OAuth client ID**
4. Application type: **Web application**
5. **Authorized redirect URIs**: `https://your-domain.com/api/auth/oauth2/google/callback` (dev: `http://localhost:8080/...`)
6. 생성 후 `Client ID` → `GOOGLE_CLIENT_ID`, `Client secret` → `GOOGLE_CLIENT_SECRET`

> 등록한 **redirect URI 는 프론트에서 브라우저로 리다이렉트되는 URL 이 아니라, 우리 백엔드 콜백 URL**. 프론트는 이 URL로 인가 코드를 받아 백엔드에 넘김.

---

## 3. 설정 (`application.yml`)

```yaml
oauth:
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
    redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:8080/api/auth/oauth2/kakao/callback}
    authorize-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me

  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8080/api/auth/oauth2/google/callback}
    authorize-uri: https://accounts.google.com/o/oauth2/v2/auth
    token-uri: https://oauth2.googleapis.com/token
    user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
    scope: openid email profile

jwt:
  secret-key: ${JWT_SECRET_KEY}                       # 필수 (32byte 이상)
  access-token-expiration: 1800000                    # ms, 기본 30분
  refresh-token-expiration: 1209600000                # ms, 기본 14일
  redis-key-prefix: "RT:"                             # 선택, 기본 RT:

signup-token:
  secret-key: ${SIGNUP_TOKEN_SECRET}                  # 필수 (JWT와 별도)
  expiration: 1800000                                 # ms, 기본 30분
  redis-key-prefix: "ST:"
  cookie:
    name: signup_token
    same-site: Lax                                    # 로컬 HTTP 개발환경
    secure: false                                     # 프로덕션에선 true + None
```

**Provider 부분 opt-in** — kakao 만 쓰고 싶으면 `oauth.google.*` 통째로 생략 가능. `oauth.google.client-id` 없으면 관련 빈은 살아있지만 호출 시 명확한 예외 발생.

**Secret 생성**
```bash
openssl rand -base64 48    # jwt.secret-key, signup-token.secret-key 각각 다른 값 권장
```

---

## 4. 콜백 컨트롤러 예시

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuthLoginService oauthLoginService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final SignupTokenProvider signupTokenProvider;
    private final SignupTokenService signupTokenService;
    private final SignupTokenCookieWriter signupCookieWriter;
    private final UserRepository userRepository;

    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<?> callback(@PathVariable String provider,
                                     @RequestParam String code,
                                     HttpServletResponse response) {
        OAuthUserInfo info = oauthLoginService.fetchUserInfo(provider, code);

        return userRepository
                .findByProviderAndProviderId(info.provider(), info.providerId())
                .map(user -> issueJwt(user, response))
                .orElseGet(() -> issueSignupToken(info, response));
    }

    private ResponseEntity<?> issueJwt(User user, HttpServletResponse response) {
        String access = jwtProvider.generateAccessToken(user.getId().toString(), user.getRole().name());
        String refresh = jwtProvider.generateRefreshToken(user.getId().toString());
        refreshTokenService.save(user.getId().toString(), refresh);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        // refresh 는 HttpOnly 쿠키 권장
        return ResponseEntity.ok(Map.of("registered", true, "accessToken", access));
    }

    private ResponseEntity<?> issueSignupToken(OAuthUserInfo info, HttpServletResponse response) {
        String token = signupTokenProvider.builder(info.provider(), info.providerId(), info.email())
                .claim("nickname", info.nickname())
                .claim("profileImage", info.profileImage())
                .build();
        signupTokenService.save(info.provider(), info.providerId(), token);
        signupCookieWriter.write(response, token);
        return ResponseEntity.ok(Map.of("registered", false));
    }
}
```

**Refresh token rotation**
```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
    String oldRefresh = body.get("refreshToken");
    JwtPayload p = jwtProvider.parseRefresh(oldRefresh);

    // 원자적 소비: 이전 refresh 는 이 시점 이후 재사용 불가
    if (!refreshTokenService.validateAndConsume(p.userId(), oldRefresh)) {
        throw new IllegalStateException("Refresh token expired or reused");
    }
    User user = userRepository.findById(Long.valueOf(p.userId())).orElseThrow();

    String newAccess = jwtProvider.generateAccessToken(user.getId().toString(), user.getRole().name());
    String newRefresh = jwtProvider.generateRefreshToken(user.getId().toString());
    refreshTokenService.save(user.getId().toString(), newRefresh);
    return ResponseEntity.ok(Map.of("accessToken", newAccess, "refreshToken", newRefresh));
}
```

**회원가입 완료 (signup token → JWT 전환)**
```java
@PostMapping("/signup")
public ResponseEntity<?> signup(@RequestBody SignupRequest req,
                               HttpServletRequest request,
                               HttpServletResponse response) {
    String token = signupCookieWriter.read(request);
    if (token == null || !signupTokenProvider.validate(token)) {
        throw new IllegalStateException("Invalid signup token");
    }
    SignupTokenPayload payload = signupTokenProvider.parse(token);

    if (!signupTokenService.validateAndConsume(payload.provider(), payload.providerId(), token)) {
        throw new IllegalStateException("Signup token expired or already used");
    }

    User user = userRepository.save(User.of(payload.provider(), payload.providerId(), payload.email(),
            payload.extra("nickname"), req.termAgreementIds()));

    String access = jwtProvider.generateAccessToken(user.getId().toString(), user.getRole().name());
    String refresh = jwtProvider.generateRefreshToken(user.getId().toString());
    refreshTokenService.save(user.getId().toString(), refresh);

    signupCookieWriter.clear(response);
    response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
    return ResponseEntity.ok(Map.of("accessToken", access));
}
```

---

## 5. API 요약

| 클래스 | 메서드 | 설명 |
|---|---|---|
| `OAuthLoginService` | `fetchUserInfo(provider, code)` | code → access_token → userinfo 한 번에 |
| `OAuthTokenClient` | `exchange(provider, code)` | 저수준 token 교환 |
| `OAuthUserInfoClient` | `fetch(provider, accessToken)` | 저수준 userinfo 조회 |
| `JwtProvider` | `generateAccessToken(userId, role)` | 🌟 access 발급 (`type=ACCESS`) |
| | `generateRefreshToken(userId)` | 🌟 refresh 발급 (`type=REFRESH`) |
| | `validateAccess/validateRefresh(token)` | type-aware 검증 |
| | `parseAccess/parseRefresh(token)` → `JwtPayload` | type 불일치 시 예외 |
| `RefreshTokenService` | `save/get/validate/delete` | Redis `RT:{userId}` |
| | `validateAndConsume` (Redis 6.2+) | 🌟 원자적 rotation |
| `SignupTokenProvider` | `builder(provider,id,email)` | fluent 발급, 임의 클레임 추가 |
| | `validate(token)`, `parse(token)` | 서명/타입 검증 |
| `SignupTokenService` | `validateAndConsume` | 회원가입 완료 시 원자적 소비 |
| `SignupTokenCookieWriter` | `write/read/clear` | HttpOnly 쿠키 관리 |
| `SignupTokenSecurity` | `corsForCookieAuth(...)` | CORS 헬퍼 |

---

## 6. Bean 커스터마이징

모든 자동 등록 빈은 `@ConditionalOnMissingBean`. 같은 타입 빈을 직접 등록하면 자동 대체됩니다.
```java
@Bean
public RestClient oauthRestClient() {
    return RestClient.builder()               // 커스텀 타임아웃/인터셉터
            .requestFactory(myFactory)
            .build();
}
```

---

## 7. 트러블슈팅

| 증상 | 원인 / 해결 |
|---|---|
| startup 시 `IllegalStateException: jwt.secret-key must be at least 32 bytes` | secret 짧음. `openssl rand -base64 48` |
| startup WARN `RefreshTokenService will NOT be registered — RedisTemplate missing` | `spring-boot-starter-data-redis` 미추가. Redis 안 쓰고 access-only auth 라면 무시 가능 |
| `OAuthException: oauth.google is not configured (client-id missing)` | 해당 provider config 미설정. `oauth.google.client-id` 부터 세팅 |
| `OAuthException: Failed to exchange code for token` | redirect_uri 불일치 or code 만료(1분). Kakao/Google 콘솔에 등록된 redirect URI 확인 |
| Kakao userinfo email 이 null | Kakao 앱 동의항목에서 이메일 필수 미설정. 라이브러리가 `kakao_{id}@kakao.user` 로 fallback |
| `IllegalArgumentException: Token type mismatch` | access token 자리에 refresh 넣었거나 반대. 정상 동작 (type 검증 통과 못함) |
| refresh 재사용 시도 시 401 | `validateAndConsume` 로 이미 GETDEL 됨. 정상 동작 (rotation 원자성 보장) |

---

## 8. 보안 특징
- ✅ **Fail-fast**: secret 길이/필수 필드/쿠키 조합 startup 검증
- ✅ **토큰 타입 강제**: `type=ACCESS/REFRESH/SIGNUP` 클레임 검증 → 재사용 차단
- ✅ **원자적 소비**: refresh rotation / signup 완료 모두 Redis `GETDEL` 로 race 방지
- ✅ **Timing-safe 비교**: `MessageDigest.isEqual`
- ✅ **HttpOnly · SameSite=None · Secure** signup 쿠키 기본
- ✅ **Secret 마스킹**: `toString()` 오버라이드
- ✅ **Redis 미설정 명시적 WARN**: 조용한 실패 방지

## 라이선스
MIT

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

**Step 2. `~/.gradle/gradle.properties`** — 커밋 금지 (홈 디렉토리)
```properties
gpr.user=<본인_github_username>          # 예: my-github-id (라이브러리 소유자 이름 아님!)
gpr.token=ghp_xxxxxxxxxxxxxxxxxxxxx      # 본인이 발급한 PAT
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

### 1-2. 인증/개인정보 관련 안내

라이브러리 사용 전에 자주 묻는 것:

**Q1. `catomat0` 이 URL 에 있는데 이걸 내가 써야 하나?**
아니오. URL 의 `catomat0` 은 **패키지가 호스팅된 GitHub 계정 경로**입니다 (도서관 주소 같은 개념 — 못 바꿈).
반면 `gpr.user` / `gpr.token` 은 **다운로드하는 본인의** GitHub 계정과 PAT 입니다. **서로 다른 값이 정상.**

| 항목 | 값 | 의미 |
|---|---|---|
| `url = ...catomat0/spring_oauth_starter` | 고정 | 패키지가 있는 위치 (내 계정) |
| `gpr.user` | 본인 GitHub username | 인증 주체 (다운받는 사람) |
| `gpr.token` | 본인이 발급한 PAT | 인증 자격 (다운받는 사람) |

**Q2. 이 라이브러리를 임포트하면 라이브러리 소유자에게 내 정보/토큰이 흘러가나?**
아니오.
- PAT 는 오직 **GitHub Packages 서버**로 인증 요청 시에만 사용됩니다 (HTTPS). 라이브러리 코드에는 흘러가지 않음.
- 라이브러리는 순수 JAR — 네트워크로 어디에 정보 보내는 코드 **없음**. OAuth 콜백에서 Kakao/Google API 를 호출하는 것 외에 외부 통신 0.
- Gradle 이 authenticated request 로 JAR 만 받아옴. 그게 끝.

**Q3. 라이브러리 저장소에 소유자의 토큰이나 개인정보가 들어있진 않나?**
없습니다. 배포 전에 audit 결과:
- 하드코딩된 PAT/API key/시크릿 → **0건**
- `build.gradle` / workflow — 모두 env 변수 (`GITHUB_TOKEN`, `GITHUB_ACTOR`) 또는 GitHub Actions 런타임 시크릿만 참조. 하드코딩 없음
- README 의 `ghp_xxxxxxxxxxxxxxxxxxxxx` → placeholder (실제 토큰 아님)
- 노출되는 정보: `catomat0` 이라는 GitHub username (이미 public repo 소유자로 공개된 정보)
- `.gitignore` 로 `gradle.properties`, `.gradle/`, `.idea/`, `build/` 등 실수 커밋 방지

**Q4. PAT 를 만들 때 최소한 어떤 권한만 주면 되나?**
`read:packages` **한 개면 충분** (라이브러리 다운로드용).
Public repo 이므로 `repo` 권한은 필요 없음. 최소 권한 원칙 준수를 위해 다른 스코프는 다 끄고 발급하세요.

**Q5. PAT 유출 시 어떻게?**
[Settings → Developer settings → PAT](https://github.com/settings/tokens) 에서 즉시 **Revoke** → 새로 발급. Gradle 캐시에는 credentials 안 남지만, 로컬 `gradle.properties` 재작성.

---

## 2. Developer Console 세팅

> ⚠️ **두 provider 모두 email 을 사용자정보로 받아야 합니다.** 콘솔에서 email scope/동의항목을 반드시 활성화하세요. 그래야 라이브러리가 `OAuthUserInfo.email()` 을 정상적으로 채워 회원 조회/가입에 사용할 수 있습니다.

### Kakao Developers
1. https://developers.kakao.com/ → 로그인 → **내 애플리케이션 → 애플리케이션 추가하기**
2. **앱 키** 탭에서 `REST API 키` 복사 → `KAKAO_CLIENT_ID`
3. **카카오 로그인 → 활성화** ON
4. **Redirect URI 등록**: `https://your-domain.com/api/auth/oauth2/kakao/callback` (개발용은 `http://localhost:8080/...`)
5. **동의항목** — ⚠️ **카카오계정(이메일) 을 반드시 "필수 동의"** 로 설정. "선택 동의" 로 두면 사용자가 거부 시 email 이 null 로 옴 (라이브러리가 `kakao_{id}@kakao.user` 로 임시 fallback 하지만, 이 값으로 실제 사용자와 통신 불가하므로 프로덕션에서는 필수 동의 강력 권장)
6. **보안 → Client Secret**: 사용 상태 ON → 코드 발급 → `KAKAO_CLIENT_SECRET`

### Google Cloud Console
1. https://console.cloud.google.com/ → 프로젝트 생성/선택
2. **APIs & Services → OAuth consent screen** 생성 (External)
3. **Scopes for Google APIs → ADD OR REMOVE SCOPES** → ⚠️ **`.../auth/userinfo.email` 반드시 추가** (`openid`, `.../auth/userinfo.profile` 도 함께 권장)
4. **Credentials → CREATE CREDENTIALS → OAuth client ID**
5. Application type: **Web application**
6. **Authorized redirect URIs**: `https://your-domain.com/api/auth/oauth2/google/callback` (dev: `http://localhost:8080/...`)
7. 생성 후 `Client ID` → `GOOGLE_CLIENT_ID`, `Client secret` → `GOOGLE_CLIENT_SECRET`
8. `application.yml` 의 `oauth.google.scope` 에 `openid email profile` 포함 필수

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

  # state 파라미터 (CSRF 방어). 아래는 디폴트값이라 미기재해도 동일
  state-redis-key-prefix: "OS:"
  state-ttl-seconds: 300

jwt:
  secret-key: ${JWT_SECRET_KEY}                       # ⚠️ 필수 (32byte 이상, 디폴트 없음)
  access-token-expiration: 1800000                    # ms, 디폴트 30분
  refresh-token-expiration: 1209600000                # ms, 디폴트 14일
  redis-key-prefix: "RT:"                             # 디폴트 RT:

signup-token:
  secret-key: ${SIGNUP_TOKEN_SECRET}                  # ⚠️ 필수 (JWT와 별도, 디폴트 없음)
  expiration: 1800000                                 # ms, 디폴트 30분
  redis-key-prefix: "ST:"                             # 디폴트 ST:
  cookie:
    name: signup_token                                # 디폴트 signup_token
    same-site: Lax                                    # 프로덕션 HTTPS 는 None
    secure: false                                     # 프로덕션 HTTPS 는 true
    http-only: true                                   # 디폴트 true
    path: /                                           # 디폴트 /
```

### 3-1. 프로퍼티 디폴트 요약표

| 프로퍼티 | 디폴트 | 필수? | 비고 |
|---|---|---|---|
| `jwt.secret-key` | — | ✅ 필수 | HS256 서명 키. 32byte 이상. 자동 디폴트 없음 (보안상) |
| `jwt.access-token-expiration` | `1800000` (30분) | 선택 | ms 단위 |
| `jwt.refresh-token-expiration` | `1209600000` (14일) | 선택 | ms 단위, access 보다 크거나 같아야 함 |
| `jwt.redis-key-prefix` | `"RT:"` | 선택 | Redis 키 접두사. 다른 서비스와 격리 시 변경 |
| `signup-token.secret-key` | — | ✅ 필수 | jwt.secret-key **와 다른 값 강력 권장** |
| `signup-token.expiration` | `1800000` (30분) | 선택 | 온보딩 이탈 방지용 짧은 TTL |
| `signup-token.redis-key-prefix` | `"ST:"` | 선택 | jwt/oauth prefix 와 달라야 함 (startup 검증) |
| `signup-token.cookie.*` | HttpOnly=true, SameSite=None, Secure=true | 선택 | 로컬 HTTP 개발환경은 SameSite=Lax + Secure=false |
| `oauth.<provider>.client-id` | — | provider별 opt-in | 세팅된 provider 만 활성화 |
| `oauth.<provider>.client-secret` / `redirect-uri` / `token-uri` / `user-info-uri` | — | provider 활성 시 필수 | token-uri / user-info-uri 는 https:// 강제 |
| `oauth.<provider>.scope` | `null` (Google 은 `openid email profile` 권장) | 선택 | |
| `oauth.state-redis-key-prefix` | `"OS:"` | 선택 | CSRF state 저장 prefix |
| `oauth.state-ttl-seconds` | `300` (5분) | 선택 | state 유효 시간 |

**Provider 부분 opt-in** — kakao 만 쓰고 싶으면 `oauth.google.*` 통째로 생략. 활성 provider 는 `client-id` 유무로 판단.

**Secret 생성** — 각각 다른 값으로:
```bash
openssl rand -base64 48    # jwt.secret-key 용
openssl rand -base64 48    # signup-token.secret-key 용 (별도로 한 번 더)
```

**Prefix 충돌 방지** — startup 시 `SignupTokenException(REDIS_PREFIX_COLLISION)` 로 fail-fast. 세 prefix 는 반드시 서로 달라야 함 (디폴트 `ST:` / `RT:` / `OS:` 그대로 쓰면 안전).

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

## 7. 에러 코드 & 트러블슈팅

라이브러리 예외는 `OAuthException` / `JwtException` / `SignupTokenException` 셋. 각각 `code()` 로 세부 케이스 분기 가능.

```java
try {
    OAuthUserInfo info = oauthLoginService.fetchUserInfo(provider, code);
} catch (OAuthException e) {
    switch (e.code()) {
        case PROVIDER_UNKNOWN            -> // 잘못된 provider 이름 (kakao/google 이외)
        case PROVIDER_NOT_CONFIGURED     -> // application.yml 미설정
        case INSECURE_URI                -> // token-uri/user-info-uri 가 http://
        case TOKEN_EXCHANGE_FAILED       -> // Kakao/Google 서버 통신 실패
        case TOKEN_EXCHANGE_EMPTY        -> // 응답이 비어있음
        case USERINFO_FETCH_FAILED       -> // userinfo 호출 실패
        case USERINFO_EMPTY              -> // userinfo 비어있음
        case STATE_INVALID               -> // CSRF state 검증 실패 (만료/재사용/불일치)
        case REDIS_PREFIX_COLLISION      -> // startup 만
    }
}
```

**JwtErrorCode**: `SECRET_KEY_MISSING`, `SECRET_KEY_TOO_SHORT`, `TOKEN_TYPE_MISMATCH`
**SignupTokenErrorCode**: `SECRET_KEY_MISSING`, `SECRET_KEY_TOO_SHORT`, `COOKIE_INSECURE_SAMESITE`, `EXPIRATION_INVALID`, `TOKEN_TYPE_MISMATCH`, `REDIS_PREFIX_COLLISION`

### 세팅/연결 실패 케이스

| 증상 / 에러 코드 | 원인 / 해결 |
|---|---|
| startup `JwtException(SECRET_KEY_MISSING)` | `jwt.secret-key` 미설정. 환경변수 확인 |
| startup `JwtException(SECRET_KEY_TOO_SHORT)` | secret 32byte 미만. `openssl rand -base64 48` |
| startup `IllegalStateException: oauth.X.Y must be configured...` | provider client-id 는 있는데 다른 필드 누락. 위 `application.yml` 예시 참고 |
| startup `IllegalStateException: oauth.X.token-uri must use https://` | http:// URL 설정 시 client_secret 평문 노출 위험 → https:// 강제 |
| startup WARN `RefreshTokenService will NOT be registered — RedisTemplate missing` | `spring-boot-starter-data-redis` 미추가. Redis 안 쓰고 access-only 라면 무시 가능 |
| `OAuthException(PROVIDER_UNKNOWN)` | provider path variable 이 kakao/google 이외. URL 오탈자 확인 |
| `OAuthException(PROVIDER_NOT_CONFIGURED)` | 호출한 provider config 미설정. `oauth.<provider>.client-id` 확인 |
| `OAuthException(TOKEN_EXCHANGE_FAILED)` | redirect_uri 불일치 or code 만료(1분) or Kakao/Google 서버 5xx. 메시지의 `uri=` 값과 콘솔 등록 URI 대조 |
| `OAuthException(TOKEN_EXCHANGE_EMPTY)` | 200 OK 지만 access_token null. Kakao/Google 앱 상태 (검수/일시 정지) 확인 |
| `OAuthException(USERINFO_FETCH_FAILED)` | userinfo 엔드포인트 호출 실패. 네트워크/scope 확인 |
| Kakao userinfo email 이 null | Kakao 앱 동의항목에서 이메일 필수 미설정. 라이브러리가 `kakao_{id}@kakao.user` 로 fallback |
| `JwtException(TOKEN_TYPE_MISMATCH)` | access token 자리에 refresh 넣었거나 반대. 정상 동작 |
| refresh 재사용 시도 시 401 | `validateAndConsume` 로 이미 GETDEL 됨. 정상 동작 (rotation 원자성 보장) |
| `RedisConnectionFailureException` | Redis 서버 다운/네트워크 단절. `spring.data.redis.host/port` 확인 |
| `io.jsonwebtoken.ExpiredJwtException` | 토큰 만료. `validateAccess()` 로 먼저 체크한 후 parse |

---

## 8. CSRF 대응 (OAuth `state` — 라이브러리 제공)

라이브러리가 `OAuthStateService` 를 자동 등록합니다 (Redis 필요). CSRF 공격 방어를 위해 **authorize 리다이렉트 전 발급, 콜백에서 원자적 소비** 하세요.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuthStateService oauthStateService;
    private final OAuthLoginService oauthLoginService;
    // ...

    /** 프론트가 이 엔드포인트로 리다이렉트 → 라이브러리가 state 발급 → Kakao/Google authorize URL 로 재리다이렉트 */
    @GetMapping("/oauth2/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String state = oauthStateService.issue(provider);   // Redis 에 자동 저장 (5분 TTL)
        // authorize URL 은 application.yml 의 oauth.<provider>.authorize-uri 사용
        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=..." + "&redirect_uri=..." + "&response_type=code"
                + "&state=" + state;
        response.sendRedirect(url);
    }

    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<?> callback(@PathVariable String provider,
                                     @RequestParam String code,
                                     @RequestParam String state) {
        if (!oauthStateService.validateAndConsume(state, provider)) {
            throw new OAuthException(OAuthErrorCode.STATE_INVALID,
                    "Invalid or reused OAuth state");
        }
        OAuthUserInfo info = oauthLoginService.fetchUserInfo(provider, code);
        // ... existing/new 분기
    }
}
```

**동작 원리**
- `issue(provider)` — 24 byte cryptographically secure random → Base64URL 인코딩 → Redis `OS:<state>` 에 provider 값 저장 (TTL 5분)
- `validateAndConsume(state, provider)` — Redis `GETDEL` 로 원자적 조회+삭제, provider 일치 여부 timing-safe 비교. 재사용/CSRF/만료 시 모두 false 반환

state 검증을 생략하면 CSRF 공격으로 피해자 계정에 공격자의 OAuth 세션이 붙는 시나리오 발생 가능.

**Refresh token 은 응답 body 대신 HttpOnly 쿠키로** 전달 권장 (XSS 방어). 위 README 예시의 body 반환은 데모용이며 프로덕션에서는 쿠키 사용.

---

## 9. 보안 특징
- ✅ **Fail-fast**: secret 길이/필수 필드/쿠키 조합 startup 검증
- ✅ **토큰 타입 강제**: `type=ACCESS/REFRESH/SIGNUP` 클레임 검증 → 재사용 차단
- ✅ **원자적 소비**: refresh rotation / signup 완료 모두 Redis `GETDEL` 로 race 방지
- ✅ **Timing-safe 비교**: `MessageDigest.isEqual`
- ✅ **HttpOnly · SameSite=None · Secure** signup 쿠키 기본
- ✅ **Secret 마스킹**: `toString()` 오버라이드
- ✅ **Redis 미설정 명시적 WARN**: 조용한 실패 방지

## 라이선스
Apache License 2.0 — 자세한 내용은 [LICENSE](LICENSE) 참조.

Apache 2.0 요약: 자유롭게 사용/수정/배포 가능. 단 (1) 저작권 및 라이선스 고지 유지, (2) 수정 시 변경 사항 명시, (3) 이 라이브러리에 대한 특허 소송 제기 시 라이선스 자동 종료.

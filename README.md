# oauth-helper

[![Release](https://img.shields.io/github/v/release/catomat0/Oah?sort=semver)](https://github.com/catomat0/Oah/releases)
[![JavaDoc](https://img.shields.io/badge/javadoc-latest-blue)](https://catomat0.github.io/Oah/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

Spring Boot용 소셜로그인 스타터. Kakao/Google OAuth2 로그인 + JWT (access/refresh) + 온보딩 signup token 을 자동 설정으로 제공합니다.

📖 **[JavaDoc API 레퍼런스](https://catomat0.github.io/Oah/)** · **[CHANGELOG](CHANGELOG.md)** · **[Releases](https://github.com/catomat0/Oah/releases)**

- **OAuth2 로그인** — Kakao/Google 콜백에서 code → access_token → userinfo 한 번에 (Kakao/Google 표준 엔드포인트 URI 하드코딩)
- **CSRF `state` + PKCE (S256)** — `OahStateService` 가 state 와 code_challenge 자동 생성/원자적 검증
- **`OahAuthorizeUrlBuilder`** — authorize URL 자동 조립 (state/PKCE/scope 포함)
- **JWT** access/refresh 발급/검증 (JJWT 0.12.6, `type=ACCESS/REFRESH` 강제 구분)
- **`OahJwtAuthenticationFilter`** — Bearer 토큰 → SecurityContext 자동 세팅 (Spring Security 있을 때만 등록)
- **Refresh token** Redis `RT:{userId}` + GETDEL 원자적 rotation + `OahRefreshTokenCookieWriter` (HttpOnly)
- **Signup token** — 온보딩 이탈 시 재개 (JWT + Redis + HttpOnly 쿠키)
- **Spring Boot AutoConfiguration** — 빈 자동 등록, provider별 opt-in
- **필수 종속성 부재 시 명시적 WARN 로그** — 조용한 실패 방지
- **에러 코드 enum** — `catch(e){ switch(e.code()){...} }` 로 세부 케이스 분기

---

## 1. 설치

**요약: 그냥 아래 두 줄 넣으면 끝.** 인증 세팅 필요 없고, 로컬/GitHub Actions/Jenkins/GitLab CI 어디서든 그대로 동작합니다.

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.catomat0:Oah:2.1.0'
}
```

**필수 런타임 의존성** (consumer 프로젝트에 이미 있어야 함):
- `spring-boot-starter-web`
- `spring-boot-starter-data-redis` (refresh/signup/state 저장용)
- Redis 서버 **6.2 이상** (`GETDEL` 사용)
- `spring-boot-starter-security` (선택 — `OahJwtAuthenticationFilter` 자동 등록 원할 때)

### 1-1. 왜 JitPack?

| 상황 | JitPack | GitHub Packages | Maven Central |
|---|---|---|---|
| 개인 레포에서 사용 | ✅ 세팅 0 | ⚠️ PAT 필요 | ✅ 세팅 0 |
| 다른 사용자 레포에서 사용 | ✅ 세팅 0 | ⚠️ PAT 필요 | ✅ 세팅 0 |
| 외부 조직 레포에서 사용 | ✅ 세팅 0 | ❌ 별도 PAT 발급 필요 | ✅ 세팅 0 |
| GitHub Actions | ✅ 그대로 됨 | ⚠️ secrets 세팅 필요 | ✅ 그대로 됨 |
| Jenkins/GitLab/CircleCI | ✅ 그대로 됨 | ⚠️ credential 저장 필요 | ✅ 그대로 됨 |

**JitPack 은 public 저장소에 대해 무인증 접근**을 제공합니다. Consumer 프로젝트가 개인이든 조직이든 CI 든 로컬이든, 위 두 줄만 있으면 그냥 동작합니다.

### 1-2. Maven 사용자

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.catomat0</groupId>
    <artifactId>Oah</artifactId>
    <version>2.1.0</version>
</dependency>
```

### 1-3. 버전 지정 방법

| 표기 | 의미 |
|---|---|
| `2.1.0` | 특정 릴리즈 태그 (권장) |
| `main-SNAPSHOT` | main 브랜치 최신 커밋 (실험용, 캐시 30분) |
| `<커밋해시>` | 특정 커밋 (재현 가능) |

**첫 요청 시 JitPack 서버가 소스로부터 빌드**하기 때문에 30초~2분 지연이 있을 수 있고, 이후 요청은 캐시에서 즉시 응답합니다. CI 에서 처음 pull 할 때만 잠깐 느립니다.

### 1-4. CI/CD 스니펫

**GitHub Actions** — 아무 추가 세팅 불필요
```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
- run: ./gradlew build
```

**Jenkins / GitLab CI / CircleCI** — Consumer 의 표준 Java 빌드 스텝 그대로. Oah 때문에 추가 credential 세팅 필요 **없음**.

**Docker 빌드** — 그냥 됨.
```dockerfile
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build --no-daemon
```

---

## 1-B. (선택) GitHub Packages 로 사용하기

JitPack 은 첫 요청이 느릴 수 있어서 **CI 캐시 히트율을 최우선**으로 하고 싶거나, **완전 private 배포**가 필요한 경우 GitHub Packages 를 쓸 수 있습니다. 다만 소비자마다 PAT 세팅이 필요합니다.

<details>
<summary>펼쳐서 보기</summary>

**Step 1. GitHub PAT 발급**
[Settings → Developer settings → Personal access tokens (classic)](https://github.com/settings/tokens/new) — Scope: `read:packages` 만 필요.

**Step 2. `~/.gradle/gradle.properties`** — 커밋 금지
```properties
gpr.user=<본인_github_username>
gpr.token=ghp_xxxxxxxxxxxxxxxxxxxxx
```

**Step 3. `build.gradle`**
```gradle
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/catomat0/Oah')
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = project.findProperty('gpr.token') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    implementation 'com.github.catomat0:oauth-helper:2.1.0'
}
```

**Step 4. CI 별 세팅**

- **GitHub Actions (같은 catomat0 계정 레포)** — workflow 에 `permissions: { packages: read }` 만 추가하면 자동 `GITHUB_TOKEN` 사용 가능
- **GitHub Actions (다른 계정/조직)** — 본인 PAT 을 secret 으로 등록 후 주입
  ```yaml
  - run: ./gradlew build
    env:
      GITHUB_ACTOR: ${{ github.actor }}
      GITHUB_TOKEN: ${{ secrets.GH_PACKAGES_READ_TOKEN }}
  ```
- **Jenkins/GitLab** — CI credential 저장소에 `gpr.user`/`gpr.token` 등록 후 build args 로 전달

**Q. 라이브러리를 임포트하면 소유자에게 내 정보/토큰이 흘러가나?**
아니오. PAT 은 GitHub Packages 서버 인증에만 쓰이고, 라이브러리 코드는 순수 JAR (OAuth 콜백에서 Kakao/Google API 호출 외 외부 통신 없음).

</details>

---

## 2. Developer Console 세팅

> ⚠️ **두 provider 모두 email 을 사용자정보로 받아야 합니다.** 콘솔에서 email scope/동의항목을 반드시 활성화하세요. 그래야 라이브러리가 `OahUserInfo.email()` 을 정상적으로 채워 회원 조회/가입에 사용할 수 있습니다.

### Kakao Developers
1. https://developers.kakao.com/ → 로그인 → **내 애플리케이션 → 애플리케이션 추가하기**
2. **앱 키** 탭에서 `REST API 키` 복사 → `KAKAO_CLIENT_ID`
3. **카카오 로그인 → 활성화** ON
4. **Redirect URI 등록**: `https://your-domain.com/api/auth/oauth2/kakao/callback` (개발용은 `http://localhost:8080/...`)
5. **동의항목** — ⚠️ **카카오계정(이메일) 을 반드시 "필수 동의"** 로 설정. 미설정 시 `OahException(EMAIL_MISSING)` 발생 (v1.1.0부터 fallback email 제거됨)
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

### 3-1. 최소 설정 (권장) — 5개 항목만

Kakao/Google 표준 엔드포인트 URI 는 라이브러리가 하드코딩 디폴트로 제공. 필수 5개만 세팅하면 동작:

```yaml
oauth:
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
    redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:8080/api/auth/oauth2/kakao/callback}
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8080/api/auth/oauth2/google/callback}

jwt:
  secret-key: ${JWT_SECRET_KEY}                       # 32byte 이상

signup-token:
  secret-key: ${SIGNUP_TOKEN_SECRET}                  # jwt.secret-key 와 다른 값
```

### 3-2. 전체 설정 (커스터마이징)

```yaml
oauth:
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
    redirect-uri: ${KAKAO_REDIRECT_URI}
    # 아래 4개는 미기재 시 하드코딩 디폴트 사용 (Kakao 공식 URI)
    authorize-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
    scope: "account_email profile_nickname profile_image"

  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_REDIRECT_URI}
    authorize-uri: https://accounts.google.com/o/oauth2/v2/auth
    token-uri: https://oauth2.googleapis.com/token
    user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
    scope: "openid email profile"

  # state (CSRF) + PKCE 세팅
  state-redis-key-prefix: "OS:"                       # 디폴트 OS:
  state-ttl-seconds: 300                              # 디폴트 5분

  # OAuth HTTP client 타임아웃 (Kakao/Google 호출)
  rest-client:
    connect-timeout-ms: 3000                          # 디폴트 3s
    read-timeout-ms: 5000                             # 디폴트 5s

jwt:
  secret-key: ${JWT_SECRET_KEY}                       # ⚠️ 필수 (32byte 이상, 디폴트 없음)
  access-token-expiration: 1800000                    # ms, 디폴트 30분
  refresh-token-expiration: 1209600000                # ms, 디폴트 14일
  redis-key-prefix: "RT:"                             # 디폴트 RT:
  refresh-cookie:                                     # OahRefreshTokenCookieWriter 세팅
    name: refresh_token
    path: /
    http-only: true
    secure: true                                      # HTTP dev 는 false
    same-site: None                                   # HTTP dev 는 Lax

signup-token:
  secret-key: ${SIGNUP_TOKEN_SECRET}                  # ⚠️ 필수 (jwt.secret-key 와 다른 값)
  expiration: 1800000                                 # ms, 디폴트 30분
  redis-key-prefix: "ST:"                             # 디폴트 ST:
  cookie:
    name: signup_token                                # 디폴트 signup_token
    same-site: Lax                                    # 프로덕션 HTTPS 는 None
    secure: false                                     # 프로덕션 HTTPS 는 true
    http-only: true                                   # 디폴트 true
    path: /                                           # 디폴트 /
```

### 3-3. 프로퍼티 디폴트 요약표

| 프로퍼티 | 디폴트 | 필수? | 비고 |
|---|---|---|---|
| `jwt.secret-key` | — | ✅ 필수 | HS256 서명 키. 32byte 이상. 자동 디폴트 없음 (보안상) |
| `jwt.access-token-expiration` | `1800000` (30분) | 선택 | ms 단위 |
| `jwt.refresh-token-expiration` | `1209600000` (14일) | 선택 | ms 단위, access 보다 크거나 같아야 함 |
| `jwt.redis-key-prefix` | `"RT:"` | 선택 | Redis 키 접두사 |
| `jwt.refresh-cookie.*` | name=`refresh_token`, HttpOnly=true, SameSite=None, Secure=true | 선택 | 로컬 HTTP 은 SameSite=Lax + Secure=false |
| `signup-token.secret-key` | — | ✅ 필수 | jwt.secret-key **와 다른 값 강력 권장** |
| `signup-token.expiration` | `1800000` (30분) | 선택 | |
| `signup-token.redis-key-prefix` | `"ST:"` | 선택 | jwt/oauth prefix 와 달라야 함 (startup 검증) |
| `signup-token.cookie.*` | HttpOnly=true, SameSite=None, Secure=true | 선택 | |
| `oauth.<provider>.client-id` | — | provider별 opt-in | 세팅된 provider 만 활성화 |
| `oauth.<provider>.client-secret` | — | provider 활성 시 필수 | |
| `oauth.<provider>.redirect-uri` | — | provider 활성 시 필수 | |
| `oauth.<provider>.authorize-uri` | Kakao/Google 공식 URI | 선택 | 미기재 시 하드코딩 디폴트 |
| `oauth.<provider>.token-uri` | Kakao/Google 공식 URI | 선택 | https:// 강제 |
| `oauth.<provider>.user-info-uri` | Kakao/Google 공식 URI | 선택 | https:// 강제 |
| `oauth.<provider>.scope` | Kakao: `account_email profile_nickname profile_image` / Google: `openid email profile` | 선택 | |
| `oauth.state-redis-key-prefix` | `"OS:"` | 선택 | CSRF state prefix |
| `oauth.state-ttl-seconds` | `300` (5분) | 선택 | |
| `oauth.rest-client.connect-timeout-ms` | `3000` | 선택 | OAuth HTTP 클라이언트 |
| `oauth.rest-client.read-timeout-ms` | `5000` | 선택 | |

**Provider 부분 opt-in** — kakao 만 쓰고 싶으면 `oauth.google.*` 통째로 생략. 활성 provider 는 `client-id` 유무로 판단.

**Secret 생성** — 각각 다른 값으로:
```bash
openssl rand -base64 48    # jwt.secret-key 용
openssl rand -base64 48    # signup-token.secret-key 용 (별도로 한 번 더)
```

**Prefix 충돌 방지** — startup 시 `OahSignupTokenException(REDIS_PREFIX_COLLISION)` 로 fail-fast. 세 prefix 는 반드시 서로 달라야 함 (디폴트 `ST:` / `RT:` / `OS:` 그대로 쓰면 안전).

---

## 4. 콜백 컨트롤러 예시 (풀 플로우)

**v2.1.0 부터 3개 도메인 파사드 (`OahOAuth` / `OahJwt` / `OahSignup`) 제공** — 개별 서비스 9개 주입 대신 파사드 3개만 주입:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final OahOAuth oauth;         // state + authorize + login
    private final OahJwt jwt;             // provider + refresh + cookie
    private final OahSignup signup;       // provider + service + cookie
    private final UserRepository userRepository;

    /** 1) 프론트가 이 엔드포인트로 redirect → 라이브러리가 state + PKCE 생성 후 Kakao/Google authorize URL 로 redirect */
    @GetMapping("/oauth2/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        OahAuthorizeParams params = oauth.state().issue(provider);
        String url = oauth.authorize().build(provider, params);
        response.sendRedirect(url);
    }

    /** 2) Kakao/Google 이 code + state 로 redirect → state 검증 → code_verifier 로 token 교환 → 회원 분기 */
    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<?> callback(@PathVariable String provider,
                                     @RequestParam String code,
                                     @RequestParam String state,
                                     HttpServletResponse response) {
        String codeVerifier = oauth.state().validateAndConsume(state, provider);
        if (codeVerifier == null) {
            throw new OahException(OahErrorCode.STATE_INVALID, "Invalid or reused state");
        }
        OahUserInfo info = oauth.login().fetchUserInfo(provider, code, codeVerifier);

        return userRepository
                .findByProviderAndProviderId(info.provider(), info.providerId())
                .<ResponseEntity<?>>map(user -> issueJwt(user, response))
                .orElseGet(() -> issueSignupToken(info, response));
    }

    private ResponseEntity<?> issueJwt(User user, HttpServletResponse response) {
        String uid = user.getId().toString();
        String access = jwt.provider().generateAccessToken(uid, user.getRole().name());
        String refresh = jwt.provider().generateRefreshToken(uid);
        jwt.refresh().save(uid, refresh);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        jwt.cookie().write(response, refresh);                     // HttpOnly 쿠키로
        return ResponseEntity.ok(Map.of("registered", true));
    }

    private ResponseEntity<?> issueSignupToken(OahUserInfo info, HttpServletResponse response) {
        String token = signup.provider().builder(info.provider(), info.providerId(), info.email())
                .claim("nickname", info.nickname())
                .claim("profileImage", info.profileImage())
                .build();
        signup.service().save(info.provider(), info.providerId(), token);
        signup.cookie().write(response, token);
        return ResponseEntity.ok(Map.of("registered", false));
    }
}
```

> **개별 서비스 직접 주입 방식도 그대로 지원** — 파사드가 마음에 안 들면 `OahStateService`, `OahJwtProvider` 등 개별 빈을 그대로 주입해도 됨.
> **파사드 자동 등록 조건**: 파사드 내부 3개 빈이 모두 존재해야 함. 예를 들어 Redis 미설정 → `OahJwt` 미등록 → `OahJwtProvider` 만 개별 주입.

### 4-1. Refresh token rotation (쿠키 기반)

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
    String oldRefresh = refreshCookieWriter.read(request);
    if (oldRefresh == null || !jwtProvider.validateRefresh(oldRefresh)) {
        throw new OahJwtException(OahJwtErrorCode.TOKEN_TYPE_MISMATCH, "Invalid refresh token");
    }
    OahJwtPayload p = jwtProvider.parseRefresh(oldRefresh);

    // 원자적 소비 — 이전 refresh 재사용 시 감지
    if (!refreshTokenService.validateAndConsume(p.userId(), oldRefresh)) {
        throw new IllegalStateException("Refresh token expired or reused");
    }
    User user = userRepository.findById(Long.valueOf(p.userId())).orElseThrow();

    String newAccess = jwtProvider.generateAccessToken(p.userId(), user.getRole().name());
    String newRefresh = jwtProvider.generateRefreshToken(p.userId());
    refreshTokenService.save(p.userId(), newRefresh);

    response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess);
    refreshCookieWriter.write(response, newRefresh);
    return ResponseEntity.ok().build();
}
```

### 4-2. 회원가입 완료 (signup token → JWT 전환)

```java
@PostMapping("/signup")
public ResponseEntity<?> signup(@RequestBody SignupRequest req,
                               HttpServletRequest request,
                               HttpServletResponse response) {
    String token = signupCookieWriter.read(request);
    if (token == null || !signupTokenProvider.validate(token)) {
        throw new OahSignupTokenException(OahSignupTokenErrorCode.TOKEN_TYPE_MISMATCH, "Invalid signup token");
    }
    OahSignupTokenPayload payload = signupTokenProvider.parse(token);
    if (!signupTokenService.validateAndConsume(payload.provider(), payload.providerId(), token)) {
        throw new IllegalStateException("Signup token expired or already used");
    }

    User user = userRepository.save(User.of(payload.provider(), payload.providerId(), payload.email(),
            payload.extra("nickname"), req.termAgreementIds()));
    String uid = user.getId().toString();

    String access = jwtProvider.generateAccessToken(uid, user.getRole().name());
    String refresh = jwtProvider.generateRefreshToken(uid);
    refreshTokenService.save(uid, refresh);

    signupCookieWriter.clear(response);
    response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
    refreshCookieWriter.write(response, refresh);
    return ResponseEntity.ok().build();
}
```

### 4-3. SecurityConfig 통합 (`OahJwtAuthenticationFilter` 등록)

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OahJwtAuthenticationFilter jwtFilter;   // 라이브러리가 자동 등록한 빈 주입

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

컨트롤러에서 `@AuthenticationPrincipal String userId` 로 access token 의 `sub` 클레임(userId) 자동 주입.

---

## 5. API 요약

| 클래스 | 메서드 | 설명 |
|---|---|---|
| `OahStateService` | `issue(provider)` → `OahAuthorizeParams` | 🌟 state + PKCE code_challenge 발급 (Redis 저장) |
| | `validateAndConsume(state, provider)` → codeVerifier | 🌟 원자적 GETDEL, code_verifier 반환 |
| `OahAuthorizeUrlBuilder` | `build(provider, params)` | authorize URL 조립 (state/PKCE/scope 포함) |
| `OahLoginService` | `fetchUserInfo(provider, code)` | 비-PKCE code → userinfo |
| | `fetchUserInfo(provider, code, codeVerifier)` | 🌟 PKCE code → userinfo |
| `OahTokenClient` | `exchange(provider, code[, codeVerifier])` | 저수준 token 교환 |
| `OahUserInfoClient` | `fetch(provider, accessToken)` | 저수준 userinfo 조회 |
| `OahJwtProvider` | `generateAccessToken(userId, role)` | access 발급 (`type=ACCESS`) |
| | `generateRefreshToken(userId)` | refresh 발급 (`type=REFRESH`) |
| | `validateAccess/validateRefresh(token)` | type-aware 검증 |
| | `parseAccess/parseRefresh(token)` → `OahJwtPayload` | type 불일치 시 예외 |
| `OahRefreshTokenService` | `save/get/validate/delete` | Redis `RT:{userId}` |
| | `validateAndConsume` (Redis 6.2+) | 🌟 원자적 rotation |
| `OahRefreshTokenCookieWriter` | `write/read/clear` | 🌟 refresh HttpOnly 쿠키 관리 |
| `OahJwtAuthenticationFilter` | (Spring Security 필터) | 🌟 Bearer → SecurityContext 자동 세팅 |
| `OahSignupTokenProvider` | `builder(provider,id,email)` | fluent 발급, 임의 클레임 추가 |
| | `validate(token)`, `parse(token)` | 서명/타입 검증 |
| `OahSignupTokenService` | `validateAndConsume` | 회원가입 완료 시 원자적 소비 |
| `OahSignupTokenCookieWriter` | `write/read/clear` | HttpOnly 쿠키 관리 |
| `OahSignupTokenSecurity` | `corsForCookieAuth(...)` | CORS 헬퍼 |
| **파사드 (v2.1.0+)** | | |
| `OahOAuth` (record) | `.state() / .authorize() / .login()` | 🌟 OAuth 3개 서비스 묶음 |
| `OahJwt` (record) | `.provider() / .refresh() / .cookie()` | 🌟 JWT 3개 서비스 묶음 |
| `OahSignup` (record) | `.provider() / .service() / .cookie()` | 🌟 Signup 3개 서비스 묶음 |

---

## 6. Bean 커스터마이징

모든 자동 등록 빈은 `@ConditionalOnMissingBean`. 같은 타입 빈을 직접 등록하면 자동 대체됩니다.

**타임아웃만 조정하고 싶으면** 프로퍼티로 처리:
```yaml
oauth:
  rest-client:
    connect-timeout-ms: 5000
    read-timeout-ms: 10000
```

**RestClient 자체를 커스터마이징** (인터셉터/프록시 등):
```java
@Bean("oauthRestClient")
public RestClient oauthRestClient(OahProperties props) {
    return RestClient.builder()
            .requestFactory(myCustomFactory)
            .requestInterceptor(myInterceptor)
            .build();
}
```

---

## 7. 에러 코드 & 트러블슈팅

라이브러리 예외는 `OahException` / `OahJwtException` / `OahSignupTokenException` 셋. 각각 `code()` 로 세부 케이스 분기 가능.

```java
try {
    OahUserInfo info = oauthLoginService.fetchUserInfo(provider, code);
} catch (OahException e) {
    switch (e.code()) {
        case PROVIDER_UNKNOWN            -> // 잘못된 provider 이름 (kakao/google 이외)
        case PROVIDER_NOT_CONFIGURED     -> // application.yml 미설정
        case PROVIDER_INCOMPLETE         -> // startup — client-id 는 있는데 다른 필드 누락
        case INSECURE_URI                -> // token-uri/user-info-uri 가 http://
        case STATE_TTL_INVALID           -> // startup — state-ttl-seconds <= 0
        case TOKEN_EXCHANGE_FAILED       -> // Kakao/Google 서버 통신 실패
        case TOKEN_EXCHANGE_EMPTY        -> // 응답이 비어있음
        case USERINFO_FETCH_FAILED       -> // userinfo 호출 실패
        case USERINFO_EMPTY              -> // userinfo 비어있음
        case EMAIL_MISSING               -> // 콘솔에서 email scope/동의항목 미설정
        case STATE_INVALID               -> // CSRF state 검증 실패 (만료/재사용/불일치)
        case REDIS_PREFIX_COLLISION      -> // startup 만
    }
}
```

**OahJwtErrorCode**: `SECRET_KEY_MISSING`, `SECRET_KEY_TOO_SHORT`, `TOKEN_TYPE_MISMATCH`, `EXPIRATION_INVALID`, `REFRESH_COOKIE_INSECURE_SAMESITE`
**OahSignupTokenErrorCode**: `SECRET_KEY_MISSING`, `SECRET_KEY_TOO_SHORT`, `COOKIE_INSECURE_SAMESITE`, `EXPIRATION_INVALID`, `TOKEN_TYPE_MISMATCH`, `REDIS_PREFIX_COLLISION`

### 세팅/연결 실패 케이스

| 증상 / 에러 코드 | 원인 / 해결 |
|---|---|
| startup `OahJwtException(SECRET_KEY_MISSING)` | `jwt.secret-key` 미설정. 환경변수 확인 |
| startup `OahJwtException(SECRET_KEY_TOO_SHORT)` | secret 32byte 미만. `openssl rand -base64 48` |
| startup `OahException(PROVIDER_INCOMPLETE)` | provider client-id 는 있는데 다른 필드 누락. 위 `application.yml` 예시 참고 |
| startup `OahException(INSECURE_URI)` | http:// URL 설정 시 client_secret 평문 노출 위험 → https:// 강제 |
| startup `OahException(STATE_TTL_INVALID)` | `oauth.state-ttl-seconds` 값이 0 이하 |
| startup `OahJwtException(EXPIRATION_INVALID)` | access/refresh expiration 이 0 이하 or refresh < access |
| startup `OahJwtException(REFRESH_COOKIE_INSECURE_SAMESITE)` | `jwt.refresh-cookie.same-site=None` 인데 `secure=false`. 브라우저가 쿠키 drop |
| startup WARN `OahRefreshTokenService will NOT be registered — RedisTemplate missing` | `spring-boot-starter-data-redis` 미추가. Redis 안 쓰고 access-only 라면 무시 가능 |
| `OahException(PROVIDER_UNKNOWN)` | provider path variable 이 kakao/google 이외. URL 오탈자 확인 |
| `OahException(PROVIDER_NOT_CONFIGURED)` | 호출한 provider config 미설정. `oauth.<provider>.client-id` 확인 |
| `OahException(TOKEN_EXCHANGE_FAILED)` | redirect_uri 불일치 or code 만료(1분) or Kakao/Google 서버 5xx. 메시지의 `uri=` 값과 콘솔 등록 URI 대조 |
| `OahException(TOKEN_EXCHANGE_EMPTY)` | 200 OK 지만 access_token null. Kakao/Google 앱 상태 (검수/일시 정지) 확인 |
| `OahException(USERINFO_FETCH_FAILED)` | userinfo 엔드포인트 호출 실패. 네트워크/scope 확인 |
| `OahException(EMAIL_MISSING)` | 콘솔에서 email scope 미설정. Kakao: '카카오계정(이메일)' 필수 동의 / Google: `email` scope 추가 |
| `OahJwtException(TOKEN_TYPE_MISMATCH)` | access token 자리에 refresh 넣었거나 반대. 정상 동작 |
| refresh 재사용 시도 시 401 | `validateAndConsume` 로 이미 GETDEL 됨. 정상 동작 (rotation 원자성 보장) |
| `RedisConnectionFailureException` | Redis 서버 다운/네트워크 단절. `spring.data.redis.host/port` 확인 |
| `io.jsonwebtoken.ExpiredJwtException` | 토큰 만료. `validateAccess()` 로 먼저 체크한 후 parse |

---

## 8. CSRF `state` + PKCE 동작 원리

라이브러리가 `OahStateService` + `OahAuthorizeUrlBuilder` 자동 등록 (Redis 필요). 위 §4 예시대로 두 엔드포인트만 만들면 됨. 내부 동작:

**authorize 단계 (`OahStateService.issue`)**
1. 24 byte cryptographically secure random 생성 → Base64URL 인코딩 → `state`
2. 32 byte 랜덤 → `code_verifier`
3. `code_challenge = base64url(sha256(code_verifier))`
4. Redis `OS:<state>` 에 `{provider}|{codeVerifier}` 저장 (TTL 5분, 프로퍼티로 조절)
5. `OahAuthorizeParams(state, codeChallenge, "S256")` 반환

**callback 단계 (`OahStateService.validateAndConsume`)**
1. Redis `GETDEL OS:<state>` — 원자적 조회+삭제
2. 저장된 provider 와 요청의 provider 를 **timing-safe compare**
3. 일치 시 `codeVerifier` 반환, 불일치/만료/재사용 시 `null`
4. `codeVerifier` 를 `OahLoginService.fetchUserInfo(provider, code, codeVerifier)` 에 전달 → token 교환 시 함께 전송 → 서버가 code_challenge 와 검증

**방어되는 공격**
- **CSRF** — state 없거나 재사용 시 검증 실패 → 요청 거부
- **Code injection** — 공격자가 훔친 code 로 자기 재현하려 해도 code_verifier 를 모름
- **Replay** — state 는 1회성 (GETDEL)

**Refresh token** — 위 §4-1 예시처럼 `OahRefreshTokenCookieWriter` 로 HttpOnly 쿠키에 저장. body/localStorage 저장은 XSS 취약.

---

## 9. 보안 특징
- ✅ **Fail-fast**: secret 길이/필수 필드/쿠키 조합/HTTPS/prefix 충돌 startup 검증
- ✅ **토큰 타입 강제**: `type=ACCESS/REFRESH/SIGNUP` 클레임 검증 → 재사용 차단
- ✅ **CSRF `state`**: 1회성 GETDEL + provider 매칭 검증
- ✅ **PKCE (S256)**: code injection / code interception 공격 방어
- ✅ **원자적 소비**: refresh rotation / signup 완료 / state 검증 모두 Redis `GETDEL`
- ✅ **Timing-safe 비교**: `MessageDigest.isEqual`
- ✅ **HttpOnly · SameSite · Secure** 쿠키 기본 (refresh/signup)
- ✅ **HTTPS 강제**: `token-uri` / `user-info-uri` http:// 시작 거부
- ✅ **Secret 마스킹**: `toString()` 오버라이드
- ✅ **Redis 미설정 명시적 WARN**: 조용한 실패 방지
- ✅ **Kakao email 필수**: 합성 email 대신 명시적 예외

## 라이선스
Apache License 2.0 — 자세한 내용은 [LICENSE](LICENSE) 참조.

Apache 2.0 요약: 자유롭게 사용/수정/배포 가능. 단 (1) 저작권 및 라이선스 고지 유지, (2) 수정 시 변경 사항 명시, (3) 이 라이브러리에 대한 특허 소송 제기 시 라이선스 자동 종료.

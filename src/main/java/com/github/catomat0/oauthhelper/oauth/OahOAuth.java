package com.github.catomat0.oauthhelper.oauth;

/**
 * OAuth 관련 서비스 3개를 묶은 파사드.
 * <p>컨트롤러에서 개별 빈 3개 대신 이 하나만 주입받아 사용:
 * <pre>{@code
 * public AuthController(OahOAuth oauth) { this.oauth = oauth; }
 *
 * @GetMapping("/authorize")
 * public void authorize(...) {
 *     OahAuthorizeParams p = oauth.state().issue("kakao");
 *     String url = oauth.authorize().build("kakao", p);
 *     // ...
 * }
 * @GetMapping("/callback")
 * public ResponseEntity<?> callback(...) {
 *     String verifier = oauth.state().validateAndConsume(state, provider);
 *     OahUserInfo info = oauth.login().fetchUserInfo(provider, code, verifier);
 *     // ...
 * }
 * }</pre>
 * <p>Redis 세팅 안 되어 있으면 {@code state} 빈이 없어 이 파사드도 자동 등록 안 됨 —
 * 그 경우 {@link OahAuthorizeUrlBuilder} / {@link OahLoginService} 를 직접 주입.
 */
public record OahOAuth(
        OahStateService state,
        OahAuthorizeUrlBuilder authorize,
        OahLoginService login
) {}

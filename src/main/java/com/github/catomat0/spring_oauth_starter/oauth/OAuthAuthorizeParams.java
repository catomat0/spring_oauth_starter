package com.github.catomat0.spring_oauth_starter.oauth;

/**
 * authorize URL 조립에 필요한 파라미터.
 * <p>{@link OAuthStateService#issue(String)} 가 반환.
 */
public record OAuthAuthorizeParams(
        String state,
        String codeChallenge,
        String codeChallengeMethod
) {
    public static OAuthAuthorizeParams of(String state, String codeChallenge) {
        return new OAuthAuthorizeParams(state, codeChallenge, "S256");
    }
}

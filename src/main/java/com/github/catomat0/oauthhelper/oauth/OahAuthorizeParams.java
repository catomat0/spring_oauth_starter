package com.github.catomat0.oauthhelper.oauth;

/**
 * authorize URL 조립에 필요한 파라미터.
 * <p>{@link OahStateService#issue(String)} 가 반환.
 */
public record OahAuthorizeParams(
        String state,
        String codeChallenge,
        String codeChallengeMethod
) {
    public static OahAuthorizeParams of(String state, String codeChallenge) {
        return new OahAuthorizeParams(state, codeChallenge, "S256");
    }
}

package com.github.catomat0.oauthhelper.oauth;

import com.github.catomat0.oauthhelper.oauth.dto.OahTokenResponse;

/**
 * OAuth 콜백 처리 파사드.
 * <p>code → access_token → userinfo 두 단계 호출을 하나로 묶는다.
 * <p>기존/신규 회원 분기, JWT/signup token 발급은 사용자 컨트롤러가 담당.
 */
public class OahLoginService {

    private final OahTokenClient tokenClient;
    private final OahUserInfoClient userInfoClient;

    public OahLoginService(OahTokenClient tokenClient, OahUserInfoClient userInfoClient) {
        this.tokenClient = tokenClient;
        this.userInfoClient = userInfoClient;
    }

    public OahUserInfo fetchUserInfo(String provider, String code) {
        return fetchUserInfo(OahProvider.from(provider), code, null);
    }

    public OahUserInfo fetchUserInfo(String provider, String code, String codeVerifier) {
        return fetchUserInfo(OahProvider.from(provider), code, codeVerifier);
    }

    public OahUserInfo fetchUserInfo(OahProvider provider, String code) {
        return fetchUserInfo(provider, code, null);
    }

    /**
     * PKCE 흐름: {@link OahStateService#validateAndConsume} 에서 반환된 code_verifier 를 전달.
     */
    public OahUserInfo fetchUserInfo(OahProvider provider, String code, String codeVerifier) {
        OahTokenResponse token = tokenClient.exchange(provider, code, codeVerifier);
        return userInfoClient.fetch(provider, token.accessToken());
    }
}

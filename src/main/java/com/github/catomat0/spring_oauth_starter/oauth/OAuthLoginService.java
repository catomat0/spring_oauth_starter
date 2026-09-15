package com.github.catomat0.spring_oauth_starter.oauth;

import com.github.catomat0.spring_oauth_starter.oauth.dto.OAuthTokenResponse;

/**
 * OAuth 콜백 처리 파사드.
 * <p>code → access_token → userinfo 두 단계 호출을 하나로 묶는다.
 * <p>기존/신규 회원 분기, JWT/signup token 발급은 사용자 컨트롤러가 담당.
 */
public class OAuthLoginService {

    private final OAuthTokenClient tokenClient;
    private final OAuthUserInfoClient userInfoClient;

    public OAuthLoginService(OAuthTokenClient tokenClient, OAuthUserInfoClient userInfoClient) {
        this.tokenClient = tokenClient;
        this.userInfoClient = userInfoClient;
    }

    public OAuthUserInfo fetchUserInfo(String provider, String code) {
        return fetchUserInfo(OAuthProvider.from(provider), code);
    }

    public OAuthUserInfo fetchUserInfo(OAuthProvider provider, String code) {
        OAuthTokenResponse token = tokenClient.exchange(provider, code);
        return userInfoClient.fetch(provider, token.accessToken());
    }
}

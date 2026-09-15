package com.github.catomat0.spring_oauth_starter.oauth;

import com.github.catomat0.spring_oauth_starter.oauth.dto.GoogleUserInfoResponse;
import com.github.catomat0.spring_oauth_starter.oauth.dto.KakaoUserInfoResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OAuthUserInfoClient {

    private final RestClient restClient;
    private final OAuthProperties properties;

    public OAuthUserInfoClient(RestClient restClient, OAuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OAuthUserInfo fetch(OAuthProvider provider, String accessToken) {
        return switch (provider) {
            case KAKAO -> fetchKakao(accessToken);
            case GOOGLE -> fetchGoogle(accessToken);
        };
    }

    private OAuthUserInfo fetchKakao(String accessToken) {
        OAuthProperties.Provider p = properties.getKakao();
        requireEnabled(p, OAuthProvider.KAKAO);

        KakaoUserInfoResponse res;
        try {
            res = restClient.get()
                    .uri(p.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new OAuthException(OAuthErrorCode.USERINFO_FETCH_FAILED,
                    "Failed to fetch userinfo from kakao (uri=" + p.getUserInfoUri() + "): " + e.getMessage(), e);
        }
        if (res == null || res.id() == null) {
            throw new OAuthException(OAuthErrorCode.USERINFO_EMPTY, "Empty userinfo response from kakao");
        }

        String providerId = String.valueOf(res.id());
        String email = res.kakaoAccount() != null ? res.kakaoAccount().email() : null;
        if (email == null || email.isBlank()) {
            throw new OAuthException(OAuthErrorCode.EMAIL_MISSING,
                    "Kakao userinfo does not contain email. "
                            + "Set '카카오계정(이메일)' as REQUIRED consent in Kakao Developers Console.");
        }

        String nickname = null;
        String profileImage = null;
        if (res.kakaoAccount() != null && res.kakaoAccount().profile() != null) {
            nickname = res.kakaoAccount().profile().nickname();
            profileImage = res.kakaoAccount().profile().profileImageUrl();
        }
        if (nickname == null && res.properties() != null) {
            nickname = res.properties().nickname();
        }
        if (profileImage == null && res.properties() != null) {
            profileImage = res.properties().profileImage();
        }

        return new OAuthUserInfo(OAuthProvider.KAKAO.lower(), providerId, email, nickname, profileImage);
    }

    private OAuthUserInfo fetchGoogle(String accessToken) {
        OAuthProperties.Provider p = properties.getGoogle();
        requireEnabled(p, OAuthProvider.GOOGLE);

        GoogleUserInfoResponse res;
        try {
            res = restClient.get()
                    .uri(p.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new OAuthException(OAuthErrorCode.USERINFO_FETCH_FAILED,
                    "Failed to fetch userinfo from google (uri=" + p.getUserInfoUri() + "): " + e.getMessage(), e);
        }
        if (res == null || res.sub() == null) {
            throw new OAuthException(OAuthErrorCode.USERINFO_EMPTY, "Empty userinfo response from google");
        }
        if (res.email() == null || res.email().isBlank()) {
            throw new OAuthException(OAuthErrorCode.EMAIL_MISSING,
                    "Google userinfo does not contain email. "
                            + "Ensure 'email' scope is included in oauth.google.scope and OAuth consent screen.");
        }

        return new OAuthUserInfo(OAuthProvider.GOOGLE.lower(), res.sub(), res.email(), res.name(), res.picture());
    }

    private static void requireEnabled(OAuthProperties.Provider p, OAuthProvider provider) {
        if (!p.isEnabled()) {
            throw new OAuthException(OAuthErrorCode.PROVIDER_NOT_CONFIGURED,
                    "oauth." + provider.lower() + " is not configured (client-id missing)");
        }
    }
}

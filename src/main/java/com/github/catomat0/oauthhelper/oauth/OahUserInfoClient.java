package com.github.catomat0.oauthhelper.oauth;

import com.github.catomat0.oauthhelper.oauth.dto.OahGoogleUserInfoResponse;
import com.github.catomat0.oauthhelper.oauth.dto.OahKakaoUserInfoResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OahUserInfoClient {

    private final RestClient restClient;
    private final OahProperties properties;

    public OahUserInfoClient(RestClient restClient, OahProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OahUserInfo fetch(OahProvider provider, String accessToken) {
        return switch (provider) {
            case KAKAO -> fetchKakao(accessToken);
            case GOOGLE -> fetchGoogle(accessToken);
        };
    }

    private OahUserInfo fetchKakao(String accessToken) {
        OahProperties.Provider p = properties.getKakao();
        requireEnabled(p, OahProvider.KAKAO);

        OahKakaoUserInfoResponse res;
        try {
            res = restClient.get()
                    .uri(p.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(OahKakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new OahException(OahErrorCode.USERINFO_FETCH_FAILED,
                    "Failed to fetch userinfo from kakao (uri=" + p.getUserInfoUri() + "): " + e.getMessage(), e);
        }
        if (res == null || res.id() == null) {
            throw new OahException(OahErrorCode.USERINFO_EMPTY, "Empty userinfo response from kakao");
        }

        String providerId = String.valueOf(res.id());
        String email = res.kakaoAccount() != null ? res.kakaoAccount().email() : null;
        if (email == null || email.isBlank()) {
            throw new OahException(OahErrorCode.EMAIL_MISSING,
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

        return new OahUserInfo(OahProvider.KAKAO.lower(), providerId, email, nickname, profileImage);
    }

    private OahUserInfo fetchGoogle(String accessToken) {
        OahProperties.Provider p = properties.getGoogle();
        requireEnabled(p, OahProvider.GOOGLE);

        OahGoogleUserInfoResponse res;
        try {
            res = restClient.get()
                    .uri(p.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(OahGoogleUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new OahException(OahErrorCode.USERINFO_FETCH_FAILED,
                    "Failed to fetch userinfo from google (uri=" + p.getUserInfoUri() + "): " + e.getMessage(), e);
        }
        if (res == null || res.sub() == null) {
            throw new OahException(OahErrorCode.USERINFO_EMPTY, "Empty userinfo response from google");
        }
        if (res.email() == null || res.email().isBlank()) {
            throw new OahException(OahErrorCode.EMAIL_MISSING,
                    "Google userinfo does not contain email. "
                            + "Ensure 'email' scope is included in oauth.google.scope and OAuth consent screen.");
        }

        return new OahUserInfo(OahProvider.GOOGLE.lower(), res.sub(), res.email(), res.name(), res.picture());
    }

    private static void requireEnabled(OahProperties.Provider p, OahProvider provider) {
        if (!p.isEnabled()) {
            throw new OahException(OahErrorCode.PROVIDER_NOT_CONFIGURED,
                    "oauth." + provider.lower() + " is not configured (client-id missing)");
        }
    }
}

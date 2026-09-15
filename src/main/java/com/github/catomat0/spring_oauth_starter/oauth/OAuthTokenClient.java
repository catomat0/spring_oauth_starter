package com.github.catomat0.spring_oauth_starter.oauth;

import com.github.catomat0.spring_oauth_starter.oauth.dto.OAuthTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OAuthTokenClient {

    private final RestClient restClient;
    private final OAuthProperties properties;

    public OAuthTokenClient(RestClient restClient, OAuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OAuthTokenResponse exchange(OAuthProvider provider, String code) {
        OAuthProperties.Provider p = resolve(provider);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", p.getClientId());
        form.add("redirect_uri", p.getRedirectUri());
        form.add("code", code);
        if (p.getClientSecret() != null && !p.getClientSecret().isBlank()) {
            form.add("client_secret", p.getClientSecret());
        }

        try {
            OAuthTokenResponse response = restClient.post()
                    .uri(p.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new OAuthException(
                        "Empty token response from " + provider.lower());
            }
            return response;
        } catch (RestClientException e) {
            throw new OAuthException(
                    "Failed to exchange code for token with " + provider.lower(), e);
        }
    }

    private OAuthProperties.Provider resolve(OAuthProvider provider) {
        OAuthProperties.Provider p = switch (provider) {
            case KAKAO -> properties.getKakao();
            case GOOGLE -> properties.getGoogle();
        };
        if (!p.isEnabled()) {
            throw new OAuthException(
                    "oauth." + provider.lower() + " is not configured (client-id missing)");
        }
        return p;
    }
}

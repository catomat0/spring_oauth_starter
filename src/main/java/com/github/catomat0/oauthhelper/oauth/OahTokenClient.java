package com.github.catomat0.oauthhelper.oauth;

import com.github.catomat0.oauthhelper.oauth.dto.OahTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OahTokenClient {

    private final RestClient restClient;
    private final OahProperties properties;

    public OahTokenClient(RestClient restClient, OahProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OahTokenResponse exchange(OahProvider provider, String code) {
        return exchange(provider, code, null);
    }

    /**
     * PKCE 흐름 지원: {@code codeVerifier} 를 함께 전달하면 form 에 {@code code_verifier} 파라미터 첨부.
     */
    public OahTokenResponse exchange(OahProvider provider, String code, String codeVerifier) {
        OahProperties.Provider p = resolve(provider);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", p.getClientId());
        form.add("redirect_uri", p.getRedirectUri());
        form.add("code", code);
        if (p.getClientSecret() != null && !p.getClientSecret().isBlank()) {
            form.add("client_secret", p.getClientSecret());
        }
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            form.add("code_verifier", codeVerifier);
        }

        try {
            OahTokenResponse response = restClient.post()
                    .uri(p.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OahTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new OahException(OahErrorCode.TOKEN_EXCHANGE_EMPTY,
                        "Empty token response from " + provider.lower());
            }
            return response;
        } catch (OahException e) {
            throw e;
        } catch (RestClientException e) {
            throw new OahException(OahErrorCode.TOKEN_EXCHANGE_FAILED,
                    "Failed to exchange code for token with " + provider.lower()
                            + " (uri=" + p.getTokenUri() + "): " + e.getMessage(), e);
        }
    }

    private OahProperties.Provider resolve(OahProvider provider) {
        OahProperties.Provider p = switch (provider) {
            case KAKAO -> properties.getKakao();
            case GOOGLE -> properties.getGoogle();
        };
        if (!p.isEnabled()) {
            throw new OahException(OahErrorCode.PROVIDER_NOT_CONFIGURED,
                    "oauth." + provider.lower() + " is not configured (client-id missing)");
        }
        return p;
    }
}

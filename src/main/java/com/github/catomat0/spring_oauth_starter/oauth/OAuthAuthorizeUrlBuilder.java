package com.github.catomat0.spring_oauth_starter.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth authorize URL 조립기.
 * <p>state / PKCE code_challenge 는 {@link OAuthStateService#issue(String)} 로 생성한 뒤 전달.
 */
public class OAuthAuthorizeUrlBuilder {

    private final OAuthProperties properties;

    public OAuthAuthorizeUrlBuilder(OAuthProperties properties) {
        this.properties = properties;
    }

    public String build(String provider, OAuthAuthorizeParams params) {
        return build(OAuthProvider.from(provider), params);
    }

    public String build(OAuthProvider provider, OAuthAuthorizeParams params) {
        OAuthProperties.Provider p = resolve(provider);

        StringBuilder sb = new StringBuilder(p.getAuthorizeUri());
        sb.append('?').append("response_type=code");
        appendParam(sb, "client_id", p.getClientId());
        appendParam(sb, "redirect_uri", p.getRedirectUri());
        if (p.getScope() != null && !p.getScope().isBlank()) {
            appendParam(sb, "scope", p.getScope());
        }
        appendParam(sb, "state", params.state());
        appendParam(sb, "code_challenge", params.codeChallenge());
        appendParam(sb, "code_challenge_method", params.codeChallengeMethod());
        return sb.toString();
    }

    private OAuthProperties.Provider resolve(OAuthProvider provider) {
        OAuthProperties.Provider p = switch (provider) {
            case KAKAO -> properties.getKakao();
            case GOOGLE -> properties.getGoogle();
        };
        if (!p.isEnabled()) {
            throw new OAuthException(OAuthErrorCode.PROVIDER_NOT_CONFIGURED,
                    "oauth." + provider.lower() + " is not configured (client-id missing)");
        }
        return p;
    }

    private static void appendParam(StringBuilder sb, String key, String value) {
        sb.append('&').append(key).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}

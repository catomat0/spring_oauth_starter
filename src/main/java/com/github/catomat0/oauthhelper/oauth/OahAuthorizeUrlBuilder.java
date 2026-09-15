package com.github.catomat0.oauthhelper.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth authorize URL 조립기.
 * <p>state / PKCE code_challenge 는 {@link OahStateService#issue(String)} 로 생성한 뒤 전달.
 */
public class OahAuthorizeUrlBuilder {

    private final OahProperties properties;

    public OahAuthorizeUrlBuilder(OahProperties properties) {
        this.properties = properties;
    }

    public String build(String provider, OahAuthorizeParams params) {
        return build(OahProvider.from(provider), params);
    }

    public String build(OahProvider provider, OahAuthorizeParams params) {
        OahProperties.Provider p = resolve(provider);

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

    private static void appendParam(StringBuilder sb, String key, String value) {
        sb.append('&').append(key).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}

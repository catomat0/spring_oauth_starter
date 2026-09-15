package com.github.catomat0.spring_oauth_starter.oauth;

import java.util.Locale;

public enum OAuthProvider {
    KAKAO, GOOGLE;

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static OAuthProvider from(String value) {
        if (value == null) {
            throw new OAuthException(OAuthErrorCode.PROVIDER_UNKNOWN, "provider must not be null");
        }
        try {
            return OAuthProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new OAuthException(OAuthErrorCode.PROVIDER_UNKNOWN,
                    "Unknown OAuth provider: '" + value + "' (supported: kakao, google)");
        }
    }
}

package com.github.catomat0.oauthhelper.oauth;

import java.util.Locale;

public enum OahProvider {
    KAKAO, GOOGLE;

    public String lower() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static OahProvider from(String value) {
        if (value == null) {
            throw new OahException(OahErrorCode.PROVIDER_UNKNOWN, "provider must not be null");
        }
        try {
            return OahProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new OahException(OahErrorCode.PROVIDER_UNKNOWN,
                    "Unknown OAuth provider: '" + value + "' (supported: kakao, google)");
        }
    }
}

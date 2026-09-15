package com.github.catomat0.oauthhelper.signuptoken;

import java.util.Collections;
import java.util.Map;

public record OahSignupTokenPayload(
        String provider,
        String providerId,
        String email,
        Map<String, String> extras
) {
    public OahSignupTokenPayload {
        extras = extras == null ? Collections.emptyMap() : Map.copyOf(extras);
    }

    public String extra(String key) {
        return extras.get(key);
    }
}

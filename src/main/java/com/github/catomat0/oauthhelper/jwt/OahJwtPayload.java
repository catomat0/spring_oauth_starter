package com.github.catomat0.oauthhelper.jwt;

import java.util.Map;

public record OahJwtPayload(
        String userId,
        String role,
        String type,
        Map<String, String> extras
) {
    public String extra(String key) {
        return extras == null ? null : extras.get(key);
    }
}

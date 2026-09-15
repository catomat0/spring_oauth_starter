package com.github.catomat0.spring_oauth_starter.jwt;

import java.util.Map;

public record JwtPayload(
        String userId,
        String role,
        String type,
        Map<String, String> extras
) {
    public String extra(String key) {
        return extras == null ? null : extras.get(key);
    }
}

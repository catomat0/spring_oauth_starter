package com.github.catomat0.oauthhelper.signuptoken;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class OahSignupTokenBuilder {

    private final OahSignupTokenProvider provider;
    private final String oauthProvider;
    private final String providerId;
    private final String email;
    private final Map<String, String> claims = new LinkedHashMap<>();
    private Long ttlMillis;

    OahSignupTokenBuilder(OahSignupTokenProvider provider, String oauthProvider, String providerId, String email) {
        this.provider = provider;
        this.oauthProvider = oauthProvider;
        this.providerId = providerId;
        this.email = email;
    }

    public OahSignupTokenBuilder claim(String key, String value) {
        if (key == null || value == null || value.isBlank()) return this;
        claims.put(key, value);
        return this;
    }

    public OahSignupTokenBuilder claims(Map<String, String> extras) {
        if (extras == null) return this;
        for (Map.Entry<String, String> e : extras.entrySet()) {
            claim(e.getKey(), e.getValue());
        }
        return this;
    }

    public OahSignupTokenBuilder ttl(long millis) {
        if (millis <= 0) throw new IllegalArgumentException("ttl must be positive");
        this.ttlMillis = millis;
        return this;
    }

    public OahSignupTokenBuilder ttl(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttlMillis = duration.toMillis();
        return this;
    }

    public String build() {
        return provider.doGenerate(oauthProvider, providerId, email, claims, ttlMillis);
    }
}

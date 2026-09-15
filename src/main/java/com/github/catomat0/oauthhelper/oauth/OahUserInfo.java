package com.github.catomat0.oauthhelper.oauth;

public record OahUserInfo(
        String provider,
        String providerId,
        String email,
        String nickname,
        String profileImage
) {}

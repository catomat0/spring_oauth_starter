package com.github.catomat0.spring_oauth_starter.oauth;

public record OAuthUserInfo(
        String provider,
        String providerId,
        String email,
        String nickname,
        String profileImage
) {}

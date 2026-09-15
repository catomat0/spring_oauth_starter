package com.github.catomat0.oauthhelper.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OahGoogleUserInfoResponse(
        String sub,
        String email,
        String name,
        String picture
) {}

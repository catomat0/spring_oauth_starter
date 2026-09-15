package com.github.catomat0.spring_oauth_starter.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponse(
        String sub,
        String email,
        String name,
        String picture
) {}

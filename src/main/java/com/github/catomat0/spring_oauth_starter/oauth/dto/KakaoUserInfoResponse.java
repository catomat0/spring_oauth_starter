package com.github.catomat0.spring_oauth_starter.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount,
        Properties properties
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Profile(
                String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            String nickname,
            @JsonProperty("profile_image") String profileImage
    ) {}
}

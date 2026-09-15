package com.github.catomat0.spring_oauth_starter.oauth;

/**
 * Kakao/Google 표준 OAuth2 엔드포인트 및 scope 디폴트.
 * <p>사용자가 application.yml 에서 명시하지 않으면 이 값이 적용된다.
 */
final class OAuthDefaults {

    private OAuthDefaults() {}

    static final class Kakao {
        static final String AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
        static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
        static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
        static final String SCOPE = "account_email profile_nickname profile_image";
    }

    static final class Google {
        static final String AUTHORIZE_URI = "https://accounts.google.com/o/oauth2/v2/auth";
        static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
        static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
        static final String SCOPE = "openid email profile";
    }
}

package com.github.catomat0.spring_oauth_starter.oauth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Provider kakao = new Provider();
    private Provider google = new Provider();

    @PostConstruct
    void validate() {
        kakao.validate("kakao");
        google.validate("google");
    }

    public Provider getKakao() { return kakao; }
    public void setKakao(Provider kakao) { this.kakao = kakao; }

    public Provider getGoogle() { return google; }
    public void setGoogle(Provider google) { this.google = google; }

    @Override
    public String toString() {
        return "OAuthProperties{kakao=" + kakao + ", google=" + google + '}';
    }

    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizeUri;
        private String tokenUri;
        private String userInfoUri;
        private String scope;

        /**
         * client-id 가 설정된 provider 만 opt-in 으로 간주하고
         * 나머지 필수 필드가 채워졌는지 검증한다.
         */
        void validate(String providerName) {
            if (clientId == null || clientId.isBlank()) return;
            requireField(providerName, "client-secret", clientSecret);
            requireField(providerName, "redirect-uri", redirectUri);
            requireField(providerName, "token-uri", tokenUri);
            requireField(providerName, "user-info-uri", userInfoUri);
        }

        private static void requireField(String provider, String field, String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "oauth." + provider + "." + field + " must be configured when "
                                + "oauth." + provider + ".client-id is set");
            }
        }

        public boolean isEnabled() {
            return clientId != null && !clientId.isBlank();
        }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

        public String getAuthorizeUri() { return authorizeUri; }
        public void setAuthorizeUri(String authorizeUri) { this.authorizeUri = authorizeUri; }

        public String getTokenUri() { return tokenUri; }
        public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }

        public String getUserInfoUri() { return userInfoUri; }
        public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        @Override
        public String toString() {
            return "Provider{clientId=" + (clientId == null || clientId.isEmpty() ? "[UNSET]" : "[SET]")
                    + ", clientSecret=" + (clientSecret == null || clientSecret.isEmpty() ? "[UNSET]" : "[MASKED]")
                    + ", redirectUri='" + redirectUri + '\''
                    + ", authorizeUri='" + authorizeUri + '\''
                    + ", tokenUri='" + tokenUri + '\''
                    + ", userInfoUri='" + userInfoUri + '\''
                    + ", scope='" + scope + "'}";
        }
    }
}

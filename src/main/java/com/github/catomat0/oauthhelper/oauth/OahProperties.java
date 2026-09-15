package com.github.catomat0.oauthhelper.oauth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public class OahProperties {

    private Provider kakao = new Provider();
    private Provider google = new Provider();
    private String stateRedisKeyPrefix = "OS:";
    private long stateTtlSeconds = 300L;
    private RestClient restClient = new RestClient();

    @PostConstruct
    void validate() {
        applyKakaoDefaults();
        applyGoogleDefaults();
        kakao.validate("kakao");
        google.validate("google");
        if (stateTtlSeconds <= 0) {
            throw new OahException(OahErrorCode.STATE_TTL_INVALID,
                    "oauth.state-ttl-seconds must be positive; got " + stateTtlSeconds);
        }
    }

    private void applyKakaoDefaults() {
        if (kakao.getAuthorizeUri() == null) kakao.setAuthorizeUri(OAuthDefaults.Kakao.AUTHORIZE_URI);
        if (kakao.getTokenUri() == null) kakao.setTokenUri(OAuthDefaults.Kakao.TOKEN_URI);
        if (kakao.getUserInfoUri() == null) kakao.setUserInfoUri(OAuthDefaults.Kakao.USER_INFO_URI);
        if (kakao.getScope() == null) kakao.setScope(OAuthDefaults.Kakao.SCOPE);
    }

    private void applyGoogleDefaults() {
        if (google.getAuthorizeUri() == null) google.setAuthorizeUri(OAuthDefaults.Google.AUTHORIZE_URI);
        if (google.getTokenUri() == null) google.setTokenUri(OAuthDefaults.Google.TOKEN_URI);
        if (google.getUserInfoUri() == null) google.setUserInfoUri(OAuthDefaults.Google.USER_INFO_URI);
        if (google.getScope() == null) google.setScope(OAuthDefaults.Google.SCOPE);
    }

    public Provider getKakao() { return kakao; }
    public void setKakao(Provider kakao) { this.kakao = kakao; }

    public Provider getGoogle() { return google; }
    public void setGoogle(Provider google) { this.google = google; }

    public String getStateRedisKeyPrefix() { return stateRedisKeyPrefix; }
    public void setStateRedisKeyPrefix(String stateRedisKeyPrefix) {
        this.stateRedisKeyPrefix = stateRedisKeyPrefix;
    }

    public long getStateTtlSeconds() { return stateTtlSeconds; }
    public void setStateTtlSeconds(long stateTtlSeconds) { this.stateTtlSeconds = stateTtlSeconds; }

    public RestClient getRestClient() { return restClient; }
    public void setRestClient(RestClient restClient) { this.restClient = restClient; }

    @Override
    public String toString() {
        return "OahProperties{kakao=" + kakao + ", google=" + google
                + ", stateRedisKeyPrefix='" + stateRedisKeyPrefix + '\''
                + ", stateTtlSeconds=" + stateTtlSeconds
                + ", restClient=" + restClient + '}';
    }

    public static class RestClient {
        private long connectTimeoutMs = 3000L;
        private long readTimeoutMs = 5000L;

        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

        public long getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

        @Override
        public String toString() {
            return "RestClient{connectTimeoutMs=" + connectTimeoutMs
                    + ", readTimeoutMs=" + readTimeoutMs + '}';
        }
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
         * <p>token-uri 와 user-info-uri 는 https:// 시작 강제 (client_secret 평문 노출 방지).
         */
        void validate(String providerName) {
            if (clientId == null || clientId.isBlank()) return;
            requireField(providerName, "client-secret", clientSecret);
            requireField(providerName, "redirect-uri", redirectUri);
            requireField(providerName, "token-uri", tokenUri);
            requireField(providerName, "user-info-uri", userInfoUri);
            requireHttps(providerName, "token-uri", tokenUri);
            requireHttps(providerName, "user-info-uri", userInfoUri);
        }

        private static void requireField(String provider, String field, String value) {
            if (value == null || value.isBlank()) {
                throw new OahException(OahErrorCode.PROVIDER_INCOMPLETE,
                        "oauth." + provider + "." + field + " must be configured when "
                                + "oauth." + provider + ".client-id is set");
            }
        }

        private static void requireHttps(String provider, String field, String uri) {
            if (!uri.startsWith("https://")) {
                throw new OahException(OahErrorCode.INSECURE_URI,
                        "oauth." + provider + "." + field + " must use https:// "
                                + "(client_secret leak risk over plaintext http). got: " + uri);
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

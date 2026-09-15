package com.github.catomat0.spring_oauth_starter.oauth.autoconfigure;

import com.github.catomat0.spring_oauth_starter.oauth.OAuthLoginService;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthProperties;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthTokenClient;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthUserInfoClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthAutoConfiguration {

    @Bean("oauthRestClient")
    @ConditionalOnMissingBean(name = "oauthRestClient")
    public RestClient oauthRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuthTokenClient oauthTokenClient(RestClient oauthRestClient, OAuthProperties properties) {
        return new OAuthTokenClient(oauthRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuthUserInfoClient oauthUserInfoClient(RestClient oauthRestClient, OAuthProperties properties) {
        return new OAuthUserInfoClient(oauthRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuthLoginService oauthLoginService(OAuthTokenClient tokenClient,
                                               OAuthUserInfoClient userInfoClient) {
        return new OAuthLoginService(tokenClient, userInfoClient);
    }
}

package com.github.catomat0.spring_oauth_starter.oauth.autoconfigure;

import com.github.catomat0.spring_oauth_starter.oauth.OAuthAuthorizeUrlBuilder;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthLoginService;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthProperties;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthStateService;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthTokenClient;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthUserInfoClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthAutoConfiguration {

    @Bean("oauthRestClient")
    @ConditionalOnMissingBean(name = "oauthRestClient")
    public RestClient oauthRestClient(OAuthProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getRestClient().getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getRestClient().getReadTimeoutMs()));
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

    @Bean
    @ConditionalOnMissingBean
    public OAuthAuthorizeUrlBuilder oauthAuthorizeUrlBuilder(OAuthProperties properties) {
        return new OAuthAuthorizeUrlBuilder(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    static class RedisConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OAuthStateService oauthStateService(
                RedisTemplate<String, String> redisTemplate,
                OAuthProperties properties
        ) {
            return new OAuthStateService(redisTemplate, properties);
        }
    }
}

package com.github.catomat0.oauthhelper.oauth.autoconfigure;

import com.github.catomat0.oauthhelper.oauth.OahAuthorizeUrlBuilder;
import com.github.catomat0.oauthhelper.oauth.OahLoginService;
import com.github.catomat0.oauthhelper.oauth.OahOAuth;
import com.github.catomat0.oauthhelper.oauth.OahProperties;
import com.github.catomat0.oauthhelper.oauth.OahStateService;
import com.github.catomat0.oauthhelper.oauth.OahTokenClient;
import com.github.catomat0.oauthhelper.oauth.OahUserInfoClient;
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
@EnableConfigurationProperties(OahProperties.class)
public class OAuthAutoConfiguration {

    @Bean("oauthRestClient")
    @ConditionalOnMissingBean(name = "oauthRestClient")
    public RestClient oauthRestClient(OahProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getRestClient().getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getRestClient().getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OahTokenClient oauthTokenClient(RestClient oauthRestClient, OahProperties properties) {
        return new OahTokenClient(oauthRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OahUserInfoClient oauthUserInfoClient(RestClient oauthRestClient, OahProperties properties) {
        return new OahUserInfoClient(oauthRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OahLoginService oauthLoginService(OahTokenClient tokenClient,
                                               OahUserInfoClient userInfoClient) {
        return new OahLoginService(tokenClient, userInfoClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public OahAuthorizeUrlBuilder oauthAuthorizeUrlBuilder(OahProperties properties) {
        return new OahAuthorizeUrlBuilder(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    static class RedisConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OahStateService oauthStateService(
                RedisTemplate<String, String> redisTemplate,
                OahProperties properties
        ) {
            return new OahStateService(redisTemplate, properties);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean({OahStateService.class, OahAuthorizeUrlBuilder.class, OahLoginService.class})
        OahOAuth oahOAuth(OahStateService state,
                          OahAuthorizeUrlBuilder authorize,
                          OahLoginService login) {
            return new OahOAuth(state, authorize, login);
        }
    }
}

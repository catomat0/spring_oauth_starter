package com.github.catomat0.oauthhelper.jwt.autoconfigure;

import com.github.catomat0.oauthhelper.jwt.OahJwt;
import com.github.catomat0.oauthhelper.jwt.OahJwtAuthenticationFilter;
import com.github.catomat0.oauthhelper.jwt.OahJwtProperties;
import com.github.catomat0.oauthhelper.jwt.OahJwtProvider;
import com.github.catomat0.oauthhelper.jwt.OahRefreshTokenCookieWriter;
import com.github.catomat0.oauthhelper.jwt.OahRefreshTokenService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(Jwts.class)
@ConditionalOnProperty(prefix = "jwt", name = "secret-key")
@EnableConfigurationProperties(OahJwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OahJwtProvider jwtProvider(OahJwtProperties properties) {
        return new OahJwtProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HttpServletResponse.class)
    public OahRefreshTokenCookieWriter refreshTokenCookieWriter(OahJwtProperties properties) {
        return new OahRefreshTokenCookieWriter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SecurityContextHolder.class)
    static class SecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OahJwtAuthenticationFilter jwtAuthenticationFilter(OahJwtProvider jwtProvider) {
            return new OahJwtAuthenticationFilter(jwtProvider);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    static class RedisConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OahRefreshTokenService refreshTokenService(
                RedisTemplate<String, String> redisTemplate,
                OahJwtProperties properties
        ) {
            return new OahRefreshTokenService(redisTemplate, properties);
        }

        @Bean
        @ConditionalOnMissingBean
        OahJwt oahJwt(OahJwtProvider provider,
                      OahRefreshTokenService refresh,
                      OahRefreshTokenCookieWriter cookie) {
            return new OahJwt(provider, refresh, cookie);
        }
    }

    /**
     * jwt.secret-key 는 세팅됐는데 RedisTemplate 빈이 없으면 refresh token 기능이 조용히 비활성화된다.
     * 이 경우 사용자가 뒤늦게 알아채기 쉬우므로 startup 시점에 명시적으로 경고를 남긴다.
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public ApplicationListener<ApplicationReadyEvent> refreshTokenRedisMissingWarner() {
        Logger log = LoggerFactory.getLogger("com.github.catomat0.oauthhelper.jwt");
        return event -> log.warn(
                "[oauth-helper] jwt.secret-key is configured but no RedisTemplate<String,String> bean was found. "
                        + "OahRefreshTokenService will NOT be registered — refresh token rotation is disabled. "
                        + "To enable it, add 'spring-boot-starter-data-redis' dependency and a Redis 6.2+ server. "
                        + "Ignore this warning if you intentionally use access-token-only auth.");
    }
}

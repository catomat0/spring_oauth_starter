package com.github.catomat0.oauthhelper.signuptoken.autoconfigure;

import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenCookieWriter;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenProperties;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenProvider;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(Jwts.class)
@EnableConfigurationProperties(OahSignupTokenProperties.class)
public class SignupTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OahSignupTokenProvider signupTokenProvider(OahSignupTokenProperties properties) {
        return new OahSignupTokenProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HttpServletResponse.class)
    public OahSignupTokenCookieWriter signupTokenCookieWriter(OahSignupTokenProperties properties) {
        return new OahSignupTokenCookieWriter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    static class RedisConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OahSignupTokenService signupTokenService(
                RedisTemplate<String, String> redisTemplate,
                OahSignupTokenProperties properties
        ) {
            return new OahSignupTokenService(redisTemplate, properties);
        }
    }
}

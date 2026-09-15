package com.github.catomat0.spring_oauth_starter.autoconfigure;

import com.github.catomat0.spring_oauth_starter.jwt.JwtProperties;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthProperties;
import com.github.catomat0.spring_oauth_starter.signuptoken.SignupTokenProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 라이브러리 전역 startup 검증기 등록.
 */
@AutoConfiguration
public class SpringOAuthStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PrefixCollisionValidator prefixCollisionValidator(
            ObjectProvider<SignupTokenProperties> signupTokenPropsProvider,
            ObjectProvider<JwtProperties> jwtPropsProvider,
            ObjectProvider<OAuthProperties> oauthPropsProvider
    ) {
        return new PrefixCollisionValidator(signupTokenPropsProvider, jwtPropsProvider, oauthPropsProvider);
    }
}

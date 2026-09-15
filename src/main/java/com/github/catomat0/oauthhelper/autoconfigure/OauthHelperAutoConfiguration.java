package com.github.catomat0.oauthhelper.autoconfigure;

import com.github.catomat0.oauthhelper.jwt.OahJwtProperties;
import com.github.catomat0.oauthhelper.oauth.OahProperties;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 라이브러리 전역 startup 검증기 등록.
 */
@AutoConfiguration
public class OauthHelperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PrefixCollisionValidator prefixCollisionValidator(
            ObjectProvider<OahSignupTokenProperties> signupTokenPropsProvider,
            ObjectProvider<OahJwtProperties> jwtPropsProvider,
            ObjectProvider<OahProperties> oauthPropsProvider
    ) {
        return new PrefixCollisionValidator(signupTokenPropsProvider, jwtPropsProvider, oauthPropsProvider);
    }
}

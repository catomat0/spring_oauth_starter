package com.github.catomat0.spring_oauth_starter.autoconfigure;

import com.github.catomat0.spring_oauth_starter.jwt.JwtProperties;
import com.github.catomat0.spring_oauth_starter.oauth.OAuthProperties;
import com.github.catomat0.spring_oauth_starter.signuptoken.SignupTokenErrorCode;
import com.github.catomat0.spring_oauth_starter.signuptoken.SignupTokenException;
import com.github.catomat0.spring_oauth_starter.signuptoken.SignupTokenProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 세 종류 토큰이 사용하는 Redis prefix 가 서로 충돌하지 않는지 startup 시 검증.
 * <p>같은 prefix 를 쓰면 서로 다른 토큰의 키가 우연히 겹칠 수 있으므로 fail-fast.
 */
public class PrefixCollisionValidator {

    private final ObjectProvider<SignupTokenProperties> signupTokenPropsProvider;
    private final ObjectProvider<JwtProperties> jwtPropsProvider;
    private final ObjectProvider<OAuthProperties> oauthPropsProvider;

    public PrefixCollisionValidator(
            ObjectProvider<SignupTokenProperties> signupTokenPropsProvider,
            ObjectProvider<JwtProperties> jwtPropsProvider,
            ObjectProvider<OAuthProperties> oauthPropsProvider
    ) {
        this.signupTokenPropsProvider = signupTokenPropsProvider;
        this.jwtPropsProvider = jwtPropsProvider;
        this.oauthPropsProvider = oauthPropsProvider;
    }

    @PostConstruct
    void validate() {
        String stPrefix = signupTokenPropsProvider.getIfAvailable() != null
                ? signupTokenPropsProvider.getIfAvailable().getRedisKeyPrefix() : null;
        String rtPrefix = jwtPropsProvider.getIfAvailable() != null
                ? jwtPropsProvider.getIfAvailable().getRedisKeyPrefix() : null;
        String osPrefix = oauthPropsProvider.getIfAvailable() != null
                ? oauthPropsProvider.getIfAvailable().getStateRedisKeyPrefix() : null;

        checkCollision("signup-token.redis-key-prefix", stPrefix,
                "jwt.redis-key-prefix", rtPrefix);
        checkCollision("signup-token.redis-key-prefix", stPrefix,
                "oauth.state-redis-key-prefix", osPrefix);
        checkCollision("jwt.redis-key-prefix", rtPrefix,
                "oauth.state-redis-key-prefix", osPrefix);
    }

    private static void checkCollision(String nameA, String a, String nameB, String b) {
        if (a == null || b == null) return;
        if (a.equals(b)) {
            throw new SignupTokenException(SignupTokenErrorCode.REDIS_PREFIX_COLLISION,
                    nameA + " and " + nameB + " use the same value '" + a
                            + "'. Redis key collision — different tokens would share the same key space. "
                            + "Use distinct prefixes (defaults: ST:, RT:, OS:).");
        }
    }
}

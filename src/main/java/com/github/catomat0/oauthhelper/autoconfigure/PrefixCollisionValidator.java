package com.github.catomat0.oauthhelper.autoconfigure;

import com.github.catomat0.oauthhelper.jwt.OahJwtProperties;
import com.github.catomat0.oauthhelper.oauth.OahProperties;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenErrorCode;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenException;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 세 종류 토큰이 사용하는 Redis prefix 가 서로 충돌하지 않는지 startup 시 검증.
 * <p>같은 prefix 를 쓰면 서로 다른 토큰의 키가 우연히 겹칠 수 있으므로 fail-fast.
 */
public class PrefixCollisionValidator {

    private final ObjectProvider<OahSignupTokenProperties> signupTokenPropsProvider;
    private final ObjectProvider<OahJwtProperties> jwtPropsProvider;
    private final ObjectProvider<OahProperties> oauthPropsProvider;

    public PrefixCollisionValidator(
            ObjectProvider<OahSignupTokenProperties> signupTokenPropsProvider,
            ObjectProvider<OahJwtProperties> jwtPropsProvider,
            ObjectProvider<OahProperties> oauthPropsProvider
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
            throw new OahSignupTokenException(OahSignupTokenErrorCode.REDIS_PREFIX_COLLISION,
                    nameA + " and " + nameB + " use the same value '" + a
                            + "'. Redis key collision — different tokens would share the same key space. "
                            + "Use distinct prefixes (defaults: ST:, RT:, OS:).");
        }
    }
}

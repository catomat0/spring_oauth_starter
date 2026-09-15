package com.github.catomat0.oauthhelper.signuptoken;

/**
 * Signup token 관련 서비스를 묶은 파사드.
 * <pre>{@code
 * public AuthController(OahSignup signup) { this.signup = signup; }
 *
 * String token = signup.provider().builder(provider, providerId, email).build();
 * signup.service().save(provider, providerId, token);
 * signup.cookie().write(response, token);
 * }</pre>
 */
public record OahSignup(
        OahSignupTokenProvider provider,
        OahSignupTokenService service,
        OahSignupTokenCookieWriter cookie
) {}

package com.github.catomat0.spring_oauth_starter.oauth;

public class OAuthException extends RuntimeException {

    private final OAuthErrorCode code;

    public OAuthException(OAuthErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public OAuthException(OAuthErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public OAuthErrorCode code() {
        return code;
    }
}

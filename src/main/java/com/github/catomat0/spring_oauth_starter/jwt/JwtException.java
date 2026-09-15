package com.github.catomat0.spring_oauth_starter.jwt;

public class JwtException extends RuntimeException {

    private final JwtErrorCode code;

    public JwtException(JwtErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public JwtException(JwtErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public JwtErrorCode code() {
        return code;
    }
}

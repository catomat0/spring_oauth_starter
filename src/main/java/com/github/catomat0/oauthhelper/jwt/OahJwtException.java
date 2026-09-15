package com.github.catomat0.oauthhelper.jwt;

public class OahJwtException extends RuntimeException {

    private final OahJwtErrorCode code;

    public OahJwtException(OahJwtErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public OahJwtException(OahJwtErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public OahJwtErrorCode code() {
        return code;
    }
}

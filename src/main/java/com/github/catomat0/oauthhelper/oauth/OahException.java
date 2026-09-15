package com.github.catomat0.oauthhelper.oauth;

public class OahException extends RuntimeException {

    private final OahErrorCode code;

    public OahException(OahErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public OahException(OahErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public OahErrorCode code() {
        return code;
    }
}

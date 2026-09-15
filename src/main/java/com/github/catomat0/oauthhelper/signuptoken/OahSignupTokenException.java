package com.github.catomat0.oauthhelper.signuptoken;

public class OahSignupTokenException extends RuntimeException {

    private final OahSignupTokenErrorCode code;

    public OahSignupTokenException(OahSignupTokenErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public OahSignupTokenException(OahSignupTokenErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public OahSignupTokenErrorCode code() {
        return code;
    }
}

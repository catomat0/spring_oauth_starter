package com.github.catomat0.spring_oauth_starter.signuptoken;

public class SignupTokenException extends RuntimeException {

    private final SignupTokenErrorCode code;

    public SignupTokenException(SignupTokenErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public SignupTokenException(SignupTokenErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SignupTokenErrorCode code() {
        return code;
    }
}

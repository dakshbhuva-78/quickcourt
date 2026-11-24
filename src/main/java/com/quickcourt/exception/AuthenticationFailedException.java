package com.quickcourt.exception;

/** Wrong credentials or missing/expired token. -> HTTP 401 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}

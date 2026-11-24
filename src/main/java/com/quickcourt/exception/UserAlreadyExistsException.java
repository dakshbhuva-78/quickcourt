package com.quickcourt.exception;

/** Thrown when registering with an email that is already used. -> HTTP 409 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("An account with email " + email + " already exists");
    }
}

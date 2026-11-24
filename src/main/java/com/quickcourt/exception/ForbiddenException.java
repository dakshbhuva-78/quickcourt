package com.quickcourt.exception;

/** Logged in, but not allowed to do this (wrong role / not your data). -> HTTP 403 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

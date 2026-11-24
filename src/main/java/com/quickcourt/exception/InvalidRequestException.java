package com.quickcourt.exception;

/** Thrown when request data is logically wrong (e.g. closing time before opening time). -> HTTP 400 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}

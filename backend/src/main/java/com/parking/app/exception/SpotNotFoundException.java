package com.parking.app.exception;

/**
 * Thrown when a requested spot id does not exist in the lot.
 */
public class SpotNotFoundException extends RuntimeException {
    public SpotNotFoundException(String message) {
        super(message);
    }
}

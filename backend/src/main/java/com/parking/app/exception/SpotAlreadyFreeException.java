package com.parking.app.exception;

/**
 * Thrown when attempting to clear a spot that is already free.
 */
public class SpotAlreadyFreeException extends RuntimeException {
    public SpotAlreadyFreeException(String message) {
        super(message);
    }
}

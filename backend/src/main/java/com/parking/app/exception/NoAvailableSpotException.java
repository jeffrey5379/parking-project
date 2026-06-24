package com.parking.app.exception;

/**
 * Thrown when no compatible spot is available for the requested vehicle type.
 */
public class NoAvailableSpotException extends RuntimeException {
    public NoAvailableSpotException(String message) {
        super(message);
    }
}

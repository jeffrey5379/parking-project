package com.parking.app.model;

/**
 * Type of vehicle requesting a parking spot.
 * Each vehicle type maps to the smallest spot size it can legally use.
 */
public enum VehicleType {
    MOTORCYCLE(SpotSize.SMALL),
    CAR(SpotSize.MEDIUM),
    TRUCK(SpotSize.LARGE);

    private final SpotSize requiredSize;

    VehicleType(SpotSize requiredSize) {
        this.requiredSize = requiredSize;
    }

    public SpotSize getRequiredSize() {
        return requiredSize;
    }
}

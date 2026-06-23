package com.parking.app.model;

/**
 * Physical size category of a parking spot.
 * Ordinal order matters: SMALL < MEDIUM < LARGE is used to determine
 * whether a spot can accommodate a given vehicle (a larger spot can
 * always host a smaller vehicle, but not vice versa).
 */
public enum SpotSize {
    SMALL,
    MEDIUM,
    LARGE;

    /**
     * Returns true if a spot of this size can physically fit a vehicle
     * that requires the given size (a LARGE spot fits SMALL/MEDIUM/LARGE vehicles,
     * a SMALL spot only fits SMALL vehicles).
     */
    public boolean canFit(SpotSize requiredSize) {
        return this.ordinal() >= requiredSize.ordinal();
    }
}

package com.parking.app.dto;

import com.parking.app.model.ParkingSpot;
import com.parking.app.model.SpotSize;
import com.parking.app.model.SpotStatus;
import com.parking.app.model.VehicleType;

import java.time.Instant;

/**
 * Read-only representation of a parking spot returned to API clients.
 */
public record ParkingSpotResponse(
        String id,
        SpotSize size,
        SpotStatus status,
        String licensePlate,
        VehicleType occupiedBy,
        Instant occupiedSince
) {
    public static ParkingSpotResponse from(ParkingSpot spot) {
        return new ParkingSpotResponse(
                spot.getId(),
                spot.getSize(),
                spot.getStatus(),
                spot.getLicensePlate(),
                spot.getOccupiedBy(),
                spot.getOccupiedSince()
        );
    }
}

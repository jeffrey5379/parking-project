package com.parking.app.dto;

import com.parking.app.model.VehicleType;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for placing a vehicle into the parking lot.
 * licensePlate is optional; if omitted, the backend generates a placeholder.
 */
public record ParkVehicleRequest(
        @NotNull(message = "vehicleType is required")
        VehicleType vehicleType,

        String licensePlate
) {
}

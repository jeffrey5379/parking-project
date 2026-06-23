package com.parking.app.dto;

import jakarta.validation.constraints.Min;

public record ConcurrentParkRequest(
        @Min(0) int motorcycleCount,
        @Min(0) int carCount,
        @Min(0) int truckCount
) {}

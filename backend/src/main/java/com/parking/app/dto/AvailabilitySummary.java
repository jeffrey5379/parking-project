package com.parking.app.dto;

import com.parking.app.model.SpotSize;

import java.util.Map;

/**
 * Aggregate availability counts per spot size, used by the frontend
 * to render summary counters (e.g. "7 / 9 Small spots free").
 */
public record AvailabilitySummary(
        Map<SpotSize, SizeAvailability> bySize,
        int totalSpots,
        int totalFree,
        int totalOccupied
) {
    public record SizeAvailability(int total, int free, int occupied) {
    }
}

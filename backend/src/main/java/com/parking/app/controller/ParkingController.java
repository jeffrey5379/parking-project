package com.parking.app.controller;

import com.parking.app.dto.AvailabilitySummary;
import com.parking.app.dto.ConcurrentParkRequest;
import com.parking.app.dto.ConcurrentParkResult;
import com.parking.app.dto.ParkVehicleRequest;
import com.parking.app.dto.ParkingSpotResponse;
import com.parking.app.model.ParkingSpot;
import com.parking.app.service.ParkingEventPublisher;
import com.parking.app.service.ParkingLotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST API for the parking lot demo.
 *
 * Endpoints:
 *   GET    /api/parking/spots          -> full grid state
 *   GET    /api/parking/availability   -> aggregate counts by size
 *   POST   /api/parking/park           -> park a vehicle (auto spot selection)
 *   POST   /api/parking/spots/{id}/clear -> free a specific spot
 *   POST   /api/parking/reset          -> clear the entire lot
 */
@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingLotService parkingLotService;
    private final ParkingEventPublisher eventPublisher;

    public ParkingController(ParkingLotService parkingLotService,
                              ParkingEventPublisher eventPublisher) {
        this.parkingLotService = parkingLotService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return eventPublisher.subscribe();
    }

    @GetMapping("/spots")
    public List<ParkingSpotResponse> getAllSpots() {
        return parkingLotService.getAllSpots().stream()
                .map(ParkingSpotResponse::from)
                .toList();
    }

    @GetMapping("/availability")
    public AvailabilitySummary getAvailability() {
        return parkingLotService.getAvailabilitySummary();
    }

    @PostMapping("/park")
    public ResponseEntity<ParkingSpotResponse> parkVehicle(@Valid @RequestBody ParkVehicleRequest request) {
        ParkingSpot spot = parkingLotService.parkVehicle(request.vehicleType(), request.licensePlate());
        return ResponseEntity.status(HttpStatus.CREATED).body(ParkingSpotResponse.from(spot));
    }

    @PostMapping("/spots/{spotId}/clear")
    public ParkingSpotResponse clearSpot(@PathVariable String spotId) {
        ParkingSpot spot = parkingLotService.clearSpot(spotId);
        return ParkingSpotResponse.from(spot);
    }

    @PostMapping("/park-concurrent")
    public ConcurrentParkResult parkConcurrently(@Valid @RequestBody ConcurrentParkRequest request) {
        return parkingLotService.parkConcurrently(request);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetLot() {
        parkingLotService.resetLot();
        return ResponseEntity.noContent().build();
    }
}

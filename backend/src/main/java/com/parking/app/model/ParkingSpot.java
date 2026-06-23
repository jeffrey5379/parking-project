package com.parking.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "parking_spots")
public class ParkingSpot {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotSize size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status;

    private String licensePlate;

    @Enumerated(EnumType.STRING)
    private VehicleType occupiedBy;

    private Instant occupiedSince;

    @Version
    private Long version;

    protected ParkingSpot() {}

    public ParkingSpot(String id, SpotSize size) {
        this.id = id;
        this.size = size;
        this.status = SpotStatus.FREE;
    }

    public String getId() { return id; }
    public SpotSize getSize() { return size; }
    public SpotStatus getStatus() { return status; }
    public String getLicensePlate() { return licensePlate; }
    public VehicleType getOccupiedBy() { return occupiedBy; }
    public Instant getOccupiedSince() { return occupiedSince; }
    public boolean isFree() { return status == SpotStatus.FREE; }

    public void occupy(VehicleType vehicleType, String licensePlate) {
        this.status = SpotStatus.OCCUPIED;
        this.occupiedBy = vehicleType;
        this.licensePlate = licensePlate;
        this.occupiedSince = Instant.now();
    }

    public void startRelocation(VehicleType vehicleType, String licensePlate) {
        this.status = SpotStatus.RELOCATING;
        this.occupiedBy = vehicleType;
        this.licensePlate = licensePlate;
        this.occupiedSince = Instant.now();
    }

    public void finishRelocation() {
        this.status = SpotStatus.OCCUPIED;
    }

    public void clear() {
        this.status = SpotStatus.FREE;
        this.occupiedBy = null;
        this.licensePlate = null;
        this.occupiedSince = null;
    }
}

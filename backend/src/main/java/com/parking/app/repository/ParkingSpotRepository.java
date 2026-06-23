package com.parking.app.repository;

import com.parking.app.model.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, String> {
}

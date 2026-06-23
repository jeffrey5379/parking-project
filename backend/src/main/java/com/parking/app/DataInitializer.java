package com.parking.app;

import com.parking.app.model.ParkingSpot;
import com.parking.app.model.SpotSize;
import com.parking.app.repository.ParkingSpotRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final ParkingSpotRepository repository;

    public DataInitializer(ParkingSpotRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        List<ParkingSpot> spots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) spots.add(new ParkingSpot("S-%02d".formatted(i), SpotSize.SMALL));
        for (int i = 1; i <= 9; i++) spots.add(new ParkingSpot("M-%02d".formatted(i), SpotSize.MEDIUM));
        for (int i = 1; i <= 9; i++) spots.add(new ParkingSpot("L-%02d".formatted(i), SpotSize.LARGE));
        repository.saveAll(spots);
    }
}

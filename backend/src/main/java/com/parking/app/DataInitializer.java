package com.parking.app;

import com.parking.app.model.ParkingSpot;
import com.parking.app.model.SpotSize;
import com.parking.app.repository.ParkingSpotRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
        IntStream.range(1, 10).forEach(num -> {
            spots.add(new ParkingSpot("S-%02d".formatted(num), SpotSize.SMALL));
            spots.add(new ParkingSpot("M-%02d".formatted(num), SpotSize.MEDIUM));
            spots.add(new ParkingSpot("L-%02d".formatted(num), SpotSize.LARGE));
        });

        repository.saveAll(spots);
    }
}

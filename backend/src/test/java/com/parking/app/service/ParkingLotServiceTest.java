package com.parking.app.service;

import com.parking.app.exception.NoAvailableSpotException;
import com.parking.app.exception.SpotAlreadyFreeException;
import com.parking.app.exception.SpotNotFoundException;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.SpotSize;
import com.parking.app.model.VehicleType;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=false",
        "parking.sqs.enabled=false"
})
class ParkingLotServiceTest {

    @MockBean
    private SqsTemplate sqsTemplate;

    @Autowired
    private ParkingLotService service;

    @BeforeEach
    void setUp() {
        service.resetLot();
    }

    @Test
    void lotStartsWith27FreeSpots() {
        List<ParkingSpot> spots = service.getAllSpots();
        assertEquals(27, spots.size());
        assertTrue(spots.stream().allMatch(ParkingSpot::isFree));
    }

    @Test
    void parkingCarUsesExactSizeSpotFirst() {
        ParkingSpot spot = service.parkVehicle(VehicleType.CAR, "ABC-123");
        assertEquals(SpotSize.MEDIUM, spot.getSize());
        assertEquals("ABC-123", spot.getLicensePlate());
    }

    @Test
    void motorcycleCanUseLargerSpotWhenSmallIsFull() {
        // fill all 9 SMALL spots with motorcycles
        for (int i = 0; i < 9; i++) {
            ParkingSpot spot = service.parkVehicle(VehicleType.MOTORCYCLE, "MOTO-" + i);
            assertEquals(SpotSize.SMALL, spot.getSize());
        }

        // 10th motorcycle should overflow into a MEDIUM spot
        ParkingSpot overflow = service.parkVehicle(VehicleType.MOTORCYCLE, "MOTO-11");
        assertEquals(SpotSize.MEDIUM, overflow.getSize());
    }

    @Test
    void truckCannotUseSmallOrMediumSpot() {
        // fill all 9 LARGE spots with trucks
        for (int i = 0; i < 9; i++) {
            service.parkVehicle(VehicleType.TRUCK, "TRUCK-" + i);
        }

        // 11th truck has nowhere to go: SMALL/MEDIUM cannot fit a TRUCK
        assertThrows(NoAvailableSpotException.class,
                () -> service.parkVehicle(VehicleType.TRUCK, "TRUCK-11"));
    }

    @Test
    void clearingASpotMakesItAvailableAgain() {
        ParkingSpot spot = service.parkVehicle(VehicleType.CAR, "XYZ-999");
        ParkingSpot cleared = service.clearSpot(spot.getId());

        assertTrue(cleared.isFree());
        assertNull(cleared.getLicensePlate());
    }

    @Test
    void clearingAlreadyFreeSpotThrows() {
        ParkingSpot spot = service.getAllSpots().get(0);
        assertThrows(SpotAlreadyFreeException.class, () -> service.clearSpot(spot.getId()));
    }

    @Test
    void clearingUnknownSpotThrows() {
        assertThrows(SpotNotFoundException.class, () -> service.clearSpot("DOES-NOT-EXIST"));
    }

    @Test
    void noVehicleIsEverAssignedToTheSameSpotTwiceUnderConcurrency() throws InterruptedException {
        // Stress test: fire 200 concurrent CAR park requests at a lot that
        // only has 9 MEDIUM + 9 LARGE spots usable by cars (18 total).
        // Exactly 18 should succeed, the rest must fail cleanly with
        // NoAvailableSpotException — and critically, no spot should ever
        // be double-assigned.
        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        ConcurrentHashMap<String, AtomicInteger> spotAssignmentCounts = new ConcurrentHashMap<>();

        IntStream.range(0, threadCount).forEach(i -> executor.submit(() -> {
            try {
                ParkingSpot spot = service.parkVehicle(VehicleType.CAR, "CAR-" + i);
                spotAssignmentCounts.computeIfAbsent(spot.getId(), k -> new AtomicInteger()).incrementAndGet();
                successCount.incrementAndGet();
            } catch (NoAvailableSpotException e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }));

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(18, successCount.get(), "exactly 18 spots (MEDIUM+LARGE) should fit a CAR");
        assertEquals(182, failureCount.get());

        // The critical correctness assertion: every spot was assigned at most once.
        spotAssignmentCounts.values().forEach(count ->
                assertEquals(1, count.get(), "a spot was assigned to more than one vehicle"));
    }
}

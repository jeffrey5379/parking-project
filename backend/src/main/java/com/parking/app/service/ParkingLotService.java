package com.parking.app.service;

import com.parking.app.dto.AvailabilitySummary;
import com.parking.app.dto.ConcurrentParkRequest;
import com.parking.app.dto.ConcurrentParkResult;
import com.parking.app.dto.RelocationMessage;
import com.parking.app.exception.NoAvailableSpotException;
import com.parking.app.exception.SpotAlreadyFreeException;
import com.parking.app.exception.SpotNotFoundException;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Relocation;
import com.parking.app.model.RelocationStatus;
import com.parking.app.model.SpotSize;
import com.parking.app.model.SpotStatus;
import com.parking.app.model.VehicleType;
import com.parking.app.repository.ParkingSpotRepository;
import com.parking.app.repository.RelocationRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ParkingLotService {

    private static final Logger log = LoggerFactory.getLogger(ParkingLotService.class);

    private final ParkingSpotRepository repository;
    private final RelocationRepository relocationRepository;
    private final SqsTemplate sqsTemplate;
    private final ParkingEventPublisher eventPublisher;

    @Value("${parking.sqs.relocation-queue}")
    private String relocationQueue;

    // Self-injection so the proxy intercepts @Transactional on tryParkVehicle.
    @Autowired @Lazy
    private ParkingLotService self;

    public ParkingLotService(ParkingSpotRepository repository,
                              RelocationRepository relocationRepository,
                              SqsTemplate sqsTemplate,
                              ParkingEventPublisher eventPublisher) {
        this.repository = repository;
        this.relocationRepository = relocationRepository;
        this.sqsTemplate = sqsTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ParkingSpot> getAllSpots() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt((ParkingSpot s) -> s.getSize().ordinal())
                        .thenComparing(ParkingSpot::getId))
                .toList();
    }

    /**
     * Parks a vehicle, retrying up to 10 times on optimistic lock conflicts.
     */
    public ParkingSpot parkVehicle(VehicleType vehicleType, String licensePlateInput) {
        String licensePlate = (licensePlateInput == null || licensePlateInput.isBlank())
                ? generatePlaceholderPlate()
                : licensePlateInput.trim();

        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                ParkingSpot result = self.tryParkVehicle(vehicleType, licensePlate);
                eventPublisher.publishUpdate();
                return result;
            } catch (OptimisticLockingFailureException ignored) {
                // another transaction grabbed the same spot; retry with fresh DB state
            }
        }
        throw new NoAvailableSpotException(
                "No available spot for vehicle type " + vehicleType +
                " (requires at least a " + vehicleType.getRequiredSize() + " spot)");
    }

    @Transactional
    public ParkingSpot tryParkVehicle(VehicleType vehicleType, String licensePlate) {
        List<ParkingSpot> all = repository.findAll();
        SpotSize required = vehicleType.getRequiredSize();

        // Normal path: smallest compatible free slot.
        Optional<ParkingSpot> freeSlot = all.stream()
                .filter(ParkingSpot::isFree)
                .filter(s -> s.getSize().canFit(required))
                .min(Comparator.comparingInt(s -> s.getSize().ordinal()));

        if (freeSlot.isPresent()) {
            ParkingSpot spot = freeSlot.get();
            spot.occupy(vehicleType, licensePlate);
            return repository.save(spot);
        }

        // Relocation path: find a compatible OCCUPIED slot whose current occupant
        // requires a smaller size and can be moved to a free smaller slot.
        // Exclude RELOCATING spots to prevent cascading relocations.
        List<ParkingSpot> relocationCandidates = all.stream()
                .filter(s -> s.getStatus() == SpotStatus.OCCUPIED)
                .filter(s -> s.getSize().canFit(required))
                .filter(s -> s.getOccupiedBy().getRequiredSize().ordinal() < s.getSize().ordinal())
                .sorted(Comparator.comparingInt(s -> s.getSize().ordinal()))
                .toList();

        for (ParkingSpot candidate : relocationCandidates) {
            SpotSize occupantRequired = candidate.getOccupiedBy().getRequiredSize();
            Optional<ParkingSpot> dest = all.stream()
                    .filter(ParkingSpot::isFree)
                    .filter(s -> s.getSize().canFit(occupantRequired))
                    .min(Comparator.comparingInt(s -> s.getSize().ordinal()));

            if (dest.isEmpty()) continue;

            ParkingSpot destination = dest.get();
            destination.startRelocation(candidate.getOccupiedBy(), candidate.getLicensePlate());
            repository.save(destination);

            candidate.clear();
            candidate.startRelocation(vehicleType, licensePlate);
            ParkingSpot result = repository.save(candidate);

            Relocation relocation = new Relocation(candidate.getId(), destination.getId());
            relocationRepository.save(relocation);

            sqsTemplate.send(to -> to.queue(relocationQueue)
                    .payload(new RelocationMessage(
                            relocation.getId(),
                            relocation.getSourceSpotId(),
                            relocation.getDestinationSpotId())));
            return result;
        }

        throw new NoAvailableSpotException("No available spot for vehicle type " + vehicleType +
            " (requires at least a " + required + " spot)");
    }

    @Transactional
    @SuppressWarnings("null")
    public ParkingSpot clearSpot(String spotId) {
        ParkingSpot spot = repository.findById(spotId)
                .orElseThrow(() -> new SpotNotFoundException("No spot found with id " + spotId));
        if (spot.isFree()) {
            throw new SpotAlreadyFreeException("Spot " + spotId + " is already free");
        }
        spot.clear();
        ParkingSpot result = repository.save(spot);
        eventPublisher.publishUpdate();
        return result;
    }

    @Transactional
    public void resetLot() {
        List<ParkingSpot> all = repository.findAll();
        all.forEach(ParkingSpot::clear);
        repository.saveAll(all);

        List<Relocation> active = relocationRepository.findByStatus(RelocationStatus.IN_PROGRESS);
        active.forEach(Relocation::fail);
        relocationRepository.saveAll(active);

        eventPublisher.publishUpdate();
    }

    @Transactional(readOnly = true)
    public AvailabilitySummary getAvailabilitySummary() {
        List<ParkingSpot> all = repository.findAll();
        Map<SpotSize, AvailabilitySummary.SizeAvailability> bySize = new LinkedHashMap<>();
        for (SpotSize size : SpotSize.values()) {
            List<ParkingSpot> ofSize = all.stream().filter(s -> s.getSize() == size).toList();
            int total = ofSize.size();
            int free = (int) ofSize.stream().filter(ParkingSpot::isFree).count();
            bySize.put(size, new AvailabilitySummary.SizeAvailability(total, free, total - free));
        }
        int totalSpots = all.size();
        int totalFree = (int) all.stream().filter(ParkingSpot::isFree).count();
        return new AvailabilitySummary(bySize, totalSpots, totalFree, totalSpots - totalFree);
    }

    /**
     * Parks multiple vehicles concurrently using one virtual thread per vehicle.
     */
    public ConcurrentParkResult parkConcurrently(ConcurrentParkRequest request) {
        List<Callable<Void>> tasks = new ArrayList<>();
        addParkTasks(tasks, VehicleType.MOTORCYCLE, request.motorcycleCount());
        addParkTasks(tasks, VehicleType.CAR, request.carCount());
        addParkTasks(tasks, VehicleType.TRUCK, request.truckCount());

        int parked = 0;
        int failed = 0;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                try {
                    future.get();
                    parked++;
                } catch (ExecutionException e) {
                    log.error(e.getMessage());
                    failed++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        eventPublisher.publishUpdate();
        return new ConcurrentParkResult(parked, failed);
    }

    private void addParkTasks(List<Callable<Void>> tasks, VehicleType type, int count) {
        for (int i = 0; i < count; i++) {
            tasks.add(() -> {
                parkVehicle(type, null);
                return null;
            });
        }
    }

    private String generatePlaceholderPlate() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

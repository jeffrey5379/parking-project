package com.parking.app.sqs;

import com.parking.app.dto.RelocationMessage;
import com.parking.app.model.RelocationStatus;
import com.parking.app.model.SpotStatus;
import com.parking.app.repository.ParkingSpotRepository;
import com.parking.app.repository.RelocationRepository;
import com.parking.app.service.ParkingEventPublisher;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "parking.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class RelocationMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RelocationMessageConsumer.class);

    private final ParkingSpotRepository spotRepository;
    private final RelocationRepository relocationRepository;
    private final ParkingEventPublisher eventPublisher;

    @Value("${parking.sqs.max-receive-count:3}")
    private int maxReceiveCount;

    @Value("${parking.sqs.relocation-failure-rate:0.5}")
    private double relocationFailureRate;

    @Autowired @Lazy
    private RelocationMessageConsumer self;

    public RelocationMessageConsumer(ParkingSpotRepository spotRepository,
                                     RelocationRepository relocationRepository,
                                     ParkingEventPublisher eventPublisher) {
        this.spotRepository = spotRepository;
        this.relocationRepository = relocationRepository;
        this.eventPublisher = eventPublisher;
    }

    @SqsListener("${parking.sqs.relocation-queue}")
    public void onRelocationMessage(RelocationMessage message) throws InterruptedException {
        if (!self.isActive(message.relocationId())) {
            log.info("Relocation {} no longer IN_PROGRESS, discarding stale message", message.relocationId());
            return;
        }

        int delayMs = 2000 + ThreadLocalRandom.current().nextInt(2000);
        log.info("Processing relocation {} — simulating {}ms physical move", message.relocationId(), delayMs);
        Thread.sleep(delayMs);

        if (ThreadLocalRandom.current().nextDouble() < relocationFailureRate) {
            int attempt = self.recordFailedAttempt(message.relocationId());

            if (attempt >= maxReceiveCount) {
                self.markFailed(message.relocationId());
                eventPublisher.publishWithNotification(
                        "Relocation from " + message.sourceId()
                                + " to " + message.destinationId() + " permanently failed after " + maxReceiveCount
                                + " attempts.", "error");
                log.error("Relocation {} → DLQ after {} failed attempts", message.relocationId(), attempt);
            } else {
                eventPublisher.publishWithNotification(
                        "Relocation failed (attempt " + attempt + "/" + maxReceiveCount + "), retrying…",
                        "error");
                log.warn("Relocation {} failed, attempt {}/{}", message.relocationId(), attempt, maxReceiveCount);
            }

            throw new RuntimeException("Simulated physical relocation failure");
        }

        self.markCompleted(message.relocationId());
        eventPublisher.publishUpdate();
        log.info("Relocation {} complete", message.relocationId());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public boolean isActive(String relocationId) {
        return relocationRepository.findById(relocationId)
                .map(r -> r.getStatus() == RelocationStatus.IN_PROGRESS)
                .orElse(false);
    }

    @Transactional
    @SuppressWarnings("null")
    public int recordFailedAttempt(String relocationId) {
        return relocationRepository.findById(relocationId)
                .map(r -> {
                    int attempt = r.incrementAttempts();
                    relocationRepository.save(r);
                    return attempt;
                })
                .orElse(0);
    }

    @Transactional
    @SuppressWarnings("null")
    public void markFailed(String relocationId) {
        relocationRepository.findById(relocationId).ifPresent(r -> {
            r.fail();
            relocationRepository.save(r);

            // Destination spot holds the evicted vehicle's data while RELOCATING.
            // Restore that vehicle to the source spot and free the destination.
            spotRepository.findById(r.getDestinationSpotId()).ifPresent(dest -> {
                if (dest.getStatus() == SpotStatus.RELOCATING) {
                    spotRepository.findById(r.getSourceSpotId()).ifPresent(source -> {
                        if (source.getStatus() == SpotStatus.RELOCATING) {
                            source.occupy(dest.getOccupiedBy(), dest.getLicensePlate());
                            spotRepository.save(source);
                        }
                    });
                    dest.clear();
                    spotRepository.save(dest);
                }
            });
        });
    }

    @Transactional
    @SuppressWarnings("null")
    public void markCompleted(String relocationId) {
        relocationRepository.findById(relocationId).ifPresent(r -> {
            r.complete();
            relocationRepository.save(r);
            spotRepository.findById(r.getDestinationSpotId()).ifPresent(spot -> {
                if (spot.getStatus() == SpotStatus.RELOCATING) {
                    spot.finishRelocation();
                    spotRepository.save(spot);
                }
            });
            spotRepository.findById(r.getSourceSpotId()).ifPresent(spot -> {
                if (spot.getStatus() == SpotStatus.RELOCATING) {
                    spot.finishRelocation();
                    spotRepository.save(spot);
                }
            });
        });
    }
}

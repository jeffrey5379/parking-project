package com.parking.app.sqs;

import com.parking.app.dto.RelocationMessage;
import com.parking.app.model.SpotStatus;
import com.parking.app.repository.ParkingSpotRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "parking.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class RelocationMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RelocationMessageConsumer.class);

    private final ParkingSpotRepository repository;

    public RelocationMessageConsumer(ParkingSpotRepository repository) {
        this.repository = repository;
    }

    @SqsListener("${parking.sqs.relocation-queue}")
    @Transactional
    public void onRelocationMessage(RelocationMessage message) throws InterruptedException {
        int delayMs = 5000 + ThreadLocalRandom.current().nextInt(5000);
        log.info("Relocating vehicle to spot {} - simulating {}s physical move", message.destinationId(), delayMs/1000);
        Thread.sleep(delayMs);
        repository.findById(message.destinationId()).ifPresent(spot -> {
            if (spot.getStatus() == SpotStatus.RELOCATING) {
                spot.finishRelocation();
                repository.save(spot);
                log.info("Relocation to spot {} complete", message.destinationId());
            }
        });
    }
}

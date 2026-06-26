package com.parking.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.app.dto.ParkingSpotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ParkingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ParkingEventPublisher.class);

    // @Lazy breaks the ParkingLotService ↔ ParkingEventPublisher circular dep.
    @Autowired @Lazy
    private ParkingLotService service;

    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public ParkingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        sendToOne(emitter, buildPayload(null, null));
        return emitter;
    }

    public void publishUpdate() {
        publishToAll(buildPayload(null, null));
    }

    public void publishWithNotification(String message, String kind) {
        publishToAll(buildPayload(message, kind));
    }

    @SuppressWarnings("null")
    private void publishToAll(String payload) {
        if (emitters.isEmpty()) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("parking-update")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    @SuppressWarnings("null")
    private void sendToOne(SseEmitter emitter, String payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name("parking-update")
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    private String buildPayload(String notificationMessage, String notificationKind) {
        List<ParkingSpotResponse> spots = service.getAllSpots().stream()
                .map(ParkingSpotResponse::from)
                .toList();
        var summary = service.getAvailabilitySummary();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("spots", spots);
        payload.put("summary", summary);
        if (notificationMessage != null) {
            payload.put("notification", Map.of("message", notificationMessage, "kind", notificationKind));
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize parking update", e);
            return "{}";
        }
    }
}

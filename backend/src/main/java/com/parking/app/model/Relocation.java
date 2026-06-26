package com.parking.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "relocations")
public class Relocation {

    @Id
    private String id;

    @Column(nullable = false)
    private String sourceSpotId;

    @Column(nullable = false)
    private String destinationSpotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelocationStatus status;

    private int attempts;

    @Column(nullable = false)
    private Instant startedAt;

    @Version
    private Long version;

    protected Relocation() {}

    public Relocation(String sourceSpotId, String destinationSpotId) {
        this.id = UUID.randomUUID().toString();
        this.sourceSpotId = sourceSpotId;
        this.destinationSpotId = destinationSpotId;
        this.status = RelocationStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSourceSpotId() { return sourceSpotId; }
    public String getDestinationSpotId() { return destinationSpotId; }
    public RelocationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getStartedAt() { return startedAt; }

    public int incrementAttempts() { return ++attempts; }
    public void complete() { this.status = RelocationStatus.COMPLETED; }
    public void fail() { this.status = RelocationStatus.FAILED; }
}

package com.ticketing.reservation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "endpoint"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String requestHash;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant ttl;
}
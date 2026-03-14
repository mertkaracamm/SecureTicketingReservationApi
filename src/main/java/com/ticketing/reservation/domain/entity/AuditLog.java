package com.ticketing.reservation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    @Column(nullable = false)
    private String action;

    private String resourceType;
    private String resourceId;
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
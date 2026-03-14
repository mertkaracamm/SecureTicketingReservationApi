package com.ticketing.reservation.repository;

import com.ticketing.reservation.domain.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKeyAndEndpoint(String key, String endpoint);

    void deleteByTtlBefore(Instant cutoff);
}
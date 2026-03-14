package com.ticketing.reservation.repository;

import com.ticketing.reservation.domain.entity.Reservation;
import com.ticketing.reservation.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // Sum active seats to prevent overselling
    @Query("SELECT COALESCE(SUM(r.seats), 0) FROM Reservation r WHERE r.eventId = :eventId AND r.status <> 'CANCELLED'")
    int sumActiveSeats(@Param("eventId") UUID eventId);

    List<Reservation> findByEventId(UUID eventId);

    List<Reservation> findByUserId(UUID userId);

    boolean existsByEventIdAndUserIdAndStatusNot(UUID eventId, UUID userId, ReservationStatus status);
}
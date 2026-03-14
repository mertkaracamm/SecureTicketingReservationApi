package com.ticketing.reservation.repository;

import com.ticketing.reservation.domain.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByOwnerId(UUID ownerId, Pageable pageable);

    @Query(value = """
    	    SELECT * FROM events
    	    WHERE published = true
    	      AND (CAST(:from AS TIMESTAMP) IS NULL OR starts_at >= CAST(:from AS TIMESTAMP))
    	      AND (CAST(:to AS TIMESTAMP) IS NULL OR starts_at <= CAST(:to AS TIMESTAMP))
    	      AND (CAST(:q AS VARCHAR) IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :q, '%'))
    	           OR LOWER(venue) LIKE LOWER(CONCAT('%', :q, '%')))
    	    ORDER BY starts_at ASC
    	    LIMIT :size OFFSET :offset
    	    """, nativeQuery = true)
    	List<Event> findPublished(
    	        @Param("from") Instant from,
    	        @Param("to") Instant to,
    	        @Param("q") String q,
    	        @Param("size") int size,
    	        @Param("offset") long offset);

    // Pessimistic lock - prevents concurrent oversell
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") UUID id);
}
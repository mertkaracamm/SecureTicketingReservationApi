package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.domain.entity.Reservation;
import com.ticketing.reservation.domain.enums.ReservationStatus;
import com.ticketing.reservation.dto.request.ReservationRequest;
import com.ticketing.reservation.dto.response.Responses.ReservationResponse;
import com.ticketing.reservation.exception.*;
import com.ticketing.reservation.repository.*;
import com.ticketing.reservation.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    // Pessimistic lock on event row prevents overselling under concurrent load
    @Transactional
    public ReservationResponse createReservation(UUID eventId, ReservationRequest request) {
        UserPrincipal principal = currentUser();

        if (reservationRepository.existsByEventIdAndUserIdAndStatusNot(
                eventId, principal.getId(), ReservationStatus.CANCELLED)) {
            throw new BusinessException("You already have an active reservation for this event");
        }
        
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!event.isPublished()) {
            throw new BusinessException("Cannot reserve seats for an unpublished event");
        }

        int activeSeats = reservationRepository.sumActiveSeats(eventId);
        if (activeSeats + request.seats() > event.getCapacity()) {
            throw new BusinessException(
                    String.format("Insufficient seats. Requested: %d, Available: %d",
                            request.seats(), event.getCapacity() - activeSeats));
        }

        Reservation reservation = Reservation.builder()
                .eventId(eventId)
                .userId(principal.getId())
                .seats(request.seats())
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse confirmReservation(UUID reservationId) {
        Reservation reservation = findAndVerifyAccess(reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Only PENDING reservations can be confirmed");
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancelReservation(UUID reservationId) {
        Reservation reservation = findAndVerifyAccess(reservationId);
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
        	    reservation.getStatus() == ReservationStatus.CONFIRMED) {
        	    throw new BusinessException("Only PENDING reservations can be cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    private Reservation findAndVerifyAccess(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        UserPrincipal principal = currentUser();
        boolean isAdmin = hasRole(principal, "ROLE_ADMIN");
        boolean isOwner = reservation.getUserId().equals(principal.getId());

        boolean isEventOrganizer = false;
        if (hasRole(principal, "ROLE_ORGANIZER")) {
            isEventOrganizer = eventRepository.findById(reservation.getEventId())
                    .map(e -> e.getOwnerId().equals(principal.getId()))
                    .orElse(false);
        }

        if (!isAdmin && !isOwner && !isEventOrganizer) {
            throw new AccessDeniedException("Access denied to this reservation");
        }
        return reservation;
    }

    private boolean hasRole(UserPrincipal p, String role) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
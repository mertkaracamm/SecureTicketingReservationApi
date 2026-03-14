package com.ticketing.reservation.controller;

import com.ticketing.reservation.dto.request.ReservationRequest;
import com.ticketing.reservation.dto.response.Responses.ReservationResponse;
import com.ticketing.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reservations & Discovery", description = "Reservation lifecycle endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/api/events/{eventId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create a reservation - requires Idempotency-Key header")
    public ReservationResponse createReservation(@PathVariable UUID eventId,
                                                  @Valid @RequestBody ReservationRequest request) {
        return reservationService.createReservation(eventId, request);
    }

    @PostMapping("/api/reservations/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm a PENDING reservation")
    public ReservationResponse confirmReservation(@PathVariable UUID id) {
        return reservationService.confirmReservation(id);
    }

    @PostMapping("/api/reservations/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a reservation")
    public ReservationResponse cancelReservation(@PathVariable UUID id) {
        return reservationService.cancelReservation(id);
    }
}
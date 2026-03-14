package com.ticketing.reservation.service;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.domain.entity.Reservation;
import com.ticketing.reservation.domain.enums.ReservationStatus;
import com.ticketing.reservation.dto.request.ReservationRequest;
import com.ticketing.reservation.exception.BusinessException;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.repository.ReservationRepository;
import com.ticketing.reservation.security.UserPrincipal;
import com.ticketing.reservation.domain.entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock EventRepository eventRepository;
    @InjectMocks ReservationService reservationService;

    private User customerUser;
    private Event publishedEvent;

    @BeforeEach
    void setup() {
        customerUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .passwordHash("hash")
                .roles("CUSTOMER")
                .build();

        publishedEvent = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .title("Test Event")
                .venue("Venue")
                .startsAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .endsAt(Instant.now().plus(6, ChronoUnit.DAYS))
                .capacity(100)
                .published(true)
                .build();

        setSecurityContext(customerUser);
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReservation_sufficientCapacity_shouldSucceed() {
        given(eventRepository.findByIdWithLock(publishedEvent.getId()))
                .willReturn(Optional.of(publishedEvent));
        given(reservationRepository.sumActiveSeats(publishedEvent.getId())).willReturn(50);
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = reservationService.createReservation(publishedEvent.getId(), new ReservationRequest(10));

        assertThat(result.seats()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void createReservation_insufficientCapacity_shouldThrowBusinessException() {
        given(eventRepository.findByIdWithLock(publishedEvent.getId()))
                .willReturn(Optional.of(publishedEvent));
        given(reservationRepository.sumActiveSeats(publishedEvent.getId())).willReturn(95);

        assertThatThrownBy(() ->
                reservationService.createReservation(publishedEvent.getId(), new ReservationRequest(10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient seats");
    }

    @Test
    void createReservation_unpublishedEvent_shouldThrowBusinessException() {
        publishedEvent.setPublished(false);
        given(eventRepository.findByIdWithLock(publishedEvent.getId()))
                .willReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() ->
                reservationService.createReservation(publishedEvent.getId(), new ReservationRequest(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unpublished");
    }

    @Test
    void confirmReservation_pendingStatus_shouldSucceed() {
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .eventId(publishedEvent.getId())
                .userId(customerUser.getId())
                .status(ReservationStatus.PENDING)
                .seats(2)
                .build();
        given(reservationRepository.findById(reservation.getId())).willReturn(Optional.of(reservation));
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var result = reservationService.confirmReservation(reservation.getId());
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void confirmReservation_alreadyCancelled_shouldThrowBusinessException() {
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .eventId(publishedEvent.getId())
                .userId(customerUser.getId())
                .status(ReservationStatus.CANCELLED)
                .seats(2)
                .build();
        given(reservationRepository.findById(reservation.getId())).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(reservation.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING");
    }

    private void setSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }
}
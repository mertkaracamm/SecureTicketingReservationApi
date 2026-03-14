package com.ticketing.reservation.concurrency;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.ReservationRequest;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.repository.ReservationRepository;
import com.ticketing.reservation.repository.UserRepository;
import com.ticketing.reservation.security.UserPrincipal;
import com.ticketing.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OversellConcurrencyTest {

    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Event testEvent;
    private List<User> testUsers;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User organizer = userRepository.save(User.builder()
                .email("org@test.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("ORGANIZER")
                .build());

        testEvent = eventRepository.save(Event.builder()
                .ownerId(organizer.getId())
                .title("Concurrent Event")
                .venue("Test Venue")
                .startsAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .endsAt(Instant.now().plus(6, ChronoUnit.DAYS))
                .capacity(10)
                .published(true)
                .build());

        testUsers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            testUsers.add(userRepository.save(User.builder()
                    .email("user" + i + "@test.com")
                    .passwordHash(passwordEncoder.encode("Test@12345!"))
                    .roles("CUSTOMER")
                    .build()));
        }
    }

    @Test
    void concurrentReservations_shouldNotOversell() throws InterruptedException {
        int threadCount = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final User user = testUsers.get(i);
            executor.submit(() -> {
                try {
                    setSecurityContext(user);
                    reservationService.createReservation(testEvent.getId(), new ReservationRequest(1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        int totalActiveSeats = reservationRepository.sumActiveSeats(testEvent.getId());
        assertThat(totalActiveSeats).isLessThanOrEqualTo(10);
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
    }

    private void setSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }
}
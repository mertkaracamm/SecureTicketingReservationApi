package com.ticketing.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.LoginRequest;
import com.ticketing.reservation.dto.request.ReservationRequest;
import com.ticketing.reservation.repository.EventRepository;
import com.ticketing.reservation.repository.ReservationRepository;
import com.ticketing.reservation.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String customerToken;
    private String organizerToken;
    private String eventId;

    @BeforeEach
    void setup() throws Exception {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .email("organizer@res.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("ORGANIZER")
                .build());

        userRepository.save(User.builder()
                .email("customer@res.com")
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles("CUSTOMER")
                .build());

        organizerToken = login("organizer@res.com");
        customerToken = login("customer@res.com");
        eventId = createAndPublishEvent(organizerToken);
    }

    @Test
    @Order(1)
    void createReservation_validRequest_shouldReturn201() throws Exception {
        var request = new ReservationRequest(2);

        mockMvc.perform(post("/api/events/" + eventId + "/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(2)
    void confirmReservation_shouldReturn200() throws Exception {
        String reservationId = createReservation(customerToken, eventId, 1);

        mockMvc.perform(post("/api/reservations/" + reservationId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(3)
    void cancelReservation_shouldReturn200() throws Exception {
        String reservationId = createReservation(customerToken, eventId, 1);

        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(4)
    void confirmReservation_alreadyCancelled_shouldReturn422() throws Exception {
        String reservationId = createReservation(customerToken, eventId, 1);

        // first cancel
        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                .header("Authorization", "Bearer " + customerToken));

        // after confirm → 422
        mockMvc.perform(post("/api/reservations/" + reservationId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().is(422));
    }

    @Test
    @Order(5)
    void createReservation_exceedsCapacity_shouldReturn422() throws Exception {
        // capacity 10, 11 seat 
        var request = new ReservationRequest(11);

        mockMvc.perform(post("/api/events/" + eventId + "/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422));
    }

    @Test
    @Order(6)
    void createReservation_unauthenticated_shouldReturn403() throws Exception {
        var request = new ReservationRequest(1);

        mockMvc.perform(post("/api/events/" + eventId + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void cancelReservation_alreadyConfirmed_shouldReturn422() throws Exception {
        String reservationId = createReservation(customerToken, eventId, 1);

        // first confirm
        mockMvc.perform(post("/api/reservations/" + reservationId + "/confirm")
                .header("Authorization", "Bearer " + customerToken));

        // after cancel → 422 (confirmed status is not cancelled)
        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().is(422));
    }

    // --- helpers ---

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Test@12345!"))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createAndPublishEvent(String token) throws Exception {
        // create
        String body = """
                {
                  "title": "Test Concert",
                  "venue": "Test Arena",
                  "startsAt": "2099-06-01T18:00:00Z",
                  "endsAt": "2099-06-01T22:00:00Z",
                  "capacity": 10
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        // publish
        mockMvc.perform(post("/api/events/" + id + "/publish")
                .header("Authorization", "Bearer " + token));

        return id;
    }

    private String createReservation(String token, String evtId, int seats) throws Exception {
        var request = new ReservationRequest(seats);
        MvcResult result = mockMvc.perform(post("/api/events/" + evtId + "/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
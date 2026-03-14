package com.ticketing.reservation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.dto.request.LoginRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleBasedAccessTest {

    @Autowired MockMvc mockMvc;    
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired PasswordEncoder passwordEncoder;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void cleanup() {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void customer_cannotCreateEvent() throws Exception {
        String token = createUserAndLogin("cust@rbac.com", "CUSTOMER");
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"venue\":\"y\",\"startsAt\":\"2099-01-01T00:00:00Z\",\"endsAt\":\"2099-01-02T00:00:00Z\",\"capacity\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events")).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_canBrowsePublicEvents() throws Exception {
        mockMvc.perform(get("/api/events/public")).andExpect(status().isOk());
    }

    @Test
    void invalidToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    private String createUserAndLogin(String email, String role) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Test@12345!"))
                .roles(role)
                .build());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Test@12345!"))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
package com.ticketing.reservation.filter;

import com.ticketing.reservation.domain.entity.IdempotencyKey;
import com.ticketing.reservation.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock FilterChain filterChain;
    @InjectMocks IdempotencyFilter idempotencyFilter;

    @Test
    void nonPostRequest_shouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        then(idempotencyKeyRepository).shouldHaveNoInteractions();
    }

    @Test
    void postRequest_noIdempotencyHeader_shouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events");
        request.setContent("{\"title\":\"test\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(any(), any());
    }

    @Test
    void postRequest_newIdempotencyKey_shouldProcessAndStore() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.addHeader("Idempotency-Key", "unique-key-123");
        request.setContent("{\"email\":\"test@test.com\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(idempotencyKeyRepository.findByKeyAndEndpoint("unique-key-123", "/api/auth/register"))
                .willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(any(), any());
        then(idempotencyKeyRepository).should(times(2)).save(any(IdempotencyKey.class));
    }

    @Test
    void postRequest_completedIdempotencyKey_shouldReplayResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.addHeader("Idempotency-Key", "existing-key-456");
        byte[] body = "{\"email\":\"test@test.com\"}".getBytes();
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // calculate real hash value
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(body);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        String realHash = sb.toString();

        IdempotencyKey existingKey = IdempotencyKey.builder()
                .key("existing-key-456")
                .endpoint("/api/auth/register")
                .requestHash(realHash)
                .responseBody("{\"id\":\"123\",\"email\":\"test@test.com\"}")
                .status("COMPLETED")
                .build();

        given(idempotencyKeyRepository.findByKeyAndEndpoint("existing-key-456", "/api/auth/register"))
                .willReturn(Optional.of(existingKey));

        idempotencyFilter.doFilterInternal(request, response, filterChain);

        then(filterChain).should(never()).doFilter(any(), any());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"id\":\"123\",\"email\":\"test@test.com\"}");
    }
}
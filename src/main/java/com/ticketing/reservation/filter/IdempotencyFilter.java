package com.ticketing.reservation.filter;

import com.ticketing.reservation.domain.entity.IdempotencyKey;
import com.ticketing.reservation.repository.IdempotencyKeyRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

// Ensures POST requests with same Idempotency-Key return cached response
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final long TTL_HOURS = 24;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (!HttpMethod.POST.matches(request.getMethod()) || !StringUtils.hasText(idempotencyKey)) {
            chain.doFilter(request, response);
            return;
        }

        String endpoint = request.getRequestURI();
        String requestHash = hashBody(request.getInputStream().readAllBytes());

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKeyAndEndpoint(idempotencyKey, endpoint);

        if (existing.isPresent()) {
            IdempotencyKey stored = existing.get();
            if ("PROCESSING".equals(stored.getStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"error\":\"Request is still processing\"}");
                return;
            }
            if (!MessageDigest.isEqual(
                    stored.getRequestHash().getBytes(StandardCharsets.UTF_8),
                    requestHash.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(422);
                response.getWriter().write("{\"error\":\"Idempotency key reuse with different payload\"}");
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(stored.getResponseBody());
            return;
        }

        IdempotencyKey record = IdempotencyKey.builder()
                .key(idempotencyKey)
                .endpoint(endpoint)
                .requestHash(requestHash)
                .status("PROCESSING")
                .ttl(Instant.now().plusSeconds(TTL_HOURS * 3600))
                .build();
        idempotencyKeyRepository.save(record);

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            record.setResponseBody(responseBody);
            record.setStatus("COMPLETED");
            idempotencyKeyRepository.save(record);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String hashBody(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (Exception e) {
            return "unknown";
        }
    }
}
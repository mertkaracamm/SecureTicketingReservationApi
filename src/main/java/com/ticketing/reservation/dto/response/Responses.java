package com.ticketing.reservation.dto.response;

import com.ticketing.reservation.domain.entity.Event;
import com.ticketing.reservation.domain.entity.Reservation;
import com.ticketing.reservation.domain.entity.User;
import com.ticketing.reservation.domain.enums.ReservationStatus;
import java.io.Serializable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Responses {

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        public static AuthResponse of(String access, String refresh, long expiresInMs) {
            return new AuthResponse(access, refresh, "Bearer", expiresInMs / 1000);
        }
    }

    public record UserResponse(UUID id, String email, String roles, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getRoles(), u.getCreatedAt());
        }
    }

    public record EventResponse(UUID id, UUID ownerId, String title, String venue,
                                Instant startsAt, Instant endsAt, int capacity,
                                boolean published, long version, Instant createdAt) implements Serializable  {
        public static EventResponse from(Event e) {
            return new EventResponse(e.getId(), e.getOwnerId(), e.getTitle(), e.getVenue(),
                    e.getStartsAt(), e.getEndsAt(), e.getCapacity(),
                    e.isPublished(), e.getVersion(), e.getCreatedAt());
        }
    }

    public record ReservationResponse(UUID id, UUID eventId, UUID userId,
                                      ReservationStatus status, int seats, Instant createdAt) {
        public static ReservationResponse from(Reservation r) {
            return new ReservationResponse(r.getId(), r.getEventId(), r.getUserId(),
                    r.getStatus(), r.getSeats(), r.getCreatedAt());
        }
    }

    public record ErrorResponse(int status, String error, String message, Instant timestamp) {
        public static ErrorResponse of(int status, String error, String message) {
            return new ErrorResponse(status, error, message, Instant.now());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size,
            long totalElements, int totalPages) implements Serializable {
		public PageResponse  {
		content = List.copyOf(content);
	}
}
}
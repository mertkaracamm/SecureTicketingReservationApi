CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    roles       VARCHAR(255) NOT NULL DEFAULT 'CUSTOMER',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    last_login_at TIMESTAMP
);

CREATE TABLE events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID NOT NULL REFERENCES users(id),
    title       VARCHAR(500) NOT NULL,
    venue       VARCHAR(500) NOT NULL,
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP NOT NULL,
    capacity    INTEGER NOT NULL CHECK (capacity > 0),
    published   BOOLEAN NOT NULL DEFAULT FALSE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reservations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events(id),
    user_id     UUID NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    seats       INTEGER NOT NULL CHECK (seats > 0),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_keys (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL,
    endpoint      VARCHAR(500) NOT NULL,
    request_hash  VARCHAR(64) NOT NULL,
    response_body TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    ttl           TIMESTAMP NOT NULL,
    UNIQUE (idempotency_key, endpoint)
);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id      UUID,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   VARCHAR(255),
    ip            VARCHAR(64),
    user_agent    TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_owner     ON events(owner_id);
CREATE INDEX idx_events_published ON events(published, starts_at);
CREATE INDEX idx_reservations_event ON reservations(event_id, status);
CREATE INDEX idx_reservations_user  ON reservations(user_id);
CREATE INDEX idx_idempotency_key    ON idempotency_keys(idempotency_key, endpoint);
CREATE INDEX idx_audit_actor        ON audit_logs(actor_id, created_at);
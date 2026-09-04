CREATE TABLE reservations
(
    id           UUID PRIMARY KEY,

    user_id      UUID                     NOT NULL,

    bike_id      UUID                     NOT NULL,

    station_id   UUID                     NOT NULL,

    reserved_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    status       VARCHAR(20)              NOT NULL,

    cancelled_at TIMESTAMP WITH TIME ZONE,

    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    version      BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT chk_reservation_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'CANCELLED',
                       'EXPIRED',
                       'CONSUMED'
                )
            ),

    CONSTRAINT chk_reservation_expiry
        CHECK (expires_at > reserved_at)
);

CREATE INDEX idx_reservations_user_status
    ON reservations (user_id, status);

CREATE INDEX idx_reservations_bike_id
    ON reservations (bike_id);

CREATE INDEX idx_reservations_expiration
    ON reservations (status, expires_at);
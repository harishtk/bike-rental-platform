CREATE TABLE bikes
(
    id            UUID PRIMARY KEY,
    serial_number VARCHAR(100) NOT NULL,
    type          VARCHAR(100) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    station_id    UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_bikes_serial_number
        UNIQUE (serial_number),

    CONSTRAINT bikes_status_valid
        CHECK (
            status IN (
                       'AVAILABLE',
                       'RESERVED',
                       'RENTED',
                       'MAINTENANCE',
                       'RETIRED'
                )
            ),

    CONSTRAINT bikes_version_non_negative
        CHECK (version >= 0)
);
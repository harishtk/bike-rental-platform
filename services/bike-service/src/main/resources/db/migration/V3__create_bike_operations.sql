CREATE TABLE bike_operations
(
    operation_id      UUID PRIMARY KEY,
    bike_id           UUID                     NOT NULL,
    operation_type    VARCHAR(20)              NOT NULL,
    result_station_id UUID,
    processed_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_bike_operation_type
        CHECK (operation_type IN ('RESERVE', 'RELEASE'))
);

CREATE INDEX idx_bike_operations_bike_id
    ON bike_operations (bike_id);
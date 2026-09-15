ALTER TABLE bike_operations
    DROP CONSTRAINT chk_bike_operation_type;

ALTER TABLE bike_operations
    ADD CONSTRAINT chk_bike_operation_type
        CHECK (
            operation_type IN (
                               'RESERVE',
                               'RELEASE',
                               'RENT',
                               'RETURN'
                )
            );
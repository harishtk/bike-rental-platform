CREATE UNIQUE INDEX uq_active_reservation_user
    ON reservations (user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_active_reservation_bike
    ON reservations (bike_id)
    WHERE status = 'ACTIVE';
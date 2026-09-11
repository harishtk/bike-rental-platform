CREATE TABLE rentals (
         id UUID PRIMARY KEY,
         user_id UUID NOT NULL,
         bike_id UUID NOT NULL,

         start_station_id UUID NOT NULL,
         return_station_id UUID,

         started_at TIMESTAMP WITH TIME ZONE NOT NULL,
         returned_at TIMESTAMP WITH TIME ZONE,

         status VARCHAR(20) NOT NULL,

         daily_rate NUMERIC(12, 2) NOT NULL,
         total_amount NUMERIC(12, 2) NOT NULL,

         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

         version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_active_rental_user
    ON rentals(user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_active_rental_bike
    ON rentals(bike_id)
    WHERE status = 'ACTIVE';
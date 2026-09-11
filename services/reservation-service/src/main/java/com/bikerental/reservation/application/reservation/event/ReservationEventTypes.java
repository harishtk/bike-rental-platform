package com.bikerental.reservation.application.reservation.event;

public class ReservationEventTypes {

    public static final String AGGREGATE_TYPE = "RESERVATION";

    public static final String CREATED = "RESERVATION_CREATED";
    public static final String CANCELLED = "RESERVATION_CANCELLED";
    public static final String EXPIRED = "RESERVATION_EXPIRED";

    private ReservationEventTypes () {}
}

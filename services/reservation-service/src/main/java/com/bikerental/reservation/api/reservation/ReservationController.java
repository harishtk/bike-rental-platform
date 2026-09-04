package com.bikerental.reservation.api.reservation;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bikerental.reservation.application.reservation.ReservationService;
import com.bikerental.reservation.domain.reservation.Reservation;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationApiMapper mapper;

    public ReservationController(
            ReservationService reservationService,
            ReservationApiMapper mapper
    ) {
        this.reservationService = reservationService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody
            CreateReservationRequest request,
            UriComponentsBuilder uriBuilder
    ) {

        Reservation reservation =
                reservationService.createReservation(
                        request.userId(),
                        request.bikeId(),
                        request.stationId(),
                        Duration.ofHours(request.durationHours())
                );

        var location = uriBuilder.path("/api/v1/reservations/{reservationId}")
                .buildAndExpand(reservation.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(reservation));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        List<ReservationResponse> reservations = reservationService.allReservations()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable UUID reservationId
    ) {
        Reservation reservation =
                reservationService.getReservation(reservationId);

        return ResponseEntity.ok(mapper.toResponse(reservation));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable UUID reservationId
    ) {
        Reservation reservation =
                reservationService.cancelReservation(reservationId);

        return ResponseEntity.accepted().body(mapper.toResponse(reservation));
    }
}
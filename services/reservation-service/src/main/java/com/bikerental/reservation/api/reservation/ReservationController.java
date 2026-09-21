package com.bikerental.reservation.api.reservation;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bikerental.reservation.application.reservation.ReservationService;
import com.bikerental.reservation.domain.reservation.Reservation;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationApiMapper mapper;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody
            CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt,
            UriComponentsBuilder uriBuilder
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Reservation reservation =
                reservationService.createReservation(
                        userId,
                        request.bikeId(),
                        Duration.ofHours(request.durationHours())
                );

        var location = uriBuilder.path("/api/v1/reservations/{reservationId}")
                .buildAndExpand(reservation.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(reservation));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId =  UUID.fromString(jwt.getSubject());

        List<ReservationResponse> reservations = reservationService.getReservations(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Reservation reservation =
                reservationService.getReservation(reservationId, userId);

        return ResponseEntity.ok(mapper.toResponse(reservation));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Reservation reservation =
                reservationService.cancelReservation(reservationId, userId);

        return ResponseEntity.accepted().body(mapper.toResponse(reservation));
    }
}
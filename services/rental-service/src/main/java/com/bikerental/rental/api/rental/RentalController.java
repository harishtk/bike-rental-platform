package com.bikerental.rental.api.rental;

import com.bikerental.rental.api.security.CustomerIdentity;
import com.bikerental.rental.application.rental.RentalService;
import com.bikerental.rental.domain.rental.Rental;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final RentalResponseMapper mapper;

    @PostMapping
    public ResponseEntity<RentalResponse> create(
            @Valid @RequestBody
            CreateRentalRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        UUID userId = CustomerIdentity.userId(jwt);

        return ResponseEntity.ok(
                mapper.toResponse(
                        rentalService.startRental(
                                userId,
                                request.bikeId(),
                                request.stationId(),
                                request.dailyRate()
                        )
                )
        );
    }

    @PostMapping("/{rentalId}/return")
    public ResponseEntity<RentalResponse> returnRental(
            @PathVariable UUID rentalId,
            @Valid @RequestBody
            ReturnRentalRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = CustomerIdentity.userId(jwt);

        return ResponseEntity.ok(
                mapper.toResponse(
                        rentalService.returnRental(
                                userId,
                                rentalId,
                                request.stationId()
                        )
                )
        );
    }

    @PostMapping("/{rentalId}/complete")
    public ResponseEntity<RentalResponse> complete(
            @PathVariable UUID rentalId
    ) {
        return ResponseEntity.ok(
                mapper.toResponse(
                        rentalService.completeRental(
                                rentalId
                        )
                )
        );
    }
}
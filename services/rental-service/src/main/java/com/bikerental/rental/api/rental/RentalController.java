package com.bikerental.rental.api.rental;

import com.bikerental.rental.application.rental.RentalService;
import com.bikerental.rental.domain.rental.Rental;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<RentalResponse> create(
            @Valid @RequestBody
            CreateRentalRequest request
    ) {
        return ResponseEntity.ok(
                toResponse(
                        rentalService.startRental(
                                request.userId(),
                                request.bikeId(),
                                request.dailyRate()
                        )
                )
        );
    }

    @PostMapping("/{rentalId}/return")
    public ResponseEntity<RentalResponse> returnRental(
            @PathVariable UUID rentalId,
            @Valid @RequestBody
            ReturnRentalRequest request
    ) {
        return ResponseEntity.ok(
                toResponse(
                        rentalService.returnRental(
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
                toResponse(
                        rentalService.completeRental(
                                rentalId
                        )
                )
        );
    }

    private RentalResponse toResponse(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                rental.getUserId(),
                rental.getBikeId(),
                rental.getStartStationId(),
                rental.getReturnStationId(),
                rental.getStatus(),
                rental.getStartedAt(),
                rental.getReturnedAt(),
                rental.getDailyRate(),
                rental.getTotalAmount()
        );
    }
}
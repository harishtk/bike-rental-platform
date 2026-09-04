package com.bikerental.reservation.api.common;

import com.bikerental.reservation.application.reservation.ActiveReservationAlreadyExistsException;
import com.bikerental.reservation.application.reservation.ReservationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse<Object>> handleBikeNotFound(
            ReservationNotFoundException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.NOT_FOUND.value(),
                "RESERVATION_NOT_FOUND",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(ActiveReservationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse<Object>> handleDuplicateBikeSerialNumber(
            ActiveReservationAlreadyExistsException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.CONFLICT.value(),
                "ACTIVE_RESERVATION_EXISTS",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse<Object>> handleConcurrentBikeModification(
            ObjectOptimisticLockingFailureException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.CONFLICT.value(),
                "BIKE_UNAVAILABLE",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse<Object>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.create(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_REQUEST",
                                exception.getMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<List<ValidationError>>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        var response = ErrorResponse.create(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    public record ValidationError(
            String field,
            String message
    ) {

    }
}

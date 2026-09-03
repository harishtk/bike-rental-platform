package com.bikerental.bike.api.common;

import com.bikerental.bike.application.bike.BikeNotFoundException;
import com.bikerental.bike.application.bike.DuplicateBikeSerialNumberException;
import com.bikerental.bike.application.station.StationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<ErrorResponse<Object>> handleStationNotFound(
            StationNotFoundException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(BikeNotFoundException.class)
    public ResponseEntity<ErrorResponse<Object>> handleBikeNotFound(
            BikeNotFoundException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.NOT_FOUND.value(),
                "BIKE_NOT_FOUND",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(DuplicateBikeSerialNumberException.class)
    public ResponseEntity<ErrorResponse<Object>> handleDuplicateBikeSerialNumber(
            DuplicateBikeSerialNumberException exception
    ) {
        var response = ErrorResponse.create(
                HttpStatus.PRECONDITION_FAILED.value(),
                "DUPLICATE_BIKE_SERIAL_NUMBER",
                exception.getMessage(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.PRECONDITION_FAILED)
                .body(response);
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

package com.gymius.config;

import com.gymius.dto.ApiErrorDto;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorDto("Please fix the highlighted fields.", fieldErrors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorDto> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request could not be completed." : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiErrorDto(message, Map.of()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorDto> handleMalformedRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Request contains an invalid value.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorDto> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorDto("Please fix the highlighted fields.", fieldErrors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorDto> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Meal photo is larger than the upload limit.");
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiErrorDto> handleMultipart(MultipartException exception) {
        return error(HttpStatus.BAD_REQUEST, "A valid meal image upload is required.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorDto> handleConflict(DataIntegrityViolationException exception) {
        LOGGER.warn("A database constraint rejected an API request", exception);
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data. Refresh and try again.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorDto> handleAuthentication(AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Your session is no longer valid. Please sign in again.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorDto> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            if (status.is5xxServerError()) {
                LOGGER.error("Unhandled framework error", exception);
            }
            return ResponseEntity
                    .status(status)
                    .body(new ApiErrorDto(frameworkErrorMessage(status), Map.of()));
        }

        LOGGER.error("Unhandled API error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while processing the request.");
    }

    private String frameworkErrorMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Request is missing or contains an invalid value.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "That request method is not supported.";
            case 406 -> "The requested response format is not supported.";
            case 415 -> "That content type is not supported.";
            default -> status.is4xxClientError()
                    ? "Request could not be completed."
                    : "Something went wrong while processing the request.";
        };
    }

    private ResponseEntity<ApiErrorDto> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorDto(message, Map.of()));
    }
}

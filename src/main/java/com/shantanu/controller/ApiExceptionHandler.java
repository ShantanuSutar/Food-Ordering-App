package com.shantanu.controller;

import com.shantanu.response.ApiErrorResponse;
import tools.jackson.core.JacksonException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException error) {
        String message = error.getReason() == null || error.getReason().isBlank()
                ? "Request could not be completed"
                : error.getReason();
        return ResponseEntity
                .status(error.getStatusCode())
                .body(new ApiErrorResponse(error.getStatusCode().value(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException error) {
        String fieldName = findInvalidField(error);
        String message = fieldName == null
                ? "Request body contains invalid values"
                : "Invalid value for field '" + fieldName + "'";
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(400, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException error) {
        return ResponseEntity
                .status(409)
                .body(new ApiErrorResponse(409, "Could not save because the data conflicts with an existing record"));
    }

    private String findInvalidField(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof JacksonException jacksonException
                    && !jacksonException.getPath().isEmpty()) {
                JacksonException.Reference reference = jacksonException.getPath()
                        .get(jacksonException.getPath().size() - 1);
                return reference.getPropertyName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}

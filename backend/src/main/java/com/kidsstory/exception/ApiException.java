package com.kidsstory.exception;

import org.springframework.http.HttpStatus;

/** Mirrors FastAPI's HTTPException(status_code, detail) pattern used throughout the Python services. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

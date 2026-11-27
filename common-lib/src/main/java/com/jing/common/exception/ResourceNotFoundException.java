package com.jing.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(HttpStatus.NOT_FOUND, "%s not found with %s: %s".formatted(resource, field, value));
    }

    // used when the message comes from another service
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}

package com.jing.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String field, String message, Object value) {
        super(HttpStatus.CONFLICT, "%s already exists with %s: %s".formatted(field, message, value));
    }

    // used when the message comes from another service
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

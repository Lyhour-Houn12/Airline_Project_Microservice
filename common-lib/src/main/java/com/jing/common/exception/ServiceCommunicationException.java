package com.jing.common.exception;


import org.springframework.http.HttpStatus;

public class ServiceCommunicationException extends BaseException {

    public ServiceCommunicationException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
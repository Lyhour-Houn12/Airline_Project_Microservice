package com.jing.common.feign;

import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.exception.ServiceCommunicationException;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeignErrorDecoder implements ErrorDecoder {

    private static final Pattern MESSAGE_FIELD =
            Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = extractMessage(response);
        return switch (response.status()) {
            case 400, 422 -> new BadRequestException(message);
            case 404 -> new ResourceNotFoundException(message);
            case 409 -> new DuplicateResourceException(message);
            default -> new ServiceCommunicationException(
                    methodKey + " failed with status " + response.status() + ": " + message);
        };
    }

    private String extractMessage(Response response) {
        try {
            if (response.body() != null) {
                try (InputStream in = response.body().asInputStream()) {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher matcher = MESSAGE_FIELD.matcher(body);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (IOException ignored) {
            // fall through to the reason phrase
        }
        return response.reason() != null ? response.reason() : "Remote call failed";
    }
}
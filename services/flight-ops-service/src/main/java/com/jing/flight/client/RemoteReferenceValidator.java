package com.jing.flight.client;


import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.location.AirportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RemoteReferenceValidator {
    private final AirportClient airportClient;

    public AirportResponse requireAirport(Long id) {
        return call(() -> airportClient.getAirportById(id), "Airport " + id + " does not exist");
    }

    private <T> T call(Supplier<T> remote, String notFoundMessage) {
        try {
            return remote.get();
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException(notFoundMessage);
        }
    }
}

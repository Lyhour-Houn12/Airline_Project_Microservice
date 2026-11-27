package com.jing.booking.client;

import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Reads data owned by other services. A missing id in the other service is the client's mistake
 * here, so it becomes a 400 ("flight instance 7 does not exist") instead of a 404 for the booking.
 * Network failures stay a 503 (ServiceCommunicationException from the Feign error decoder).
 */
@Component
@RequiredArgsConstructor
public class RemoteLookup {

    private final FlightInstanceClient flightInstanceClient;
    private final FareClient fareClient;

    public FlightInstanceResponse requireFlightInstance(Long id) {
        return call(() -> flightInstanceClient.getFlightInstance(id), "Flight instance " + id + " does not exist");
    }

    public FareResponse requireFare(Long id) {
        return call(() -> fareClient.getFare(id), "Fare " + id + " does not exist");
    }

    private static <T> T call(Supplier<T> remote, String notFound) {
        try {
            return remote.get();
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException(notFound);
        }
    }
}

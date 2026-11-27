package com.jing.pricing.client;

import com.jing.common.dto.flight.FlightResponse;
import com.jing.common.dto.seat.CabinClassResponse;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * A fare points at a flight (flight-ops) and a cabin class (seat-service). This checks that both exist
 * and fit together: the flight belongs to the fare's airline, the cabin is on that flight's aircraft,
 * and the cabin type matches the fare's.
 */
@Component
@RequiredArgsConstructor
public class FareReferences {

    private final FlightOpsClient flightOps;
    private final CabinClassClient cabinClasses;

    public void requireConsistent(Long airlineId, Long flightId, Long cabinClassId, CabinClassType type) {
        FlightResponse flight;
        try {
            flight = flightOps.flight(flightId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Flight " + flightId + " does not exist");
        }
        if (!Objects.equals(flight.getAirlineId(), airlineId)) {
            throw new BadRequestException("Flight " + flight.getFlightNumber() + " belongs to another airline");
        }

        CabinClassResponse cabin;
        try {
            cabin = cabinClasses.cabinClass(cabinClassId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Cabin class " + cabinClassId + " does not exist");
        }
        if (!Objects.equals(cabin.getAircraftId(), flight.getAircraftId())) {
            throw new BadRequestException("Cabin class " + cabin.getName() + " is not on the aircraft of flight "
                    + flight.getFlightNumber());
        }
        if (type != null && cabin.getCabinClassType() != type) {
            throw new BadRequestException("Cabin class " + cabin.getName() + " is " + cabin.getCabinClassType()
                    + ", not " + type);
        }
    }
}

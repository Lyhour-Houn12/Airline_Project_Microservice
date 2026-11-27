package com.jing.seat.service;

import com.jing.common.airline.AirlineAccess;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.seat.FlightInstanceCabinBulkRequest;
import com.jing.common.dto.seat.FlightInstanceCabinRequest;
import com.jing.common.dto.seat.SeatMapRequest;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import com.jing.seat.client.FlightInstanceClient;
import com.jing.seat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Who may change what in seat-service. Controllers call this before the service.
 * <ul>
 *   <li>Cabin classes, seat maps and seats belong to an aircraft, so to the aircraft's airline
 *       (airline-core). Creating them also checks the aircraft exists.</li>
 *   <li>Flight-instance cabins and seat instances belong to a flight instance, so to its airline
 *       (flight-ops). Its cabins must be cabins of the aircraft that flies it.</li>
 * </ul>
 * A missing record is left to the service (which answers 404).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatOwnership {

    private final AirlineAccess airlineAccess;
    private final CabinClassRepository cabinClasses;
    private final SeatMapRepository seatMaps;
    private final SeatRepository seats;
    private final FlightInstanceCabinRepository flightInstanceCabins;
    private final SeatInstanceRepository seatInstances;
    private final FlightInstanceClient flightInstanceClient;

    // ------------------------------------------------------------ aircraft side

    public void aircraft(CurrentUser user, Long aircraftId) {
        if (aircraftId == null) return;   // the service rejects a missing id
        airlineAccess.requireCanManage(user, airlineAccess.airlineOfAircraft(aircraftId));
    }

    public void cabinClass(CurrentUser user, Long id) {
        cabinClasses.findById(id).ifPresent(c -> aircraft(user, c.getAircraftId()));
    }

    /** A seat map's airline must be the airline of its cabin's aircraft. */
    public void newSeatMap(CurrentUser user, SeatMapRequest request) {
        cabinClasses.findById(request.getCabinClassId()).ifPresent(cabin -> {
            Long airlineId = airlineAccess.airlineOfAircraft(cabin.getAircraftId());
            airlineAccess.requireCanManage(user, airlineId);
            if (request.getAirlineId() != null && !request.getAirlineId().equals(airlineId)) {
                throw new BadRequestException("Cabin class " + cabin.getName() + " belongs to airline " + airlineId
                        + ", not " + request.getAirlineId());
            }
        });
    }

    public void seatMap(CurrentUser user, Long id) {
        seatMaps.findById(id).ifPresent(m -> aircraft(user, m.getCabinClass().getAircraftId()));
    }

    public void seat(CurrentUser user, Long id) {
        seats.findById(id).ifPresent(s -> aircraft(user, s.getAircraftId()));
    }

    // ------------------------------------------------------------ flight side

    public void flightInstance(CurrentUser user, Long flightInstanceId) {
        airlineAccess.requireCanManage(user, requireFlightInstance(flightInstanceId).getAirlineId());
    }

    /** Cabins for a flight instance: the flight's airline, and cabins of the aircraft flying it. */
    public void newFlightInstanceCabins(CurrentUser user, FlightInstanceCabinBulkRequest request) {
        FlightInstanceResponse flight = requireFlightInstance(request.flightInstanceId());
        airlineAccess.requireCanManage(user, flight.getAirlineId());
        for (FlightInstanceCabinRequest item : request.cabins()) {
            cabinClasses.findById(item.cabinClassId())
                    .filter(c -> !Objects.equals(c.getAircraftId(), flight.getAircraftId()))
                    .ifPresent(c -> {
                        throw new BadRequestException("Cabin class " + c.getName() + " is not on the aircraft of flight "
                                + flight.getFlightNumber());
                    });
        }
    }

    public void flightInstanceCabin(CurrentUser user, Long id) {
        flightInstanceCabins.findById(id).ifPresent(c -> flightInstance(user, c.getFlightInstanceId()));
    }

    public void seatInstance(CurrentUser user, Long id) {
        seatInstances.findById(id).ifPresent(s -> flightInstance(user, s.getFlightInstanceId()));
    }

    private FlightInstanceResponse requireFlightInstance(Long flightInstanceId) {
        try {
            return flightInstanceClient.flightInstance(flightInstanceId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Flight instance " + flightInstanceId + " does not exist");
        }
    }
}

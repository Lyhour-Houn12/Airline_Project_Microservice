package com.jing.flight.client;

import com.jing.common.dto.location.AirportResponse;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The time zone of an airport, from location-service. Also the check that the airport exists:
 * flight-ops only stores airport ids, so this is where a wrong id is caught.
 *
 * <p>Zones are cached: an airport practically never moves to another time zone, and flight-ops asks
 * for the same few airports all the time (schedule generation creates hundreds of instances at once).
 */
@Component
@RequiredArgsConstructor
public class AirportZones {

    private final AirportClient airportClient;
    private final Map<Long, ZoneId> cache = new ConcurrentHashMap<>();

    /**
     * @throws BadRequestException if the airport doesn't exist or has no time zone set
     */
    public ZoneId zoneOf(Long airportId) {
        ZoneId cached = cache.get(airportId);
        if (cached != null) return cached;

        AirportResponse airport;
        try {
            airport = airportClient.getAirportById(airportId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Airport " + airportId + " does not exist");
        }
        if (airport.getTimeZone() == null) {
            throw new BadRequestException("Airport " + airport.getIataCode() + " (" + airportId
                    + ") has no time zone; set it in location-service first");
        }
        cache.put(airportId, airport.getTimeZone());
        return airport.getTimeZone();
    }
}

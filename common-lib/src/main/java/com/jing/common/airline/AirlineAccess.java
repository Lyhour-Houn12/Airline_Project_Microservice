package com.jing.common.airline;

import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.enumeration.AircraftStatus;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ownership rules shared by every service that stores airline data (flights, fares, seats, meals...):
 * <ul>
 *   <li>a <b>system admin</b> may manage every airline;</li>
 *   <li>an <b>airline owner</b> may manage only the airline they own (airline-core decides who owns what);</li>
 *   <li>an aircraft may only be used by the airline it belongs to.</li>
 * </ul>
 *
 * <p>Not a Spring component on purpose: only services that store airline data need it. Such a service
 * declares {@code @Bean AirlineAccess airlineAccess(AirlineCoreClient c) { return new AirlineAccess(c); }}
 * and adds {@code com.jing.common.airline} to its {@code @EnableFeignClients} packages.
 */
public class AirlineAccess {

    private static final Duration CACHE_FOR = Duration.ofMinutes(5);

    private final AirlineCoreClient client;
    private final Map<Long, Cached> ownedAirline = new ConcurrentHashMap<>();

    private record Cached(Long airlineId, Instant until) {}

    public AirlineAccess(AirlineCoreClient client) {
        this.client = client;
    }

    /** The airline this owner runs. 403 if they don't own one (yet). Cached for a few minutes. */
    public Long ownedAirlineId(CurrentUser owner) {
        Cached hit = ownedAirline.get(owner.id());
        if (hit != null && hit.until().isAfter(Instant.now())) return hit.airlineId();
        AirlineResponse airline;
        try {
            airline = client.airlineOfOwner(owner.id());
        } catch (ResourceNotFoundException e) {
            throw new ForbiddenException("You don't own an airline yet. Create your airline first");
        }
        ownedAirline.put(owner.id(), new Cached(airline.getId(), Instant.now().plus(CACHE_FOR)));
        return airline.getId();
    }

    /** Like {@link #ownedAirlineId} but empty instead of 403: for reads that simply show less. */
    public Optional<Long> ownedAirlineIdIfAny(CurrentUser user) {
        if (!user.isAirlineOwner()) return Optional.empty();
        try {
            return Optional.of(ownedAirlineId(user));
        } catch (ForbiddenException noAirlineYet) {
            return Optional.empty();
        }
    }

    /** Only admins, or the owner of {@code airlineId}, get past this. */
    public void requireCanManage(CurrentUser user, Long airlineId) {
        if (user.isAdmin()) return;
        if (!user.isAirlineOwner() || !Objects.equals(ownedAirlineId(user), airlineId)) {
            throw new ForbiddenException("This belongs to another airline");
        }
    }

    /**
     * The airline a new record belongs to. An owner always gets their own airline (sending another
     * airline's id is a 403); an admin must name an existing, ACTIVE airline.
     */
    public Long airlineForNew(CurrentUser user, Long requestedAirlineId) {
        Long airlineId;
        if (user.isAirlineOwner()) {
            airlineId = ownedAirlineId(user);
            if (requestedAirlineId != null && !requestedAirlineId.equals(airlineId)) {
                throw new ForbiddenException("You can only create records for your own airline (" + airlineId + ")");
            }
        } else if (user.isAdmin()) {
            if (requestedAirlineId == null) throw new BadRequestException("airlineId is required");
            airlineId = requestedAirlineId;
        } else {
            throw new ForbiddenException("Only system admins and airline owners can do this");
        }
        requireActive(airlineId);
        return airlineId;
    }

    /** The airline must exist and be ACTIVE (not INACTIVE or BANNED). */
    public AirlineResponse requireActive(Long airlineId) {
        AirlineResponse airline;
        try {
            airline = client.airline(airlineId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Airline " + airlineId + " does not exist");
        }
        if (airline.getStatus() != AirlineStatus.ACTIVE) {
            throw new BadRequestException("Airline " + airline.getAirlineName() + " is " + airline.getStatus());
        }
        return airline;
    }

    /** The aircraft must exist, belong to {@code airlineId} and be ACTIVE. */
    public AircraftResponse requireAircraftOf(Long aircraftId, Long airlineId) {
        AircraftResponse aircraft = aircraft(aircraftId);
        if (!Objects.equals(aircraft.getAirlineId(), airlineId)) {
            throw new BadRequestException("Aircraft " + aircraft.getCode() + " belongs to another airline");
        }
        if (aircraft.getStatus() != AircraftStatus.ACTIVE) {
            throw new BadRequestException("Aircraft " + aircraft.getCode() + " is " + aircraft.getStatus());
        }
        return aircraft;
    }

    /** The airline an aircraft belongs to (400 if the aircraft doesn't exist). */
    public Long airlineOfAircraft(Long aircraftId) {
        return aircraft(aircraftId).getAirlineId();
    }

    private AircraftResponse aircraft(Long aircraftId) {
        try {
            return client.aircraft(aircraftId);
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Aircraft " + aircraftId + " does not exist");
        }
    }
}

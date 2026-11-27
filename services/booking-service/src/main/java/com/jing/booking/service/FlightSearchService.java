package com.jing.booking.service;

import com.jing.booking.client.CabinAvailabilityClient;
import com.jing.booking.client.FareClient;
import com.jing.booking.client.FlightInstanceClient;
import com.jing.booking.payload.response.FlightSearchResponse;
import com.jing.booking.payload.response.FlightSearchResponse.CabinOption;
import com.jing.booking.payload.response.FlightSearchResponse.FareOption;
import com.jing.booking.payload.response.FlightSearchResponse.FlightOption;
import com.jing.booking.payload.response.FlightSearchResponse.Party;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.enumeration.PassengerType;
import com.jing.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Customer flight search: one call that combines flight-ops (which flights), seat-service (how many places
 * are left in each cabin) and pricing (what each fare costs for this party, with the same child / infant
 * rules a booking uses). Only flights a booking would accept are returned: sellable, with UTC times, and
 * with room for the whole party.
 */
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    public static final int MAX_PASSENGERS = 9;
    private static final int MAX_RESULTS = 20;

    private final FlightInstanceClient flights;
    private final CabinAvailabilityClient cabins;
    private final FareClient fares;
    private final FareCalculator fareCalculator;

    public enum Sort { DEPARTURE, PRICE }

    public FlightSearchResponse search(Long from, Long to, LocalDate date, int adults, int children, int infants,
                                       CabinClassType cabinFilter, String currency, Sort sort) {
        Party party = validate(from, to, date, adults, children, infants);

        List<FlightInstanceResponse> found = flights.search(from, to, date.toString(), 1, MAX_RESULTS,
                "id", "asc").getContent();

        List<FlightOption> options = new ArrayList<>();
        for (FlightInstanceResponse flight : found) {
            if (flight.getDepartureAt() == null) continue;            // no time-zone data: can't be booked
            FlightOption option = priceFlight(flight, party, cabinFilter, currency);
            if (option != null) options.add(option);
        }

        Comparator<FlightOption> order = sort == Sort.PRICE
                ? Comparator.comparing(FlightOption::cheapestTotal).thenComparing(FlightOption::departureAt)
                : Comparator.comparing(FlightOption::departureAt);
        options.sort(order);
        return new FlightSearchResponse(from, to, date.toString(), party, options);
    }

    private FlightOption priceFlight(FlightInstanceResponse flight, Party party, CabinClassType cabinFilter, String currency) {
        List<FareResponse> flightFares = fares.faresOfFlight(flight.getFlightId(), 1, 100).getContent();
        List<CabinOption> cabinOptions = new ArrayList<>();

        for (FlightInstanceCabinResponse cabin : cabins.cabinsOf(flight.getId())) {
            if (!Boolean.TRUE.equals(cabin.isBookable())) continue;
            if (cabinFilter != null && cabin.cabinClassType() != cabinFilter) continue;
            if (cabin.availableSeats() < party.seatsNeeded()) continue;   // not enough room for everyone

            List<FareOption> fareOptions = flightFares.stream()
                    .filter(f -> Objects.equals(f.getCabinClassId(), cabin.cabinClassId()))
                    .filter(f -> currency == null || currency.equalsIgnoreCase(f.getCurrency()))
                    .map(f -> fareOption(f, party))
                    .sorted(Comparator.comparing(FareOption::totalPrice))
                    .toList();
            if (fareOptions.isEmpty()) continue;                       // nothing on sale in this cabin

            cabinOptions.add(new CabinOption(cabin.cabinClassId(), cabin.cabinClassType(), cabin.cabinClassName(),
                    cabin.availableSeats(), fareOptions));
        }
        if (cabinOptions.isEmpty()) return null;

        FareOption cheapest = cabinOptions.stream().flatMap(c -> c.fares().stream())
                .min(Comparator.comparing(FareOption::totalPrice)).orElseThrow();
        return new FlightOption(flight.getId(), flight.getFlightId(), flight.getFlightNumber(), flight.getAirlineId(),
                flight.getDepartureAirportId(), flight.getArrivalAirportId(),
                flight.getDepartureDateTime(), flight.getDepartureTimeZone(),
                flight.getArrivalDateTime(), flight.getArrivalTimeZone(),
                flight.getDepartureAt(), flight.getArrivalAt(), flight.getFormattedDuration(),
                cheapest.totalPrice(), cheapest.currency(), cabinOptions);
    }

    FareOption fareOption(FareResponse fare, Party party) {
        BigDecimal adult = fareCalculator.quote(fare, PassengerType.ADULT);
        BigDecimal child = fareCalculator.quote(fare, PassengerType.CHILD);
        BigDecimal infant = fareCalculator.quote(fare, PassengerType.INFANT);
        BigDecimal total = adult.multiply(BigDecimal.valueOf(party.adults()))
                .add(child.multiply(BigDecimal.valueOf(party.children())))
                .add(infant.multiply(BigDecimal.valueOf(party.infants())));
        var bags = fare.getBaggagePolicy();
        return new FareOption(fare.getId(), fare.getFareName(), fare.getFareLabel(), adult,
                party.children() > 0 ? child : null, party.infants() > 0 ? infant : null,
                total, fare.getCurrency(),
                bags != null ? bags.getCheckInBaggagePieces() : null,
                bags != null ? bags.getCheckInBaggageMaxWeight() : null,
                Boolean.TRUE.equals(fare.getFreeDateChange()), Boolean.TRUE.equals(fare.getFullRefund()),
                Boolean.TRUE.equals(fare.getComplimentaryMeals()));
    }

    static Party validate(Long from, Long to, LocalDate date, int adults, int children, int infants) {
        if (from == null || to == null) throw new BadRequestException("from and to (airport ids) are required");
        if (from.equals(to)) throw new BadRequestException("from and to must be different airports");
        if (date == null) throw new BadRequestException("date is required (e.g. 2026-12-24)");
        if (date.isBefore(LocalDate.now().minusDays(1))) throw new BadRequestException("date is in the past");
        if (adults < 1) throw new BadRequestException("at least one adult is required");
        if (children < 0 || infants < 0) throw new BadRequestException("children and infants can't be negative");
        if (infants > adults) throw new BadRequestException("each infant must travel on an adult's lap");
        if (adults + children + infants > MAX_PASSENGERS) {
            throw new BadRequestException("at most " + MAX_PASSENGERS + " passengers per booking");
        }
        return new Party(adults, children, infants);
    }
}

package com.jing.booking.payload.response;

import com.jing.common.enumeration.CabinClassType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Flights for a route, date and party, each with the cabins that still have room for the whole party
 * and every fare in them, priced for that party. Book one with
 * {@code POST /api/v1/bookings/{id}/segments {flightInstanceId, fareId}}.
 */
public record FlightSearchResponse(
        Long departureAirportId,
        Long arrivalAirportId,
        String date,
        Party party,
        List<FlightOption> flights
) {
    public record Party(int adults, int children, int infants) {
        public int seatsNeeded() {
            return adults + children;   // infants sit on a lap
        }
    }

    public record FlightOption(
            Long flightInstanceId,
            Long flightId,
            String flightNumber,
            Long airlineId,
            Long departureAirportId,
            Long arrivalAirportId,
            LocalDateTime departureTime,      // local, at the departure airport
            String departureTimeZone,
            LocalDateTime arrivalTime,        // local, at the arrival airport
            String arrivalTimeZone,
            Instant departureAt,              // UTC
            Instant arrivalAt,
            String duration,
            BigDecimal cheapestTotal,         // cheapest total for the whole party on this flight
            String currency,
            List<CabinOption> cabins
    ) {}

    public record CabinOption(
            Long cabinClassId,
            CabinClassType cabinClass,
            String name,
            int seatsLeft,
            List<FareOption> fares            // cheapest first
    ) {}

    public record FareOption(
            Long fareId,
            String fareName,
            String fareLabel,
            BigDecimal pricePerAdult,
            BigDecimal pricePerChild,
            BigDecimal pricePerInfant,
            BigDecimal totalPrice,            // for the whole party
            String currency,
            Integer checkedBags,
            Double checkedBagKg,
            boolean freeDateChange,
            boolean fullRefund,
            boolean mealIncluded
    ) {}
}

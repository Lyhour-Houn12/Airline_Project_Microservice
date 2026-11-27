package com.jing.booking.service;

import com.jing.booking.client.CabinAvailabilityClient;
import com.jing.booking.client.FareClient;
import com.jing.booking.client.FlightInstanceClient;
import com.jing.booking.payload.response.FlightSearchResponse;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.dto.seat.FlightInstanceCabinResponse;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlightSearchServiceTest {

    private static final LocalDate DAY = LocalDate.now().plusDays(20);

    private final FlightInstanceClient flights = mock(FlightInstanceClient.class);
    private final CabinAvailabilityClient cabins = mock(CabinAvailabilityClient.class);
    private final FareClient fares = mock(FareClient.class);
    private final FlightSearchService search = new FlightSearchService(flights, cabins, fares,
            new FareCalculator(new BookingProperties(15, 75, 10, 60000)));   // child 75 %, infant 10 %

    private static FlightInstanceResponse flight(long id, long flightId, int hour, boolean hasUtc) {
        LocalDateTime dep = DAY.atTime(hour, 0);
        return FlightInstanceResponse.builder().id(id).flightId(flightId).flightNumber("VN9" + id).airlineId(3L)
                .departureAirportId(1L).arrivalAirportId(4L).departureDateTime(dep).arrivalDateTime(dep.plusHours(6))
                .departureAt(hasUtc ? dep.atZone(ZoneId.of("Asia/Phnom_Penh")).toInstant() : null)
                .departureTimeZone("Asia/Phnom_Penh").arrivalTimeZone("Asia/Tokyo").build();
    }

    private static FlightInstanceCabinResponse cabin(long cabinClassId, CabinClassType type, int available) {
        return new FlightInstanceCabinResponse(cabinClassId * 10, 0L, cabinClassId, type.name(), "Y", type, 1L,
                180, 0, 0, available, true, null, null);
    }

    private static FareResponse fare(long id, long cabinClassId, String base) {
        return FareResponse.builder().id(id).fareName("Fare " + id).cabinClassId(cabinClassId)
                .baseFare(new BigDecimal(base)).taxesAndFees(new BigDecimal("20.00")).airlineFees(BigDecimal.ZERO)
                .currency("USD").build();
    }

    private static <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, items.size(), (long) items.size(), 1, true, true, items.isEmpty());
    }

    @BeforeEach
    void setUp() {
        when(flights.search(eq(1L), eq(4L), eq(DAY.toString()), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page(List.of(flight(71, 4, 9, true), flight(72, 5, 7, true), flight(73, 6, 8, false))));
        when(cabins.cabinsOf(71L)).thenReturn(List.of(cabin(4, CabinClassType.ECONOMY, 5), cabin(5, CabinClassType.BUSINESS, 1)));
        when(cabins.cabinsOf(72L)).thenReturn(List.of(cabin(4, CabinClassType.ECONOMY, 1)));   // too small for 3
        when(fares.faresOfFlight(eq(4L), anyInt(), anyInt())).thenReturn(page(List.of(
                fare(10, 4, "200.00"), fare(11, 4, "80.00"), fare(12, 5, "900.00"))));
        when(fares.faresOfFlight(eq(5L), anyInt(), anyInt())).thenReturn(page(List.of(fare(13, 4, "50.00"))));
    }

    @Test
    void pricesTheWholePartyAndHidesCabinsWithoutRoom() {
        FlightSearchResponse r = search.search(1L, 4L, DAY, 2, 1, 1, null, null, FlightSearchService.Sort.DEPARTURE);

        // 72 has 1 seat left for 3 seated passengers; 73 has no UTC times: neither is bookable
        assertThat(r.flights()).extracting(FlightSearchResponse.FlightOption::flightInstanceId).containsExactly(71L);
        var economy = r.flights().getFirst().cabins().getFirst();
        assertThat(economy.cabinClass()).isEqualTo(CabinClassType.ECONOMY);
        assertThat(economy.seatsLeft()).isEqualTo(5);

        var cheapest = economy.fares().getFirst();                // sorted: the 80 fare first
        assertThat(cheapest.fareId()).isEqualTo(11L);
        assertThat(cheapest.pricePerAdult()).isEqualByComparingTo("100.00");   // 80 + 20
        assertThat(cheapest.pricePerChild()).isEqualByComparingTo("75.00");    // 75 %
        assertThat(cheapest.pricePerInfant()).isEqualByComparingTo("10.00");   // 10 %
        assertThat(cheapest.totalPrice()).isEqualByComparingTo("285.00");      // 2*100 + 75 + 10
        assertThat(r.flights().getFirst().cheapestTotal()).isEqualByComparingTo("285.00");

        // business has 1 seat: not enough for the party
        assertThat(r.flights().getFirst().cabins()).hasSize(1);
    }

    @Test
    void aSoloTravellerAlsoSeesTheSmallCabin() {
        FlightSearchResponse r = search.search(1L, 4L, DAY, 1, 0, 0, null, null, FlightSearchService.Sort.PRICE);

        // sorted by price: 72's 70 fare before 71's 100 fare
        assertThat(r.flights()).extracting(FlightSearchResponse.FlightOption::flightInstanceId).containsExactly(72L, 71L);
        assertThat(r.flights().get(1).cabins()).hasSize(2);       // economy and business both fit one person
    }

    @Test
    void cabinFilter() {
        FlightSearchResponse r = search.search(1L, 4L, DAY, 1, 0, 0, CabinClassType.BUSINESS, null, FlightSearchService.Sort.DEPARTURE);

        assertThat(r.flights()).hasSize(1);
        assertThat(r.flights().getFirst().cabins()).extracting(FlightSearchResponse.CabinOption::cabinClass)
                .containsExactly(CabinClassType.BUSINESS);
    }

    @Test
    void invalidParties() {
        assertThatThrownBy(() -> FlightSearchService.validate(1L, 4L, DAY, 0, 1, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> FlightSearchService.validate(1L, 4L, DAY, 1, 0, 2)).hasMessageContaining("lap");
        assertThatThrownBy(() -> FlightSearchService.validate(1L, 1L, DAY, 1, 0, 0)).hasMessageContaining("different");
        assertThatThrownBy(() -> FlightSearchService.validate(1L, 4L, DAY, 6, 4, 0)).hasMessageContaining("at most 9");
    }
}

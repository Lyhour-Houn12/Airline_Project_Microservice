package com.jing.seat.service;

import com.jing.common.dto.seat.CabinConfirmRequest;
import com.jing.common.dto.seat.CabinReservationRequest;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.seat.model.CabinClass;
import com.jing.seat.model.CabinReservation;
import com.jing.seat.model.FlightInstanceCabin;
import com.jing.seat.repository.CabinReservationRepository;
import com.jing.seat.repository.FlightInstanceCabinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CabinReservationServiceTest {

    private static final LocalDateTime LATER = LocalDateTime.now().plusMinutes(15);

    private final FlightInstanceCabinRepository cabins = mock(FlightInstanceCabinRepository.class);
    private final CabinReservationRepository reservations = mock(CabinReservationRepository.class);
    private final CabinReservationService service = new CabinReservationService(cabins, reservations);
    private final List<CabinReservation> table = new ArrayList<>();   // in-memory reservations
    private FlightInstanceCabin cabin;

    @BeforeEach
    void setUp() {
        cabin = FlightInstanceCabin.builder().id(3L).flightInstanceId(68L).totalSeats(4)
                .cabinClass(CabinClass.builder().id(4L).name("Economy").build()).build();
        when(cabins.lockByFlightInstanceAndCabinClass(68L, 4L)).thenReturn(Optional.of(cabin));
        when(cabins.lockById(3L)).thenReturn(Optional.of(cabin));
        when(reservations.save(any())).thenAnswer(inv -> {
            CabinReservation r = inv.getArgument(0);
            if (!table.contains(r)) table.add(r);
            return r;
        });
        doAnswer(inv -> table.remove(inv.<CabinReservation>getArgument(0))).when(reservations).delete(any());
        when(reservations.findByFlightInstanceCabinAndHoldReference(eq(cabin), anyString())).thenAnswer(inv ->
                table.stream().filter(r -> r.getHoldReference().equals(inv.getArgument(1))).findFirst());
        when(reservations.findByFlightInstanceCabinAndStatus(eq(cabin), any())).thenAnswer(inv ->
                table.stream().filter(r -> r.getStatus() == inv.getArgument(1)).toList());
        when(reservations.findByHoldReference(anyString())).thenAnswer(inv ->
                table.stream().filter(r -> r.getHoldReference().equals(inv.getArgument(0))).toList());
    }

    private CabinReservationRequest places(String booking, int seats) {
        return new CabinReservationRequest(68L, 4L, booking, seats, LATER);
    }

    @Test
    void theCabinCanNotBeOversold() {
        service.reserve(places("AAA111", 3));            // 3 of 4 taken

        assertThatThrownBy(() -> service.reserve(places("BBB222", 2)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Only 1 seat(s) left in Economy");
        assertThat(service.reserve(places("BBB222", 1)).availableSeats()).isZero();
    }

    @Test
    void settingTheCountAgainIsNotAddingIt() {
        service.reserve(places("AAA111", 2));
        service.reserve(places("AAA111", 2));            // repeat: still 2
        service.reserve(places("AAA111", 3));            // a passenger more

        assertThat(cabin.getHeldSeats()).isEqualTo(3);
        assertThat(cabin.getAvailableSeats()).isEqualTo(1);
        assertThatThrownBy(() -> service.reserve(places("AAA111", 5)))
                .hasMessageContaining("this booking needs 2 more, 1 left");
    }

    @Test
    void anAbandonedBookingsLapsedHoldIsFreedForTheNextCustomer() {
        service.reserve(places("AAA111", 4));            // full
        table.getFirst().setHeldUntil(LocalDateTime.now().minusMinutes(1));   // never paid

        assertThat(service.reserve(places("BBB222", 4)).availableSeats()).isZero();
        assertThat(table).extracting(CabinReservation::getHoldReference).containsExactly("BBB222");
    }

    @Test
    void payingTurnsHeldPlacesIntoBookedOnes() {
        service.reserve(places("AAA111", 2));

        service.confirm(new CabinConfirmRequest("AAA111", List.of(new CabinConfirmRequest.Item(68L, 4L, 2))));

        assertThat(cabin.getHeldSeats()).isZero();
        assertThat(cabin.getBookedSeats()).isEqualTo(2);
        assertThat(table.getFirst().getStatus()).isEqualTo(CabinReservation.Status.BOOKED);
    }

    @Test
    void cancellingGivesEveryPlaceBack() {
        service.reserve(places("AAA111", 2));
        service.confirm(new CabinConfirmRequest("AAA111", List.of(new CabinConfirmRequest.Item(68L, 4L, 2))));

        assertThat(service.releaseAll("AAA111")).isEqualTo(2);
        assertThat(cabin.getAvailableSeats()).isEqualTo(4);
    }
}

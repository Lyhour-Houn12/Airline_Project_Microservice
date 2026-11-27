package com.jing.flight.service;

import com.jing.common.airline.AirlineAccess;
import com.jing.common.dto.flight.FlightInstanceRequest;
import com.jing.common.dto.flight.FlightInstanceResponse;
import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import com.jing.flight.client.AirportZones;
import com.jing.flight.model.Flight;
import com.jing.flight.model.FlightInstance;
import com.jing.flight.model.FlightSchedule;
import com.jing.flight.outbox.Outbox;
import com.jing.flight.repository.FlightInstanceRepository;
import com.jing.flight.repository.FlightRepository;
import com.jing.flight.repository.FlightScheduleRepository;
import com.jing.flight.service.impl.FlightInstanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A flight instance can be created without a schedule (a one-off flight), or from one of its own flight. */
class OneOffFlightInstanceTest {

    private final FlightInstanceRepository instances = mock(FlightInstanceRepository.class);
    private final FlightRepository flights = mock(FlightRepository.class);
    private final FlightScheduleRepository schedules = mock(FlightScheduleRepository.class);
    private final AirportZones zones = mock(AirportZones.class);
    private final FlightInstanceService service = new FlightInstanceServiceImpl(
            instances, flights, schedules, zones, mock(AirlineAccess.class), mock(Outbox.class));

    private final CurrentUser owner = new CurrentUser(16L, UserRole.ROLE_AIRLINE_OWNER);
    private final Flight vn901 = flight(4L, "VN901");

    private static Flight flight(Long id, String number) {
        return Flight.builder().id(id).flightNumber(number).airlineId(3L).departureAirportId(1L).arrivalAirportId(4L).build();
    }

    private static FlightInstanceRequest request(Long scheduleId) {
        FlightInstanceRequest r = new FlightInstanceRequest();
        r.setFlightId(4L);
        r.setScheduleId(scheduleId);
        r.setDepartureDateTime(LocalDateTime.of(2026, 12, 24, 23, 0));   // Phnom Penh
        r.setArrivalDateTime(LocalDateTime.of(2026, 12, 25, 7, 0));      // Tokyo
        r.setTotalSeats(180);
        return r;
    }

    @BeforeEach
    void setUp() {
        when(flights.findById(4L)).thenReturn(Optional.of(vn901));
        when(zones.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Phnom_Penh"));
        when(zones.zoneOf(4L)).thenReturn(ZoneId.of("Asia/Tokyo"));
        when(instances.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aOneOffFlightNeedsNoSchedule() {
        FlightInstanceResponse created = service.createFlightInstanceWithCabins(owner, request(null));

        assertThat(created.getScheduleId()).isNull();
        assertThat(created.getDepartureAt()).isNotNull();          // time zones applied as usual
        verifyNoInteractions(schedules);
    }

    @Test
    void aGivenScheduleIsKept() {
        when(schedules.findById(9L)).thenReturn(Optional.of(FlightSchedule.builder().id(9L).flight(vn901).build()));

        service.createFlightInstanceWithCabins(owner, request(9L));

        ArgumentCaptor<FlightInstance> saved = ArgumentCaptor.forClass(FlightInstance.class);
        verify(instances).save(saved.capture());
        assertThat(saved.getValue().getScheduleId()).isEqualTo(9L);
    }

    @Test
    void aScheduleOfAnotherFlightIsRefused() {
        when(schedules.findById(9L)).thenReturn(Optional.of(
                FlightSchedule.builder().id(9L).flight(flight(5L, "VN126")).build()));

        assertThatThrownBy(() -> service.createFlightInstanceWithCabins(owner, request(9L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("belongs to flight VN126, not VN901");
        verify(instances, never()).save(any());
    }

    @Test
    void anUnknownScheduleIsA404() {
        when(schedules.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFlightInstanceWithCabins(owner, request(99L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

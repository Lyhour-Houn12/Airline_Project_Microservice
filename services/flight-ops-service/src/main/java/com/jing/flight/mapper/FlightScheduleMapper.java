package com.jing.flight.mapper;

import com.jing.common.enumeration.RecurrenceType;
import com.jing.common.dto.flight.FlightScheduleRequest;
import com.jing.common.dto.flight.FlightScheduleResponse;
import com.jing.flight.model.Flight;
import com.jing.flight.model.FlightSchedule;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.TreeSet;

public class FlightScheduleMapper {
    public static int durationMinutes(LocalTime departure, LocalTime arrival) {
        long m = Duration.between(departure, arrival).toMinutes();
        return (int) (m <= 0 ? m + 24 * 60 : m);
    }

    public static FlightSchedule toEntity(FlightScheduleRequest r, Flight flight) {
        if (r == null) return null;
        return FlightSchedule.builder()
                .flight(flight)
                .departureAirportId(r.getDepartureAirportId() != null
                        ? r.getDepartureAirportId() : flight.getDepartureAirportId())
                .arrivalAirportId(r.getArrivalAirportId() != null
                        ? r.getArrivalAirportId() : flight.getArrivalAirportId())
                .departureTime(r.getDepartureTime())
                .arrivalTime(r.getArrivalTime())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .operatingDays(r.getOperatingDays() != null
                        ? new HashSet<>(r.getOperatingDays()) : new HashSet<>())
                .recurrenceType(r.getRecurrenceType() == null ? RecurrenceType.DAILY : r.getRecurrenceType())
                .isActive(r.getIsActive() != null ? r.getIsActive() : true)
                .build();
    }

    public static FlightScheduleResponse toResponse(FlightSchedule s) {
        if (s == null) return null;
        return FlightScheduleResponse.builder()
                .id(s.getId())
                .flightId(s.getFlight().getId())
                .flightNumber(s.getFlight().getFlightNumber())
                .departureAirportId(s.getDepartureAirportId())
                .arrivalAirportId(s.getArrivalAirportId())
                .departureTime(s.getDepartureTime())
                .arrivalTime(s.getArrivalTime())
                .durationMinutes(durationMinutes(s.getDepartureTime(), s.getArrivalTime()))
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .operatingDays(new TreeSet<>(s.getOperatingDays()))
                .recurrenceType(s.getRecurrenceType())
                .isActive(s.getIsActive())
                .version(s.getVersion())
                .build();

    }

    public static void updateEntity(FlightScheduleRequest r, FlightSchedule e) {
        if (r == null || e == null) return;
        if (r.getDepartureAirportId() != null) e.setDepartureAirportId(r.getDepartureAirportId());
        if (r.getArrivalAirportId() != null) e.setArrivalAirportId(r.getArrivalAirportId());
        if (r.getDepartureTime() != null) e.setDepartureTime(r.getDepartureTime());
        if (r.getArrivalTime() != null) e.setArrivalTime(r.getArrivalTime());
        if (r.getStartDate() != null) e.setStartDate(r.getStartDate());
        if (r.getEndDate() != null) e.setEndDate(r.getEndDate());
        if (r.getOperatingDays() != null) {
            e.getOperatingDays().clear();
            e.getOperatingDays().addAll(r.getOperatingDays());
        }
        if (r.getRecurrenceType() != null) e.setRecurrenceType(r.getRecurrenceType());
        if (r.getIsActive() != null) e.setIsActive(r.getIsActive());
    }
}

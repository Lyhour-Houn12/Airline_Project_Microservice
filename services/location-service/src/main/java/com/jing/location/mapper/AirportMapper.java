package com.jing.location.mapper;

import com.jing.common.dto.location.AirportRequest;
import com.jing.common.dto.location.AirportResponse;
import com.jing.location.model.Airport;

public class AirportMapper {

    public static Airport toEntity(AirportRequest airportRequest) {
        if (airportRequest == null)  return null;

        return Airport.builder()
                .airportName(airportRequest.getAirportName())
                .iataCode(airportRequest.getIataCode())
                .timeZoneId(airportRequest.getTimeZone() != null ? airportRequest.getTimeZone().getId() : null)
                .address(airportRequest.getAddress())
                .geoCode(airportRequest.getGeoCode())
                .build();
    }

    public static AirportResponse toResponse(Airport airport) {
        if (airport == null) return null;

        return AirportResponse.builder()
                .id(airport.getId())
                .iataCode(airport.getIataCode())
                .airportName(airport.getAirportName())
                .detailedName(airport.getDetailedName())
                .timeZone(airport.getTimeZone())
                .address(airport.getAddress())
                .city(CityMapper.toResponse(airport.getCity()))
                .geoCode(airport.getGeoCode())
                .analytic(airport.getAnalytic())
                .build();
    }

    public static void updateEntity(AirportRequest request, Airport existingAirport) {
        if (request == null || existingAirport == null) return;

        if (request.getIataCode() != null) {
            existingAirport.setIataCode(request.getIataCode());
        }
        if (request.getAirportName() != null) {
            existingAirport.setAirportName(request.getAirportName());
        }
        if (request.getTimeZone() != null) {
            existingAirport.setTimeZone(request.getTimeZone());
        }
        if (request.getAddress() != null) {
            existingAirport.setAddress(request.getAddress());
        }
        if (request.getGeoCode() != null) {
            existingAirport.setGeoCode(request.getGeoCode());
        }
    }


}

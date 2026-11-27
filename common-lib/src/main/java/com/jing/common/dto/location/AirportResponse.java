package com.jing.common.dto.location;


import com.jing.common.embeddable.Address;
import com.jing.common.embeddable.Analytic;
import com.jing.common.embeddable.GeoCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AirportResponse {
    private Long id;
    private String iataCode;
    private String airportName;
    private String detailedName;
    private ZoneId timeZone;
    private Address address;
    private GeoCode geoCode;
    private CityResponse city;
    private Analytic analytic;
}

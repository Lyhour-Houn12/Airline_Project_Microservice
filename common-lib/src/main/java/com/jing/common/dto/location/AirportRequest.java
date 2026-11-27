package com.jing.common.dto.location;

import com.jing.common.embeddable.Address;
import com.jing.common.embeddable.GeoCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportRequest {

    @NotBlank(message = "IATA code is required")
    @Size(min = 3, max = 3, message = "IATA code must be exactly 3 characters")
    private String iataCode;

    @NotBlank(message = "Airport name is required")
    private String airportName;

    private ZoneId timeZone;

    @Valid
    private Address address;

    @NotNull(message = "City ID is required")
    private Long cityId;

    @Valid
    private GeoCode geoCode;
}

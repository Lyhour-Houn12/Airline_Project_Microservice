package com.jing.common.dto.airline;


import com.jing.common.enumeration.AirlineStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AirlineRequest {
    @NotBlank
    @Size(min = 2, max = 2, message = "IATA code must be exactly 2 characters")
    private String iataCode;

    @NotBlank
    @Size(min = 3, max = 3, message = "ICAO code must be exactly 3 characters")
    private String icaoCode;

    @NotBlank
    private String airlineName;

    private String alias;

    @NotBlank
    private String country;

    private String logoUrl;

    private String website;

    private AirlineStatus status;

    private String alliance;

    private Long headquartersCityId;

    private String supportEmail;
    private String supportPhone;
    private String supportHours;

}

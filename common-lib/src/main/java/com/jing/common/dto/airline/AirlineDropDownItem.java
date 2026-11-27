package com.jing.common.dto.airline;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AirlineDropDownItem {
    private Long id;
    private String airlineName;
    private String iataCode;
    private String icaoCode;
    private String logoUrl;
    private String country;
}

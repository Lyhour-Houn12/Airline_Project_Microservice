package com.jing.common.dto.airline;

import com.jing.common.embeddable.Support;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.dto.user.UserDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineResponse {

    private Long id;

    private String iataCode;
    private String icaoCode;

    private String airlineName;
    private String alias;
    private String country;

    private String logoUrl;
    private String website;

    private AirlineStatus status;
    private String alliance;


    private Long headquartersCityId;

    private Long ownerId;
    private UserDto owner;
    private Long updatedById;

    //    private CityResponse headquartersCity;
    private Support support;
}

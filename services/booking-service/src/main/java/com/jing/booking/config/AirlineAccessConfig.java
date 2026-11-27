package com.jing.booking.config;

import com.jing.common.airline.AirlineAccess;
import com.jing.common.airline.AirlineCoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Lets airline owners see the bookings on their flights (see common-lib's AirlineAccess). */
@Configuration
public class AirlineAccessConfig {

    @Bean
    public AirlineAccess airlineAccess(AirlineCoreClient airlineCoreClient) {
        return new AirlineAccess(airlineCoreClient);
    }
}

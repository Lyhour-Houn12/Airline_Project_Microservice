package com.jing.booking.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * booking.* settings in application.yaml.
 *
 * @param holdMinutes        how long an unpaid booking (and its held seats) lives
 * @param childFarePercent   share of the fare a CHILD pays
 * @param infantFarePercent  share of the fare an INFANT (no own seat) pays
 * @param expiryCheckMillis  how often the expiry job looks for unpaid bookings
 */
@ConfigurationProperties(prefix = "booking")
public record BookingProperties(
        @DefaultValue("15") long holdMinutes,
        @DefaultValue("100") int childFarePercent,
        @DefaultValue("10") int infantFarePercent,
        @DefaultValue("60000") long expiryCheckMillis
) {
}

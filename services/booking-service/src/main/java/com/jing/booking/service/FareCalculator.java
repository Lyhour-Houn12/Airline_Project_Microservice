package com.jing.booking.service;

import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.enumeration.PassengerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Prices a passenger on a segment from the segment's fare snapshot.
 *
 * <p>Adults pay the full fare. Children and infants pay a configurable share of every part of it
 * (booking.child-fare-percent, booking.infant-fare-percent). The seat surcharge is added on top.
 */
@Component
@RequiredArgsConstructor
public class FareCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BookingProperties properties;

    /**
     * Copies the price of one adult into the segment. When pricing-service has an override price
     * ({@code currentPrice}), the base fare is adjusted so that base + taxes + fees equals it.
     */
    public static void snapshot(BookingSegment segment, FareResponse fare) {
        BigDecimal taxes = orZero(fare.getTaxesAndFees());
        BigDecimal fees = orZero(fare.getAirlineFees());
        BigDecimal base = orZero(fare.getBaseFare());
        if (fare.getCurrentPrice() != null) {
            base = fare.getCurrentPrice().subtract(taxes).subtract(fees).max(BigDecimal.ZERO);
        }
        segment.setBaseFare(base);
        segment.setTaxesAndFees(taxes);
        segment.setAirlineFees(fees);
    }

    public void price(PassengerSegment ps) {
        BookingSegment s = ps.getSegment();
        int percent = percentFor(ps.getPassenger().getPassengerType());
        ps.setBaseFare(share(s.getBaseFare(), percent));
        ps.setTaxesAndFees(share(s.getTaxesAndFees(), percent));
        ps.setAirlineFees(share(s.getAirlineFees(), percent));
    }

    int percentFor(PassengerType type) {
        return switch (type) {
            case ADULT -> 100;
            case CHILD -> properties.childFarePercent();
            case INFANT -> properties.infantFarePercent();
        };
    }

    private static BigDecimal share(BigDecimal amount, int percent) {
        return orZero(amount).multiply(BigDecimal.valueOf(percent))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** What one passenger of this type pays for this fare (before any seat surcharge). */
    public BigDecimal quote(FareResponse fare, PassengerType type) {
        BookingSegment s = BookingSegment.builder().build();
        snapshot(s, fare);
        int percent = percentFor(type);
        return share(s.getBaseFare(), percent).add(share(s.getTaxesAndFees(), percent)).add(share(s.getAirlineFees(), percent));
    }
}

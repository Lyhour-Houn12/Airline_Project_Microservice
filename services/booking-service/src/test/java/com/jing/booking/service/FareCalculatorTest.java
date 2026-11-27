package com.jing.booking.service;

import com.jing.booking.model.BookingSegment;
import com.jing.booking.model.Passenger;
import com.jing.booking.model.PassengerSegment;
import com.jing.common.dto.pricing.FareResponse;
import com.jing.common.enumeration.PassengerType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator(new BookingProperties(15, 75, 10, 60000));

    private static BookingSegment segment(String base, String taxes, String fees, String override) {
        FareResponse fare = FareResponse.builder()
                .baseFare(new BigDecimal(base)).taxesAndFees(new BigDecimal(taxes))
                .airlineFees(new BigDecimal(fees))
                .currentPrice(override == null ? null : new BigDecimal(override))
                .build();
        BookingSegment s = BookingSegment.builder().build();
        FareCalculator.snapshot(s, fare);
        return s;
    }

    private PassengerSegment priced(BookingSegment s, PassengerType type) {
        PassengerSegment ps = PassengerSegment.builder()
                .segment(s).passenger(Passenger.builder().passengerType(type).build()).build();
        calculator.price(ps);
        return ps;
    }

    @Test
    void adultPaysTheFullFare() {
        PassengerSegment ps = priced(segment("100.00", "20.00", "5.00", null), PassengerType.ADULT);

        assertThat(ps.getTotalPrice()).isEqualByComparingTo("125.00");
    }

    @Test
    void childAndInfantPayTheirShare() {
        BookingSegment s = segment("100.00", "20.00", "5.00", null);

        assertThat(priced(s, PassengerType.CHILD).getTotalPrice()).isEqualByComparingTo("93.75");   // 75 %
        assertThat(priced(s, PassengerType.INFANT).getTotalPrice()).isEqualByComparingTo("12.50");  // 10 %
    }

    @Test
    void overridePriceIsWhatTheAdultPays() {
        BookingSegment s = segment("100.00", "20.00", "5.00", "99.00");   // promo price

        assertThat(s.getBaseFare()).isEqualByComparingTo("74.00");      // 99 - 20 - 5
        assertThat(priced(s, PassengerType.ADULT).getTotalPrice()).isEqualByComparingTo("99.00");
    }

    @Test
    void seatSurchargeIsAddedOnTop() {
        PassengerSegment ps = priced(segment("100.00", "20.00", "5.00", null), PassengerType.ADULT);
        ps.setSeatSurcharge(new BigDecimal("15.00"));

        assertThat(ps.getTotalPrice()).isEqualByComparingTo("140.00");
    }
}

package com.jing.seat.model;

import com.jing.common.enumeration.SeatAvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatInstanceHoldTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 10, 0);

    private static SeatInstance seat() {
        return SeatInstance.builder().id(1L).flightInstanceId(9L).build();   // AVAILABLE
    }

    @Test
    void holdThenConfirm() {
        SeatInstance s = seat();
        s.hold("ABC123", NOW.plusMinutes(15), NOW);

        assertThat(s.getStatus()).isEqualTo(SeatAvailabilityStatus.HELD);
        assertThat(s.confirm("ABC123")).isTrue();
        assertThat(s.getStatus()).isEqualTo(SeatAvailabilityStatus.BOOKED);
        assertThat(s.confirm("ABC123")).isTrue();            // repeat is harmless
    }

    @Test
    void anotherBookingCannotTakeAnActiveHold() {
        SeatInstance s = seat();
        s.hold("ABC123", NOW.plusMinutes(15), NOW);

        assertThatThrownBy(() -> s.hold("XYZ789", NOW.plusMinutes(15), NOW.plusMinutes(5)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(s.confirm("XYZ789")).isFalse();
    }

    @Test
    void aLapsedHoldCanBeTakenOver() {
        SeatInstance s = seat();
        s.hold("ABC123", NOW.plusMinutes(15), NOW);

        s.hold("XYZ789", NOW.plusMinutes(40), NOW.plusMinutes(20));   // ABC123 never paid

        assertThat(s.getHoldReference()).isEqualTo("XYZ789");
        assertThat(s.confirm("ABC123")).isFalse();                   // the late payer lost the seat
    }

    @Test
    void releaseOnlyTouchesYourOwnSeat() {
        SeatInstance s = seat();
        s.hold("ABC123", NOW.plusMinutes(15), NOW);

        assertThat(s.release("XYZ789")).isNull();
        assertThat(s.getStatus()).isEqualTo(SeatAvailabilityStatus.HELD);

        assertThat(s.release("ABC123")).isEqualTo(SeatAvailabilityStatus.HELD);
        assertThat(s.getStatus()).isEqualTo(SeatAvailabilityStatus.AVAILABLE);
        assertThat(s.getHoldReference()).isNull();
    }

    @Test
    void blockedSeatCannotBeHeld() {
        SeatInstance s = seat();
        s.setStatus(SeatAvailabilityStatus.BLOCKED);

        assertThatThrownBy(() -> s.hold("ABC123", NOW.plusMinutes(15), NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}

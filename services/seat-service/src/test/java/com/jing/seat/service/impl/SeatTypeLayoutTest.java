package com.jing.seat.service.impl;

import com.jing.common.enumeration.SeatType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static com.jing.common.enumeration.SeatType.AISLE;
import static com.jing.common.enumeration.SeatType.MIDDLE;
import static com.jing.common.enumeration.SeatType.WINDOW;
import static org.assertj.core.api.Assertions.assertThat;

class SeatTypeLayoutTest {

    private static List<SeatType> row(int left, int right) {
        return IntStream.range(0, left + right).mapToObj(i -> SeatServiceImpl.typeFor(i, left, right)).toList();
    }

    @Test
    void symmetricLayout() {          // A B C | D E F
        assertThat(row(3, 3)).containsExactly(WINDOW, MIDDLE, AISLE, AISLE, MIDDLE, WINDOW);
    }

    @Test
    void asymmetricLayout() {         // A B | C D E  (was MIDDLE for B before the fix)
        assertThat(row(2, 3)).containsExactly(WINDOW, AISLE, AISLE, MIDDLE, WINDOW);
    }

    @Test
    void narrowLayout() {             // A B | C D
        assertThat(row(2, 2)).containsExactly(WINDOW, AISLE, AISLE, WINDOW);
    }
}

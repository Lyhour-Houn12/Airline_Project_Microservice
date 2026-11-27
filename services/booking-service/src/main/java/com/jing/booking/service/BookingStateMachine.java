package com.jing.booking.service;


import com.jing.common.enumeration.BookingStatus;
import com.jing.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED.put(BookingStatus.PENDING, EnumSet.of(BookingStatus.COMPLETED, BookingStatus.CONFIRMED,  BookingStatus.CANCELLED, BookingStatus.EXPIRED));
        ALLOWED.put(BookingStatus.CONFIRMED, EnumSet.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED));
        ALLOWED.put(BookingStatus.CANCELLED, EnumSet.of(BookingStatus.REFUNDED));
        ALLOWED.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED.put(BookingStatus.REFUNDED, EnumSet.noneOf(BookingStatus.class));
    }

    public boolean canTransition(BookingStatus from, BookingStatus to){
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertTransition(BookingStatus from, BookingStatus to){
        if(!canTransition(from, to)){
            throw new BadRequestException("Illegal booking status change: " + from + " -> " + to );
        }
    }
}

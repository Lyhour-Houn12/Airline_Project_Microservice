package com.jing.booking.payload.request;

import com.jing.common.enumeration.BookingStatus;

import java.time.LocalDate;

public record BookingFilter(
        Long userId,                 // honoured for admins only
        BookingStatus status,
        String bookingReference,
        LocalDate createdFrom,
        LocalDate createdTo) {}

package com.jing.booking.payload.response;

import com.jing.common.enumeration.BookingStatus;

import java.time.LocalDateTime;

public record BookingStatusHistoryResponse(
        Long id,
        BookingStatus fromStatus,
        BookingStatus toStatus,
        String changedBy,
        String reason,
        LocalDateTime changedAt) {}
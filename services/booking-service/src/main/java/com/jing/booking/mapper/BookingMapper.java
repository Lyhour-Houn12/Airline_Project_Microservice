package com.jing.booking.mapper;

import com.jing.booking.model.Booking;
import com.jing.booking.model.BookingStatusHistory;
import com.jing.booking.payload.response.BookingResponse;
import com.jing.booking.payload.response.BookingStatusHistoryResponse;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingResponse toResponse(Booking b){
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getUserId(),
                b.getTripType(),
                b.getStatus(),
                b.getTotalAmount(),
                b.getCurrency(),
                b.isFlexibleTicket(),
                b.getExpiresAt(),
                b.getTicketTimeLimit(),
                b.getPaymentId(),
                b.getContactInfo(),
                b.getSegments().size(),
                b.getPassengers().size(),
                b.getBookingDate(),
                b.getLastModified()
        );
    }

    public static BookingStatusHistoryResponse toHistoryResponse(BookingStatusHistory h){
        return new BookingStatusHistoryResponse(
            h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getReason(), h.getChangedAt()
        );
    }
}

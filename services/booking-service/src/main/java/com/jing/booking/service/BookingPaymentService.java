package com.jing.booking.service;

import com.jing.booking.model.Booking;
import com.jing.booking.repository.BookingRepository;
import com.jing.booking.validatation.BookingSegmentValidator;
import com.jing.common.dto.booking.BookingPaymentInfo;
import com.jing.common.dto.booking.PaymentResultRequest;
import com.jing.common.dto.booking.PaymentResultResponse;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PaymentStatus;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** The booking side of paying: what to charge, and what to do once the money arrived. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingSegmentValidator segmentValidator;
    private final BookingLifecycle lifecycle;

    /** What payment-service may charge. Rejected (400) unless the booking is ready to be paid. */
    @Transactional(readOnly = true)
    public BookingPaymentInfo paymentInfo(Long bookingId) {
        Booking booking = find(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking " + booking.getBookingReference() + " is " + booking.getStatus()
                    + " and cannot be paid");
        }
        if (booking.isExpired(LocalDateTime.now())) {
            throw new BadRequestException("Booking " + booking.getBookingReference() + " has expired");
        }
        if (booking.getPassengers().isEmpty()) {
            throw new BadRequestException("Add at least one passenger before paying");
        }
        segmentValidator.validateComplete(booking);   // has flights, round trip has its return...
        if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Booking " + booking.getBookingReference() + " has nothing to pay");
        }
        return new BookingPaymentInfo(booking.getId(), booking.getBookingReference(), booking.getUserId(),
                booking.getStatus(), booking.getTotalAmount(), booking.getCurrency(), booking.getExpiresAt());
    }

    /**
     * Idempotent: payment-service may send the same result more than once (callback + status sync).
     * Only SUCCESS changes the booking; a failed or cancelled payment leaves it PENDING so the
     * customer can try again until it expires.
     */
    @Transactional
    public PaymentResultResponse applyPaymentResult(Long bookingId, PaymentResultRequest result) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (result.status() == PaymentStatus.REFUNDED) {
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                booking.changeStatus(BookingStatus.REFUNDED, "PAYMENT:" + result.paymentId(), "Payment refunded");
                return response(booking, false, "Booking refunded");
            }
            return response(booking, false, "Refund noted; booking stays " + booking.getStatus());
        }
        if (result.status() != PaymentStatus.SUCCESS) {
            return response(booking, false, "Payment " + result.paymentId() + " is " + result.status()
                    + "; booking unchanged");
        }

        return switch (booking.getStatus()) {
            case PENDING -> {
                // paid, even if the hold window passed a moment ago: the money is here, honour it
                lifecycle.confirm(booking, result.paymentId(), "PAYMENT:" + result.paymentId());
                log.info("Booking {} confirmed by payment {}", booking.getBookingReference(), result.paymentId());
                yield response(booking, false, "Booking confirmed");
            }
            case CONFIRMED, COMPLETED -> {
                if (Objects.equals(booking.getPaymentId(), result.paymentId())) {
                    lifecycle.confirmHeldSeats(booking);   // retry seats that could not be booked last time
                    yield response(booking, false, "Already confirmed by this payment");
                }
                log.error("Booking {} was already paid by payment {}; payment {} must be refunded",
                        booking.getBookingReference(), booking.getPaymentId(), result.paymentId());
                yield response(booking, true, "Booking was already paid by another payment");
            }
            default -> {
                log.error("Payment {} arrived for booking {} which is {}; it must be refunded",
                        result.paymentId(), booking.getBookingReference(), booking.getStatus());
                yield response(booking, true, "Booking is " + booking.getStatus() + "; refund required");
            }
        };
    }

    private Booking find(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }

    private static PaymentResultResponse response(Booking b, boolean refund, String message) {
        return new PaymentResultResponse(b.getId(), b.getStatus(), refund, message);
    }
}

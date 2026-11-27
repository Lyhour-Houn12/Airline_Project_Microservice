package com.jing.payment.payload.request;

import com.jing.common.enumeration.PaymentStatus;

public record PaymentStatusChangedEvent(Long paymentId, Long bookingId,
                                        String bookingReference, PaymentStatus status) {}
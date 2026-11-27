package com.jing.payment.repository;

import com.jing.payment.model.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    List<PaymentRefund> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    List<PaymentRefund> findTop50ByStatusOrderByCreatedAtAsc(PaymentRefund.Status status);

    /** Money already promised back for a payment: refunds in the given statuses. */
    @Query("""
        select coalesce(sum(r.amount), 0) from PaymentRefund r
        where r.paymentId = :paymentId and r.status in :statuses
        """)
    BigDecimal sumByStatuses(@Param("paymentId") Long paymentId,
                             @Param("statuses") Collection<PaymentRefund.Status> statuses);
}

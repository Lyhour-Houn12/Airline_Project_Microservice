package com.jing.pricing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "airline_id")
    private Long airlineId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    private Fare fare;

    @Builder.Default
    @Column(name = "is_refundable", nullable = false)
    private Boolean isRefundable = false;

    @Builder.Default
    @Column(name = "is_changeable", nullable = false)
    private Boolean isChangeable = false;

    @Column(name = "change_fee", precision = 12, scale = 2)
    private BigDecimal changeFee;

    @Column(name = "cancellation_fee", precision = 12, scale = 2)
    private BigDecimal cancellationFee;

    @Column(name = "refundable_deadline_days")
    private Integer refundableDeadlineDays;

    @Column(name = "change_deadline_hours")
    private Integer changeDeadlineHours;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
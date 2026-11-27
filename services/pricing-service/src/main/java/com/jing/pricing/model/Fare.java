package com.jing.pricing.model;

import com.jing.common.embeddable.BoardingBenefits;
import com.jing.common.embeddable.InFlightBenefits;
import com.jing.common.embeddable.PremiumServiceBenefits;
import com.jing.common.embeddable.SeatBenefits;
import com.jing.common.enumeration.CabinClassType;
import com.jing.common.embeddable.FlexibilityBenefits;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * totalPrice   = baseFare + taxesAndFees + airlineFees (derived, never stored)
 * currentPrice = optional override (promo / dynamic price). null means "use totalPrice".
 */
@Entity
@Table(
        name = "fares",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fare_flight_cabin_name",
                columnNames = {"flight_id", "cabin_class_id", "fare_name"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fare_name", nullable = false)
    private String fareName;

    @Column(name = "rbd_code")
    private Character rbdCode;

    // Cross-service refs (plain ids)
    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "cabin_class_id", nullable = false)
    private Long cabinClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class_type", nullable = false)
    private CabinClassType cabinClass;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "taxes_and_fees", precision = 12, scale = 2)
    private BigDecimal taxesAndFees;

    @Column(name = "airline_fees", precision = 12, scale = 2)
    private BigDecimal airlineFees;

    @Column(name = "current_price", precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "fare_label", length = 100)
    private String fareLabel;

    @OneToOne(mappedBy = "fare", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private FareRule fareRule;

    @OneToOne(mappedBy = "fare", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private BaggagePolicy baggagePolicy;

    @Embedded
    @Builder.Default
    private SeatBenefits seatBenefits = new SeatBenefits();

    @Embedded
    @Builder.Default
    private BoardingBenefits boardingBenefits = new BoardingBenefits();

    @Embedded
    @Builder.Default
    private InFlightBenefits inFlightBenefits = new InFlightBenefits();

    @Embedded
    @Builder.Default
    private FlexibilityBenefits flexibilityBenefits = new FlexibilityBenefits();

    @Embedded
    @Builder.Default
    private PremiumServiceBenefits premiumServiceBenefits = new PremiumServiceBenefits();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public BigDecimal getTotalPrice() {
        BigDecimal total = baseFare != null ? baseFare : BigDecimal.ZERO;
        if (taxesAndFees != null) total = total.add(taxesAndFees);
        if (airlineFees != null) total = total.add(airlineFees);
        return total;
    }

    /** The price a customer actually pays: the override if set, otherwise the total. */
    @Transient
    public BigDecimal getEffectivePrice() {
        return currentPrice != null ? currentPrice : getTotalPrice();
    }
}
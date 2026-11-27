package com.jing.ancillary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "flight_cabin_ancillaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fca_cabin_ancillary",
                columnNames = {"flight_instance_cabin_id", "ancillary_id"}),
        indexes = {
                @Index(name = "idx_fca_cabin", columnList = "flight_instance_cabin_id"),
                @Index(name = "idx_fca_ancillary", columnList = "ancillary_id")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightCabinAncillary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // flight instance cabin lives in another service, so only the id is stored
    @Column(name = "flight_instance_cabin_id", nullable = false, updatable = false)
    private Long flightInstanceCabinId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ancillary_id", nullable = false, updatable = false)
    private Ancillary ancillary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // always copied from the ancillary, so price and currency never disagree
    @Column(nullable = false, length = 3)
    private String currency;


    // included in the fare: price is always 0
    @Builder.Default
    @Column(name = "is_free", nullable = false)
    private Boolean isFree = false;

    // null = unlimited
    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Builder.Default
    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}

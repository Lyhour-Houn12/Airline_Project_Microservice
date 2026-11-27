package com.jing.ancillary.model;


import com.jing.common.enumeration.AncillaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ancillaries",
uniqueConstraints = @UniqueConstraint(
        name = "uk_ancillary_airline_code",
        columnNames = {"airline_id", "code"}),   // a code is unique within one airline
        indexes = {
        @Index(name = "idx_ancillary_airline", columnList = "airline_id"),
        @Index(name = "idx_ancillary_type", columnList = "type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ancillary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // airline lives in airline-core-service, so only the id is stored
    @Column(name = "airline_id", nullable = false, updatable = false)
    private Long airlineId;

    @Column(nullable = false, updatable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AncillaryType type;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency;

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

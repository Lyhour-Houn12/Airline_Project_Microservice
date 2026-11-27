package com.jing.ancillary.model;


import com.jing.common.enumeration.CoverageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "insurance_coverages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insurance_airline_code",
                columnNames = {"airline_id", "code"}),
        indexes = {
                @Index(name = "idx_insurance_airline", columnList = "airline_id"),
                @Index(name = "idx_insurance_type", columnList = "coverage_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "airline_id", nullable = false, updatable = false)
    private Long airlineId;

    @Column(nullable = false, updatable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 150)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type", nullable = false, length = 30)
    private CoverageType coverageType;


    @Column(name = "coverage_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverageAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal premium;

    @Column(nullable = false, length = 3)
    private String currency;

    // null = no lower / upper age limit
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "terms_url", length = 500)
    private String termsUrl;

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

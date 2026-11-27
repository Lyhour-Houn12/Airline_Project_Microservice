package com.jing.ancillary.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_instance_meals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fim_instance_meal",
                columnNames = {"flight_instance_id", "meal_id"}),
        indexes = {
                @Index(name = "idx_fim_flight_instance", columnList = "flight_instance_id"),
                @Index(name = "idx_fim_meal", columnList = "meal_id")
        })
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlightInstanceMeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // flight instance lives in flight-service, so only the id is stored
    @Column(name = "flight_instance_id", nullable = false, updatable = false)
    private Long flightInstanceId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false, updatable = false)
    private Meal meal;


    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // always copied from the meal, so price and currency never disagree
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Builder.Default
    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "order_cutoff_hours", nullable = false)
    private Integer orderCutoffHours;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

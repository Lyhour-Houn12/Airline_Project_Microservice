package com.jing.ancillary.model;


import com.jing.common.enumeration.DietaryType;
import com.jing.common.enumeration.MealType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meal_airline_code",
                columnNames = {"airline_id", "code"}),
        indexes = {
                @Index(name = "idx_meal_airline", columnList = "airline_id"),
                @Index(name = "idx_meal_type", columnList = "meal_type"),
                @Index(name = "idx_meal_dietary", columnList = "dietary_type")
        })
@Getter
@Setter
@Service
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Meal {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", nullable = false, length = 20)
    private DietaryType dietaryType;

    @Column(length = 100)
    private String cuisine;

    private Integer calories;

    @Column(length = 255)
    private String allergens;

    // what the frontend displays
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // what the backend needs to replace/delete the file in Cloudinary
    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;

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

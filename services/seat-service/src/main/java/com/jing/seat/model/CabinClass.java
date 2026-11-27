package com.jing.seat.model;

import com.jing.common.enumeration.CabinClassType;
import com.jing.common.enumeration.SeatType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cabin_classes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cabin_class_aircraft_code",
                        columnNames = {"aircraft_id", "code"}
                )
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CabinClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClassType cabinClassType;

    @NotBlank
    @Size(max = 5)
    @Column(nullable = false, length = 5)
    private String code;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    // Cross-service reference: Aircraft belongs to airline-core-service.
    @Column(name = "aircraft_id", nullable = false)
    private Long aircraftId;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBookable = true;

    private Integer typicalSeatPitch;

    private Integer typicalSeatWidth;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
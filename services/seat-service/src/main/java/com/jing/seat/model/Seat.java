package com.jing.seat.model;

import com.jing.common.enumeration.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    @Column(name = "column_letter", nullable = false, length = 1)
    private Character columnLetter;

    @Column(name = "aircraft_id", nullable = false, updatable = false)
    private Long aircraftId;

    @Builder.Default
    private Boolean isAvailable = true;

    @Builder.Default
    private Boolean isBlocked = false;

    @Builder.Default
    private Boolean isEmergencyExit = false;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean hasExtraRoom = false;
    @Builder.Default
    private Boolean hasBassinet = false;

    @Builder.Default
    @Column(name = "is_near_lavatory", nullable = false)
    private Boolean isNearLavatory = false;

    @Builder.Default
    @Column(name = "is_near_galley", nullable = false)
    private Boolean isNearGalley = false;

    @Builder.Default
    @Column(name = "has_power_outlet", nullable = false)
    private Boolean hasPowerOutlet = false;

    @Builder.Default
    @Column(name = "has_tv_screen", nullable = false)
    private Boolean hasTvScreen = false;

    @Builder.Default
    @Column(name = "is_wheelchair_accessible", nullable = false)
    private Boolean isWheelchairAccessible = false;

    @Builder.Default
    @Column(name = "has_extra_width", nullable = false)
    private Boolean hasExtraWidth = false;

    private Integer seatPitch;
    private Integer seatWidth;
    private Integer reclineAngle;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_map_id", nullable = false)
    private SeatMap seatMap;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



}

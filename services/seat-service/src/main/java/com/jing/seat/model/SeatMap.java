package com.jing.seat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_maps",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_map_seat_number",
                columnNames = {"seat_map_id", "seat_number"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Min(1)
    @Column(nullable = false)
    private int totalRows;

    @Min(1)
    @Column(name = "right_seats_per_row", nullable = false)
    private int rightSeatsPerRow;

    @Min(1)
    @Column(name = "left_seats_per_row", nullable = false)
    private int leftSeatsPerRow;

    @NotNull
    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabin_class_id", nullable = false)
    private CabinClass cabinClass;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
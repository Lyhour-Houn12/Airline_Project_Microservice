package com.jing.pricing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "baggage_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggagePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    private Fare fare;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // Cabin baggage (weights in kg, dimension in cm = length + width + height)
    private Double cabinBaggageMaxWeight;

    @Builder.Default
    private Integer cabinBaggagePieces = 1;

    private Double cabinBaggageWeightPerPiece;
    private Double cabinBaggageMaxDimension;

    // Check-in baggage (kg)
    private Double checkInBaggageMaxWeight;

    @Builder.Default
    private Integer checkInBaggagePieces = 0;

    private Double checkInBaggageWeightPerPiece;

    @Builder.Default
    @Column(nullable = false)
    private Boolean priorityBaggage = false;

    @Builder.Default
    @Column(name = "extra_baggage_purchasable", nullable = false)
    private Boolean extraBaggagePurchasable = false;

    // always copied from the fare, never from the request
    @Column(name = "airline_id")
    private Long airlineId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
package com.jing.airline.model;


import com.jing.common.embeddable.Support;
import com.jing.common.enumeration.AirlineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "airlines")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iata_code", length = 2, nullable = false, unique = true)
    @NotBlank @Size(min = 2, max = 2, message = "IATA code must be exactly 2 characters")
    private String iataCode;

    @Column(name = "icao_code", length = 3, nullable = false, unique = true)
    @NotBlank
    @Size(min = 3, max = 3, message = "ICAO code must be exactly 3 characters")
    private String icaoCode;

    @Column(name = "airline_name", nullable = false)
    private String airlineName;

    private String alias;

    @Column(nullable = false)
    private String country;

    private String logoUrl;

    private String website;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AirlineStatus status = AirlineStatus.ACTIVE;


    private String alliance;

    @Embedded
    private Support support;

    @Column(name = "headquarters_city_id")
    private Long headquartersCityId;

    @Column(name = "owner_id", updatable = false, nullable = false, unique = true)
    private Long ownerId;


    @Column(name = "updated_by_user_id")
    private Long updatedById;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



}

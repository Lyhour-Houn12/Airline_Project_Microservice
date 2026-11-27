package com.jing.location.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jing.common.embeddable.Address;
import com.jing.common.embeddable.Analytic;
import com.jing.common.embeddable.GeoCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "airports", indexes = {
        @Index(name = "idx_airport_iata", columnList = "iataCode"),
        @Index(name = "idx_airport_city_id", columnList = "city_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "city"})
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 3, unique = true)
    @EqualsAndHashCode.Include
    private String iataCode;

    @Column(nullable = false, length = 255)
    private String airportName;

    @Column(name = "time_zone_id", length = 50)
    private String timeZoneId;

    @Embedded
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    @JsonIgnore
    private City city;

    @Embedded
    private GeoCode geoCode;

    @Embedded
    private Analytic analytic;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @JsonIgnore
    public ZoneId getTimeZone() {
        return timeZoneId != null ? ZoneId.of(timeZoneId) : null;
    }
    public void setTimeZone(ZoneId zoneId) {
        this.timeZoneId = zoneId != null ? zoneId.getId() : null;
    }

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDetailedName() {
        if (city != null && city.getCountryCode() != null) {
            return airportName.toUpperCase() + "/" + city.getCountryCode();
        }
        return airportName.toUpperCase();
    }








}

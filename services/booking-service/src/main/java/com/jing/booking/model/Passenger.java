package com.jing.booking.model;


import com.jing.common.enumeration.Gender;
import com.jing.common.enumeration.PassengerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passengers", indexes = @Index(columnList = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassengerType passengerType;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Email
    private String email;

    private String phone;

    private String passportNumber;

    private LocalDate passportExpiry;

    private String passportIssuingCountry; // ISO 3166 alpha-2/3

    private String nationality;    // ISO 3166

    private String frequentFlyerNumber;

    // Infants have no seat and travel with an adult on the same booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accompanying_adult_id")
    private Passenger accompanyingAdult;

    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PassengerSegment> passengerSegments = new ArrayList<>();

    @Builder.Default
    private boolean wheelchairAssistance = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void normalize(){
        if (email != null) email = email.trim().toLowerCase();
        if (firstName != null) firstName = firstName.trim();
        if (lastName != null) lastName = lastName.trim();
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }

    public int ageOn(LocalDate date){
        return Period.between(dateOfBirth, date).getYears();
    }
    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Passenger p && id != null && id.equals(p.id));
    }
    @Override public int hashCode() { return getClass().hashCode(); }


}

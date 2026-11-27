package com.jing.booking.model;


import com.jing.common.embeddable.ContactInfo;
import com.jing.common.enumeration.BookingStatus;
import com.jing.common.enumeration.PassengerSegmentStatus;
import com.jing.common.enumeration.TripType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_status_expires", columnList = "status, expires_at")
})@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String bookingReference;   // PNR, e.g. "K7M2QX"

    // Cross-service ref: User (user-service)
    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripType tripType = TripType.ONE_WAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    private boolean flexibleTicket;

    // Seat-hold payment window: the expiry scheduler uses this
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Airline "must ticket by" deadlines
    private LocalDateTime ticketTimeLimit;

    // cross-service preference: (payment-service)
    private Long paymentId;

    // Client-supplied key to make create idempotent
    @Column(unique = true, length = 64)
    private String idempotencyKey;

    @Embedded
    private ContactInfo contactInfo;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("segmentOrder ASC")
    @Builder.Default
    private List<BookingSegment> segments = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Passenger> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<BookingStatusHistory> history = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime bookingDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    @Version
    private Long version;

    public void addSegment(BookingSegment s){
        segments.add(s);
        s.setBooking(this);
    }

    public void addPassenger(Passenger p){
        passengers.add(p);
        p.setBooking(this);
    }

    public void changeStatus(BookingStatus next, String changedBy, String reason){
        this.history.add(BookingStatusHistory.builder()
                        .booking(this).fromStatus(this.status).toStatus(next).changedBy(changedBy).reason(reason)
                .build());
        this.status = next;
    }

    public void recordCreation(String changedBy, String reason){
        this.history.add(BookingStatusHistory.builder()
                .booking(this).fromStatus(null).toStatus(this.status)
                .changedBy(changedBy).reason(reason).build());
    }

    public PassengerSegment link(Passenger p, BookingSegment s){
        PassengerSegment ps = PassengerSegment.builder()
                .passenger(p)
                .segment(s)
                .build();
            p.getPassengerSegments().add(ps);
            s.getPassengerSegments().add(ps);
        return ps;
    }

    public void removeSegment(BookingSegment s){
        s.getPassengerSegments().forEach(ps -> ps.getPassenger().getPassengerSegments().remove(ps));
        segments.remove(s);
    }

    public void removePassenger(Passenger p) {
        p.getPassengerSegments().forEach(ps -> ps.getSegment().getPassengerSegments().remove(ps));
        passengers.remove(p);
    }

    public void recalculateTotal(){
        this.totalAmount = segments.stream()
                .flatMap(s -> s.getPassengerSegments().stream())
                .filter(ps -> ps.getStatus() != PassengerSegmentStatus.CANCELLED)
                .map(PassengerSegment::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }




    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Booking b && id != null && id.equals(b.id));
    }
    @Override public int hashCode() { return getClass().hashCode(); }





}

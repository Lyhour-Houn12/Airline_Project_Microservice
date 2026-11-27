package com.jing.booking.model;

import com.jing.common.enumeration.BookingItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_segment_id")
    private PassengerSegment passengerSegment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingItemType itemType;

    // Cross-service ref: FlightInstanceMeal id or FlightCabinAncillary id (ancillary-service)
    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false)
    private String name;                         // snapshot

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;                // snapshot

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

}

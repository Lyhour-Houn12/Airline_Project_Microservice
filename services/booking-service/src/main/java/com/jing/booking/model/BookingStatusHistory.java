package com.jing.booking.model;

import com.jing.common.enumeration.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private BookingStatus toStatus;

    private String changedBy;                    // user id or "SYSTEM"
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime changedAt;



}

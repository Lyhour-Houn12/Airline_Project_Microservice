package com.jing.booking.repository;

import com.jing.booking.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepostory extends JpaRepository<Passenger, Long> {
}

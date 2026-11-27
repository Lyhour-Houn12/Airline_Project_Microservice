package com.jing.seat.repository;

import com.jing.seat.model.CabinReservation;
import com.jing.seat.model.FlightInstanceCabin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CabinReservationRepository extends JpaRepository<CabinReservation, Long> {

    Optional<CabinReservation> findByFlightInstanceCabinAndHoldReference(FlightInstanceCabin cabin, String holdReference);

    List<CabinReservation> findByFlightInstanceCabinAndStatus(FlightInstanceCabin cabin, CabinReservation.Status status);

    List<CabinReservation> findByHoldReference(String holdReference);
}

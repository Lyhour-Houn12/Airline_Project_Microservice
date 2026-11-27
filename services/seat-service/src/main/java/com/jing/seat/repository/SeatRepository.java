package com.jing.seat.repository;

import com.jing.seat.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long>, JpaSpecificationExecutor<Seat> {
    boolean existsByAircraftIdAndSeatNumber(Long aircraftId, String seatNumber);

    List<Seat> findByAircraftIdAndSeatNumberIn(Long aircraftId, Collection<String> seatNumbers);

    long countBySeatMapId(Long seatMapId);

    @Query("""
        SELECT COUNT(s) FROM Seat s
                WHERE s.seatMap.cabinClass.id = :cabinClassId
                        and s.isActive = true
                        and s.isBlocked = false
        """)
    long countUsableByCabinClassId(@Param("cabinClassId") Long cabinClassId);


    @Query("""
       select s from Seat s
       where s.seatMap.cabinClass.id = :cabinClassId
         and s.isActive = true
         and s.isBlocked = false
       order by s.seatRow, s.seatNumber
       """)
    List<Seat> findGeneratableSeatsByCabinClassId(@Param("cabinClassId") Long cabinClassId);

    List<Seat> findBySeatMapId(Long seatMapId);
}

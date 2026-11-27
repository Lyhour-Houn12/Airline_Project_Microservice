package com.jing.seat.repository;

import com.jing.common.enumeration.SeatAvailabilityStatus;
import com.jing.seat.repository.projection.CabinStatusCount;
import com.jing.seat.model.SeatInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatInstanceRepository extends JpaRepository<SeatInstance, Long> {
    boolean existsByFlightInstanceId(Long flightInstanceId);

    long countByFlightInstanceId(Long flightInstanceId);

    @Query("""
        SELECT si FROM SeatInstance si
        JOIN FETCH si.seat s
        JOIN FETCH si.flightInstanceCabin fic
        JOIN FETCH fic.cabinClass
        WHERE fic.flightInstanceId = :flightInstanceId
        ORDER BY fic.id, s.seatRow, s.columnLetter
        """)
    List<SeatInstance> findAllForSeatMap(Long flightInstanceId);


    @Query("""
        SELECT new com.jing.seat.repository.projection.CabinStatusCount(
            si.flightInstanceCabin.id,
            si.status,
            COUNT(si)
        )
        FROM SeatInstance si
        WHERE si.flightInstanceId = :flightInstanceId
        GROUP BY si.flightInstanceCabin.id, si.status
        """)
    List<CabinStatusCount> countByCabinAndStatus(@Param("flightInstanceId") Long flightInstanceId);


    @Query("""
       SELECT si FROM SeatInstance si
       JOIN FETCH si.seat
       JOIN FETCH si.flightInstanceCabin fic
       JOIN FETCH fic.cabinClass
       WHERE si.id = :id
       """)
    Optional<SeatInstance> findDetailById(@Param("id") Long id);

    boolean existsByFlightInstanceIdAndStatusIn(Long flightInstanceId, Collection<SeatAvailabilityStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("delete from SeatInstance si where si.flightInstanceId = :flightInstanceId")
    int deleteAllByFlightInstanceId(@Param("flightInstanceId") Long flightInstanceId);

    @Query("""
       SELECT si FROM SeatInstance si
       JOIN FETCH si.seat
       JOIN FETCH si.flightInstanceCabin fic
       JOIN FETCH fic.cabinClass
       WHERE si.holdReference = :holdReference
       """)
    List<SeatInstance> findAllByHoldReference(@Param("holdReference") String holdReference);
}

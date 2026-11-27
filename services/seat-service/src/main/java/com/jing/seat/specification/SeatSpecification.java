package com.jing.seat.specification;

import com.jing.seat.filter.SeatFilter;
import com.jing.seat.model.Seat;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SeatSpecification {

    private SeatSpecification() {}

    public static Specification<Seat> toSpec(SeatFilter f){
        return ((root, query, cb) ->{
            List<Predicate> p = new ArrayList<>();
            if (f == null) return cb.conjunction();
            if (f.seatMapId() != null) p.add(cb.equal(root.get("seatMap").get("id"), f.seatMapId()));
            if (f.aircraftId() != null) p.add(cb.equal(root.get("aircraftId"), f.aircraftId()));
            if (f.seatType() != null) p.add(cb.equal(root.get("seatType"), f.seatType()));
            if (f.seatRow() != null) p.add(cb.equal(root.get("seatRow"), f.seatRow()));
            if (f.columnLetter() != null) p.add(cb.equal(root.get("columnLetter"), Character.toUpperCase(f.columnLetter())));
            if (f.isAvailable() != null) p.add(cb.equal(root.get("isAvailable"), f.isAvailable()));
            if (f.isBlocked() != null) p.add(cb.equal(root.get("isBlocked"), f.isBlocked()));
            if (f.isActive() != null) p.add(cb.equal(root.get("isActive"), f.isActive()));
            if (f.isEmergencyExit() != null) p.add(cb.equal(root.get("isEmergencyExit"), f.isEmergencyExit()));
            if (f.isWheelchairAccessible() != null)
                p.add(cb.equal(root.get("isWheelchairAccessible"), f.isWheelchairAccessible()));
            return cb.and(p.toArray(new Predicate[0]));
        });
    }
}

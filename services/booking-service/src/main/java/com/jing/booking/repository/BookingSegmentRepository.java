package com.jing.booking.repository;

import com.jing.booking.model.BookingSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingSegmentRepository extends JpaRepository<BookingSegment, Long>, JpaSpecificationExecutor<BookingSegment> {


}

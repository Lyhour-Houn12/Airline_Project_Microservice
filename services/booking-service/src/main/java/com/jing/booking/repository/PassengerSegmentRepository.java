package com.jing.booking.repository;

import com.jing.booking.model.PassengerSegment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PassengerSegmentRepository
        extends JpaRepository<PassengerSegment, Long>, JpaSpecificationExecutor<PassengerSegment> {

    @Override
    @EntityGraph(attributePaths = {"passenger", "segment"})
    Page<PassengerSegment> findAll(Specification<PassengerSegment> spec, Pageable pageable);

}
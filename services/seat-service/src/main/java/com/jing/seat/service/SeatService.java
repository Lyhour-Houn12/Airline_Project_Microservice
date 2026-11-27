package com.jing.seat.service;

import com.jing.common.dto.seat.*;
import com.jing.seat.filter.*;
import com.jing.common.pagination.PageResponse;
import com.jing.common.dto.seat.SeatResponse;

import java.util.List;

public interface SeatService {

    SeatResponse createSeat(SeatRequest request);

    List<SeatResponse> createSeats(SeatBulkRequest request);

    List<SeatResponse> generateSeats(SeatGenerateRequest request);

    SeatResponse getById(Long id);

    PageResponse<SeatResponse> search(SeatFilter filter, int page, int size, String sortBy, String sortDirection);

    SeatResponse updateSeat(Long id, SeatUpdateRequest request);

    SeatResponse blockSeat(Long id);

    SeatResponse unblockSeat(Long id);
    SeatResponse setActive(Long id, boolean active);

    void deleteSeat(Long id);
}

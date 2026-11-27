package com.jing.seat.service;

import com.jing.common.dto.seat.SeatMapRequest;
import com.jing.common.pagination.PageResponse;
import com.jing.common.dto.seat.SeatMapResponse;

public interface SeatMapService {

    SeatMapResponse createSeatMap(SeatMapRequest request);

    SeatMapResponse getSeatMapById(Long id);

    SeatMapResponse getSeatMapByCabin(Long cabinId);

    PageResponse<SeatMapResponse> getSeatMaps(String keyword, Long cabinId, Long airlineId, int page, int size, String sortBy, String sortDirection);

    SeatMapResponse updateSeatMap(Long id, SeatMapRequest request);

    void deleteSeatMap(Long id);
}

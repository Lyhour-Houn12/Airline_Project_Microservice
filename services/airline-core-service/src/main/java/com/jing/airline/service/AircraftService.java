package com.jing.airline.service;

import com.jing.common.enumeration.AircraftStatus;
import com.jing.common.dto.airline.AircraftRequest;
import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface AircraftService {
    AircraftResponse getAircraftById(Long id);

    List<AircraftResponse> listAllAircraftByOwner(Long ownerId);

    AircraftResponse createAircraft(AircraftRequest request,
                                    Long ownerId);

    AircraftResponse updateAircraft(Long id, AircraftRequest request, Long ownerId);

    void deleteAircraft(Long id, Long ownerId);

    PageResponse<AircraftResponse> getAllAircraft(String keyword, int page, int size, String sortBy, String sortDirection);

    AircraftResponse changeStatusByAdmin(Long id, AircraftStatus status);

    void deleteAircraftByAdmin(Long id);
}

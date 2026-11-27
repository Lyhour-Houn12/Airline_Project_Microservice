package com.jing.airline.service;

import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.dto.airline.AirlineRequest;
import com.jing.common.dto.airline.AirlineDropDownItem;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.pagination.PageResponse;

import java.util.List;

public interface AirlineService{

    AirlineResponse createAirline(AirlineRequest request, Long ownerId);
    AirlineResponse getAirlineByOwner(Long ownerId);
    AirlineResponse getAirlineById(Long id);
    PageResponse<AirlineResponse> getAllAirlines(String keyword, int page, int size, String sortBy, String sortDirection);
    AirlineResponse updateAirline(AirlineRequest request, Long ownerId);
    void deleteAirline(Long id, Long ownerId);

    AirlineResponse changeStatusByAdmin(Long airlineId, AirlineStatus status);

    // ----- Dropdown -----
    List<AirlineDropDownItem> getAirlinesForDropdown();


}

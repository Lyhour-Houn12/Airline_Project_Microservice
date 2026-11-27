package com.jing.seat.service;

import com.jing.common.enumeration.CabinClassType;
import com.jing.common.dto.seat.CabinClassRequest;
import com.jing.seat.filter.CabinClassSearchRequest;
import com.jing.common.dto.seat.CabinClassResponse;
import com.jing.common.pagination.PageResponse;

public interface CabinClassService {

    CabinClassResponse createCabinClass(CabinClassRequest request);

    CabinClassResponse getCabinClassById(Long id);

    PageResponse<CabinClassResponse> getAllCabinClasses(CabinClassSearchRequest searchRequest,int page, int size, String sortBy, String sortDirection);

    CabinClassResponse getCabinClassByAircraftIdAndType(
            Long aircraftId,
            CabinClassType cabinClassType
    );

    CabinClassResponse updateCabinClass(
            Long id,
            CabinClassRequest request
    );

    void deleteCabinClass(Long id);
}

package com.jing.ancillary.service;

import com.jing.ancillary.filter.MealFilter;
import com.jing.common.dto.ancillary.MealRequest;
import com.jing.common.dto.ancillary.MealResponse;
import com.jing.common.pagination.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MealService {
    // file can be null (meal without image)
    MealResponse createMeal(MealRequest request, MultipartFile file);

    MealResponse getById(Long id);

    PageResponse<MealResponse> getAll(MealFilter filter, int page, int size,
                                      String sortBy, String sortDirection);

    // updates text fields only, never the image
    MealResponse updateMeal(Long id, MealRequest request);

    MealResponse changeStatus(Long id, boolean active);

    void deleteMeal(Long id);

    // replaces the current image (old one is deleted automatically)
    MealResponse uploadImage(Long id, MultipartFile file);

    MealResponse deleteImage(Long id);
}

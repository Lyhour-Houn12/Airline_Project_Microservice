package com.jing.ancillary.service.impl;

import com.jing.ancillary.filter.MealFilter;
import com.jing.ancillary.mapper.MealMapper;
import com.jing.ancillary.model.Meal;
import com.jing.ancillary.repository.FlightInstanceMealRepository;
import com.jing.ancillary.repository.MealRepository;
import com.jing.ancillary.service.MealService;
import com.jing.ancillary.specification.MealSpecification;
import com.jing.ancillary.storage.ImageStorageService;
import com.jing.ancillary.storage.StoredImage;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.ancillary.MealRequest;
import com.jing.common.dto.ancillary.MealResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MealServiceImpl implements MealService {
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE = Set.of(
            "id", "code", "name", "mealType", "dietaryType", "calories",
            "basePrice", "createdAt", "updatedAt");

    private final MealRepository mealRepository;
    private final FlightInstanceMealRepository fimRepository;
    private final ImageStorageService imageStorageService;


    @Override
    public MealResponse createMeal(MealRequest request, MultipartFile file) {
        String code = request.code().trim().toLowerCase();

        if (mealRepository.existsByAirlineIdAndCode(request.airlineId(), code)) {
            throw new DuplicateResourceException(
                    "Meal", "code", code + " for airlineId " + request.airlineId());
        }
        Meal meal = MealMapper.toEntity(request);

        StoredImage storedImage = null;
        if (file != null && !file.isEmpty()) {
            storedImage = imageStorageService.upload(file, imageFolder(request.airlineId()));
            meal.setImageUrl(storedImage.url());
            meal.setImagePublicId(storedImage.publicId());
        }

        try {
            Meal saved = mealRepository.saveAndFlush(meal);
            log.info("Created meal id={} code={} airlineId={} withImage={}",
                    saved.getId(), saved.getCode(), saved.getAirlineId(), storedImage != null);
            return MealMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            deleteQuietly(storedImage);
            throw new DuplicateResourceException(
                    "Meal", "code", code + " for airlineId " + request.airlineId());
        } catch (RuntimeException ex) {
            deleteQuietly(storedImage);
            throw ex;
        }

    }

    @Override
    @Transactional(readOnly = true)
    public MealResponse getById(Long id) {
        return MealMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<MealResponse> getAll(MealFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                mealRepository.findAll(MealSpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection))
.map(MealMapper::toResponse)        );
    }

    @Override
    @Transactional
    public MealResponse updateMeal(Long id, MealRequest request) {

        Meal entity = findOrThrow(id);

        if (!entity.getAirlineId().equals(request.airlineId())) {
            throw new BadRequestException("airlineId cannot be changed");
        }
        if (!entity.getCode().equalsIgnoreCase(request.code().trim())) {
            throw new BadRequestException("code cannot be changed");
        }

        MealMapper.updateEntity(entity, request);   // image is not touched
        return MealMapper.toResponse(mealRepository.saveAndFlush(entity));
    }

    @Override
    public MealResponse changeStatus(Long id, boolean active) {
        Meal entity = findOrThrow(id);
        entity.setIsActive(active);
        return MealMapper.toResponse(mealRepository.save(entity));
    }

    @Override
    public void deleteMeal(Long id) {
        Meal entity = findOrThrow(id);
        if (fimRepository.existsByMealId(id)) {
            throw new BadRequestException("This meal is assigned to flight instances. Deactivate it instead");
        }
        String publicId = entity.getImagePublicId();
        mealRepository.delete(entity);
        deleteImageAfterCommit(publicId);
        log.info("Deleted meal id={}", id);
    }

    @Override
    public MealResponse uploadImage(Long id, MultipartFile file) {
        Meal meal = findOrThrow(id);
        String oldPublicId = meal.getImagePublicId();

        // 1. upload the new image first
        StoredImage stored = imageStorageService.upload(file, imageFolder(meal.getAirlineId()));

        // 2. save it on the meal
        try {
            meal.setImageUrl(stored.url());
            meal.setImagePublicId(stored.publicId());
            mealRepository.saveAndFlush(meal);
        } catch (RuntimeException ex) {
            deleteQuietly(stored);   // DB failed: don't leave the new image orphaned
            throw ex;
        }

        // 3. only after the DB commit, delete the old image
        deleteImageAfterCommit(oldPublicId);
        log.info("Uploaded image for meal id={} publicId={}", id, stored.publicId());
        return MealMapper.toResponse(meal);
    }

    @Override
    @Transactional
    public MealResponse deleteImage(Long id) {
        Meal meal = findOrThrow(id);
        if (meal.getImagePublicId() == null) {
            throw new BadRequestException("Meal with id " + id + " does not have an image to delete");
        }

        String oldPublicId = meal.getImagePublicId();
        meal.setImageUrl(null);
        meal.setImagePublicId(null);
        mealRepository.saveAndFlush(meal);
        deleteImageAfterCommit(oldPublicId);
        log.info("Deleted image for meal id={}", id);
        return MealMapper.toResponse(meal);
    }


    private Meal findOrThrow(Long id) {
        return mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal", "id", id));
    }

    private static String imageFolder(Long airlineId) {
        return "ancillary/meals/airline-" + airlineId;
    }

    private void deleteQuietly(StoredImage stored){
        if(stored != null){
            imageStorageService.delete(stored.publicId());
        }
    }
    // remove the old file from Cloudinary only once the DB change is really committed
    private void deleteImageAfterCommit(String publicId){
        if(publicId == null) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorageService.delete(publicId);
            }
        });
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}

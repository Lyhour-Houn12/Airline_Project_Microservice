package com.jing.ancillary.service.impl;

import com.jing.ancillary.filter.InsuranceCoverageFilter;
import com.jing.ancillary.mapper.InsuranceCoverageMapper;
import com.jing.ancillary.model.InsuranceCoverage;
import com.jing.ancillary.repository.InsuranceCoverageRepository;
import com.jing.ancillary.service.InsuranceService;
import com.jing.ancillary.specification.InsuranceCoverageSpecification;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.ancillary.InsuranceCoverageRequest;
import com.jing.common.dto.ancillary.InsuranceCoverageResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InsuranceServiceImpl implements InsuranceService {

    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE = Set.of(
            "id", "code", "name", "provider", "coverageType",
            "coverageAmount", "premium", "createdAt", "updatedAt");

    private final InsuranceCoverageRepository coverageRepository;



    @Override
    @Transactional
    public InsuranceCoverageResponse createCoverage(InsuranceCoverageRequest request) {
        validateRules(request);
        String code = request.code().trim().toUpperCase();

        if (coverageRepository.existsByAirlineIdAndCode(request.airlineId(), code)) {
            throw new DuplicateResourceException(
                    "InsuranceCoverage", "code", code + " for airlineId " + request.airlineId());
        }

        InsuranceCoverage saved = coverageRepository.saveAndFlush(
                InsuranceCoverageMapper.toEntity(request));
        log.info("Created insurance coverage id={} code={} airlineId={}",
                saved.getId(), saved.getCode(), saved.getAirlineId());
        return InsuranceCoverageMapper.toResponse(saved);
    }

    @Override
    public InsuranceCoverageResponse getById(Long id) {
        return InsuranceCoverageMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsuranceCoverageResponse> getAll(InsuranceCoverageFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                coverageRepository.findAll(InsuranceCoverageSpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(InsuranceCoverageMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public InsuranceCoverageResponse updateCoverage(Long id, InsuranceCoverageRequest request) {
        InsuranceCoverage entity = findOrThrow(id);

        if (!entity.getAirlineId().equals(request.airlineId())) {
            throw new BadRequestException("airlineId cannot be changed");
        }
        if (!entity.getCode().equalsIgnoreCase(request.code().trim())) {
            throw new BadRequestException("code cannot be changed");
        }
        validateRules(request);

        InsuranceCoverageMapper.updateEntity(entity, request);
        return InsuranceCoverageMapper.toResponse(coverageRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public InsuranceCoverageResponse changeStatus(Long id, boolean active) {
        InsuranceCoverage entity = findOrThrow(id);
        entity.setIsActive(active);
        return InsuranceCoverageMapper.toResponse(coverageRepository.saveAndFlush(entity));
    }

    @Override
    public void deleteCoverage(Long id) {
        InsuranceCoverage entity = findOrThrow(id);
        coverageRepository.delete(entity);
    }

    private void validateRules(InsuranceCoverageRequest r) {
        if (r.minAge() != null && r.maxAge() != null && r.minAge() > r.maxAge()) {
            throw new BadRequestException("minAge must not be greater than maxAge");
        }
        if (r.premium().compareTo(r.coverageAmount()) > 0) {
            throw new BadRequestException("premium must not be greater than coverageAmount");
        }
    }

    private InsuranceCoverage findOrThrow(Long id) {
        return coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceCoverage", "id", id));
    }


    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}

package com.jing.ancillary.service.impl;

import com.jing.ancillary.filter.AncillaryFilter;
import com.jing.ancillary.mapper.AncillaryMapper;
import com.jing.ancillary.model.Ancillary;
import com.jing.ancillary.repository.AncillaryRepository;
import com.jing.ancillary.repository.FlightCabinAncillaryRepository;
import com.jing.ancillary.service.AncillaryService;
import com.jing.ancillary.specification.AncillarySpecification;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.ancillary.AncillaryRequest;
import com.jing.common.dto.ancillary.AncillaryResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AncillaryServiceImpl implements AncillaryService {
    private static final Set<String> SORTABLE =
            Set.of("id", "code", "name", "type", "basePrice", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT_FIELD = "id";

    private final AncillaryRepository ancillaryRepository;
    private final FlightCabinAncillaryRepository fcaRepository;


    @Override
    @Transactional
    public AncillaryResponse createAncillary(AncillaryRequest request) {
        String code = request.code().trim().toUpperCase();

        if (ancillaryRepository.existsByAirlineIdAndCode(request.airlineId(), code)) {
            throw new DuplicateResourceException(
                    "Ancillary", "code", code + " for airlineId " + request.airlineId());
        }

        try {
            Ancillary saved = ancillaryRepository.saveAndFlush(AncillaryMapper.toEntity(request));
            log.info("Created ancillary id={} code={} airlineId={}",
                    saved.getId(), saved.getCode(), saved.getAirlineId());
            return AncillaryMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // race condition: another request inserted the same (airline, code)
            throw new DuplicateResourceException(
                    "Ancillary", "code", code + " for airlineId " + request.airlineId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AncillaryResponse getById(Long id) {
        return AncillaryMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AncillaryResponse> getAll(AncillaryFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                ancillaryRepository.findAll(AncillarySpecification.withFilter(filter), buildPageable(page, size, sortBy, sortDirection)).map(AncillaryMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public AncillaryResponse updateAncillary(Long id, AncillaryRequest request) {
        Ancillary entity = findOrThrow(id);

        if (!entity.getAirlineId().equals(request.airlineId())) {
            throw new BadRequestException("airlineId cannot be changed");
        }
        if (!entity.getCode().equalsIgnoreCase(request.code().trim())) {
            throw new BadRequestException("code cannot be changed");
        }

        AncillaryMapper.updateEntity(entity, request);
        return AncillaryMapper.toResponse(ancillaryRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public AncillaryResponse changeStatus(Long id, boolean active) {
        Ancillary entity = findOrThrow(id);
        entity.setIsActive(active);
        return AncillaryMapper.toResponse(ancillaryRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAncillary(Long id) {
        Ancillary entity = findOrThrow(id);
        if (fcaRepository.existsByAncillaryId(id)) {
            throw new BadRequestException("This ancillary is assigned to flight cabins. Deactivate it instead");
        }
        ancillaryRepository.delete(entity);
        log.info("Deleted ancillary id={}", id);
    }



    private Ancillary findOrThrow(Long id) {
        return ancillaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ancillary", "id", id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE, DEFAULT_SORT_FIELD, Sort.Direction.DESC);
    }
}

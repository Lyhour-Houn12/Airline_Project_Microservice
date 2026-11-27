package com.jing.seat.service.impl;

import com.jing.common.enumeration.CabinClassType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.seat.CabinClassRequest;
import com.jing.seat.filter.CabinClassSearchRequest;
import com.jing.common.dto.seat.CabinClassResponse;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.seat.mapper.CabinClassMapper;
import com.jing.seat.model.CabinClass;
import com.jing.seat.repository.CabinClassRepository;
import com.jing.seat.service.CabinClassService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CabinClassServiceImpl implements CabinClassService {

    private static final int MAX_CODE_LENGTH = 5;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id",
            "name",
            "code",
            "cabinClassType",
            "displayOrder",
            "createdAt",
            "updatedAt"
    );

    private final CabinClassRepository cabinClassRepository;


    @Override
    @Transactional
    public CabinClassResponse createCabinClass(CabinClassRequest request) {
        validateRequest(request);

        Long aircraftId = request.getAircraftId();
        String code = normalizeCode(request.getCode());

        // a cabin code must be unique within the same aircraft
        if (cabinClassRepository.existsByAircraftIdAndCodeIgnoreCase(aircraftId, code)) {
            throw duplicateCode(code, aircraftId);
        }

        // TODO (when Feign clients exist): verify that the aircraft exists.

        CabinClass cabinClass = CabinClassMapper.toEntity(request);
        cabinClass.setName(request.getName().trim());
        cabinClass.setCode(code);

        CabinClass saved = cabinClassRepository.save(cabinClass);
        log.info("Created cabin class id={} code={} aircraftId={}", saved.getId(), code, aircraftId);
        return CabinClassMapper.toResponse(saved);
    }


    @Override
    public CabinClassResponse getCabinClassById(Long id) {
        return CabinClassMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<CabinClassResponse> getAllCabinClasses(
            CabinClassSearchRequest searchRequest,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        // a null search request means no filters
        CabinClassSearchRequest search = searchRequest != null ? searchRequest : new CabinClassSearchRequest();
        validateSearchRequest(search);
        return PageResponse.from(cabinClassRepository.findAll(buildSearchSpecification(search), pageable)
                        .map(CabinClassMapper::toResponse)
        );
    }

    @Override
    public CabinClassResponse getCabinClassByAircraftIdAndType(Long aircraftId, CabinClassType cabinClassType) {
        if (aircraftId == null || aircraftId <= 0) {
            throw new BadRequestException("Aircraft ID must be a positive number");
        }
        if (cabinClassType == null) {
            throw new BadRequestException("Cabin class type is required");
        }
        List<CabinClass> matches = cabinClassRepository.findByAircraftIdAndCabinClassType(aircraftId, cabinClassType);

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Cabin class (" + cabinClassType + ")", "aircraftId", aircraftId);
        }
        if (matches.size() > 1) {
            // data problem, not a client error: an aircraft should have one cabin per type
            throw new IllegalStateException(
                    "Multiple cabin classes of type " + cabinClassType + " exist for aircraft ID " + aircraftId);
        }
        return CabinClassMapper.toResponse(matches.getFirst());
    }


    @Override
    @Transactional
    public CabinClassResponse updateCabinClass(Long id, CabinClassRequest request) {
        CabinClass existing = findOrThrow(id);
        validateRequest(request);

        // the aircraft is fixed after creation: code uniqueness is checked per aircraft
        if (!Objects.equals(existing.getAircraftId(), request.getAircraftId())) {
            throw new BadRequestException("aircraftId cannot be changed after the cabin class is created");
        }

        String newCode = normalizeCode(request.getCode());
        if (!newCode.equalsIgnoreCase(existing.getCode())
                && cabinClassRepository.existsByAircraftIdAndCodeIgnoreCaseAndIdNot(
                existing.getAircraftId(), newCode, id)) {
            throw duplicateCode(newCode, existing.getAircraftId());
        }
        CabinClassMapper.updateEntity(request, existing);
        existing.setName(request.getName().trim());
        existing.setCode(newCode);

        CabinClass saved = cabinClassRepository.save(existing);
        log.info("Updated cabin class id={}", id);
        return CabinClassMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public void deleteCabinClass(Long id) {
        CabinClass cabinClass = findOrThrow(id);
        // TODO: block the delete when seats or fares still reference this cabin class
        cabinClassRepository.delete(cabinClass);
        log.info("Deleted cabin class id={}", id);
    }

    private CabinClass findOrThrow(Long id) {
        return cabinClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cabin class", "id", id));
    }

    private DuplicateResourceException duplicateCode(String code, Long aircraftId) {
        return new DuplicateResourceException("Cabin class", "code (aircraft " + aircraftId + ")", code);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void validateRequest(CabinClassRequest request) {
        if (request == null) {
            throw new BadRequestException("Cabin class request must not be null");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Cabin class name is required");
        }
        if (request.getCabinClassType() == null) {
            throw new BadRequestException("Cabin class type is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Cabin class code is required");
        }
        if (normalizeCode(request.getCode()).length() > MAX_CODE_LENGTH) {
            throw new BadRequestException("Cabin class code must not exceed " + MAX_CODE_LENGTH + " characters");
        }
        if (request.getAircraftId() == null || request.getAircraftId() <= 0) {
            throw new BadRequestException("Aircraft ID must be a positive number");
        }
        if (request.getDescription() != null && request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (request.getDisplayOrder() != null && request.getDisplayOrder() < 0) {
            throw new BadRequestException("Display order cannot be negative");
        }
        if (request.getTypicalSeatPitch() != null && request.getTypicalSeatPitch() <= 0) {
            throw new BadRequestException("Typical seat pitch must be positive");
        }
        if (request.getTypicalSeatWidth() != null && request.getTypicalSeatWidth() <= 0) {
            throw new BadRequestException("Typical seat width must be positive");
        }
    }

    private void validateSearchRequest(CabinClassSearchRequest search) {
        if (search.getAircraftId() != null && search.getAircraftId() <= 0) {
            throw new BadRequestException("Aircraft ID must be positive");
        }
        if (search.getCode() != null && search.getCode().length() > MAX_CODE_LENGTH) {
            throw new BadRequestException("Cabin class code must not exceed " + MAX_CODE_LENGTH + " characters");
        }
        if (search.getKeyword() != null && search.getKeyword().length() > MAX_KEYWORD_LENGTH) {
            throw new BadRequestException("Search keyword must not exceed " + MAX_KEYWORD_LENGTH + " characters");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.ASC);
    }

    private Specification<CabinClass> buildSearchSpecification(CabinClassSearchRequest search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search.getAircraftId() != null) {
                predicates.add(cb.equal(root.get("aircraftId"), search.getAircraftId()));
            }
            if (search.getCabinClassType() != null) {
                predicates.add(cb.equal(root.get("cabinClassType"), search.getCabinClassType()));
            }
            if (search.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), search.getIsActive()));
            }
            if (search.getIsBookable() != null) {
                predicates.add(cb.equal(root.get("isBookable"), search.getIsBookable()));
            }
            if (search.getCode() != null && !search.getCode().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("code")), normalizeCode(search.getCode())));
            }
            if (search.getKeyword() != null && !search.getKeyword().isBlank()) {
                String pattern = likePattern(search.getKeyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("code")), pattern, '\\')
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String likePattern(String keyword) {
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
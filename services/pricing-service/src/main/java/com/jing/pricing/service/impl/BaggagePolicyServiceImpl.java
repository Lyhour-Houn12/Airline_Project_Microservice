package com.jing.pricing.service.impl;

import com.jing.common.airline.AirlineAccess;
import com.jing.common.security.CurrentUser;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.pricing.BaggagePolicyRequest;
import com.jing.common.dto.pricing.BaggagePolicyResponse;
import com.jing.pricing.mapper.BaggagePolicyMapper;
import com.jing.pricing.model.BaggagePolicy;
import com.jing.pricing.model.Fare;
import com.jing.pricing.repository.BaggagePolicyRepository;
import com.jing.pricing.repository.FareRepository;
import com.jing.pricing.service.BaggagePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaggagePolicyServiceImpl implements BaggagePolicyService {

    private static final int MAX_PIECES = 10;
    private static final double MAX_WEIGHT = 100;      // kg
    private static final double MAX_DIMENSION = 300;   // cm (length + width + height)

    private final BaggagePolicyRepository baggagePolicyRepository;
    private final FareRepository fareRepository;
    private final AirlineAccess airlineAccess;

    @Override
    @Transactional
    public BaggagePolicyResponse createBaggagePolicy(CurrentUser user, BaggagePolicyRequest request) {
        normalizeAndValidateBasics(request);

        Fare fare = fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare", "id", request.getFareId()));
        airlineAccess.requireCanManage(user, fare.getAirlineId());

        if (baggagePolicyRepository.existsByFareId(fare.getId())) {
            throw new DuplicateResourceException("BaggagePolicy", "fareId", fare.getId());
        }

        validateFields(request);

        BaggagePolicy policy = BaggagePolicyMapper.toEntity(request, fare);
        fare.setBaggagePolicy(policy);   // keep both sides of the one-to-one in sync
        BaggagePolicy saved = baggagePolicyRepository.save(policy);

        log.info("Created baggage policy id={} fareId={}", saved.getId(), fare.getId());
        return BaggagePolicyMapper.toResponse(saved);
    }

    @Override
    public BaggagePolicyResponse getById(Long id) {
        return BaggagePolicyMapper.toResponse(findOrThrow(id));
    }

    @Override
    public BaggagePolicyResponse getByFareId(Long fareId) {
        BaggagePolicy policy = baggagePolicyRepository.findByFareId(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("BaggagePolicy", "fareId", fareId));
        return BaggagePolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional
    public BaggagePolicyResponse updateBaggagePolicy(CurrentUser user, Long id, BaggagePolicyRequest request) {
        BaggagePolicy policy = findOrThrow(id);
        airlineAccess.requireCanManage(user, policy.getFare().getAirlineId());
        normalizeAndValidateBasics(request);

        if (!Objects.equals(policy.getFare().getId(), request.getFareId())) {
            throw new BadRequestException("fareId cannot be changed after the policy is created");
        }

        validateFields(request);

        // update in place (full replace), never swap the child instance
        BaggagePolicyMapper.updateEntity(request, policy);
        return BaggagePolicyMapper.toResponse(baggagePolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public void deleteBaggagePolicy(CurrentUser user, Long id) {
        BaggagePolicy policy = findOrThrow(id);
        airlineAccess.requireCanManage(user, policy.getFare().getAirlineId());
        policy.getFare().setBaggagePolicy(null);   // detach from the parent first
        baggagePolicyRepository.delete(policy);
        log.info("Deleted baggage policy id={}", id);
    }

    // ------------------------------------------------------------- helpers

    private BaggagePolicy findOrThrow(Long id) {
        return baggagePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaggagePolicy", "id", id));
    }

    private void normalizeAndValidateBasics(BaggagePolicyRequest r) {
        if (r == null) throw new BadRequestException("Request body is required");

        if (r.getName() == null || r.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }
        r.setName(r.getName().trim());
        if (r.getName().length() > 100) {
            throw new BadRequestException("name must be at most 100 characters");
        }

        if (r.getDescription() != null) {
            r.setDescription(r.getDescription().trim());
            if (r.getDescription().length() > 500) {
                throw new BadRequestException("description must be at most 500 characters");
            }
        }

        if (r.getFareId() == null) throw new BadRequestException("fareId is required");
    }

    private void validateFields(BaggagePolicyRequest r) {
        // 1. ranges
        requireRange("cabinBaggagePieces", r.getCabinBaggagePieces(), MAX_PIECES);
        requireRange("checkInBaggagePieces", r.getCheckInBaggagePieces(), MAX_PIECES);
        requireRange("cabinBaggageMaxWeight", r.getCabinBaggageMaxWeight(), MAX_WEIGHT);
        requireRange("cabinBaggageWeightPerPiece", r.getCabinBaggageWeightPerPiece(), MAX_WEIGHT);
        requireRange("checkInBaggageMaxWeight", r.getCheckInBaggageMaxWeight(), MAX_WEIGHT);
        requireRange("checkInBaggageWeightPerPiece", r.getCheckInBaggageWeightPerPiece(), MAX_WEIGHT);
        requireRange("cabinBaggageMaxDimension", r.getCabinBaggageMaxDimension(), MAX_DIMENSION);

        // 2. per-piece weight cannot exceed the total weight
        requirePerPieceWithinMax("cabin", r.getCabinBaggageWeightPerPiece(), r.getCabinBaggageMaxWeight());
        requirePerPieceWithinMax("checkIn", r.getCheckInBaggageWeightPerPiece(), r.getCheckInBaggageMaxWeight());

        // 3. zero pieces means no weight / size limits (same defaults as the mapper)
        int cabinPieces = r.getCabinBaggagePieces() != null ? r.getCabinBaggagePieces() : 1;
        int checkInPieces = r.getCheckInBaggagePieces() != null ? r.getCheckInBaggagePieces() : 0;

        if (cabinPieces == 0 && (r.getCabinBaggageMaxWeight() != null
                || r.getCabinBaggageWeightPerPiece() != null
                || r.getCabinBaggageMaxDimension() != null)) {
            throw new BadRequestException(
                    "cabin weight and dimension must be empty when cabinBaggagePieces is 0");
        }
        if (checkInPieces == 0 && (r.getCheckInBaggageMaxWeight() != null
                || r.getCheckInBaggageWeightPerPiece() != null)) {
            throw new BadRequestException(
                    "check-in weight must be empty when checkInBaggagePieces is 0");
        }
    }

    private void requireRange(String field, Integer value, int max) {
        if (value == null) return;
        if (value < 0 || value > max) {
            throw new BadRequestException(field + " must be between 0 and " + max);
        }
    }

    private void requireRange(String field, Double value, double max) {
        if (value == null) return;
        if (!Double.isFinite(value) || value < 0 || value > max) {
            throw new BadRequestException(field + " must be between 0 and " + max);
        }
    }

    private void requirePerPieceWithinMax(String group, Double perPiece, Double max) {
        if (perPiece != null && max != null && perPiece > max) {
            throw new BadRequestException(group + " weight per piece cannot exceed the max weight");
        }
    }
}
package com.jing.pricing.service.impl;

import com.jing.common.airline.AirlineAccess;
import com.jing.common.security.CurrentUser;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.pricing.FareRulesRequest;
import com.jing.common.dto.pricing.FareRulesResponse;
import com.jing.pricing.mapper.FareRulesMapper;
import com.jing.pricing.model.Fare;
import com.jing.pricing.model.FareRule;
import com.jing.pricing.repository.FareRepository;
import com.jing.pricing.repository.FareRuleRepository;
import com.jing.pricing.service.FareRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareRuleServiceImpl implements FareRuleService {

    private final FareRuleRepository fareRuleRepository;
    private final FareRepository fareRepository;
    private final AirlineAccess airlineAccess;

    @Override
    @Transactional
    public FareRulesResponse createFareRule(CurrentUser user, FareRulesRequest request) {
        normalizeAndValidateBasics(request);

        Fare fare = fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare", "id", request.getFareId()));
        airlineAccess.requireCanManage(user, fare.getAirlineId());

        if (fareRuleRepository.existsByFareId(fare.getId())) {
            throw new DuplicateResourceException("FareRule", "fareId", fare.getId());
        }

        validateRuleFields(request, fare);

        FareRule rule = FareRulesMapper.toEntity(request, fare);
        fare.setFareRule(rule);   // keep both sides of the one-to-one in sync
        FareRule saved = fareRuleRepository.save(rule);

        log.info("Created fare rule id={} fareId={}", saved.getId(), fare.getId());
        return FareRulesMapper.toResponse(saved);
    }

    @Override
    public FareRulesResponse getById(Long id) {
        return FareRulesMapper.toResponse(findOrThrow(id));
    }

    @Override
    public FareRulesResponse getByFareId(Long fareId) {
        FareRule rule = fareRuleRepository.findByFareId(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("FareRule", "fareId", fareId));
        return FareRulesMapper.toResponse(rule);
    }

    @Override
    @Transactional
    public FareRulesResponse updateFareRule(CurrentUser user, Long id, FareRulesRequest request) {
        FareRule rule = findOrThrow(id);
        airlineAccess.requireCanManage(user, rule.getFare().getAirlineId());
        normalizeAndValidateBasics(request);

        Fare fare = rule.getFare();
        if (!Objects.equals(fare.getId(), request.getFareId())) {
            throw new BadRequestException("fareId cannot be changed after the rule is created");
        }

        validateRuleFields(request, fare);

        // update in place (full replace), never swap the child instance
        FareRulesMapper.updateEntity(request, rule);
        return FareRulesMapper.toResponse(fareRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteFareRule(CurrentUser user, Long id) {
        FareRule rule = findOrThrow(id);
        airlineAccess.requireCanManage(user, rule.getFare().getAirlineId());
        Fare fare = rule.getFare();
        fare.setFareRule(null);          // detach from the parent first so cascade doesn't re-save it
        fareRuleRepository.delete(rule);
        log.info("Deleted fare rule id={}", id);
    }


    private FareRule findOrThrow(Long id) {
        return fareRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FareRule", "id", id));
    }

    private void normalizeAndValidateBasics(FareRulesRequest r) {
        if (r == null) throw new BadRequestException("Request body is required");
        if (r.getRuleName() == null || r.getRuleName().isBlank()) {
            throw new BadRequestException("ruleName is required");
        }
        r.setRuleName(r.getRuleName().trim());
        if (r.getFareId() == null) throw new BadRequestException("fareId is required");
    }

    private void validateRuleFields(FareRulesRequest r, Fare fare) {
        boolean refundable = Boolean.TRUE.equals(r.getIsRefundable());
        boolean changeable = Boolean.TRUE.equals(r.getIsChangeable());

        if (!refundable && (r.getCancellationFee() != null || r.getRefundDeadlineDays() != null)) {
            throw new BadRequestException(
                    "cancellationFee and refundDeadlineDays can only be set when isRefundable is true");
        }
        if (!changeable && (r.getChangeFee() != null || r.getChangeDeadlineHours() != null)) {
            throw new BadRequestException(
                    "changeFee and changeDeadlineHours can only be set when isChangeable is true");
        }

        requireMoney("changeFee", r.getChangeFee());
        requireMoney("cancellationFee", r.getCancellationFee());
        requireNotNegative("refundDeadlineDays", r.getRefundDeadlineDays());
        requireNotNegative("changeDeadlineHours", r.getChangeDeadlineHours());

        if (r.getCancellationFee() != null
                && r.getCancellationFee().compareTo(fare.getEffectivePrice()) > 0) {
            throw new BadRequestException("cancellationFee cannot be greater than the fare's price");
        }
    }

    private void requireMoney(String field, BigDecimal value) {
        if (value == null) return;
        if (value.signum() < 0) {
            throw new BadRequestException(field + " must not be negative");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException(field + " must have at most 2 decimal places");
        }
        if (value.precision() - value.scale() > 10) {
            throw new BadRequestException(field + " is too large");
        }
    }

    private void requireNotNegative(String field, Integer value) {
        if (value != null && value < 0) {
            throw new BadRequestException(field + " must not be negative");
        }
    }
}
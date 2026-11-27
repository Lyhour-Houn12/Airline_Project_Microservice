package com.jing.ancillary.mapper;

import com.jing.ancillary.model.InsuranceCoverage;
import com.jing.common.dto.ancillary.InsuranceCoverageRequest;
import com.jing.common.dto.ancillary.InsuranceCoverageResponse;

public class InsuranceCoverageMapper {
    private InsuranceCoverageMapper() {}

    public static InsuranceCoverage toEntity(InsuranceCoverageRequest r) {
        return InsuranceCoverage.builder()
                .airlineId(r.airlineId())
                .code(r.code().trim().toUpperCase())
                .name(r.name().trim())
                .description(clean(r.description()))
                .provider(r.provider().trim())
                .coverageType(r.coverageType())
                .coverageAmount(r.coverageAmount())
                .premium(r.premium())
                .currency(r.currency().trim().toUpperCase())
                .minAge(r.minAge())
                .maxAge(r.maxAge())
                .termsUrl(clean(r.termsUrl()))
                .isActive(r.isActive() == null ? Boolean.TRUE : r.isActive())
                .build();
    }

    public static void updateEntity(InsuranceCoverage e, InsuranceCoverageRequest r) {
        e.setName(r.name().trim());
        e.setDescription(clean(r.description()));
        e.setProvider(r.provider().trim());
        e.setCoverageType(r.coverageType());
        e.setCoverageAmount(r.coverageAmount());
        e.setPremium(r.premium());
        e.setCurrency(r.currency().trim().toUpperCase());
        e.setMinAge(r.minAge());
        e.setMaxAge(r.maxAge());
        e.setTermsUrl(clean(r.termsUrl()));
        if (r.isActive() != null) {
            e.setIsActive(r.isActive());
        }
    }
    public static InsuranceCoverageResponse toResponse(InsuranceCoverage e) {
        return new InsuranceCoverageResponse(
                e.getId(),
                e.getAirlineId(),
                e.getCode(),
                e.getName(),
                e.getDescription(),
                e.getProvider(),
                e.getCoverageType(),
                e.getCoverageAmount(),
                e.getPremium(),
                e.getCurrency(),
                e.getMinAge(),
                e.getMaxAge(),
                e.getTermsUrl(),
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }



    private static String clean(String input){
        return (input == null || input.isBlank()) ? null : input.trim();
    }

}

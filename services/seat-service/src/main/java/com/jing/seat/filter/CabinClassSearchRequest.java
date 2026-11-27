package com.jing.seat.filter;

import com.jing.common.enumeration.CabinClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinClassSearchRequest {

    private String keyword;

    private Long aircraftId;

    private String code;

    private CabinClassType cabinClassType;

    private Boolean isActive;

    private Boolean isBookable;
}
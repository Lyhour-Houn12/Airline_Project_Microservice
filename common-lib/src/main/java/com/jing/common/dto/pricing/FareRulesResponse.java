package com.jing.common.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRulesResponse {

    private Long id;
    private String ruleName;
    private Long fareId;
    private Long airlineId;
    private Boolean isRefundable;
    private BigDecimal changeFee;
    private BigDecimal cancellationFee;
    private Integer refundDeadlineDays;
    private Integer changeDeadlineHours;
    private Boolean isChangeable;
}
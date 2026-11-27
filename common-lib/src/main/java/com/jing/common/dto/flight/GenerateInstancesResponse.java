package com.jing.common.dto.flight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateInstancesResponse {
    private Long scheduleId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int created;
    private int skipped;   // dates that already had an instance
}
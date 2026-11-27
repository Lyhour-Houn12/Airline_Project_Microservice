package com.jing.common.dto.flight;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateInstancesRequest {
    private LocalDate fromDate;   // default: schedule start (never earlier than today)
    private LocalDate toDate;     // default: schedule end
    @NotNull(message = "Total seats is required")
    @Positive
    private Integer totalSeats;   // aircraft isn't wired yet, so the caller supplies capacity
    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private String terminal;
    private String gate;
}
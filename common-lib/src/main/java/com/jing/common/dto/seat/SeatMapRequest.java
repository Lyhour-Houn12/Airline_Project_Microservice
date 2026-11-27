package com.jing.common.dto.seat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapRequest {

    @NotBlank(message = "Seat map name is required")
    private String name;

    @NotNull(message = "Total rows is required")
    @Positive
    private Integer totalRows;

    @NotNull(message = "Airline ID is required")
    @Positive(message = "Airline ID must be positive")
    private Long airlineId;

    @NotNull(message = "Left seats per row is required")
    @Positive
    private Integer leftSeatsPerRow;

    @NotNull(message = "Right seats per row is required")
    @Positive
    private Integer rightSeatsPerRow;

    @NotNull(message = "Cabin class ID is required")
    @Positive(message = "Cabin class ID must be positive")
    private Long cabinClassId;


}

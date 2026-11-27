package com.jing.common.dto.seat;

import com.jing.common.enumeration.CabinClassType;
import com.jing.common.enumeration.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinClassRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 5, message = "Code must not exceed 5 characters")
    private String code;

    @NotNull(message = "Cabin class type is required")
    private CabinClassType cabinClassType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Aircraft ID is required")
    @Positive(message = "Aircraft ID must be positive")
    private Long aircraftId;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private Boolean isActive;

    private Boolean isBookable;

    @Positive(message = "Typical seat pitch must be positive")
    private Integer typicalSeatPitch;

    @Positive(message = "Typical seat width must be positive")
    private Integer typicalSeatWidth;

    private SeatType seatType;
}
package com.jing.common.dto.seat;

import com.jing.common.enumeration.CabinClassType;
import com.jing.common.enumeration.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinClassResponse {

    private Long id;

    private String name;

    private CabinClassType cabinClassType;

    private String code;

    private String description;

    private Long aircraftId;

    private Integer displayOrder;

    private Boolean isActive;

    private Boolean isBookable;

    private Integer typicalSeatPitch;

    private Integer typicalSeatWidth;

    private SeatType seatType;
}
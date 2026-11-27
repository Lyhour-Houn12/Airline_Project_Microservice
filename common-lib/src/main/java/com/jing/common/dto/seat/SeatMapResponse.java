package com.jing.common.dto.seat;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatMapResponse {

    private Long id;
    private String name;
    private Integer totalRows;
    private Integer leftSeatsPerRow;
    private Integer rightSeatsPerRow;

    private Long airlineId;

    private Long cabinClassId;
    private String cabinClassName;
    private String cabinClassCode;
}

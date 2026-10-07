package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaremeIrppDTO {
    private Long id;
    private LocalDate effectiveFrom;
    private List<TrancheIrppDTO> tranches;
    private String notes;
}

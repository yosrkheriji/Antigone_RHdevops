package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelanceClientDTO {
    private Long id;
    private Long factureId;
    private String factureNumero;
    private String clientNom;
    private LocalDate dateRelance;
    private Boolean envoyee;
    private String note;
}

package com.antigone.rh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametresPaieDTO {
    private Double cnssSalarie;
    private Double solidariteSalarie;
    private Double cnssPatronale;
    private Double tfp;
    private Double foprolos;
    private Double at;
    private Double abattement;
    private String notes;
}

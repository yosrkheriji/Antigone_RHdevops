package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Demande d'explication d'un bulletin de paie. */
@Data
public class ExplainPayslipRequest {

    /**
     * Reserve aux administrateurs. Pour tout autre compte la valeur est ignoree et
     * remplacee par l'identifiant du JWT ; une demande visant un tiers est refusee.
     */
    private Long employeeId;

    @NotBlank(message = "month est obligatoire")
    @Pattern(regexp = "[0-9]{4}-[0-9]{2}", message = "month doit etre au format YYYY-MM")
    private String month;
}

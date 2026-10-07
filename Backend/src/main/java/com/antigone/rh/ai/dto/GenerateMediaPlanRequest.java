package com.antigone.rh.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Demande de generation d'un media plan mensuel. */
@Data
public class GenerateMediaPlanRequest {

    @NotNull(message = "clientId est obligatoire")
    private Long clientId;

    @NotBlank(message = "month est obligatoire")
    @Pattern(regexp = "[0-9]{4}-[0-9]{2}", message = "month doit etre au format YYYY-MM")
    private String month;
}

package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.ClientFactureDTO;
import com.antigone.rh.dto.EmployePaieDTO;
import com.antigone.rh.dto.ParametresFacturationDTO;
import com.antigone.rh.service.FinanceReferentielService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Données de référence du module Finance : clients, employés, paramètres. */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceReferentielController {

    private final FinanceReferentielService financeReferentielService;

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<ClientFactureDTO>>> getClients() {
        return ResponseEntity.ok(ApiResponse.ok(financeReferentielService.getClients()));
    }

    /** Employés dont le contrat couvre le mois demandé (archivés en cours de mois inclus). */
    @GetMapping("/employes")
    public ResponseEntity<ApiResponse<List<EmployePaieDTO>>> getEmployes(
            @RequestParam(required = false) String mois) {
        List<EmployePaieDTO> employes = (mois != null && !mois.isBlank())
                ? financeReferentielService.getEmployesDuMois(mois)
                : financeReferentielService.getTousEmployes();
        return ResponseEntity.ok(ApiResponse.ok(employes));
    }

    @GetMapping("/parametres-facturation")
    public ResponseEntity<ApiResponse<ParametresFacturationDTO>> getParametresFacturation() {
        return ResponseEntity.ok(ApiResponse.ok(financeReferentielService.getParametresFacturation()));
    }
}

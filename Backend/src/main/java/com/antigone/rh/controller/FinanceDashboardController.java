package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.DeclarationTvaDTO;
import com.antigone.rh.dto.ResultatNetDTO;
import com.antigone.rh.dto.VueDecaissementsDTO;
import com.antigone.rh.dto.VueEncaissementsDTO;
import com.antigone.rh.service.DashboardFinanceService;
import com.antigone.rh.service.TvaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/dashboard")
@RequiredArgsConstructor
public class FinanceDashboardController {

    private final DashboardFinanceService dashboardFinanceService;
    private final TvaService tvaService;

    @GetMapping("/encaissements")
    public ResponseEntity<ApiResponse<VueEncaissementsDTO>> getEncaissements(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardFinanceService.getVueEncaissements(mois)));
    }

    @GetMapping("/decaissements")
    public ResponseEntity<ApiResponse<VueDecaissementsDTO>> getDecaissements(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardFinanceService.getVueDecaissements(mois)));
    }

    @GetMapping("/resultat")
    public ResponseEntity<ApiResponse<ResultatNetDTO>> getResultatNet(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardFinanceService.getResultatNet(mois)));
    }

    @GetMapping("/tva")
    public ResponseEntity<ApiResponse<DeclarationTvaDTO>> getTva(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(tvaService.getDeclaration(mois)));
    }
}

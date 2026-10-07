package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.ChargeFixeDTO;
import com.antigone.rh.dto.ChargeFixeRequest;
import com.antigone.rh.dto.ChargeVariableDTO;
import com.antigone.rh.dto.EtatChargeFixeDTO;
import com.antigone.rh.dto.PaiementChargeFixeDTO;
import com.antigone.rh.dto.ResumeChargesDTO;
import com.antigone.rh.service.ChargesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/charges")
@RequiredArgsConstructor
public class ChargesController {

    private final ChargesService chargesService;

    // ── Fixes ────────────────────────────────────────────────────────────────

    @GetMapping("/fixes")
    public ResponseEntity<ApiResponse<List<ChargeFixeDTO>>> getFixes() {
        return ResponseEntity.ok(ApiResponse.ok(chargesService.getChargesFixes()));
    }

    @PostMapping("/fixes")
    public ResponseEntity<ApiResponse<ChargeFixeDTO>> createFixe(@RequestBody ChargeFixeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Charge fixe créée", chargesService.createChargeFixe(req)));
    }

    @PutMapping("/fixes/{id}")
    public ResponseEntity<ApiResponse<ChargeFixeDTO>> updateFixe(@PathVariable Long id, @RequestBody ChargeFixeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Charge fixe mise à jour", chargesService.updateChargeFixe(id, req)));
    }

    @DeleteMapping("/fixes/{id}")
    public ResponseEntity<ApiResponse<Void>> archiveFixe(@PathVariable Long id) {
        chargesService.archiveChargeFixe(id);
        return ResponseEntity.ok(ApiResponse.ok("Charge fixe archivée", null));
    }

    @GetMapping("/fixes/{id}/paiements")
    public ResponseEntity<ApiResponse<List<PaiementChargeFixeDTO>>> getPaiements(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(chargesService.getPaiements(id)));
    }

    @PostMapping("/fixes/{id}/paiements")
    public ResponseEntity<ApiResponse<PaiementChargeFixeDTO>> enregistrerPaiement(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String mois = String.valueOf(body.get("mois"));
        Double montant = body.get("montant") != null ? Double.valueOf(String.valueOf(body.get("montant"))) : null;
        LocalDate date = body.get("datePaiement") != null ? LocalDate.parse(String.valueOf(body.get("datePaiement"))) : null;
        return ResponseEntity.ok(ApiResponse.ok("Paiement enregistré",
                chargesService.enregistrerPaiement(id, mois, montant, date)));
    }

    // ── Variables ────────────────────────────────────────────────────────────

    @GetMapping("/variables")
    public ResponseEntity<ApiResponse<List<ChargeVariableDTO>>> getVariables(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(chargesService.getChargesVariables(mois)));
    }

    @PostMapping("/variables")
    public ResponseEntity<ApiResponse<ChargeVariableDTO>> createVariable(@RequestBody ChargeVariableDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Charge variable créée", chargesService.createChargeVariable(dto)));
    }

    @PutMapping("/variables/{id}")
    public ResponseEntity<ApiResponse<ChargeVariableDTO>> updateVariable(@PathVariable Long id, @RequestBody ChargeVariableDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Charge variable mise à jour", chargesService.updateChargeVariable(id, dto)));
    }

    @DeleteMapping("/variables/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVariable(@PathVariable Long id) {
        chargesService.deleteChargeVariable(id);
        return ResponseEntity.ok(ApiResponse.ok("Charge variable supprimée", null));
    }

    // ── Échéancier & synthèse ────────────────────────────────────────────────

    @GetMapping("/fixes/etat")
    public ResponseEntity<ApiResponse<List<EtatChargeFixeDTO>>> getEtatFixes(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(chargesService.getEtatChargesFixes(mois)));
    }

    @GetMapping("/resume")
    public ResponseEntity<ApiResponse<ResumeChargesDTO>> getResume(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(chargesService.getResume(mois)));
    }
}

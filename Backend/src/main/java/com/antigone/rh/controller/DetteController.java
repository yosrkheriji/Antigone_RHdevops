package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.DetteDTO;
import com.antigone.rh.dto.DettePaiementDTO;
import com.antigone.rh.service.DetteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/dettes")
@RequiredArgsConstructor
public class DetteController {

    private final DetteService detteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DetteDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(detteService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DetteDTO>> create(@RequestBody DetteDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Dette créée", detteService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DetteDTO>> update(@PathVariable Long id, @RequestBody DetteDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Dette mise à jour", detteService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        detteService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Dette supprimée", null));
    }

    @GetMapping("/{id}/paiements")
    public ResponseEntity<ApiResponse<List<DettePaiementDTO>>> getPaiements(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(detteService.getPaiements(id)));
    }

    @PostMapping("/{id}/paiements")
    public ResponseEntity<ApiResponse<DetteDTO>> enregistrerPaiement(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double montant = Double.valueOf(String.valueOf(body.get("montant")));
        LocalDate date = body.get("datePaiement") != null
                ? LocalDate.parse(String.valueOf(body.get("datePaiement"))) : null;
        String note = body.get("note") != null ? String.valueOf(body.get("note")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Remboursement enregistré",
                detteService.enregistrerPaiement(id, montant, date, note)));
    }

    @DeleteMapping("/paiements/{paiementId}")
    public ResponseEntity<ApiResponse<DetteDTO>> supprimerPaiement(@PathVariable Long paiementId) {
        return ResponseEntity.ok(ApiResponse.ok("Remboursement annulé", detteService.supprimerPaiement(paiementId)));
    }
}

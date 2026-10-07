package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.DeclarationCnssDTO;
import com.antigone.rh.service.CnssService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/cnss")
@RequiredArgsConstructor
public class CnssController {

    private final CnssService cnssService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeclarationCnssDTO>>> getByAnnee(@RequestParam Integer annee) {
        return ResponseEntity.ok(ApiResponse.ok(cnssService.getByAnnee(annee)));
    }

    @GetMapping("/suggestion")
    public ResponseEntity<ApiResponse<DeclarationCnssDTO>> getSuggestion(
            @RequestParam Integer annee, @RequestParam Integer trimestre) {
        return ResponseEntity.ok(ApiResponse.ok(cnssService.calculerSuggestionDepuisBulletins(annee, trimestre)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeclarationCnssDTO>> save(@RequestBody DeclarationCnssDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Déclaration CNSS enregistrée", cnssService.saveDeclaration(dto)));
    }

    @PostMapping("/{id}/payer")
    public ResponseEntity<ApiResponse<DeclarationCnssDTO>> payer(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        LocalDate date = (body != null && body.get("datePaiement") != null) ? LocalDate.parse(body.get("datePaiement")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Déclaration CNSS marquée payée", cnssService.marquerPayee(id, date)));
    }
}

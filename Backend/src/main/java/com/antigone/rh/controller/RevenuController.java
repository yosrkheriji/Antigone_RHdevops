package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.AutreRevenuDTO;
import com.antigone.rh.service.RevenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/revenus")
@RequiredArgsConstructor
public class RevenuController {

    private final RevenuService revenuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AutreRevenuDTO>>> getByMois(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(revenuService.getByMois(mois)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AutreRevenuDTO>> create(@RequestBody AutreRevenuDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Revenu enregistré", revenuService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AutreRevenuDTO>> update(@PathVariable Long id, @RequestBody AutreRevenuDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Revenu mis à jour", revenuService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        revenuService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Revenu supprimé", null));
    }
}

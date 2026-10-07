package com.antigone.rh.controller;

import com.antigone.rh.dto.*;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.service.FacturationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/facturation")
@RequiredArgsConstructor
public class FacturationController {

    private final FacturationService facturationService;

    // ── Factures & devis ─────────────────────────────────────────────────────

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<FactureDTO>>> getByType(@RequestParam TypeDocument type) {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getByType(type)));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<FactureDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getById(id)));
    }

    @GetMapping("/documents/client/{clientId}")
    public ResponseEntity<ApiResponse<List<FactureDTO>>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getByClient(clientId)));
    }

    @GetMapping("/impayees")
    public ResponseEntity<ApiResponse<List<FactureDTO>>> getImpayees() {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getImpayees()));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<FactureDTO>> create(@RequestBody FactureRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Document créé", facturationService.create(req)));
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<FactureDTO>> update(@PathVariable Long id, @RequestBody FactureRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Document mis à jour", facturationService.update(id, req)));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        facturationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Document supprimé", null));
    }

    @PostMapping("/documents/{id}/payer")
    public ResponseEntity<ApiResponse<FactureDTO>> marquerPayee(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        LocalDate date = (body != null && body.get("datePaiement") != null)
                ? LocalDate.parse(body.get("datePaiement")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Facture soldée", facturationService.marquerPayee(id, date)));
    }

    // ── Paiements partiels ───────────────────────────────────────────────────

    @GetMapping("/documents/{id}/paiements")
    public ResponseEntity<ApiResponse<List<PaiementFactureDTO>>> getPaiements(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getPaiements(id)));
    }

    @PostMapping("/documents/{id}/paiements")
    public ResponseEntity<ApiResponse<FactureDTO>> enregistrerPaiement(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double montant = Double.valueOf(String.valueOf(body.get("montant")));
        LocalDate date = body.get("datePaiement") != null
                ? LocalDate.parse(String.valueOf(body.get("datePaiement"))) : null;
        String note = body.get("note") != null ? String.valueOf(body.get("note")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Paiement enregistré",
                facturationService.enregistrerPaiement(id, montant, date, note)));
    }

    @DeleteMapping("/paiements/{paiementId}")
    public ResponseEntity<ApiResponse<FactureDTO>> supprimerPaiement(@PathVariable Long paiementId) {
        return ResponseEntity.ok(ApiResponse.ok("Paiement annulé", facturationService.supprimerPaiement(paiementId)));
    }

    // ── Catalogue de services ────────────────────────────────────────────────

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceCatalogueDTO>>> getServices() {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getServices()));
    }

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<ServiceCatalogueDTO>> createService(@RequestBody ServiceCatalogueDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Service créé", facturationService.createService(dto)));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ApiResponse<ServiceCatalogueDTO>> updateService(
            @PathVariable Long id, @RequestBody ServiceCatalogueDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Service mis à jour", facturationService.updateService(id, dto)));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        facturationService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.ok("Service archivé", null));
    }

    // ── Templates ────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<TemplateFactureDTO>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getTemplates()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<TemplateFactureDTO>> createTemplate(@RequestBody TemplateFactureDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Template créé", facturationService.createTemplate(dto)));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        facturationService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok("Template supprimé", null));
    }

    // ── Relances ─────────────────────────────────────────────────────────────

    @GetMapping("/relances")
    public ResponseEntity<ApiResponse<List<RelanceClientDTO>>> getRelances() {
        return ResponseEntity.ok(ApiResponse.ok(facturationService.getRelancesEnAttente()));
    }

    @PostMapping("/relances")
    public ResponseEntity<ApiResponse<RelanceClientDTO>> createRelance(@RequestBody Map<String, Object> body) {
        Long factureId = Long.valueOf(String.valueOf(body.get("factureId")));
        LocalDate date = body.get("dateRelance") != null
                ? LocalDate.parse(String.valueOf(body.get("dateRelance"))) : null;
        String note = body.get("note") != null ? String.valueOf(body.get("note")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Relance programmée",
                facturationService.createRelance(factureId, date, note)));
    }

    @PostMapping("/relances/{id}/envoyee")
    public ResponseEntity<ApiResponse<RelanceClientDTO>> marquerEnvoyee(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Relance marquée envoyée",
                facturationService.marquerRelanceEnvoyee(id)));
    }
}

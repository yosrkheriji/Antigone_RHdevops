package com.antigone.rh.controller;

import com.antigone.rh.dto.*;
import com.antigone.rh.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/paie")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    // ── Paramètres & barème ──────────────────────────────────────────────────

    @GetMapping("/parametres")
    public ResponseEntity<ApiResponse<ParametresPaieDTO>> getParametres() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getParametres()));
    }

    @PutMapping("/parametres")
    public ResponseEntity<ApiResponse<ParametresPaieDTO>> updateParametres(@RequestBody ParametresPaieDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Paramètres mis à jour", payrollService.updateParametres(dto)));
    }

    @GetMapping("/baremes-irpp")
    public ResponseEntity<ApiResponse<List<BaremeIrppDTO>>> listBaremes() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.listBaremes()));
    }

    @PostMapping("/baremes-irpp")
    public ResponseEntity<ApiResponse<BaremeIrppDTO>> createBareme(@RequestBody BaremeIrppDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Barème IRPP créé", payrollService.createBareme(dto)));
    }

    // ── Calcul / génération ──────────────────────────────────────────────────

    @PostMapping("/calcul")
    public ResponseEntity<ApiResponse<FichePaieDTO>> calculer(@RequestBody CalculPaieRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.calculerApercu(req.getEmployeId(), req.getMois(), req.getElements())));
    }

    @PostMapping("/generer")
    public ResponseEntity<ApiResponse<BulletinPaieDTO>> genererBulletin(@RequestBody CalculPaieRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Bulletin généré",
                payrollService.genererBulletin(req.getEmployeId(), req.getMois(), req.getElements())));
    }

    @PostMapping("/generer-tout/{mois}")
    public ResponseEntity<ApiResponse<List<BulletinPaieDTO>>> genererTout(@PathVariable String mois) {
        return ResponseEntity.ok(ApiResponse.ok("Bulletins générés", payrollService.genererBulletinsDuMois(mois)));
    }

    // ── Consultation ─────────────────────────────────────────────────────────

    @GetMapping("/bulletins")
    public ResponseEntity<ApiResponse<List<BulletinPaieDTO>>> getBulletins(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getBulletins(mois)));
    }

    @GetMapping("/bulletins/employe/{employeId}")
    public ResponseEntity<ApiResponse<List<BulletinPaieDTO>>> getBulletinsByEmploye(@PathVariable Long employeId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getBulletinsByEmploye(employeId)));
    }

    @GetMapping("/impayes")
    public ResponseEntity<ApiResponse<List<BulletinPaieDTO>>> getImpayes(@RequestParam String avant) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getImpayes(avant)));
    }

    @GetMapping("/totaux")
    public ResponseEntity<ApiResponse<TotauxPaieDTO>> getTotaux(@RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getTotaux(mois)));
    }

    @GetMapping("/acomptes")
    public ResponseEntity<ApiResponse<List<AcompteSalaireDTO>>> getAcomptes(
            @RequestParam Long employeId, @RequestParam String mois) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAcomptes(employeId, mois)));
    }

    // ── Paiement ─────────────────────────────────────────────────────────────

    @PostMapping("/bulletins/{id}/payer")
    public ResponseEntity<ApiResponse<BulletinPaieDTO>> marquerPaye(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        LocalDate date = (body != null && body.get("date") != null) ? LocalDate.parse(body.get("date")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Bulletin marqué payé", payrollService.marquerPaye(id, date)));
    }

    @PostMapping("/acompte")
    public ResponseEntity<ApiResponse<BulletinPaieDTO>> enregistrerAcompte(@RequestBody Map<String, Object> body) {
        Long employeId = Long.valueOf(String.valueOf(body.get("employeId")));
        String mois = String.valueOf(body.get("mois"));
        Double montant = Double.valueOf(String.valueOf(body.get("montant")));
        LocalDate date = body.get("date") != null ? LocalDate.parse(String.valueOf(body.get("date"))) : null;
        String note = body.get("note") != null ? String.valueOf(body.get("note")) : null;
        return ResponseEntity.ok(ApiResponse.ok("Acompte enregistré",
                payrollService.enregistrerAcompte(employeId, mois, montant, date, note)));
    }
}

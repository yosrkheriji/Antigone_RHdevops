package com.antigone.rh.service;

import com.antigone.rh.dto.*;
import com.antigone.rh.entity.*;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facturation et encaissements : émission de factures/devis, calcul de la tarification
 * et suivi des paiements partiels.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FacturationService {

    private final FactureRepository factureRepository;
    private final PaiementFactureRepository paiementFactureRepository;
    private final CompteurDocumentRepository compteurDocumentRepository;
    private final ServiceCatalogueRepository serviceCatalogueRepository;
    private final TemplateFactureRepository templateFactureRepository;
    private final RelanceClientRepository relanceClientRepository;
    private final ClientRepository clientRepository;
    private final FinanceReferentielService financeReferentielService;

    // ── Tarification ─────────────────────────────────────────────────────────

    /** Σ des lignes sélectionnées (quantité × prix unitaire). */
    private double sommeLignes(List<LigneDocumentDTO> lignes) {
        if (lignes == null) return 0;
        return lignes.stream()
                .filter(l -> !Boolean.FALSE.equals(l.getSelectionnee()))
                .mapToDouble(l -> nz(l.getQuantite()) * nz(l.getPrixUnitaire()))
                .sum();
    }

    /**
     * Applique la formule de tarification à la facture.
     * Le total HT saisi manuellement agit comme un plancher, pas un remplacement.
     * TVA et timbre fiscal ne s'appliquent jamais à un devis.
     * À défaut de valeur explicite, les taux viennent des référentiels.
     */
    private void appliquerTarification(Facture facture, Double totalHtManuel, Double tauxTva, Double timbreFiscal) {
        boolean estDevis = facture.getType() == TypeDocument.DEVIS;

        double ht = Math.max(sommeLignes(facture.getLignes()), nz(totalHtManuel));
        double taux = estDevis ? 0 : (tauxTva != null ? tauxTva : financeReferentielService.getTvaDefaut());
        double tva = estDevis ? 0 : ht * taux / 100.0;
        double timbre = estDevis ? 0
                : (timbreFiscal != null ? timbreFiscal : financeReferentielService.getTimbreFiscalDefaut());

        facture.setTotalHtManuel(totalHtManuel);
        facture.setTotalHt(round3(ht));
        facture.setTauxTva(round3(taux));
        facture.setMontantTva(round3(tva));
        facture.setTimbreFiscal(round3(timbre));
        facture.setTotalTtc(round3(ht + tva + timbre));
    }

    // ── Numérotation séquentielle ────────────────────────────────────────────

    private String genererNumero(TypeDocument type, int annee) {
        CompteurDocument compteur = compteurDocumentRepository.findByTypeAndAnnee(type, annee)
                .orElseGet(() -> CompteurDocument.builder().type(type).annee(annee).dernierNumero(0).build());
        compteur.setDernierNumero(compteur.getDernierNumero() + 1);
        compteurDocumentRepository.save(compteur);

        String prefixe = type == TypeDocument.FACTURE ? "FAC" : "DEV";
        return String.format("%s-%d-%04d", prefixe, annee, compteur.getDernierNumero());
    }

    // ── CRUD factures / devis ────────────────────────────────────────────────

    public List<FactureDTO> getByType(TypeDocument type) {
        return factureRepository.findByTypeOrderByDateEmissionDesc(type).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<FactureDTO> getByClient(Long clientId) {
        return factureRepository.findByClientIdOrderByDateEmissionDesc(clientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public FactureDTO getById(Long id) {
        return toDTO(findFactureOrThrow(id));
    }

    /** Factures émises mais pas encore soldées. */
    public List<FactureDTO> getImpayees() {
        return factureRepository.findByTypeAndStatutNot(TypeDocument.FACTURE, StatutFacture.PAYEE).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public FactureDTO create(FactureRequest req) {
        TypeDocument type = req.getType() != null ? req.getType() : TypeDocument.FACTURE;
        LocalDate dateEmission = req.getDateEmission() != null ? req.getDateEmission() : LocalDate.now();

        Facture facture = Facture.builder()
                .numero(genererNumero(type, dateEmission.getYear()))
                .type(type)
                .dateEmission(dateEmission)
                .dateEcheance(req.getDateEcheance())
                .lignes(req.getLignes() != null ? req.getLignes() : Collections.emptyList())
                .notes(req.getNotes())
                .statut(StatutFacture.EN_ATTENTE)
                .montantPaye(0.0)
                .build();

        if (req.getClientId() != null) {
            facture.setClient(clientRepository.findById(req.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé")));
        }

        appliquerTarification(facture, req.getTotalHtManuel(), req.getTauxTva(), req.getTimbreFiscal());
        return toDTO(factureRepository.save(facture));
    }

    public FactureDTO update(Long id, FactureRequest req) {
        Facture facture = findFactureOrThrow(id);
        // Le numéro et le type restent figés : ils identifient le document émis.
        facture.setDateEmission(req.getDateEmission() != null ? req.getDateEmission() : facture.getDateEmission());
        facture.setDateEcheance(req.getDateEcheance());
        facture.setLignes(req.getLignes() != null ? req.getLignes() : Collections.emptyList());
        facture.setNotes(req.getNotes());

        if (req.getClientId() != null) {
            facture.setClient(clientRepository.findById(req.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé")));
        }

        appliquerTarification(facture, req.getTotalHtManuel(), req.getTauxTva(), req.getTimbreFiscal());
        recalculerStatut(facture);
        return toDTO(factureRepository.save(facture));
    }

    public void delete(Long id) {
        Facture facture = findFactureOrThrow(id);
        paiementFactureRepository.deleteAll(paiementFactureRepository.findByFactureIdOrderByDatePaiementDesc(id));
        relanceClientRepository.deleteAll(relanceClientRepository.findByFactureIdOrderByDateRelanceDesc(id));
        factureRepository.delete(facture);
    }

    /** Force le statut payé et aligne le montant payé sur le total TTC. */
    public FactureDTO marquerPayee(Long id, LocalDate datePaiement) {
        Facture facture = findFactureOrThrow(id);
        double restant = Math.max(0, nz(facture.getTotalTtc()) - nz(facture.getMontantPaye()));
        if (restant > 0) {
            enregistrerPaiementInterne(facture, restant, datePaiement, "Solde forcé");
        }
        facture.setMontantPaye(facture.getTotalTtc());
        facture.setStatut(StatutFacture.PAYEE);
        facture.setPaidAt(LocalDateTime.now());
        return toDTO(factureRepository.save(facture));
    }

    // ── Paiements partiels ───────────────────────────────────────────────────

    public List<PaiementFactureDTO> getPaiements(Long factureId) {
        return paiementFactureRepository.findByFactureIdOrderByDatePaiementDesc(factureId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public FactureDTO enregistrerPaiement(Long factureId, Double montant, LocalDate datePaiement, String note) {
        Facture facture = findFactureOrThrow(factureId);
        enregistrerPaiementInterne(facture, montant, datePaiement, note);
        return toDTO(factureRepository.save(facture));
    }

    private void enregistrerPaiementInterne(Facture facture, Double montant, LocalDate datePaiement, String note) {
        paiementFactureRepository.save(PaiementFacture.builder()
                .facture(facture).montant(montant)
                .datePaiement(datePaiement != null ? datePaiement : LocalDate.now())
                .note(note)
                .build());

        double nouveauMontant = round3(nz(facture.getMontantPaye()) + montant);
        facture.setMontantPaye(nouveauMontant);
        recalculerStatut(facture);
    }

    /** Annule un paiement : déduit le montant, jamais de solde négatif. */
    public FactureDTO supprimerPaiement(Long paiementId) {
        PaiementFacture paiement = paiementFactureRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));
        Facture facture = paiement.getFacture();

        double nouveauMontant = Math.max(0, round3(nz(facture.getMontantPaye()) - paiement.getMontant()));
        facture.setMontantPaye(nouveauMontant);
        paiementFactureRepository.delete(paiement);

        if (nouveauMontant == 0) {
            facture.setStatut(StatutFacture.EN_ATTENTE);
            facture.setPaidAt(null);
        } else {
            recalculerStatut(facture);
        }
        return toDTO(factureRepository.save(facture));
    }

    private void recalculerStatut(Facture facture) {
        double paye = nz(facture.getMontantPaye());
        double ttc = nz(facture.getTotalTtc());

        if (paye >= ttc && ttc > 0) {
            facture.setStatut(StatutFacture.PAYEE);
            if (facture.getPaidAt() == null) facture.setPaidAt(LocalDateTime.now());
        } else if (paye > 0) {
            facture.setStatut(StatutFacture.PARTIEL);
            facture.setPaidAt(null);
        } else {
            facture.setStatut(StatutFacture.EN_ATTENTE);
            facture.setPaidAt(null);
        }
    }

    // ── Catalogue de services ────────────────────────────────────────────────

    public List<ServiceCatalogueDTO> getServices() {
        return serviceCatalogueRepository.findAllByActifTrue().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ServiceCatalogueDTO createService(ServiceCatalogueDTO dto) {
        return toDTO(serviceCatalogueRepository.save(ServiceCatalogue.builder()
                .designation(dto.getDesignation()).prixDefaut(dto.getPrixDefaut())
                .categorie(dto.getCategorie()).description(dto.getDescription())
                .actif(dto.getActif() == null || dto.getActif())
                .build()));
    }

    public ServiceCatalogueDTO updateService(Long id, ServiceCatalogueDTO dto) {
        ServiceCatalogue s = serviceCatalogueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));
        s.setDesignation(dto.getDesignation());
        s.setPrixDefaut(dto.getPrixDefaut());
        s.setCategorie(dto.getCategorie());
        s.setDescription(dto.getDescription());
        if (dto.getActif() != null) s.setActif(dto.getActif());
        return toDTO(serviceCatalogueRepository.save(s));
    }

    public void deleteService(Long id) {
        ServiceCatalogue s = serviceCatalogueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));
        s.setActif(false);
        serviceCatalogueRepository.save(s);
    }

    // ── Templates ────────────────────────────────────────────────────────────

    public List<TemplateFactureDTO> getTemplates() {
        return templateFactureRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TemplateFactureDTO createTemplate(TemplateFactureDTO dto) {
        return toDTO(templateFactureRepository.save(TemplateFacture.builder()
                .nom(dto.getNom())
                .type(dto.getType() != null ? dto.getType() : TypeDocument.FACTURE)
                .lignes(dto.getLignes() != null ? dto.getLignes() : Collections.emptyList())
                .tauxTva(dto.getTauxTva()).timbreFiscal(dto.getTimbreFiscal()).notes(dto.getNotes())
                .build()));
    }

    public void deleteTemplate(Long id) {
        templateFactureRepository.deleteById(id);
    }

    // ── Relances ─────────────────────────────────────────────────────────────

    public List<RelanceClientDTO> getRelancesEnAttente() {
        return relanceClientRepository.findAllByEnvoyeeFalseOrderByDateRelanceAsc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public RelanceClientDTO createRelance(Long factureId, LocalDate dateRelance, String note) {
        Facture facture = findFactureOrThrow(factureId);
        return toDTO(relanceClientRepository.save(RelanceClient.builder()
                .facture(facture)
                .dateRelance(dateRelance != null ? dateRelance : LocalDate.now())
                .note(note).envoyee(false)
                .build()));
    }

    public RelanceClientDTO marquerRelanceEnvoyee(Long id) {
        RelanceClient r = relanceClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relance non trouvée"));
        r.setEnvoyee(true);
        return toDTO(relanceClientRepository.save(r));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Facture findFactureOrThrow(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private FactureDTO toDTO(Facture f) {
        return FactureDTO.builder()
                .id(f.getId()).numero(f.getNumero()).type(f.getType())
                .clientId(f.getClient() != null ? f.getClient().getId() : null)
                .clientNom(f.getClient() != null ? f.getClient().getNom() : null)
                .dateEmission(f.getDateEmission()).dateEcheance(f.getDateEcheance())
                .lignes(f.getLignes()).totalHtManuel(f.getTotalHtManuel())
                .totalHt(f.getTotalHt()).tauxTva(f.getTauxTva()).montantTva(f.getMontantTva())
                .timbreFiscal(f.getTimbreFiscal()).totalTtc(f.getTotalTtc())
                .montantPaye(f.getMontantPaye())
                .montantRestant(round3(Math.max(0, nz(f.getTotalTtc()) - nz(f.getMontantPaye()))))
                .statut(f.getStatut()).paidAt(f.getPaidAt()).notes(f.getNotes())
                .build();
    }

    private PaiementFactureDTO toDTO(PaiementFacture p) {
        return PaiementFactureDTO.builder()
                .id(p.getId()).factureId(p.getFacture().getId()).montant(p.getMontant())
                .datePaiement(p.getDatePaiement()).note(p.getNote())
                .build();
    }

    private ServiceCatalogueDTO toDTO(ServiceCatalogue s) {
        return ServiceCatalogueDTO.builder()
                .id(s.getId()).designation(s.getDesignation()).prixDefaut(s.getPrixDefaut())
                .categorie(s.getCategorie()).description(s.getDescription()).actif(s.getActif())
                .build();
    }

    private TemplateFactureDTO toDTO(TemplateFacture t) {
        return TemplateFactureDTO.builder()
                .id(t.getId()).nom(t.getNom()).type(t.getType()).lignes(t.getLignes())
                .tauxTva(t.getTauxTva()).timbreFiscal(t.getTimbreFiscal()).notes(t.getNotes())
                .build();
    }

    private RelanceClientDTO toDTO(RelanceClient r) {
        Facture f = r.getFacture();
        return RelanceClientDTO.builder()
                .id(r.getId()).factureId(f.getId()).factureNumero(f.getNumero())
                .clientNom(f.getClient() != null ? f.getClient().getNom() : null)
                .dateRelance(r.getDateRelance()).envoyee(r.getEnvoyee()).note(r.getNote())
                .build();
    }
}

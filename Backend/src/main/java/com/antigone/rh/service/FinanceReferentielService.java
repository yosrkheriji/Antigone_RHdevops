package com.antigone.rh.service;

import com.antigone.rh.dto.ClientFactureDTO;
import com.antigone.rh.dto.EmployePaieDTO;
import com.antigone.rh.dto.ParametresFacturationDTO;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.Referentiel;
import com.antigone.rh.enums.TypeReferentiel;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.ReferentielRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alimente le module Finance en données de référence : clients et employés déjà
 * stockés en base, et paramètres de facturation issus des référentiels.
 *
 * <p>Existe parce que {@code /api/clients} et {@code /api/employes} sont gardés par
 * les permissions RH/Projets : un comptable n'a que {@code VIEW_FINANCE}. Ces
 * endpoints exposent donc une vue restreinte aux seuls champs utiles à la finance.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FinanceReferentielService {

    private final ClientRepository clientRepository;
    private final EmployeRepository employeRepository;
    private final ReferentielRepository referentielRepository;
    private final PayrollCalculator payrollCalculator;

    // ── Clients ──────────────────────────────────────────────────────────────

    /** Tous les clients enregistrés, triés par nom. */
    public List<ClientFactureDTO> getClients() {
        return clientRepository.findAll().stream()
                .sorted(Comparator.comparing(Client::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Employés ─────────────────────────────────────────────────────────────

    /**
     * Employés dont le contrat couvre au moins une partie du mois demandé, archivés
     * inclus : un départ en cours de mois donne droit à un salaire au prorata.
     */
    public List<EmployePaieDTO> getEmployesDuMois(String mois) {
        return employeRepository.findAll().stream()
                .filter(e -> payrollCalculator.estActifPourMois(e, mois))
                .sorted(Comparator.comparing(Employe::getNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employe::getPrenom, String.CASE_INSENSITIVE_ORDER))
                .map(e -> toDTO(e, true))
                .collect(Collectors.toList());
    }

    /** Tous les employés, sans filtre de mois. */
    public List<EmployePaieDTO> getTousEmployes() {
        return employeRepository.findAll().stream()
                .sorted(Comparator.comparing(Employe::getNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employe::getPrenom, String.CASE_INSENSITIVE_ORDER))
                .map(e -> toDTO(e, null))
                .collect(Collectors.toList());
    }

    // ── Paramètres de facturation ────────────────────────────────────────────

    public ParametresFacturationDTO getParametresFacturation() {
        return ParametresFacturationDTO.builder()
                .tvaDefaut(getParametreDouble("TVA_DEFAUT", 19.0))
                .timbreFiscalDefaut(getParametreDouble("TIMBRE_FISCAL_DEFAUT", 1.0))
                .cyclesChargeFixe(getCyclesChargeFixe())
                .categoriesRevenu(getLibelles(TypeReferentiel.CATEGORIE_REVENU))
                .categoriesCharge(getLibelles(TypeReferentiel.CATEGORIE_CHARGE))
                .build();
    }

    /** Taux de TVA par défaut, lu depuis les référentiels. */
    public double getTvaDefaut() {
        return getParametreDouble("TVA_DEFAUT", 19.0);
    }

    /** Timbre fiscal par défaut, lu depuis les référentiels. */
    public double getTimbreFiscalDefaut() {
        return getParametreDouble("TIMBRE_FISCAL_DEFAUT", 1.0);
    }

    private List<Integer> getCyclesChargeFixe() {
        String valeur = referentielRepository
                .findByLibelleAndTypeReferentiel("CYCLES_CHARGE_FIXE", TypeReferentiel.PARAMETRE_SYSTEME)
                .map(Referentiel::getValeur)
                .orElse("1,3,6,12,24,48");

        return Arrays.stream(valeur.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        log.warn("Cycle de charge fixe invalide ignoré : {}", s);
                        return null;
                    }
                })
                .filter(v -> v != null && v > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getLibelles(TypeReferentiel type) {
        return referentielRepository.findByTypeReferentielAndActifTrue(type).stream()
                .map(Referentiel::getLibelle)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private double getParametreDouble(String libelle, double defaut) {
        return referentielRepository
                .findByLibelleAndTypeReferentiel(libelle, TypeReferentiel.PARAMETRE_SYSTEME)
                .map(Referentiel::getValeur)
                .map(v -> {
                    try {
                        return Double.parseDouble(v.trim());
                    } catch (NumberFormatException e) {
                        log.warn("Paramètre {} illisible ({}), valeur par défaut utilisée", libelle, v);
                        return defaut;
                    }
                })
                .orElse(defaut);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ClientFactureDTO toDTO(Client c) {
        return ClientFactureDTO.builder()
                .id(c.getId()).nom(c.getNom()).email(c.getEmail()).telephone(c.getTelephone())
                .adresse(c.getAdresse())
                .matriculeFiscale(c.getMatriculeFiscale()).rne(c.getRne())
                .cycleFacturation(c.getCycleFacturation())
                .emailReceiverNom(c.getEmailReceiverNom()).emailReceiverGenre(c.getEmailReceiverGenre())
                .contactNom(c.getContactNom()).contactEmail(c.getContactEmail())
                .contactTelephone(c.getContactTelephone())
                .build();
    }

    private EmployePaieDTO toDTO(Employe e, Boolean actifCeMois) {
        return EmployePaieDTO.builder()
                .id(e.getId()).matricule(e.getMatricule())
                .nom(e.getNom()).prenom(e.getPrenom()).email(e.getEmail())
                .cin(e.getCin()).cnss(e.getCnss()).ribBancaire(e.getRibBancaire())
                .poste(e.getPoste()).departement(e.getDepartement())
                .typeContrat(e.getTypeContrat())
                .dateEmbauche(e.getDateEmbauche()).dateFinContrat(e.getDateFinContrat())
                .salaire(e.getSalaire())
                .modeSalaire(e.getModeSalaire() != null ? e.getModeSalaire() : "BRUT")
                .archived(e.getArchived())
                .dateArchivage(e.getArchivedAt() != null ? e.getArchivedAt().toLocalDate() : null)
                .actifCeMois(actifCeMois)
                .exonere(payrollCalculator.estExonere(e.getTypeContrat()))
                .build();
    }
}

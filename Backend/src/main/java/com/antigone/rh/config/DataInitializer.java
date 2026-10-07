package com.antigone.rh.config;

import com.antigone.rh.dto.TrancheIrppDTO;
import com.antigone.rh.entity.BaremeIrpp;
import com.antigone.rh.entity.Compte;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.Permission;
import com.antigone.rh.entity.Referentiel;
import com.antigone.rh.entity.Role;
import com.antigone.rh.enums.TypeConge;
import com.antigone.rh.enums.TypeReferentiel;
import com.antigone.rh.repository.BaremeIrppRepository;
import com.antigone.rh.repository.CompteRepository;
import com.antigone.rh.repository.EmployeRepository;
import com.antigone.rh.repository.PermissionRepository;
import com.antigone.rh.repository.ReferentielRepository;
import com.antigone.rh.repository.RoleRepository;
import com.antigone.rh.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ReferentielRepository referentielRepository;
    private final EmployeRepository employeRepository;
    private final CompteRepository compteRepository;
    private final PasswordEncoder passwordEncoder;
    private final BaremeIrppRepository baremeIrppRepository;

    @Override
    public void run(String... args) {
        // 1. Initialisation des permissions et rôles
        initPermissionsAndRoles();

        // 2. Initialisation des paramètres système (référentiels)
        initSystemParameters();

        // 3. Initialisation des types de congé
        initTypeConge();

        // 4. Référentiels essentiels pour le bon fonctionnement
        initGenres();
        initTypesContrat();
        initDepartements();
        initPostes();
        initNiveauxHierarchiques();
        initSitesEtablissement();
        initTypesDemande();
        initFormatsMediaPlan();
        initTypesMediaPlan();
        initPlateformesMediaPlan();
        initCategoriesRevenu();
        initCategoriesCharge();

        // 5. Création du compte admin par défaut
        initDefaultAdmin();

        // 6. Barème IRPP par défaut (finance / paie)
        initBaremeIrpp();
    }

    // ===================== Barème IRPP (Finance) =====================

    private void initBaremeIrpp() {
        if (!baremeIrppRepository.findAll().isEmpty()) return;

        List<TrancheIrppDTO> tranches = List.of(
                TrancheIrppDTO.builder().plafond(5000.0).taux(0.0).build(),
                TrancheIrppDTO.builder().plafond(10000.0).taux(0.15).build(),
                TrancheIrppDTO.builder().plafond(20000.0).taux(0.25).build(),
                TrancheIrppDTO.builder().plafond(30000.0).taux(0.30).build(),
                TrancheIrppDTO.builder().plafond(40000.0).taux(0.33).build(),
                TrancheIrppDTO.builder().plafond(50000.0).taux(0.36).build(),
                TrancheIrppDTO.builder().plafond(70000.0).taux(0.38).build(),
                TrancheIrppDTO.builder().plafond(null).taux(0.40).build());

        baremeIrppRepository.save(BaremeIrpp.builder()
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .tranches(tranches)
                .notes("Barème IRPP par défaut — Tunisie")
                .build());
        log.info("Barème IRPP par défaut créé");
    }

    // ===================== Permissions & Rôles =====================

    private void initPermissionsAndRoles() {
        log.info("Initialisation des permissions...");
        roleService.initPermissions();
        log.info("Permissions initialisées avec succès");

        // Créer le rôle ADMIN avec toutes les permissions s'il n'existe pas
        if (roleRepository.findByNom("ADMIN").isEmpty()) {
            List<Permission> allPermissions = permissionRepository.findAll();
            Role adminRole = Role.builder()
                    .nom("ADMIN")
                    .permissions(new HashSet<>(allPermissions))
                    .build();
            roleRepository.save(adminRole);
            log.info("Rôle ADMIN créé avec toutes les permissions ({})", allPermissions.size());
        } else {
            // Mettre à jour le rôle ADMIN existant pour s'assurer qu'il a toutes les
            // permissions
            Role adminRole = roleRepository.findByNom("ADMIN").get();
            List<Permission> allPermissions = permissionRepository.findAll();
            adminRole.setPermissions(new HashSet<>(allPermissions));
            roleRepository.save(adminRole);
            log.info("Rôle ADMIN mis à jour avec toutes les permissions ({})", allPermissions.size());
        }
    }

    // ===================== Paramètres Système =====================

    private void initSystemParameters() {
        createParamIfNotExists("MAX_AUTORISATION_MINUTES", "120",
                "Nombre maximum de minutes d'autorisation par mois par employé",
                TypeReferentiel.PARAMETRE_SYSTEME);

        // Solde congé basé sur l'ancienneté
        createParamIfNotExists("SOLDE_CONGE_AN1", "18",
                "Solde congé annuel pour la 1ère année (1.5 jours/mois × 12)",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("SOLDE_CONGE_AN2_PLUS", "24",
                "Solde congé annuel à partir de la 2ème année (2 jours/mois × 12)",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("TAUX_MENSUEL_AN1", "1.5",
                "Taux d'acquisition congé mensuel pour la 1ère année",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("TAUX_MENSUEL_AN2_PLUS", "2",
                "Taux d'acquisition congé mensuel à partir de la 2ème année",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("MAX_REPORT_CONGE", "5",
                "Nombre maximum de jours de congé reportables d'une année sur l'autre",
                TypeReferentiel.PARAMETRE_SYSTEME);

        // --- Paramètres réseau / Agent Desktop ---
        createParamIfNotExists("SSID_ENTREPRISE", "",
                "SSID WiFi du réseau de l'entreprise (ex: Antigone-Corp). Laisser vide si non applicable.",
                TypeReferentiel.PARAMETRE_SYSTEME);

        // --- Paramètres de facturation (module Finance) ---
        createParamIfNotExists("TVA_DEFAUT", "19",
                "Taux de TVA par défaut appliqué aux factures (en %)",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("TIMBRE_FISCAL_DEFAUT", "1.000",
                "Montant du timbre fiscal par défaut sur les factures (en DT)",
                TypeReferentiel.PARAMETRE_SYSTEME);

        createParamIfNotExists("CYCLES_CHARGE_FIXE", "1,3,6,12,24,48",
                "Périodicités proposées pour les charges fixes récurrentes (en mois, séparées par des virgules)",
                TypeReferentiel.PARAMETRE_SYSTEME);
    }

    // ===================== Types de Congé =====================

    private void initTypeConge() {
        for (TypeConge tc : TypeConge.values()) {
            createParamIfNotExists(tc.name(), null, tc.getLabel(), TypeReferentiel.TYPE_CONGE);
        }
    }

    // ===================== Genres =====================

    private void initGenres() {
        createParamIfNotExists("Masculin", null, "Genre masculin", TypeReferentiel.GENRE);
        createParamIfNotExists("Féminin", null, "Genre féminin", TypeReferentiel.GENRE);
    }

    // ===================== Types de contrat =====================

    private void initTypesContrat() {
        createParamIfNotExists("CDI", null, "Contrat à Durée Indéterminée", TypeReferentiel.TYPE_CONTRAT);
        createParamIfNotExists("CDD", null, "Contrat à Durée Déterminée", TypeReferentiel.TYPE_CONTRAT);
        createParamIfNotExists("Stage", null, "Convention de stage", TypeReferentiel.TYPE_CONTRAT);
        createParamIfNotExists("Freelance", null, "Contrat freelance / prestataire", TypeReferentiel.TYPE_CONTRAT);
        createParamIfNotExists("Alternance", null, "Contrat d'alternance / apprentissage", TypeReferentiel.TYPE_CONTRAT);
    }

    // ===================== Départements =====================

    private void initDepartements() {
        createParamIfNotExists("Direction Générale", null, "Direction générale de l'entreprise", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Ressources Humaines", null, "Département RH", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Finance & Comptabilité", null, "Département finance et comptabilité", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Informatique & Technique", null, "Département IT et technique", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Commercial & Ventes", null, "Département commercial et ventes", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Marketing & Communication", null, "Département marketing et communication", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Production", null, "Département production", TypeReferentiel.DEPARTEMENT);
        createParamIfNotExists("Juridique", null, "Département juridique", TypeReferentiel.DEPARTEMENT);
    }

    // ===================== Postes =====================

    private void initPostes() {
        createParamIfNotExists("Directeur Général", null, "DG / CEO", TypeReferentiel.POSTE);
        createParamIfNotExists("Responsable RH", null, "Responsable des ressources humaines", TypeReferentiel.POSTE);
        createParamIfNotExists("Chef de Projet", null, "Chef de projet", TypeReferentiel.POSTE);
        createParamIfNotExists("Développeur", null, "Développeur informatique", TypeReferentiel.POSTE);
        createParamIfNotExists("Designer", null, "Designer graphique / UX", TypeReferentiel.POSTE);
        createParamIfNotExists("Commercial", null, "Commercial / Chargé de ventes", TypeReferentiel.POSTE);
        createParamIfNotExists("Comptable", null, "Comptable", TypeReferentiel.POSTE);
        createParamIfNotExists("Assistant(e) Administratif(ve)", null, "Assistant administratif", TypeReferentiel.POSTE);
        createParamIfNotExists("Stagiaire", null, "Stagiaire", TypeReferentiel.POSTE);
    }

    // ===================== Niveaux Hiérarchiques =====================

    private void initNiveauxHierarchiques() {
        createParamIfNotExists("Junior", null, "Profil junior (0-2 ans d'expérience)", TypeReferentiel.NIVEAU_HIERARCHIQUE);
        createParamIfNotExists("Confirmé", null, "Profil confirmé (2-5 ans d'expérience)", TypeReferentiel.NIVEAU_HIERARCHIQUE);
        createParamIfNotExists("Senior", null, "Profil senior (5+ ans d'expérience)", TypeReferentiel.NIVEAU_HIERARCHIQUE);
        createParamIfNotExists("Manager", null, "Manager d'équipe", TypeReferentiel.NIVEAU_HIERARCHIQUE);
        createParamIfNotExists("Directeur", null, "Directeur de département", TypeReferentiel.NIVEAU_HIERARCHIQUE);
    }

    // ===================== Sites / Établissements =====================

    private void initSitesEtablissement() {
        createParamIfNotExists("Siège principal", null, "Siège principal de l'entreprise", TypeReferentiel.SITE_ETABLISSEMENT);
        createParamIfNotExists("Télétravail", null, "Travail à distance / home office", TypeReferentiel.SITE_ETABLISSEMENT);
    }

    // ===================== Types de Demande =====================

    private void initTypesDemande() {
        createParamIfNotExists("Attestation de travail", null, "Demande d'attestation de travail", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Attestation de salaire", null, "Demande d'attestation de salaire", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Ordre de mission", null, "Demande d'ordre de mission", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Bulletin de paie", null, "Demande de bulletin de paie", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Autorisation de sortie", null, "Demande d'autorisation de sortie anticipée", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Avance sur salaire", null, "Demande d'avance sur salaire", TypeReferentiel.TYPE_DEMANDE);
        createParamIfNotExists("Autre", null, "Autre type de demande", TypeReferentiel.TYPE_DEMANDE);
    }

    // ===================== Formats Media Plan =====================

    private void initFormatsMediaPlan() {
        createParamIfNotExists("Bannière", null, "Bannière publicitaire", TypeReferentiel.FORMAT_MEDIA_PLAN);
        createParamIfNotExists("Story", null, "Format story (vertical)", TypeReferentiel.FORMAT_MEDIA_PLAN);
        createParamIfNotExists("Post carré", null, "Post format carré 1:1", TypeReferentiel.FORMAT_MEDIA_PLAN);
        createParamIfNotExists("Vidéo", null, "Format vidéo", TypeReferentiel.FORMAT_MEDIA_PLAN);
        createParamIfNotExists("Carrousel", null, "Format carrousel multi-images", TypeReferentiel.FORMAT_MEDIA_PLAN);
        createParamIfNotExists("Réels / Short", null, "Vidéo courte format reels/shorts", TypeReferentiel.FORMAT_MEDIA_PLAN);
    }

    // ===================== Types Media Plan =====================

    private void initTypesMediaPlan() {
        createParamIfNotExists("Organique", null, "Publication organique sans budget publicitaire", TypeReferentiel.TYPE_MEDIA_PLAN);
        createParamIfNotExists("Payant", null, "Publication sponsorisée / publicité payante", TypeReferentiel.TYPE_MEDIA_PLAN);
        createParamIfNotExists("Partenariat", null, "Publication en partenariat ou collaboration", TypeReferentiel.TYPE_MEDIA_PLAN);
    }

    // ===================== Plateformes Media Plan =====================

    private void initPlateformesMediaPlan() {
        createParamIfNotExists("Instagram", null, "Réseau social Instagram", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("Facebook", null, "Réseau social Facebook", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("LinkedIn", null, "Réseau social LinkedIn", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("TikTok", null, "Réseau social TikTok", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("YouTube", null, "Plateforme vidéo YouTube", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("Twitter / X", null, "Réseau social Twitter/X", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("Google Ads", null, "Régie publicitaire Google", TypeReferentiel.PLATFORME_MEDIA_PLAN);
        createParamIfNotExists("Site Web", null, "Site web de l'entreprise", TypeReferentiel.PLATFORME_MEDIA_PLAN);
    }

    // ===================== Catégories de revenu (Finance) =====================

    private void initCategoriesRevenu() {
        createParamIfNotExists("Consulting", null, "Prestation de conseil", TypeReferentiel.CATEGORIE_REVENU);
        createParamIfNotExists("Vente", null, "Vente de produit ou de licence", TypeReferentiel.CATEGORIE_REVENU);
        createParamIfNotExists("Subvention", null, "Subvention ou aide publique", TypeReferentiel.CATEGORIE_REVENU);
        createParamIfNotExists("Remboursement", null, "Remboursement reçu", TypeReferentiel.CATEGORIE_REVENU);
        createParamIfNotExists("Loyer", null, "Revenu locatif", TypeReferentiel.CATEGORIE_REVENU);
        createParamIfNotExists("Autre", null, "Autre type de revenu", TypeReferentiel.CATEGORIE_REVENU);
    }

    // ===================== Catégories de charge (Finance) =====================

    private void initCategoriesCharge() {
        createParamIfNotExists("Loyer", null, "Loyer des locaux", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Abonnements & logiciels", null, "Licences et abonnements SaaS", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Télécom & Internet", null, "Téléphonie et connexion", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Énergie", null, "Électricité, eau, gaz", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Assurance", null, "Contrats d'assurance", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Fournitures", null, "Fournitures de bureau et consommables", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Prestations externes", null, "Sous-traitance et freelances", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Transport & déplacement", null, "Carburant, missions, déplacements", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Honoraires", null, "Comptable, juridique, conseil", TypeReferentiel.CATEGORIE_CHARGE);
        createParamIfNotExists("Autre", null, "Autre type de charge", TypeReferentiel.CATEGORIE_CHARGE);
    }

    private void createParamIfNotExists(String libelle, String valeur, String description, TypeReferentiel type) {
        if (referentielRepository.findByLibelleAndTypeReferentiel(libelle, type).isEmpty()) {
            Referentiel param = Referentiel.builder()
                    .libelle(libelle)
                    .valeur(valeur)
                    .description(description)
                    .typeReferentiel(type)
                    .actif(true)
                    .build();
            referentielRepository.save(param);
            log.info("Référentiel créé: [{}] {} = {}", type, libelle, valeur);
        }
    }

    // ===================== Compte Admin par défaut =====================

    private void initDefaultAdmin() {
        // Récupérer le rôle ADMIN
        Role adminRole = roleRepository.findByNom("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN introuvable"));

        if (compteRepository.existsByUsername("admin")) {
            // Réinitialiser le mot de passe admin à chaque démarrage
            Compte adminCompte = compteRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("Compte admin introuvable"));
            adminCompte.setPasswordHash(passwordEncoder.encode("Admin@123"));
            adminCompte.setEnabled(true);
            adminCompte.setMustChangePassword(true);
            adminCompte.setRoles(Set.of(adminRole));
            compteRepository.save(adminCompte);
            log.info("Compte admin réinitialisé - username: admin, mot de passe: Admin@123 (changement obligatoire à la première connexion)");
            return;
        }

        // Créer l'employé admin
        Employe adminEmploye = Employe.builder()
                .matricule("ADMIN001")
                .nom("Admin")
                .prenom("System")
                .email("admin@antigone.tn")
                .dateEmbauche(LocalDate.now())
                .poste("Administrateur Système")
                .build();
        adminEmploye = employeRepository.save(adminEmploye);
        log.info("Employé admin créé: {} {}", adminEmploye.getPrenom(), adminEmploye.getNom());

        // Créer le compte admin (mot de passe: Admin@123)
        Compte adminCompte = Compte.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .mustChangePassword(true)
                .employe(adminEmploye)
                .roles(Set.of(adminRole))
                .build();
        compteRepository.save(adminCompte);
        log.info("Compte admin créé - username: admin, mot de passe: Admin@123 (changement obligatoire à la première connexion)");
    }
}

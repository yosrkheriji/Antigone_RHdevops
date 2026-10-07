package com.antigone.rh.ai.support;

import com.antigone.rh.entity.*;
import com.antigone.rh.enums.EtatPublication;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.StatutMediaPlan;
import com.antigone.rh.enums.StatutPaie;
import com.antigone.rh.enums.StatutProjet;
import com.antigone.rh.enums.TypeDocument;
import com.antigone.rh.repository.*;
import com.antigone.rh.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Jeux de donnees pour les tests d'integration.
 *
 * <p>Construit de vraies lignes dans les tables metier — pas de doublures — afin que
 * les tests de cloisonnement portent sur le SQL reellement execute.
 */
@Component
@RequiredArgsConstructor
public class TestFixtures {

    private final EmployeRepository employeRepository;
    private final CompteRepository compteRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final MediaPlanRepository mediaPlanRepository;
    private final MediaPlanAssignmentRepository assignmentRepository;
    private final FactureRepository factureRepository;
    private final BulletinPaieRepository bulletinRepository;
    private final com.antigone.rh.repository.ParametresPaieRepository parametresPaieRepository;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private int next() {
        return COUNTER.incrementAndGet();
    }

    // ---- Comptes et identites ---------------------------------------------

    public Employe employe(String nom, String prenom) {
        int n = next();
        return employeRepository.save(Employe.builder()
                .matricule("MAT-" + n)
                .nom(nom)
                .prenom(prenom)
                .email("user" + n + "@antigone.tn")
                // L'entite impose un CIN de 8 chiffres exactement.
                .cin(String.format("%08d", 10_000_000 + n))
                .dateEmbauche(LocalDate.of(2023, 1, 1))
                .salaire(2500.0)
                .poste("Collaborateur")
                .archived(false)
                .build());
    }

    public Compte compte(Employe employe, String roleName, String... permissions) {
        Set<Permission> perms = new HashSet<>();
        for (String permission : permissions) {
            perms.add(permissionRepository.findByPermission(permission)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder().permission(permission).build())));
        }
        String uniqueRole = roleName + "-" + next();
        Role role = roleRepository.save(Role.builder().nom(uniqueRole).permissions(perms).build());

        return compteRepository.save(Compte.builder()
                .username("user" + next())
                .passwordHash("{noop}x")
                .employe(employe)
                .roles(new HashSet<>(Set.of(role)))
                .enabled(true)
                .build());
    }

    /** Principal equivalent a celui qu'aurait produit le JWT pour ce compte. */
    public AuthPrincipal principal(Compte compte) {
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        compte.getRoles().forEach(role -> {
            roles.add(role.getNom());
            role.getPermissions().forEach(permission -> permissions.add(permission.getPermission()));
        });
        return AuthPrincipal.builder()
                .principalType("EMPLOYEE")
                .accountId(compte.getId())
                .employeId(compte.getEmploye().getId())
                .username(compte.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /** Compte social media limite aux marques qui lui sont assignees. */
    public AuthPrincipal socialMediaPrincipal(Employe employe) {
        return principal(compte(employe, "SOCIAL_MEDIA", "VIEW_MEDIA_PLAN"));
    }

    public AuthPrincipal adminPrincipal(Employe employe) {
        Compte compte = compte(employe, "ADMIN", "VIEW_FINANCE", "VIEW_TOUS_MEDIA_PLAN");
        AuthPrincipal base = principal(compte);
        Set<String> roles = new HashSet<>(base.getRoles());
        roles.add("ADMIN");
        return AuthPrincipal.builder()
                .principalType("EMPLOYEE")
                .accountId(base.getAccountId())
                .employeId(base.getEmployeId())
                .username(base.getUsername())
                .roles(roles)
                .permissions(base.getPermissions())
                .build();
    }

    /** Employe simple : aucune permission au-dela de son propre espace. */
    public AuthPrincipal employeePrincipal(Employe employe) {
        return principal(compte(employe, "EMPLOYE", "VIEW_MES_DEMANDES"));
    }

    // ---- Metier ------------------------------------------------------------

    public Client client(String nom) {
        return clientRepository.save(Client.builder()
                .nom(nom)
                .email("contact@" + nom.toLowerCase().replace(" ", "") + ".tn")
                .identite("Marque " + nom + ", ton chaleureux et direct")
                .activite("Pret-a-porter feminin")
                .positionnement("Accessible et engage sur le fait-main tunisien")
                .objectifs("Augmenter la notoriete et generer du trafic en boutique")
                .emailReceiverNom("Responsable " + nom)
                .emailReceiverGenre("F")
                .build());
    }

    public void assign(Employe employe, Client client) {
        assignmentRepository.save(MediaPlanAssignment.builder()
                .employe(employe)
                .client(client)
                .build());
    }

    public Projet projet(Client client, String nom, LocalDate debut, LocalDate fin) {
        return projetRepository.save(Projet.builder()
                .nom(nom)
                .client(client)
                .statut(StatutProjet.EN_COURS)
                .dateDebut(debut)
                .dateFin(fin)
                .description("Action " + nom + " pour " + client.getNom())
                .build());
    }

    public MediaPlan publication(Client client, Employe createur, LocalDate date,
                                 String titre, String plateforme, String format, String type) {
        return mediaPlanRepository.save(MediaPlan.builder()
                .client(client)
                .createur(createur)
                .datePublication(date)
                .heure("10:00")
                .titre(titre)
                .texteSurVisuel("Visuel : " + titre)
                .inspiration("Inspiration " + titre)
                .platforme(plateforme)
                .format(format)
                .type(type)
                .lienDrive("https://drive.google.com/drive/folders/historique")
                .etatPublication(EtatPublication.PUBLIEE)
                .statut(StatutMediaPlan.APPROUVE)
                .build());
    }

    /** Trois mois d'historique, thematiques distinctes et reperables. */
    public List<MediaPlan> historique(Client client, Employe createur, LocalDate moisCible) {
        return List.of(
                publication(client, createur, moisCible.minusMonths(3).withDayOfMonth(8),
                        "Coulisses de l atelier de couture", "Instagram", "Reel", "Brand content"),
                publication(client, createur, moisCible.minusMonths(2).withDayOfMonth(14),
                        "Portrait de nos artisanes", "Instagram", "Carrousel", "Storytelling"),
                publication(client, createur, moisCible.minusMonths(1).withDayOfMonth(21),
                        "Lancement de la collection printemps", "Facebook", "Video", "Produit"));
    }

    public Facture facture(Client client, String numero, LocalDate echeance,
                           double totalTtc, double montantPaye) {
        return factureRepository.save(Facture.builder()
                .numero(numero)
                .type(TypeDocument.FACTURE)
                .client(client)
                .dateEmission(echeance.minusDays(30))
                .dateEcheance(echeance)
                .totalHt(totalTtc / 1.19)
                .tauxTva(19.0)
                .totalTtc(totalTtc)
                .montantPaye(montantPaye)
                .statut(montantPaye > 0 ? StatutFacture.PARTIEL : StatutFacture.EN_ATTENTE)
                .build());
    }

    /**
     * Parametres de paie, crees a la demande.
     *
     * En production, la ligne existe des qu'une paie a ete calculee — un bulletin
     * ne peut pas exister sans elle. La fixture inserant des bulletins directement,
     * elle doit reproduire cette contrainte, sinon les taux manqueraient au
     * contexte alors qu'ils sont toujours presents en vrai.
     */
    public com.antigone.rh.entity.ParametresPaie parametresPaie() {
        return parametresPaieRepository.findById(1L).orElseGet(() -> parametresPaieRepository.save(
                com.antigone.rh.entity.ParametresPaie.builder()
                        .id(1L)
                        .cnssSalarie(0.0918)
                        .solidariteSalarie(0.0050)
                        .cnssPatronale(0.1657)
                        .tfp(0.01)
                        .foprolos(0.01)
                        .at(0.004)
                        .abattement(0.10)
                        .notes("Taux par defaut — Tunisie")
                        .dateMiseAJour(java.time.LocalDateTime.now())
                        .build()));
    }

    public BulletinPaie bulletin(Employe employe, String mois, double brut, double netAPayer,
                                 double irpp, double cnss, double acompte) {
        // Un bulletin implique des parametres de paie : on les garantit ici.
        parametresPaie();
        return bulletinRepository.save(BulletinPaie.builder()
                .employe(employe)
                .mois(mois)
                .salaireBrut(brut)
                .brutEffectif(brut)
                .cnssSalarie(cnss)
                .salaireImposable(brut - cnss)
                .revenuNetImposable(brut - cnss)
                .irppMensuel(irpp)
                .net(netAPayer + acompte)
                .acompte(acompte)
                .netAPayer(netAPayer)
                .statut(StatutPaie.IMPAYE)
                .build());
    }
}

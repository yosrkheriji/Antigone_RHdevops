package com.antigone.rh.bi;

import com.antigone.rh.bi.dto.EtatEntrepotDTO;
import com.antigone.rh.bi.dto.FinanceDashboardDTO;
import com.antigone.rh.bi.dto.PresenceDashboardDTO;
import com.antigone.rh.bi.dto.ProjetsDashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Interroge l'entrepot {@code dwh} pour alimenter les pages Analytique.
 *
 * <p>Les requetes portent uniquement sur le schema decisionnel, jamais sur les
 * tables OLTP : c'est ce qui garantit que les ecrans de saisie ne ralentissent
 * pas quand un tableau de bord agrege plusieurs annees d'historique.
 *
 * <p>Les agregats sont volontairement calcules en SQL (et non en Java) : c'est
 * la meme logique que celle qu'appliquera Power BI sur les memes tables, donc
 * les deux restitutions donnent les memes chiffres.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbc;

    // ========================================================================
    // 1. Presence et productivite
    // ========================================================================

    public PresenceDashboardDTO presence(LocalDate debut, LocalDate fin, String departement) {
        Date d = Date.valueOf(debut);
        Date f = Date.valueOf(fin);
        // Chaine vide plutot que null : evite a Postgres d'avoir a deviner le
        // type d'un parametre nul dans la comparaison.
        String dep = departement == null ? "" : departement.trim();

        PresenceDashboardDTO.Kpis kpis = jdbc.queryForObject("""
                SELECT COUNT(*)                                        AS jours_suivis,
                       COUNT(DISTINCT p.employe_key)                   AS effectif,
                       COALESCE(AVG(p.est_present) * 100, 0)           AS taux_presence,
                       COALESCE(AVG(p.est_absent) * 100, 0)            AS taux_absenteisme,
                       COALESCE(AVG(p.est_teletravail) * 100, 0)       AS taux_teletravail,
                       COALESCE(SUM(p.heures_travaillees), 0)          AS heures,
                       COALESCE(AVG(NULLIF(p.retard_minutes, 0)), 0)   AS retard_moyen,
                       COALESCE(SUM(p.est_retard), 0)                  AS jours_retard
                FROM dwh.fait_presence p
                JOIN dwh.dim_employe e ON e.employe_key = p.employe_key
                WHERE p.date_key BETWEEN ? AND ?
                  AND (? = '' OR e.departement = ?)
                """,
                (rs, i) -> new PresenceDashboardDTO.Kpis(
                        rs.getLong("jours_suivis"),
                        rs.getLong("effectif"),
                        arrondi(rs.getDouble("taux_presence")),
                        arrondi(rs.getDouble("taux_absenteisme")),
                        arrondi(rs.getDouble("taux_teletravail")),
                        arrondi(rs.getDouble("heures")),
                        arrondi(rs.getDouble("retard_moyen")),
                        rs.getLong("jours_retard")),
                d, f, dep, dep);

        List<PresenceDashboardDTO.PointMensuel> evolution = jdbc.query("""
                SELECT dd.mois_cle,
                       MIN(dd.mois_libelle || ' ' || dd.annee::TEXT)   AS libelle,
                       COALESCE(AVG(p.est_present) * 100, 0)           AS taux_presence,
                       COALESCE(AVG(p.est_absent) * 100, 0)            AS taux_absenteisme,
                       COALESCE(SUM(p.heures_travaillees), 0)          AS heures
                FROM dwh.fait_presence p
                JOIN dwh.dim_date    dd ON dd.date_key    = p.date_key
                JOIN dwh.dim_employe e  ON e.employe_key  = p.employe_key
                WHERE p.date_key BETWEEN ? AND ?
                  AND (? = '' OR e.departement = ?)
                GROUP BY dd.mois_cle
                ORDER BY dd.mois_cle
                """,
                (rs, i) -> new PresenceDashboardDTO.PointMensuel(
                        rs.getString("mois_cle"),
                        rs.getString("libelle"),
                        arrondi(rs.getDouble("taux_presence")),
                        arrondi(rs.getDouble("taux_absenteisme")),
                        arrondi(rs.getDouble("heures"))),
                d, f, dep, dep);

        List<PresenceDashboardDTO.RepartitionStatut> statuts = jdbc.query("""
                SELECT p.statut, COUNT(*) AS jours
                FROM dwh.fait_presence p
                JOIN dwh.dim_employe e ON e.employe_key = p.employe_key
                WHERE p.date_key BETWEEN ? AND ?
                  AND (? = '' OR e.departement = ?)
                GROUP BY p.statut
                ORDER BY jours DESC
                """,
                (rs, i) -> new PresenceDashboardDTO.RepartitionStatut(
                        rs.getString("statut"), rs.getLong("jours")),
                d, f, dep, dep);

        List<PresenceDashboardDTO.LigneDepartement> parDepartement = jdbc.query("""
                SELECT e.departement,
                       COUNT(DISTINCT p.employe_key)                 AS effectif,
                       COALESCE(AVG(p.est_present) * 100, 0)         AS taux_presence,
                       COALESCE(AVG(NULLIF(p.retard_minutes, 0)), 0) AS retard_moyen,
                       COALESCE(SUM(p.heures_travaillees), 0)        AS heures
                FROM dwh.fait_presence p
                JOIN dwh.dim_employe e ON e.employe_key = p.employe_key
                WHERE p.date_key BETWEEN ? AND ?
                GROUP BY e.departement
                ORDER BY effectif DESC
                """,
                (rs, i) -> new PresenceDashboardDTO.LigneDepartement(
                        rs.getString("departement"),
                        rs.getLong("effectif"),
                        arrondi(rs.getDouble("taux_presence")),
                        arrondi(rs.getDouble("retard_moyen")),
                        arrondi(rs.getDouble("heures"))),
                d, f);

        List<PresenceDashboardDTO.LigneEmploye> topRetards = jdbc.query("""
                SELECT e.nom_complet, e.departement,
                       SUM(p.retard_minutes)                  AS retard_total,
                       SUM(p.est_retard)                      AS jours_retard,
                       COALESCE(SUM(p.heures_travaillees), 0) AS heures
                FROM dwh.fait_presence p
                JOIN dwh.dim_employe e ON e.employe_key = p.employe_key
                WHERE p.date_key BETWEEN ? AND ?
                  AND (? = '' OR e.departement = ?)
                GROUP BY e.nom_complet, e.departement
                HAVING SUM(p.retard_minutes) > 0
                ORDER BY retard_total DESC
                LIMIT 10
                """,
                (rs, i) -> new PresenceDashboardDTO.LigneEmploye(
                        rs.getString("nom_complet"),
                        rs.getString("departement"),
                        rs.getLong("retard_total"),
                        rs.getLong("jours_retard"),
                        arrondi(rs.getDouble("heures"))),
                d, f, dep, dep);

        List<PresenceDashboardDTO.PointInactivite> inactivite = jdbc.query("""
                SELECT to_char(fi.date_key, 'YYYY-MM-DD')        AS semaine,
                       COALESCE(SUM(fi.inactivite_excedentaire), 0) AS minutes,
                       COALESCE(SUM(fi.montant_deduction), 0)       AS deduction
                FROM dwh.fait_inactivite fi
                JOIN dwh.dim_employe e ON e.employe_key = fi.employe_key
                WHERE fi.date_key BETWEEN ? AND ?
                  AND (? = '' OR e.departement = ?)
                GROUP BY fi.date_key
                ORDER BY fi.date_key
                """,
                (rs, i) -> new PresenceDashboardDTO.PointInactivite(
                        rs.getString("semaine"),
                        rs.getLong("minutes"),
                        arrondi(rs.getDouble("deduction"))),
                d, f, dep, dep);

        List<PresenceDashboardDTO.LigneConge> conges = jdbc.query("""
                SELECT fc.type_conge,
                       COALESCE(SUM(fc.nombre_jours), 0) AS jours,
                       COUNT(*)                          AS demandes
                FROM dwh.fait_conge fc
                JOIN dwh.dim_employe e ON e.employe_key = fc.employe_key
                WHERE fc.date_key BETWEEN ? AND ?
                  AND fc.est_approuve = 1
                  AND (? = '' OR e.departement = ?)
                GROUP BY fc.type_conge
                ORDER BY jours DESC
                """,
                (rs, i) -> new PresenceDashboardDTO.LigneConge(
                        rs.getString("type_conge"),
                        rs.getLong("jours"),
                        rs.getLong("demandes")),
                d, f, dep, dep);

        return new PresenceDashboardDTO(kpis, evolution, statuts, parDepartement, topRetards, inactivite, conges);
    }

    /** Departements presents dans l'entrepot — alimente le selecteur de filtre. */
    public List<String> departements() {
        return jdbc.queryForList(
                "SELECT DISTINCT departement FROM dwh.dim_employe WHERE employe_key <> -1 ORDER BY 1",
                String.class);
    }

    // ========================================================================
    // 2. Finance et tresorerie
    // ========================================================================

    public FinanceDashboardDTO finance(LocalDate debut, LocalDate fin) {
        Date d = Date.valueOf(debut);
        Date f = Date.valueOf(fin);

        FinanceDashboardDTO.Kpis kpis = jdbc.queryForObject("""
                WITH flux AS (
                    SELECT
                        COALESCE(SUM(CASE WHEN base = 'ENGAGEMENT' AND sens = 'ENTREE'
                                          THEN montant_ht END), 0) AS produits,
                        COALESCE(SUM(CASE WHEN base = 'ENGAGEMENT' AND sens = 'SORTIE'
                                          THEN montant_ht END), 0) AS charges,
                        COALESCE(SUM(CASE WHEN base = 'ENGAGEMENT' AND sens = 'SORTIE'
                                           AND nature = 'SALAIRE' THEN montant_ht END), 0) AS masse,
                        COALESCE(SUM(CASE WHEN base = 'TRESORERIE' AND sens = 'ENTREE'
                                          THEN montant_ttc END), 0) AS encaissements,
                        COALESCE(SUM(CASE WHEN base = 'TRESORERIE' AND sens = 'SORTIE'
                                          THEN montant_ttc END), 0) AS decaissements
                    FROM dwh.fait_finance
                    WHERE date_key BETWEEN ? AND ?
                ), creances AS (
                    SELECT COALESCE(SUM(reste_du), 0) AS en_cours,
                           COALESCE(SUM(CASE WHEN jours_retard > 0 THEN reste_du END), 0) AS en_retard
                    FROM dwh.fait_facture
                    WHERE type_document = 'FACTURE' AND date_key BETWEEN ? AND ?
                )
                SELECT flux.*, creances.en_cours, creances.en_retard FROM flux, creances
                """,
                (rs, i) -> {
                    double produits = rs.getDouble("produits");
                    double charges = rs.getDouble("charges");
                    double encaissements = rs.getDouble("encaissements");
                    double decaissements = rs.getDouble("decaissements");
                    double resultat = produits - charges;
                    return new FinanceDashboardDTO.Kpis(
                            arrondi(produits),
                            arrondi(charges),
                            arrondi(rs.getDouble("masse")),
                            arrondi(resultat),
                            produits == 0 ? 0 : arrondi(resultat * 100 / produits),
                            arrondi(encaissements),
                            arrondi(decaissements),
                            arrondi(encaissements - decaissements),
                            arrondi(rs.getDouble("en_cours")),
                            arrondi(rs.getDouble("en_retard")));
                },
                d, f, d, f);

        List<FinanceDashboardDTO.PointMensuel> evolution = jdbc.query("""
                SELECT dd.mois_cle,
                       MIN(dd.mois_libelle || ' ' || dd.annee::TEXT) AS libelle,
                       COALESCE(SUM(CASE WHEN ff.base = 'ENGAGEMENT' AND ff.sens = 'ENTREE'
                                         THEN ff.montant_ht END), 0) AS produits,
                       COALESCE(SUM(CASE WHEN ff.base = 'ENGAGEMENT' AND ff.sens = 'SORTIE'
                                         THEN ff.montant_ht END), 0) AS charges,
                       COALESCE(SUM(CASE WHEN ff.base = 'TRESORERIE' AND ff.sens = 'ENTREE'
                                         THEN ff.montant_ttc END), 0) AS encaissements,
                       COALESCE(SUM(CASE WHEN ff.base = 'TRESORERIE' AND ff.sens = 'SORTIE'
                                         THEN ff.montant_ttc END), 0) AS decaissements
                FROM dwh.fait_finance ff
                JOIN dwh.dim_date dd ON dd.date_key = ff.date_key
                WHERE ff.date_key BETWEEN ? AND ?
                GROUP BY dd.mois_cle
                ORDER BY dd.mois_cle
                """,
                (rs, i) -> {
                    double produits = rs.getDouble("produits");
                    double charges = rs.getDouble("charges");
                    return new FinanceDashboardDTO.PointMensuel(
                            rs.getString("mois_cle"),
                            rs.getString("libelle"),
                            arrondi(produits),
                            arrondi(charges),
                            arrondi(produits - charges),
                            arrondi(rs.getDouble("encaissements")),
                            arrondi(rs.getDouble("decaissements")));
                },
                d, f);

        List<FinanceDashboardDTO.RepartitionCharge> repartition = jdbc.query("""
                SELECT nature, COALESCE(SUM(montant_ht), 0) AS montant
                FROM dwh.fait_finance
                WHERE base = 'ENGAGEMENT' AND sens = 'SORTIE' AND date_key BETWEEN ? AND ?
                GROUP BY nature
                ORDER BY montant DESC
                """,
                (rs, i) -> new FinanceDashboardDTO.RepartitionCharge(
                        rs.getString("nature"), arrondi(rs.getDouble("montant"))),
                d, f);

        List<FinanceDashboardDTO.LigneClient> topClients = jdbc.query("""
                SELECT c.nom,
                       COALESCE(SUM(ff.total_ht), 0) AS ca_ht,
                       COUNT(*)                      AS nb_factures,
                       COALESCE(SUM(ff.reste_du), 0) AS reste_du
                FROM dwh.fait_facture ff
                JOIN dwh.dim_client c ON c.client_key = ff.client_key
                WHERE ff.type_document = 'FACTURE' AND ff.date_key BETWEEN ? AND ?
                GROUP BY c.nom
                ORDER BY ca_ht DESC
                LIMIT 10
                """,
                (rs, i) -> new FinanceDashboardDTO.LigneClient(
                        rs.getString("nom"),
                        arrondi(rs.getDouble("ca_ht")),
                        rs.getLong("nb_factures"),
                        arrondi(rs.getDouble("reste_du"))),
                d, f);

        List<FinanceDashboardDTO.TrancheAge> balanceAgee = jdbc.query("""
                SELECT tranche_age,
                       COALESCE(SUM(reste_du), 0) AS montant,
                       COUNT(*)                   AS nb_factures
                FROM dwh.fait_facture
                WHERE type_document = 'FACTURE' AND reste_du > 0 AND date_key BETWEEN ? AND ?
                GROUP BY tranche_age
                ORDER BY MIN(jours_retard)
                """,
                (rs, i) -> new FinanceDashboardDTO.TrancheAge(
                        rs.getString("tranche_age"),
                        arrondi(rs.getDouble("montant")),
                        rs.getLong("nb_factures")),
                d, f);

        List<FinanceDashboardDTO.FactureEnRetard> enRetard = jdbc.query("""
                SELECT ff.numero, c.nom,
                       to_char(ff.date_echeance, 'YYYY-MM-DD') AS echeance,
                       ff.reste_du, ff.jours_retard
                FROM dwh.fait_facture ff
                JOIN dwh.dim_client c ON c.client_key = ff.client_key
                WHERE ff.jours_retard > 0 AND ff.reste_du > 0 AND ff.date_key BETWEEN ? AND ?
                ORDER BY ff.jours_retard DESC
                LIMIT 15
                """,
                (rs, i) -> new FinanceDashboardDTO.FactureEnRetard(
                        rs.getString("numero"),
                        rs.getString("nom"),
                        rs.getString("echeance"),
                        arrondi(rs.getDouble("reste_du")),
                        rs.getLong("jours_retard")),
                d, f);

        return new FinanceDashboardDTO(kpis, evolution, repartition, topClients, balanceAgee, enRetard);
    }

    // ========================================================================
    // 3. Projets
    // ========================================================================

    public ProjetsDashboardDTO projets(LocalDate debut, LocalDate fin) {
        Date d = Date.valueOf(debut);
        Date f = Date.valueOf(fin);

        ProjetsDashboardDTO.Kpis kpis = jdbc.queryForObject("""
                WITH projets_actifs AS (
                    SELECT COUNT(*) AS total,
                           SUM(CASE WHEN statut = 'EN_COURS' THEN 1 ELSE 0 END) AS en_cours,
                           SUM(CASE WHEN statut IN ('CLOTURE', 'CLOTURE_INCOMPLET') THEN 1 ELSE 0 END) AS clotures,
                           COALESCE(AVG(duree_jours), 0) AS duree_moyenne
                    FROM dwh.dim_projet
                    WHERE projet_key <> -1
                      AND (date_creation IS NULL OR date_creation <= ?)
                      AND (date_cloture IS NULL OR date_cloture >= ?)
                ), taches AS (
                    SELECT COUNT(*)                   AS total,
                           COALESCE(SUM(est_terminee), 0)  AS terminees,
                           COALESCE(SUM(est_en_retard), 0) AS en_retard,
                           COALESCE(AVG(CASE WHEN est_terminee = 1 THEN duree_reelle_jours END), 0) AS delai_moyen
                    FROM dwh.fait_tache
                    WHERE date_key BETWEEN ? AND ?
                )
                SELECT p.total AS projets_total, p.en_cours, p.clotures, p.duree_moyenne,
                       t.total AS taches_total, t.terminees, t.en_retard, t.delai_moyen
                FROM projets_actifs p, taches t
                """,
                (rs, i) -> {
                    long tachesTotal = rs.getLong("taches_total");
                    long terminees = rs.getLong("terminees");
                    return new ProjetsDashboardDTO.Kpis(
                            rs.getLong("projets_total"),
                            rs.getLong("en_cours"),
                            rs.getLong("clotures"),
                            tachesTotal,
                            terminees,
                            rs.getLong("en_retard"),
                            tachesTotal == 0 ? 0 : arrondi(terminees * 100.0 / tachesTotal),
                            arrondi(rs.getDouble("delai_moyen")),
                            arrondi(rs.getDouble("duree_moyenne")));
                },
                f, d, d, f);

        List<ProjetsDashboardDTO.RepartitionStatut> repartition = jdbc.query("""
                SELECT statut, COUNT(*) AS nombre
                FROM dwh.dim_projet
                WHERE projet_key <> -1
                  AND (date_creation IS NULL OR date_creation <= ?)
                  AND (date_cloture IS NULL OR date_cloture >= ?)
                GROUP BY statut
                ORDER BY nombre DESC
                """,
                (rs, i) -> new ProjetsDashboardDTO.RepartitionStatut(
                        rs.getString("statut"), rs.getLong("nombre")),
                f, d);

        List<ProjetsDashboardDTO.PointMensuel> evolution = jdbc.query("""
                SELECT dd.mois_cle,
                       MIN(dd.mois_libelle || ' ' || dd.annee::TEXT) AS libelle,
                       COUNT(*)                                      AS creees,
                       COALESCE(SUM(t.est_terminee), 0)              AS terminees,
                       COALESCE(SUM(t.est_en_retard), 0)             AS en_retard
                FROM dwh.fait_tache t
                JOIN dwh.dim_date dd ON dd.date_key = t.date_key
                WHERE t.date_key BETWEEN ? AND ?
                GROUP BY dd.mois_cle
                ORDER BY dd.mois_cle
                """,
                (rs, i) -> new ProjetsDashboardDTO.PointMensuel(
                        rs.getString("mois_cle"),
                        rs.getString("libelle"),
                        rs.getLong("creees"),
                        rs.getLong("terminees"),
                        rs.getLong("en_retard")),
                d, f);

        List<ProjetsDashboardDTO.LigneCollaborateur> charge = jdbc.query("""
                SELECT e.nom_complet, e.departement,
                       COUNT(*)                          AS total,
                       COALESCE(SUM(t.est_terminee), 0)  AS terminees,
                       COALESCE(SUM(t.est_en_retard), 0) AS en_retard,
                       COALESCE(AVG(CASE WHEN t.est_terminee = 1 THEN t.duree_reelle_jours END), 0) AS delai
                FROM dwh.fait_tache t
                JOIN dwh.dim_employe e ON e.employe_key = t.assignee_key
                WHERE t.date_key BETWEEN ? AND ? AND t.assignee_key <> -1
                GROUP BY e.nom_complet, e.departement
                ORDER BY total DESC
                LIMIT 15
                """,
                (rs, i) -> {
                    long total = rs.getLong("total");
                    long terminees = rs.getLong("terminees");
                    return new ProjetsDashboardDTO.LigneCollaborateur(
                            rs.getString("nom_complet"),
                            rs.getString("departement"),
                            total,
                            terminees,
                            rs.getLong("en_retard"),
                            total == 0 ? 0 : arrondi(terminees * 100.0 / total),
                            arrondi(rs.getDouble("delai")));
                },
                d, f);

        List<ProjetsDashboardDTO.ProjetARisque> risques = jdbc.query("""
                SELECT p.nom AS projet, c.nom AS client, p.statut,
                       to_char(p.date_fin, 'YYYY-MM-DD')  AS date_fin,
                       COUNT(t.tache_key)                 AS total,
                       COALESCE(SUM(t.est_en_retard), 0)  AS en_retard,
                       COALESCE(SUM(t.est_terminee), 0)   AS terminees
                FROM dwh.dim_projet p
                JOIN dwh.dim_client c      ON c.client_key = p.client_key
                LEFT JOIN dwh.fait_tache t ON t.projet_key = p.projet_key
                WHERE p.projet_key <> -1
                  AND p.statut NOT IN ('CLOTURE', 'ANNULE')
                GROUP BY p.nom, c.nom, p.statut, p.date_fin
                HAVING COALESCE(SUM(t.est_en_retard), 0) > 0
                ORDER BY en_retard DESC
                LIMIT 12
                """,
                (rs, i) -> {
                    long total = rs.getLong("total");
                    long terminees = rs.getLong("terminees");
                    return new ProjetsDashboardDTO.ProjetARisque(
                            rs.getString("projet"),
                            rs.getString("client"),
                            rs.getString("statut"),
                            rs.getString("date_fin"),
                            total,
                            rs.getLong("en_retard"),
                            total == 0 ? 0 : arrondi(terminees * 100.0 / total));
                });

        List<ProjetsDashboardDTO.LigneDelai> delais = jdbc.query("""
                SELECT p.nom,
                       COALESCE(AVG(t.duree_reelle_jours), 0) AS delai,
                       COALESCE(AVG(t.ecart_jours), 0)        AS ecart,
                       COUNT(*)                               AS terminees
                FROM dwh.fait_tache t
                JOIN dwh.dim_projet p ON p.projet_key = t.projet_key
                WHERE t.est_terminee = 1
                  AND t.duree_reelle_jours IS NOT NULL
                  AND t.date_key BETWEEN ? AND ?
                GROUP BY p.nom
                ORDER BY delai DESC
                LIMIT 12
                """,
                (rs, i) -> new ProjetsDashboardDTO.LigneDelai(
                        rs.getString("nom"),
                        arrondi(rs.getDouble("delai")),
                        arrondi(rs.getDouble("ecart")),
                        rs.getLong("terminees")),
                d, f);

        return new ProjetsDashboardDTO(kpis, repartition, evolution, charge, risques, delais);
    }

    // ========================================================================
    // 4. Etat de l'entrepot
    // ========================================================================

    public EtatEntrepotDTO etat() {
        List<EtatEntrepotDTO.VolumeTable> volumes = jdbc.query("""
                SELECT 'fait_presence'   AS t, COUNT(*) AS n FROM dwh.fait_presence
                UNION ALL SELECT 'fait_inactivite', COUNT(*) FROM dwh.fait_inactivite
                UNION ALL SELECT 'fait_conge',      COUNT(*) FROM dwh.fait_conge
                UNION ALL SELECT 'fait_finance',    COUNT(*) FROM dwh.fait_finance
                UNION ALL SELECT 'fait_facture',    COUNT(*) FROM dwh.fait_facture
                UNION ALL SELECT 'fait_tache',      COUNT(*) FROM dwh.fait_tache
                """,
                (rs, i) -> new EtatEntrepotDTO.VolumeTable(rs.getString("t"), rs.getLong("n")));

        List<EtatEntrepotDTO> dernier = jdbc.query("""
                SELECT to_char(date_execution, 'YYYY-MM-DD HH24:MI') AS quand,
                       duree_ms, lignes_chargees, statut, message
                FROM dwh.etl_log
                ORDER BY id DESC
                LIMIT 1
                """,
                (rs, i) -> new EtatEntrepotDTO(
                        rs.getString("quand"),
                        rs.getLong("duree_ms"),
                        rs.getLong("lignes_chargees"),
                        rs.getString("statut"),
                        rs.getString("message"),
                        volumes));

        return dernier.isEmpty()
                ? new EtatEntrepotDTO(null, null, null, "JAMAIS_CHARGE", null, volumes)
                : dernier.get(0);
    }

    /** Deux decimales : au-dela, le bruit de la virgule flottante s'affiche a l'ecran. */
    private static double arrondi(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}

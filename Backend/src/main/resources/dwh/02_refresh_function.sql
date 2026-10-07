-- ============================================================================
-- Procedure ETL — reconstruit integralement l'entrepot dwh depuis l'OLTP.
--
-- Strategie : rechargement complet (truncate + insert). Au volume d'une agence
-- (quelques dizaines de milliers de lignes) il s'execute en quelques secondes,
-- et il est bien plus simple a justifier qu'un chargement incremental : aucun
-- risque de desynchronisation apres une correction manuelle en base.
--
-- Appelee par DwhEtlService (planifiee chaque nuit) ou manuellement :
--     SELECT dwh.refresh_all();
-- ============================================================================

CREATE OR REPLACE FUNCTION dwh.refresh_all() RETURNS BIGINT AS $BODY$
DECLARE
    v_debut    TIMESTAMP := clock_timestamp();
    v_date_min DATE;
    v_date_max DATE;
    v_lignes   BIGINT;
BEGIN
    -- Un seul TRUNCATE pour toutes les tables : l'ordre des cles etrangeres
    -- n'a pas a etre respecte tant qu'aucune table referencee n'est omise.
    TRUNCATE TABLE
        dwh.fait_presence,
        dwh.fait_inactivite,
        dwh.fait_conge,
        dwh.fait_finance,
        dwh.fait_facture,
        dwh.fait_tache,
        dwh.dim_projet,
        dwh.dim_client,
        dwh.dim_employe,
        dwh.dim_date
    RESTART IDENTITY;

    -- ------------------------------------------------------------------
    -- Dimension temps
    -- Bornes calculees sur les donnees reelles : si une source contient une
    -- date anterieure a 2020, la dimension s'etend au lieu de rejeter le fait
    -- (les faits sont joints a dim_date en INNER JOIN).
    -- ------------------------------------------------------------------
    SELECT LEAST(
        DATE '2020-01-01',
        COALESCE((SELECT MIN(date_pointage)        FROM pointages),                DATE '2020-01-01'),
        COALESCE((SELECT MIN(date_emission)        FROM factures),                 DATE '2020-01-01'),
        COALESCE((SELECT MIN(date_debut)           FROM conges),                   DATE '2020-01-01'),
        COALESCE((SELECT MIN(semaine_debut)        FROM rapports_inactivite),      DATE '2020-01-01'),
        COALESCE((SELECT MIN(date_creation)::DATE  FROM taches),                   DATE '2020-01-01'),
        COALESCE((SELECT MIN(date_paiement)        FROM paiements_factures),       DATE '2020-01-01'),
        COALESCE((SELECT MIN(date_paiement)        FROM paiements_charges_fixes),  DATE '2020-01-01'),
        COALESCE((SELECT MIN("date")               FROM charges_variables),        DATE '2020-01-01'),
        COALESCE((SELECT MIN("date")               FROM autres_revenus),           DATE '2020-01-01'),
        COALESCE((SELECT MIN(to_date(mois || '-01', 'YYYY-MM-DD')) FROM bulletins_paie), DATE '2020-01-01')
    ) INTO v_date_min;

    SELECT GREATEST(
        CURRENT_DATE,
        COALESCE((SELECT MAX(date_pointage)        FROM pointages),                CURRENT_DATE),
        COALESCE((SELECT MAX(date_emission)        FROM factures),                 CURRENT_DATE),
        COALESCE((SELECT MAX(date_echeance)        FROM factures),                 CURRENT_DATE),
        COALESCE((SELECT MAX(date_fin)             FROM conges),                   CURRENT_DATE),
        COALESCE((SELECT MAX(date_echeance)        FROM taches),                   CURRENT_DATE),
        COALESCE((SELECT MAX(date_fin)             FROM projets),                  CURRENT_DATE)
    ) + 365 INTO v_date_max;

    INSERT INTO dwh.dim_date (date_key, annee, trimestre, mois, mois_cle, mois_libelle,
                              semaine_iso, jour, jour_semaine, jour_semaine_libelle, est_weekend)
    SELECT g::DATE,
           EXTRACT(YEAR    FROM g)::SMALLINT,
           EXTRACT(QUARTER FROM g)::SMALLINT,
           EXTRACT(MONTH   FROM g)::SMALLINT,
           to_char(g, 'YYYY-MM'),
           CASE EXTRACT(MONTH FROM g)::INT
               WHEN  1 THEN 'Janvier'   WHEN  2 THEN 'Fevrier'  WHEN  3 THEN 'Mars'
               WHEN  4 THEN 'Avril'     WHEN  5 THEN 'Mai'      WHEN  6 THEN 'Juin'
               WHEN  7 THEN 'Juillet'   WHEN  8 THEN 'Aout'     WHEN  9 THEN 'Septembre'
               WHEN 10 THEN 'Octobre'   WHEN 11 THEN 'Novembre' ELSE 'Decembre'
           END,
           EXTRACT(WEEK  FROM g)::SMALLINT,
           EXTRACT(DAY   FROM g)::SMALLINT,
           EXTRACT(ISODOW FROM g)::SMALLINT,
           CASE EXTRACT(ISODOW FROM g)::INT
               WHEN 1 THEN 'Lundi'    WHEN 2 THEN 'Mardi'  WHEN 3 THEN 'Mercredi'
               WHEN 4 THEN 'Jeudi'    WHEN 5 THEN 'Vendredi' WHEN 6 THEN 'Samedi'
               ELSE 'Dimanche'
           END,
           EXTRACT(ISODOW FROM g)::INT >= 6
    FROM generate_series(v_date_min, v_date_max, INTERVAL '1 day') AS g;

    -- ------------------------------------------------------------------
    -- Dimension employe (+ membre inconnu -1)
    -- ------------------------------------------------------------------
    INSERT INTO dwh.dim_employe (employe_key, matricule, nom_complet, poste, departement,
                                 type_contrat, genre, date_embauche, anciennete_mois, manager_nom, actif)
    VALUES (-1, NULL, 'Non affecte', NULL, 'Non renseigne', 'Non renseigne', NULL, NULL, NULL, NULL, false);

    INSERT INTO dwh.dim_employe (employe_key, matricule, nom_complet, poste, departement,
                                 type_contrat, genre, date_embauche, anciennete_mois, manager_nom, actif)
    SELECT e.id,
           e.matricule,
           TRIM(COALESCE(e.prenom, '') || ' ' || COALESCE(e.nom, '')),
           e.poste,
           COALESCE(NULLIF(TRIM(e.departement), ''), 'Non renseigne'),
           COALESCE(NULLIF(TRIM(e.type_contrat), ''), 'Non renseigne'),
           e.genre,
           e.date_embauche,
           CASE WHEN e.date_embauche IS NOT NULL
                THEN (EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.date_embauche)) * 12
                    + EXTRACT(MONTH FROM AGE(CURRENT_DATE, e.date_embauche)))::INTEGER
           END,
           NULLIF(TRIM(COALESCE(m.prenom, '') || ' ' || COALESCE(m.nom, '')), ''),
           NOT COALESCE(e.archived, false)
    FROM employes e
    LEFT JOIN employes m ON m.id = e.manager_id;

    -- ------------------------------------------------------------------
    -- Dimension client (+ membre inconnu -1)
    -- ------------------------------------------------------------------
    INSERT INTO dwh.dim_client (client_key, nom, matricule_fiscale, cycle_facturation, date_creation)
    VALUES (-1, 'Client non renseigne', NULL, NULL, NULL);

    INSERT INTO dwh.dim_client (client_key, nom, matricule_fiscale, cycle_facturation, date_creation)
    SELECT c.id, c.nom, c.matricule_fiscale, c.cycle_facturation, c.date_creation::DATE
    FROM clients c;

    -- ------------------------------------------------------------------
    -- Dimension projet (+ membre inconnu -1)
    -- ------------------------------------------------------------------
    INSERT INTO dwh.dim_projet (projet_key, nom, statut, type_projet, client_key, chef_nom,
                                date_debut, date_fin, date_creation, date_cloture, cloture_forcee, duree_jours)
    VALUES (-1, 'Projet non renseigne', 'INCONNU', NULL, -1, NULL, NULL, NULL, NULL, NULL, false, NULL);

    INSERT INTO dwh.dim_projet (projet_key, nom, statut, type_projet, client_key, chef_nom,
                                date_debut, date_fin, date_creation, date_cloture, cloture_forcee, duree_jours)
    SELECT p.id,
           p.nom,
           COALESCE(p.statut, 'INCONNU'),
           p.type_projet,
           COALESCE(p.client_id, -1),
           NULLIF(TRIM(COALESCE(ch.prenom, '') || ' ' || COALESCE(ch.nom, '')), ''),
           p.date_debut,
           p.date_fin,
           p.date_creation::DATE,
           p.date_cloture::DATE,
           COALESCE(p.cloture_forcee, false),
           CASE WHEN p.date_cloture IS NOT NULL AND p.date_creation IS NOT NULL
                THEN (p.date_cloture::DATE - p.date_creation::DATE) END
    FROM projets p
    LEFT JOIN employes ch ON ch.id = p.chef_projet_id;

    -- ------------------------------------------------------------------
    -- Fait 1 — presence quotidienne
    -- ------------------------------------------------------------------
    INSERT INTO dwh.fait_presence (date_key, employe_key, statut, heures_travaillees, retard_minutes,
                                   est_present, est_retard, est_absent, est_conge, est_teletravail,
                                   sur_reseau_entreprise)
    SELECT d.date_key,
           COALESCE(pt.employe_id, -1),
           COALESCE(NULLIF(TRIM(pt.statut), ''), 'INCONNU'),
           CASE WHEN pt.heure_entree IS NOT NULL AND pt.heure_sortie IS NOT NULL
                THEN ROUND(GREATEST(EXTRACT(EPOCH FROM (pt.heure_sortie - pt.heure_entree)) / 3600.0, 0)::NUMERIC, 2)
                ELSE 0 END,
           COALESCE(pt.retard_minutes, 0),
           CASE WHEN pt.statut IN ('PRESENT', 'RETARD', 'TELETRAVAIL') THEN 1 ELSE 0 END,
           CASE WHEN pt.statut = 'RETARD' THEN 1 ELSE 0 END,
           CASE WHEN pt.statut = 'ABSENT' THEN 1 ELSE 0 END,
           CASE WHEN pt.statut IN ('EN_CONGE', 'EN_AUTORISATION') THEN 1 ELSE 0 END,
           CASE WHEN pt.statut = 'TELETRAVAIL' OR COALESCE(pt.teletravail, false) THEN 1 ELSE 0 END,
           CASE WHEN COALESCE(pt.sur_reseau_entreprise, false) THEN 1 ELSE 0 END
    FROM pointages pt
    JOIN dwh.dim_date    d ON d.date_key    = pt.date_pointage
    JOIN dwh.dim_employe e ON e.employe_key = COALESCE(pt.employe_id, -1);

    -- ------------------------------------------------------------------
    -- Fait 2 — inactivite hebdomadaire (agent desktop)
    -- ------------------------------------------------------------------
    INSERT INTO dwh.fait_inactivite (date_key, employe_key, semaine_fin, total_inactivite_minutes,
                                     inactivite_excedentaire, retard_cumule, montant_deduction, decision)
    SELECT d.date_key,
           COALESCE(r.employe_id, -1),
           r.semaine_fin,
           COALESCE(r.total_inactivite_minutes, 0),
           COALESCE(r.inactivite_excedentaire, 0),
           COALESCE(r.retard_cumule, 0),
           COALESCE(r.montant_deduction, 0),
           r.decision
    FROM rapports_inactivite r
    JOIN dwh.dim_date    d ON d.date_key    = r.semaine_debut
    JOIN dwh.dim_employe e ON e.employe_key = COALESCE(r.employe_id, -1);

    -- ------------------------------------------------------------------
    -- Fait 3 — conges
    -- ------------------------------------------------------------------
    INSERT INTO dwh.fait_conge (conge_key, date_key, employe_key, type_conge, statut, date_fin,
                                nombre_jours, jours_ouvrables, est_approuve)
    SELECT cg.id,
           d.date_key,
           COALESCE(dm.employe_id, -1),
           COALESCE(cg.type_conge, 'INCONNU'),
           COALESCE(dm.statut, 'INCONNU'),
           cg.date_fin,
           COALESCE(cg.nombre_jours, 0),
           COALESCE(cg.jours_ouvrables, 0),
           CASE WHEN dm.statut IN ('APPROUVEE', 'VALIDEE') THEN 1 ELSE 0 END
    FROM conges cg
    JOIN demandes        dm ON dm.id         = cg.id
    JOIN dwh.dim_date    d  ON d.date_key    = cg.date_debut
    JOIN dwh.dim_employe e  ON e.employe_key = COALESCE(dm.employe_id, -1);

    -- ------------------------------------------------------------------
    -- Fait 4 — flux financiers
    -- ------------------------------------------------------------------

    -- 4a. Produit : factures emises (les devis sont exclus, ils n'engagent rien).
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, COALESCE(f.client_id, -1), -1, 'ENTREE', 'ENGAGEMENT', 'FACTURE',
           'Facture ' || f.numero,
           COALESCE(f.total_ht, 0), COALESCE(f.montant_tva, 0), COALESCE(f.total_ttc, 0)
    FROM factures f
    JOIN dwh.dim_date   d ON d.date_key   = f.date_emission
    JOIN dwh.dim_client c ON c.client_key = COALESCE(f.client_id, -1)
    WHERE f.type = 'FACTURE';

    -- 4b. Tresorerie : encaissements reellement recus.
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, COALESCE(f.client_id, -1), -1, 'ENTREE', 'TRESORERIE', 'FACTURE',
           'Encaissement ' || f.numero, 0, 0, COALESCE(pf.montant, 0)
    FROM paiements_factures pf
    JOIN factures       f ON f.id         = pf.facture_id
    JOIN dwh.dim_date   d ON d.date_key   = pf.date_paiement
    JOIN dwh.dim_client c ON c.client_key = COALESCE(f.client_id, -1);

    -- 4c. Autres revenus — montant saisi TTC, HT reconstitue.
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, -1, -1, 'ENTREE', b.base, 'AUTRE_REVENU',
           ar.label,
           ROUND((ar.montant / (1 + COALESCE(ar.taux_tva, 0) / 100.0))::NUMERIC, 3),
           ROUND((ar.montant - ar.montant / (1 + COALESCE(ar.taux_tva, 0) / 100.0))::NUMERIC, 3),
           ar.montant
    FROM autres_revenus ar
    CROSS JOIN (VALUES ('ENGAGEMENT'), ('TRESORERIE')) AS b(base)
    JOIN dwh.dim_date d ON d.date_key = COALESCE(ar."date", to_date(ar.mois || '-01', 'YYYY-MM-DD'));

    -- 4d. Salaires — engagement au cout employeur total, tresorerie au net verse.
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, -1, COALESCE(bp.employe_id, -1), 'SORTIE', 'ENGAGEMENT', 'SALAIRE',
           'Paie ' || bp.mois,
           COALESCE(bp.cout_total, 0), 0, COALESCE(bp.cout_total, 0)
    FROM bulletins_paie bp
    JOIN dwh.dim_date    d ON d.date_key    = to_date(bp.mois || '-01', 'YYYY-MM-DD')
    JOIN dwh.dim_employe e ON e.employe_key = COALESCE(bp.employe_id, -1);

    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    -- `netapayer` et non `net_a_payer` : la strategie de nommage de Hibernate
    -- n'insere un underscore que devant une majuscule ENTOUREE de minuscules.
    -- Dans le champ Java `netAPayer`, le A est suivi du P majuscule, donc aucune
    -- coupure n'est introduite. C'est le seul champ du modele dans ce cas.
    SELECT d.date_key, -1, COALESCE(bp.employe_id, -1), 'SORTIE', 'TRESORERIE', 'SALAIRE',
           'Paie versee ' || bp.mois,
           COALESCE(bp.netapayer, 0), 0, COALESCE(bp.netapayer, 0)
    FROM bulletins_paie bp
    JOIN dwh.dim_date    d ON d.date_key    = COALESCE(bp.date_paiement, to_date(bp.mois || '-01', 'YYYY-MM-DD'))
    JOIN dwh.dim_employe e ON e.employe_key = COALESCE(bp.employe_id, -1)
    WHERE bp.statut IN ('PAYE', 'PARTIEL');

    -- 4e. Charges fixes — seuls les paiements enregistres sont connus, ils
    -- alimentent donc les deux bases.
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, -1, -1, 'SORTIE', b.base, 'CHARGE_FIXE',
           cf.label,
           ROUND((pcf.montant / (1 + COALESCE(cf.taux_tva, 0) / 100.0))::NUMERIC, 3),
           ROUND((pcf.montant - pcf.montant / (1 + COALESCE(cf.taux_tva, 0) / 100.0))::NUMERIC, 3),
           pcf.montant
    FROM paiements_charges_fixes pcf
    JOIN charges_fixes cf ON cf.id = pcf.charge_fixe_id
    CROSS JOIN (VALUES ('ENGAGEMENT'), ('TRESORERIE')) AS b(base)
    JOIN dwh.dim_date d ON d.date_key = pcf.date_paiement;

    -- 4f. Charges variables.
    INSERT INTO dwh.fait_finance (date_key, client_key, employe_key, sens, base, nature,
                                  libelle, montant_ht, montant_tva, montant_ttc)
    SELECT d.date_key, -1, -1, 'SORTIE', b.base, 'CHARGE_VARIABLE',
           cv.label,
           ROUND((cv.montant / (1 + COALESCE(cv.taux_tva, 0) / 100.0))::NUMERIC, 3),
           ROUND((cv.montant - cv.montant / (1 + COALESCE(cv.taux_tva, 0) / 100.0))::NUMERIC, 3),
           cv.montant
    FROM charges_variables cv
    CROSS JOIN (VALUES ('ENGAGEMENT'), ('TRESORERIE')) AS b(base)
    JOIN dwh.dim_date d ON d.date_key = COALESCE(cv."date", to_date(cv.mois || '-01', 'YYYY-MM-DD'));

    -- ------------------------------------------------------------------
    -- Fait 5 — factures (snapshot pour la balance agee)
    -- ------------------------------------------------------------------
    INSERT INTO dwh.fait_facture (facture_key, date_key, client_key, numero, type_document, date_echeance,
                                  statut, total_ht, montant_tva, total_ttc, montant_paye, reste_du,
                                  jours_retard, tranche_age)
    SELECT s.id, s.date_key, s.client_key, s.numero, s.type_document, s.date_echeance, s.statut,
           s.total_ht, s.montant_tva, s.total_ttc, s.montant_paye, s.reste_du, s.jours_retard,
           CASE WHEN s.reste_du <= 0        THEN 'Soldee'
                WHEN s.jours_retard = 0     THEN 'Non echue'
                WHEN s.jours_retard <= 30   THEN '1-30 jours'
                WHEN s.jours_retard <= 60   THEN '31-60 jours'
                WHEN s.jours_retard <= 90   THEN '61-90 jours'
                ELSE 'Plus de 90 jours'
           END
    FROM (
        SELECT f.id,
               d.date_key,
               COALESCE(f.client_id, -1)                                AS client_key,
               f.numero,
               f.type                                                   AS type_document,
               f.date_echeance,
               f.statut,
               COALESCE(f.total_ht, 0)                                  AS total_ht,
               COALESCE(f.montant_tva, 0)                               AS montant_tva,
               COALESCE(f.total_ttc, 0)                                 AS total_ttc,
               COALESCE(f.montant_paye, 0)                              AS montant_paye,
               GREATEST(COALESCE(f.total_ttc, 0) - COALESCE(f.montant_paye, 0), 0) AS reste_du,
               CASE WHEN f.date_echeance IS NOT NULL
                     AND f.date_echeance < CURRENT_DATE
                     AND COALESCE(f.total_ttc, 0) - COALESCE(f.montant_paye, 0) > 0
                    THEN (CURRENT_DATE - f.date_echeance) ELSE 0 END    AS jours_retard
        FROM factures f
        JOIN dwh.dim_date   d ON d.date_key   = f.date_emission
        JOIN dwh.dim_client c ON c.client_key = COALESCE(f.client_id, -1)
    ) s;

    -- ------------------------------------------------------------------
    -- Fait 6 — taches de projet
    -- ------------------------------------------------------------------
    INSERT INTO dwh.fait_tache (tache_key, date_key, projet_key, assignee_key, statut, urgente,
                                date_echeance, date_fin_execution, duree_prevue_jours, duree_reelle_jours,
                                ecart_jours, est_terminee, est_en_retard, retard_jours)
    SELECT s.id, s.date_key, s.projet_key, s.assignee_key, s.statut, s.urgente,
           s.date_echeance, s.date_fin, s.duree_prevue_jours, s.duree_reelle,
           CASE WHEN s.duree_prevue_jours IS NOT NULL AND s.duree_reelle IS NOT NULL
                THEN s.duree_reelle - s.duree_prevue_jours END,
           CASE WHEN s.statut = 'DONE' THEN 1 ELSE 0 END,
           CASE WHEN s.retard_jours > 0 THEN 1 ELSE 0 END,
           s.retard_jours
    FROM (
        SELECT t.id,
               d.date_key,
               COALESCE(t.projet_id, -1)   AS projet_key,
               COALESCE(t.assignee_id, -1) AS assignee_key,
               COALESCE(t.statut, 'TODO')  AS statut,
               COALESCE(t.urgente, false)  AS urgente,
               t.date_echeance,
               t.date_fin_execution::DATE  AS date_fin,
               t.duree_prevue_jours,
               CASE WHEN t.date_fin_execution IS NOT NULL
                    THEN t.date_fin_execution::DATE
                       - COALESCE(t.date_debut_execution::DATE, t.date_assignation::DATE, t.date_creation::DATE)
               END AS duree_reelle,
               CASE
                   WHEN t.date_echeance IS NULL THEN 0
                   WHEN t.statut = 'DONE' AND t.date_fin_execution IS NOT NULL
                        AND t.date_fin_execution::DATE > t.date_echeance
                        THEN (t.date_fin_execution::DATE - t.date_echeance)
                   WHEN t.statut <> 'DONE' AND t.date_echeance < CURRENT_DATE
                        THEN (CURRENT_DATE - t.date_echeance)
                   ELSE 0
               END AS retard_jours
        FROM taches t
        JOIN dwh.dim_date    d ON d.date_key     = COALESCE(t.date_creation::DATE, t.date_assignation::DATE, CURRENT_DATE)
        JOIN dwh.dim_projet  p ON p.projet_key   = COALESCE(t.projet_id, -1)
        JOIN dwh.dim_employe e ON e.employe_key  = COALESCE(t.assignee_id, -1)
        WHERE COALESCE(t.archived, false) = false
    ) s;

    -- ------------------------------------------------------------------
    -- Journalisation
    -- ------------------------------------------------------------------
    SELECT (SELECT COUNT(*) FROM dwh.fait_presence)
         + (SELECT COUNT(*) FROM dwh.fait_inactivite)
         + (SELECT COUNT(*) FROM dwh.fait_conge)
         + (SELECT COUNT(*) FROM dwh.fait_finance)
         + (SELECT COUNT(*) FROM dwh.fait_facture)
         + (SELECT COUNT(*) FROM dwh.fait_tache)
    INTO v_lignes;

    INSERT INTO dwh.etl_log (duree_ms, lignes_chargees, statut, message)
    VALUES ((EXTRACT(EPOCH FROM (clock_timestamp() - v_debut)) * 1000)::BIGINT,
            v_lignes, 'SUCCES',
            'Periode couverte : ' || v_date_min::TEXT || ' -> ' || v_date_max::TEXT);

    RETURN v_lignes;
END;
$BODY$ LANGUAGE plpgsql;

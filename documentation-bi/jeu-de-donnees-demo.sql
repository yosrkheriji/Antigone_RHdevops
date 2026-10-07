-- ============================================================================
-- Jeu de donnees de demonstration — Antigone 360°
--
-- Alimente les tables METIER (et non l'entrepot) sur les 12 derniers mois, afin
-- que les tableaux de bord decisionnels aient de quoi montrer des tendances.
-- L'ETL les reprendra au prochain rechargement : la chaine complete est donc
-- exercee, pas court-circuitee.
--
-- CARACTERISTIQUES
--   - Transactionnel : en cas d'erreur, rien n'est ecrit.
--   - Rejouable : chaque section se desactive si elle a deja tourne.
--   - Reversible : chaque enregistrement porte le marqueur 'JEU-DEMO'
--     (voir jeu-de-donnees-demo-nettoyage.sql).
--   - Deterministe : setseed fige le tirage aleatoire, deux executions sur une
--     base vierge produisent les memes chiffres.
--
-- UTILISATION
--   psql -U postgres -d antigone_rh -f jeu-de-donnees-demo.sql
--   puis, dans l'application : bouton « Recharger l'entrepot »
--
-- PREREQUIS : au moins un employe actif. Les sections Facturation supposent
-- au moins un client, la section Taches au moins un projet ; elles sont
-- simplement sautees sinon.
-- ============================================================================

BEGIN;

SELECT setseed(0.42);

-- ────────────────────────────────────────────────────────────────────────────
-- 1. POINTAGES — un enregistrement par employe actif et par jour ouvre
--    Distribution visee : ~58 % present, ~18 % retard, ~14 % teletravail,
--    ~6 % conge, ~4 % absent. Les week-ends sont exclus.
-- ────────────────────────────────────────────────────────────────────────────
-- random() est appele directement dans la liste de selection, et NON dans une
-- sous-requete LATERAL independante de la ligne courante : PostgreSQL calculerait
-- alors cette sous-requete une seule fois et reutiliserait le meme tirage pour
-- toutes les lignes (toutes les journees auraient le meme statut). Une CTE
-- contenant une fonction volatile est materialisee : r et r2 sont donc evalues
-- une fois PAR LIGNE, puis reutilises tels quels dans la CTE suivante.
WITH jours AS (
    SELECT e.id                        AS employe_id,
           d::DATE                     AS jour,
           random()                    AS r,
           random()                    AS r2
    FROM employes e
    CROSS JOIN generate_series(
        CURRENT_DATE - INTERVAL '12 months',
        CURRENT_DATE - INTERVAL '1 day',
        INTERVAL '1 day') AS d
    WHERE COALESCE(e.archived, false) = false
      AND EXTRACT(ISODOW FROM d) <= 5
      AND NOT EXISTS (
          SELECT 1 FROM pointages p
          WHERE p.employe_id = e.id AND p.date_pointage = d::DATE)
),
qualifies AS (
    SELECT employe_id,
           jour,
           r2,
           CASE WHEN r < 0.04 THEN 'ABSENT'
                WHEN r < 0.10 THEN 'EN_CONGE'
                WHEN r < 0.24 THEN 'TELETRAVAIL'
                WHEN r < 0.42 THEN 'RETARD'
                ELSE               'PRESENT'
           END AS statut,
           CASE WHEN r >= 0.24 AND r < 0.42
                THEN 5 + (r2 * 40)::INT ELSE 0 END AS retard
    FROM jours
)
INSERT INTO pointages (employe_id, date_pointage, heure_entree, heure_sortie,
                       ip_entree, ssid_entree, retard_minutes, statut,
                       sur_reseau_entreprise, teletravail, created_at)
SELECT employe_id,
       jour,
       CASE WHEN statut IN ('ABSENT', 'EN_CONGE') THEN NULL
            ELSE TIME '08:30' + (retard * INTERVAL '1 minute') END,
       CASE WHEN statut IN ('ABSENT', 'EN_CONGE') THEN NULL
            ELSE TIME '17:30' + (((r2 * 60)::INT - 30) * INTERVAL '1 minute') END,
       CASE WHEN statut IN ('ABSENT', 'EN_CONGE') THEN NULL
            ELSE '192.168.1.' || (10 + (r2 * 200)::INT)::TEXT END,
       'JEU-DEMO',
       retard,
       statut,
       statut IN ('PRESENT', 'RETARD'),
       statut = 'TELETRAVAIL',
       now()
FROM qualifies;

-- ────────────────────────────────────────────────────────────────────────────
-- 2. CONGES — quatre demandes approuvees par employe, etalees sur l'annee.
--    Conge herite de Demande (heritage JOINED) : il faut inserer dans les deux
--    tables avec le meme identifiant, d'ou la boucle.
-- ────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    r     RECORD;
    v_id  BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM demandes WHERE raison = 'JEU-DEMO') THEN
        RAISE NOTICE 'Conges : jeu deja present, section ignoree.';
        RETURN;
    END IF;

    FOR r IN
        SELECT e.id AS employe_id,
               (CURRENT_DATE - ((n * 80) + (random() * 25)::INT))::DATE AS debut,
               (ARRAY['CONGE_PAYE', 'CONGE_MALADIE', 'CONGE_PAYE',
                      'CONGE_RECUPERATION', 'CONGE_EXCEPTIONNEL']
               )[1 + (random() * 4)::INT] AS type_conge,
               1 + (random() * 4)::INT AS jours
        FROM employes e
        CROSS JOIN generate_series(1, 4) AS n
        WHERE COALESCE(e.archived, false) = false
    LOOP
        INSERT INTO demandes (type, date_creation, statut, raison, employe_id)
        VALUES ('CONGE', (r.debut - 7)::TIMESTAMP, 'APPROUVEE', 'JEU-DEMO', r.employe_id)
        RETURNING id INTO v_id;

        INSERT INTO conges (id, type_conge, date_debut, date_fin,
                            nombre_jours, jours_ouvrables)
        VALUES (v_id, r.type_conge, r.debut, r.debut + r.jours - 1, r.jours, r.jours);
    END LOOP;
END $$;

-- ────────────────────────────────────────────────────────────────────────────
-- 4. BULLETINS DE PAIE — un par employe actif et par mois.
--    Ratios volontairement simples (net = 78 % du brut, charges patronales
--    17 %) : ce jeu sert a peupler des graphiques, pas a verifier le moteur de
--    paie, qui a ses propres tests.
-- ────────────────────────────────────────────────────────────────────────────
INSERT INTO bulletins_paie (employe_id, mois, elements, salaire_brut, brut_effectif,
                            net, netapayer, charges_employeur, cout_total,
                            statut, date_paiement, date_calcul)
SELECT e.id,
       to_char(m, 'YYYY-MM'),
       '[]',
       brut.v,
       brut.v,
       ROUND(brut.v * 0.78, 3),
       ROUND(brut.v * 0.78, 3),
       ROUND(brut.v * 0.17, 3),
       ROUND(brut.v * 1.17, 3),
       'PAYE',
       (m + INTERVAL '1 month' - INTERVAL '3 days')::DATE,
       now()
FROM employes e
CROSS JOIN generate_series(
    date_trunc('month', CURRENT_DATE) - INTERVAL '11 months',
    date_trunc('month', CURRENT_DATE),
    INTERVAL '1 month') AS m
CROSS JOIN LATERAL (SELECT COALESCE(e.salaire, 1800)::NUMERIC(14, 3) AS v) AS brut
WHERE COALESCE(e.archived, false) = false
  AND NOT EXISTS (
      SELECT 1 FROM bulletins_paie bp
      WHERE bp.employe_id = e.id AND bp.mois = to_char(m, 'YYYY-MM'));

-- ────────────────────────────────────────────────────────────────────────────
-- 5. CHARGES ET AUTRES REVENUS
-- ────────────────────────────────────────────────────────────────────────────

-- 5a. Quatre charges fixes recurrentes
INSERT INTO charges_fixes (label, montant, taux_tva, jour_echeance, cycle_mois,
                           archived, date_creation)
SELECT v.label, v.montant, v.tva, v.jour, 1, false,
       (CURRENT_DATE - INTERVAL '12 months')::TIMESTAMP
FROM (VALUES
        ('Loyer bureau [JEU-DEMO]',        2400.000, 0.0,  5),
        ('Abonnement internet [JEU-DEMO]',  180.000, 19.0, 10),
        ('Assurance locaux [JEU-DEMO]',     320.000, 0.0,  15),
        ('Expert-comptable [JEU-DEMO]',     650.000, 19.0, 20)
     ) AS v(label, montant, tva, jour)
WHERE NOT EXISTS (SELECT 1 FROM charges_fixes cf WHERE cf.label = v.label);

-- 5b. Leur paiement, chaque mois
INSERT INTO paiements_charges_fixes (charge_fixe_id, mois, montant, date_paiement)
SELECT cf.id,
       to_char(m, 'YYYY-MM'),
       cf.montant,
       (m + (COALESCE(cf.jour_echeance, 5) - 1) * INTERVAL '1 day')::DATE
FROM charges_fixes cf
CROSS JOIN generate_series(
    date_trunc('month', CURRENT_DATE) - INTERVAL '11 months',
    date_trunc('month', CURRENT_DATE),
    INTERVAL '1 month') AS m
WHERE cf.label LIKE '%[JEU-DEMO]'
  AND NOT EXISTS (
      SELECT 1 FROM paiements_charges_fixes p
      WHERE p.charge_fixe_id = cf.id AND p.mois = to_char(m, 'YYYY-MM'));

-- 5c. Charges variables — deux par mois, categories realistes
INSERT INTO charges_variables (mois, label, montant, taux_tva, date, categorie, description)
SELECT to_char(m, 'YYYY-MM'),
       v.label,
       (v.base + random() * v.base * 0.5)::NUMERIC(14, 3),
       19.0,
       (m + (5 + (random() * 20)::INT) * INTERVAL '1 day')::DATE,
       v.categorie,
       'JEU-DEMO'
FROM generate_series(
        date_trunc('month', CURRENT_DATE) - INTERVAL '11 months',
        date_trunc('month', CURRENT_DATE),
        INTERVAL '1 month') AS m
CROSS JOIN (VALUES
        ('Achat materiel [JEU-DEMO]',     450.000, 'materiel'),
        ('Prestation freelance [JEU-DEMO]', 900.000, 'sous-traitance')
     ) AS v(label, base, categorie)
WHERE NOT EXISTS (
    SELECT 1 FROM charges_variables cv
    WHERE cv.mois = to_char(m, 'YYYY-MM') AND cv.label = v.label);

-- 5d. Revenus hors facturation — un par trimestre
INSERT INTO autres_revenus (mois, label, montant, taux_tva, date, categorie, description)
SELECT to_char(m, 'YYYY-MM'),
       'Prestation conseil [JEU-DEMO]',
       (1200 + random() * 2000)::NUMERIC(14, 3),
       19.0,
       (m + 12 * INTERVAL '1 day')::DATE,
       'consulting',
       'JEU-DEMO'
FROM generate_series(
        date_trunc('month', CURRENT_DATE) - INTERVAL '9 months',
        date_trunc('month', CURRENT_DATE),
        INTERVAL '3 months') AS m
WHERE NOT EXISTS (
    SELECT 1 FROM autres_revenus ar
    WHERE ar.mois = to_char(m, 'YYYY-MM') AND ar.label = 'Prestation conseil [JEU-DEMO]');

-- ────────────────────────────────────────────────────────────────────────────
-- 5e. FACTURES ET ENCAISSEMENTS — huit factures par client sur 12 mois.
--
--     Cette section est placee APRES les charges : elle en depend. Le chiffre
--     d'affaires de demonstration n'est pas fixe, il est calcule pour que
--     l'agence degage la marge cible v_marge_cible sur l'ensemble des donnees :
--
--         produits cibles = charges / (1 - marge cible)
--         CA de demo      = produits cibles - produits deja en base (hors demo)
--
--     Le CA de demo est ensuite reparti entre les factures selon des poids
--     aleatoires, puis normalise pour que la somme soit exacte. Le resultat reste
--     juste quel que soit le nombre d'employes, de clients ou de projets.
--
--     Statuts : 60 % soldees, 20 % partielles, 20 % impayees, de quoi remplir la
--     balance agee des creances sans la rendre caricaturale.
-- ────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    -- Marge cible : 0.20 = 20 %. A ajuster librement (0.15 a 0.25 est realiste
    -- pour une agence de communication).
    v_marge_cible  CONSTANT NUMERIC := 0.20;

    v_charges      NUMERIC;
    v_produits_ext NUMERIC;
    v_ca_demo      NUMERIC;
    v_total_poids  NUMERIC;
    v_nb           INT;

    r        RECORD;
    v_id     BIGINT;
    v_ht     NUMERIC(14, 3);
    v_tva    NUMERIC(14, 3);
    v_ttc    NUMERIC(14, 3);
    v_paye   NUMERIC(14, 3);
    v_statut TEXT;
    v_seq    INT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM factures WHERE notes = 'JEU-DEMO') THEN
        RAISE NOTICE 'Factures : jeu deja present, section ignoree.';
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM clients) THEN
        RAISE NOTICE 'Factures : aucun client en base, section ignoree.';
        RETURN;
    END IF;

    -- Charges HT sur toute la base, calculees comme le fait l'ETL :
    -- salaires au cout employeur, charges fixes et variables ramenees au HT.
    SELECT COALESCE((SELECT SUM(cout_total) FROM bulletins_paie), 0)
         + COALESCE((SELECT SUM(p.montant / (1 + COALESCE(cf.taux_tva, 0) / 100.0))
                     FROM paiements_charges_fixes p
                     JOIN charges_fixes cf ON cf.id = p.charge_fixe_id), 0)
         + COALESCE((SELECT SUM(montant / (1 + COALESCE(taux_tva, 0) / 100.0))
                     FROM charges_variables), 0)
    INTO v_charges;

    -- Produits deja presents hors de ce jeu : factures reelles et revenus divers.
    SELECT COALESCE((SELECT SUM(total_ht) FROM factures
                     WHERE type = 'FACTURE' AND notes IS DISTINCT FROM 'JEU-DEMO'), 0)
         + COALESCE((SELECT SUM(montant / (1 + COALESCE(taux_tva, 0) / 100.0))
                     FROM autres_revenus), 0)
    INTO v_produits_ext;

    -- Plancher : si les produits existants depassent deja la cible, on emet quand
    -- meme des factures de demonstration de montant modeste.
    v_ca_demo := GREATEST(v_charges / (1 - v_marge_cible) - v_produits_ext, 5000);

    RAISE NOTICE 'Factures : charges % DT, produits existants % DT, CA de demo % DT (marge cible %)',
        ROUND(v_charges), ROUND(v_produits_ext), ROUND(v_ca_demo), v_marge_cible;

    -- Poids et dates tires UNE FOIS PAR LIGNE dans la liste de selection, puis
    -- figes dans une table temporaire (meme raison que pour les pointages).
    CREATE TEMP TABLE tmp_factures_demo ON COMMIT DROP AS
    SELECT c.id                                                             AS client_id,
           (CURRENT_DATE - ((n * 44) + (random() * 20)::INT))::DATE         AS emission,
           -- NUMERIC obligatoire : ROUND(x, 3) n'existe pas pour un double precision.
           (0.5 + random())::NUMERIC                                        AS poids,
           random()                                                         AS tirage
    FROM clients c
    CROSS JOIN generate_series(1, 8) AS n;

    SELECT SUM(poids), COUNT(*) INTO v_total_poids, v_nb FROM tmp_factures_demo;

    FOR r IN SELECT * FROM tmp_factures_demo ORDER BY emission
    LOOP
        v_seq := v_seq + 1;
        v_ht  := ROUND(v_ca_demo * r.poids / v_total_poids, 3);
        v_tva := ROUND(v_ht * 0.19, 3);
        v_ttc := ROUND(v_ht + v_tva + 1.000, 3);   -- 1 DT de timbre fiscal

        IF r.tirage < 0.60 THEN
            v_statut := 'PAYEE';   v_paye := v_ttc;
        ELSIF r.tirage < 0.80 THEN
            v_statut := 'PARTIEL'; v_paye := ROUND(v_ttc * 0.4, 3);
        ELSE
            v_statut := 'EN_ATTENTE'; v_paye := 0;
        END IF;

        INSERT INTO factures (numero, type, client_id, date_emission, date_echeance,
                              lignes, total_ht, taux_tva, montant_tva, timbre_fiscal,
                              total_ttc, montant_paye, statut, paid_at, notes, date_creation)
        VALUES ('DEMO-' || to_char(r.emission, 'YYYY') || '-' || lpad(v_seq::TEXT, 4, '0'),
                'FACTURE', r.client_id, r.emission, r.emission + 30,
                '[]', v_ht, 19, v_tva, 1.000, v_ttc, v_paye, v_statut,
                CASE WHEN v_statut = 'PAYEE' THEN (r.emission + 25)::TIMESTAMP END,
                'JEU-DEMO', r.emission::TIMESTAMP)
        RETURNING id INTO v_id;

        IF v_paye > 0 THEN
            INSERT INTO paiements_factures (facture_id, montant, date_paiement, note)
            VALUES (v_id, v_paye, r.emission + 25, 'JEU-DEMO');
        END IF;
    END LOOP;
END $$;

-- ────────────────────────────────────────────────────────────────────────────
-- 6. TACHES — huit par projet existant, avec des dates d'execution coherentes
--    pour que les mesures de delai et de retard aient du sens.
-- ────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    r          RECORD;
    v_statut   TEXT;
    v_creation DATE;
    v_echeance DATE;
    v_debut    DATE;
    v_fin      DATE;
    v_tirage   NUMERIC;
    v_prevu    INT;
    v_equipe   BIGINT[];
    v_assignee BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM taches WHERE description = 'JEU-DEMO') THEN
        RAISE NOTICE 'Taches : jeu deja present, section ignoree.';
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM projets) THEN
        RAISE NOTICE 'Taches : aucun projet en base, section ignoree.';
        RETURN;
    END IF;

    -- Liste des collaborateurs, lue une fois ; l'assigne est ensuite tire dans la
    -- boucle, une fois par tache. Une sous-requete `ORDER BY random() LIMIT 1`
    -- placee dans le SELECT de la boucle serait independante de la ligne courante :
    -- PostgreSQL la calculerait une seule fois et toutes les taches auraient le
    -- meme assigne.
    SELECT array_agg(e.id) INTO v_equipe
    FROM employes e
    WHERE COALESCE(e.archived, false) = false;

    FOR r IN
        SELECT p.id AS projet_id, n
        FROM projets p
        CROSS JOIN generate_series(1, 8) AS n
    LOOP
        v_assignee := v_equipe[1 + floor(random() * array_length(v_equipe, 1))::INT];
        v_creation := (CURRENT_DATE - (30 + (random() * 300)::INT))::DATE;
        v_prevu    := 2 + (random() * 8)::INT;
        v_echeance := v_creation + v_prevu;
        v_tirage   := random();

        IF v_tirage < 0.55 THEN
            -- Terminee dans les temps
            v_statut := 'DONE';
            v_debut  := v_creation + 1;
            v_fin    := v_debut + (random() * v_prevu)::INT;
        ELSIF v_tirage < 0.70 THEN
            -- Terminee en retard
            v_statut := 'DONE';
            v_debut  := v_creation + 2;
            v_fin    := v_echeance + 1 + (random() * 10)::INT;
        ELSIF v_tirage < 0.88 THEN
            v_statut := 'IN_PROGRESS';
            v_debut  := v_creation + 1;
            v_fin    := NULL;
        ELSE
            v_statut := 'TODO';
            v_debut  := NULL;
            v_fin    := NULL;
        END IF;

        INSERT INTO taches (titre, statut, date_echeance, urgente, archived, description,
                            duree_prevue_jours, projet_id, assignee_id,
                            date_creation, date_assignation,
                            date_debut_execution, date_fin_execution)
        VALUES ('Tache demo ' || r.n || ' — projet ' || r.projet_id,
                v_statut, v_echeance, random() < 0.15, false, 'JEU-DEMO',
                v_prevu, r.projet_id, v_assignee,
                v_creation::TIMESTAMP, (v_creation + 1)::TIMESTAMP,
                v_debut::TIMESTAMP, v_fin::TIMESTAMP);
    END LOOP;
END $$;

COMMIT;

-- ────────────────────────────────────────────────────────────────────────────
-- Recapitulatif
-- ────────────────────────────────────────────────────────────────────────────
SELECT 'pointages'          AS table_alimentee, COUNT(*) AS lignes FROM pointages  WHERE ssid_entree = 'JEU-DEMO'
UNION ALL SELECT 'conges',              COUNT(*) FROM demandes           WHERE raison      = 'JEU-DEMO'
UNION ALL SELECT 'factures',            COUNT(*) FROM factures           WHERE notes       = 'JEU-DEMO'
UNION ALL SELECT 'paiements_factures',  COUNT(*) FROM paiements_factures WHERE note        = 'JEU-DEMO'
UNION ALL SELECT 'bulletins_paie',      COUNT(*) FROM bulletins_paie     WHERE elements    = '[]'
UNION ALL SELECT 'charges_variables',   COUNT(*) FROM charges_variables  WHERE description = 'JEU-DEMO'
UNION ALL SELECT 'autres_revenus',      COUNT(*) FROM autres_revenus     WHERE description = 'JEU-DEMO'
UNION ALL SELECT 'taches',              COUNT(*) FROM taches             WHERE description = 'JEU-DEMO';

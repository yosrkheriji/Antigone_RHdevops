-- ============================================================================
-- Suppression du jeu de donnees de demonstration
--
-- Supprime exactement ce qu'a insere jeu-de-donnees-demo.sql, en s'appuyant sur
-- le marqueur 'JEU-DEMO' present sur chaque enregistrement. Les donnees reelles
-- de l'agence ne portent pas ce marqueur et ne sont donc jamais touchees.
--
--   psql -U postgres -d antigone_rh -f jeu-de-donnees-demo-nettoyage.sql
--   puis, dans l'application : bouton « Recharger l'entrepot »
--
-- ATTENTION : la section Bulletins de paie ne dispose d'aucun champ libre ou
-- poser un marqueur. Le critere `elements = '[]'` ne suffit PAS : l'application
-- produit elle aussi des bulletins sans lignes de salaire (constate en base :
-- 13 bulletins reels repondaient a ce critere). Il faut donc croiser avec
-- `date_calcul`, que le script de demonstration positionne a l'instant de son
-- execution. Voir la section 6 ci-dessous.
-- ============================================================================

BEGIN;

DELETE FROM taches              WHERE description = 'JEU-DEMO';

DELETE FROM paiements_factures  WHERE note = 'JEU-DEMO';
DELETE FROM factures            WHERE notes = 'JEU-DEMO';

-- Conges : l'heritage JOINED impose de supprimer la table fille en premier.
DELETE FROM conges   WHERE id IN (SELECT id FROM demandes WHERE raison = 'JEU-DEMO');
DELETE FROM demandes WHERE raison = 'JEU-DEMO';

DELETE FROM pointages           WHERE ssid_entree = 'JEU-DEMO';

DELETE FROM paiements_charges_fixes
       WHERE charge_fixe_id IN (SELECT id FROM charges_fixes WHERE label LIKE '%[JEU-DEMO]');
DELETE FROM charges_fixes       WHERE label LIKE '%[JEU-DEMO]';

DELETE FROM charges_variables   WHERE description = 'JEU-DEMO';
DELETE FROM autres_revenus      WHERE description = 'JEU-DEMO';

-- 6. Bulletins de paie — supprimes a part, car aucun marqueur textuel n'est
--    possible sur cette table. Le couple (elements vide, date_calcul recente)
--    isole les bulletins du script : ceux calcules par l'application portent
--    une date_calcul anterieure.
--
--    ETAPE 1 — verifier ce qui serait supprime :
--      SELECT employe_id, mois, salaire_brut, date_calcul
--      FROM bulletins_paie
--      WHERE elements = '[]'
--        AND date_calcul >= DATE '2026-09-24'   -- date d'execution du script
--      ORDER BY employe_id, mois;
--
--    ETAPE 2 — si le resultat ne contient que des bulletins de demonstration,
--    decommenter et ajuster la date :
--
-- DELETE FROM bulletins_paie
--  WHERE elements = '[]'
--    AND date_calcul >= DATE '2026-09-24';

COMMIT;

SELECT 'pointages'          AS reste_marque, COUNT(*) FROM pointages  WHERE ssid_entree = 'JEU-DEMO'
UNION ALL SELECT 'conges',              COUNT(*) FROM demandes           WHERE raison      = 'JEU-DEMO'
UNION ALL SELECT 'factures',            COUNT(*) FROM factures           WHERE notes       = 'JEU-DEMO'
UNION ALL SELECT 'charges_variables',   COUNT(*) FROM charges_variables  WHERE description = 'JEU-DEMO'
UNION ALL SELECT 'taches',              COUNT(*) FROM taches             WHERE description = 'JEU-DEMO';

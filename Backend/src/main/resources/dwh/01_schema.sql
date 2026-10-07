-- ============================================================================
-- Entrepot de donnees decisionnel (Data Warehouse) — Antigone RH
-- Modelisation dimensionnelle en etoile (Kimball).
--
-- Le schema `dwh` ne contient QUE des donnees derivees : il est integralement
-- reconstruit par dwh.refresh_all(). On peut donc le supprimer sans perte.
-- C'est ce que fait DwhSchemaInitializer quand la version ci-dessous change.
--
-- Consommateurs :
--   - Power BI Desktop (connexion PostgreSQL directe sur le schema dwh)
--   - /api/analytics/** (pages Analytique des trois frontends)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS dwh;

-- Version du modele. DwhSchemaInitializer compare cette valeur a celle qu'il
-- attend : si elles different, il recree le schema de zero.
CREATE TABLE dwh.meta (
    cle    VARCHAR(50)  PRIMARY KEY,
    valeur VARCHAR(100) NOT NULL
);

INSERT INTO dwh.meta (cle, valeur) VALUES ('schema_version', '2');

-- Journal des executions ETL : alimente /api/analytics/etat et prouve en
-- soutenance que le rafraichissement est bien planifie.
CREATE TABLE dwh.etl_log (
    id             BIGSERIAL PRIMARY KEY,
    date_execution TIMESTAMP NOT NULL DEFAULT now(),
    duree_ms       BIGINT,
    lignes_chargees BIGINT,
    statut         VARCHAR(20) NOT NULL,
    message        TEXT
);

-- ============================================================================
-- DIMENSIONS
-- ============================================================================

-- Dimension temps : generee, jamais alimentee depuis l'OLTP. Elle permet les
-- agregations par mois/trimestre/annee sans manipuler de dates dans les mesures.
CREATE TABLE dwh.dim_date (
    date_key             DATE        PRIMARY KEY,
    annee                SMALLINT    NOT NULL,
    trimestre            SMALLINT    NOT NULL,
    mois                 SMALLINT    NOT NULL,
    mois_cle             CHAR(7)     NOT NULL,          -- 'YYYY-MM'
    mois_libelle         VARCHAR(20) NOT NULL,
    semaine_iso          SMALLINT    NOT NULL,
    jour                 SMALLINT    NOT NULL,
    jour_semaine         SMALLINT    NOT NULL,          -- 1 = lundi ... 7 = dimanche
    jour_semaine_libelle VARCHAR(12) NOT NULL,
    est_weekend          BOOLEAN     NOT NULL
);

CREATE INDEX idx_dim_date_mois ON dwh.dim_date (mois_cle);

-- Dimension employe. La cle -1 est le membre « inconnu » : elle garantit que
-- tout fait trouve une ligne dans la dimension, meme si la FK source est nulle.
-- Les colonnes de libelle sont en TEXT et non en VARCHAR(n) : elles recopient ou
-- concatenent des colonnes source de 255 caracteres, et une longueur trop courte
-- ferait echouer tout le chargement sur un seul enregistrement atypique. TEXT
-- n'a aucun cout supplementaire en PostgreSQL.
CREATE TABLE dwh.dim_employe (
    employe_key     BIGINT  PRIMARY KEY,
    matricule       TEXT,
    nom_complet     TEXT    NOT NULL,
    poste           TEXT,
    departement     TEXT    NOT NULL,
    type_contrat    TEXT    NOT NULL,
    genre           TEXT,
    date_embauche   DATE,
    anciennete_mois INTEGER,
    manager_nom     TEXT,
    actif           BOOLEAN NOT NULL
);

CREATE TABLE dwh.dim_client (
    client_key        BIGINT PRIMARY KEY,
    nom               TEXT   NOT NULL,
    matricule_fiscale TEXT,
    cycle_facturation INTEGER,
    date_creation     DATE
);

CREATE TABLE dwh.dim_projet (
    projet_key     BIGINT  PRIMARY KEY,
    nom            TEXT    NOT NULL,
    statut         TEXT    NOT NULL,
    type_projet    TEXT,
    client_key     BIGINT  NOT NULL REFERENCES dwh.dim_client (client_key),
    chef_nom       TEXT,
    date_debut     DATE,
    date_fin       DATE,
    date_creation  DATE,
    date_cloture   DATE,
    cloture_forcee BOOLEAN      NOT NULL DEFAULT false,
    duree_jours    INTEGER
);

CREATE INDEX idx_dim_projet_client ON dwh.dim_projet (client_key);

-- ============================================================================
-- TABLES DE FAITS
-- ============================================================================

-- Fait 1 — PRESENCE. Grain : un employe, un jour (source : pointages).
-- Les colonnes est_* sont des indicateurs additifs 0/1 : une moyenne sur
-- est_present donne directement le taux de presence, sans DAX complexe.
CREATE TABLE dwh.fait_presence (
    presence_key          BIGSERIAL    PRIMARY KEY,
    date_key              DATE         NOT NULL REFERENCES dwh.dim_date (date_key),
    employe_key           BIGINT       NOT NULL REFERENCES dwh.dim_employe (employe_key),
    statut                TEXT         NOT NULL,
    heures_travaillees    NUMERIC(6, 2) NOT NULL DEFAULT 0,
    retard_minutes        INTEGER      NOT NULL DEFAULT 0,
    est_present           SMALLINT     NOT NULL DEFAULT 0,
    est_retard            SMALLINT     NOT NULL DEFAULT 0,
    est_absent            SMALLINT     NOT NULL DEFAULT 0,
    est_conge             SMALLINT     NOT NULL DEFAULT 0,
    est_teletravail       SMALLINT     NOT NULL DEFAULT 0,
    sur_reseau_entreprise SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_fait_presence_date    ON dwh.fait_presence (date_key);
CREATE INDEX idx_fait_presence_employe ON dwh.fait_presence (employe_key);

-- Fait 2 — INACTIVITE. Grain : un employe, une semaine (source : agent desktop).
CREATE TABLE dwh.fait_inactivite (
    inactivite_key           BIGSERIAL PRIMARY KEY,
    date_key                 DATE      NOT NULL REFERENCES dwh.dim_date (date_key), -- lundi de la semaine
    employe_key              BIGINT    NOT NULL REFERENCES dwh.dim_employe (employe_key),
    semaine_fin              DATE,
    total_inactivite_minutes INTEGER   NOT NULL DEFAULT 0,
    inactivite_excedentaire  INTEGER   NOT NULL DEFAULT 0,
    retard_cumule            INTEGER   NOT NULL DEFAULT 0,
    montant_deduction        NUMERIC(12, 3) NOT NULL DEFAULT 0,
    decision                 TEXT
);

CREATE INDEX idx_fait_inactivite_date ON dwh.fait_inactivite (date_key);

-- Fait 3 — CONGE. Grain : une demande de conge (source : demandes + conges).
CREATE TABLE dwh.fait_conge (
    conge_key       BIGINT      PRIMARY KEY,
    date_key        DATE        NOT NULL REFERENCES dwh.dim_date (date_key), -- date de debut
    employe_key     BIGINT      NOT NULL REFERENCES dwh.dim_employe (employe_key),
    type_conge      TEXT        NOT NULL,
    statut          TEXT        NOT NULL,
    date_fin        DATE,
    nombre_jours    INTEGER     NOT NULL DEFAULT 0,
    jours_ouvrables INTEGER     NOT NULL DEFAULT 0,
    est_approuve    SMALLINT    NOT NULL DEFAULT 0
);

CREATE INDEX idx_fait_conge_date ON dwh.fait_conge (date_key);

-- Fait 4 — FLUX FINANCIERS. Grain : une ligne de flux.
--   sens  : ENTREE | SORTIE
--   base  : ENGAGEMENT (comptable, date d'emission) | TRESORERIE (encaisse)
--   nature: FACTURE | AUTRE_REVENU | SALAIRE | CHARGE_FIXE | CHARGE_VARIABLE
-- Sommer sans filtrer sur `base` compterait deux fois le meme euro : les
-- endpoints et les mesures Power BI filtrent donc toujours cette colonne.
CREATE TABLE dwh.fait_finance (
    finance_key  BIGSERIAL    PRIMARY KEY,
    date_key     DATE         NOT NULL REFERENCES dwh.dim_date (date_key),
    client_key   BIGINT       NOT NULL REFERENCES dwh.dim_client (client_key),
    employe_key  BIGINT       NOT NULL REFERENCES dwh.dim_employe (employe_key),
    sens         VARCHAR(10)  NOT NULL,
    base         VARCHAR(12)  NOT NULL,
    nature       VARCHAR(20)  NOT NULL,
    libelle      TEXT,
    montant_ht   NUMERIC(14, 3) NOT NULL DEFAULT 0,
    montant_tva  NUMERIC(14, 3) NOT NULL DEFAULT 0,
    montant_ttc  NUMERIC(14, 3) NOT NULL DEFAULT 0
);

CREATE INDEX idx_fait_finance_date   ON dwh.fait_finance (date_key);
CREATE INDEX idx_fait_finance_filtre ON dwh.fait_finance (base, sens, nature);

-- Fait 5 — FACTURE. Grain : une facture. Fait « snapshot » conserve a part du
-- flux car il porte un etat courant (reste du, retard) et non un montant periodique.
CREATE TABLE dwh.fait_facture (
    facture_key    BIGINT       PRIMARY KEY,
    date_key       DATE         NOT NULL REFERENCES dwh.dim_date (date_key), -- date d'emission
    client_key     BIGINT       NOT NULL REFERENCES dwh.dim_client (client_key),
    numero         TEXT         NOT NULL,
    type_document  TEXT         NOT NULL,
    date_echeance  DATE,
    statut         TEXT         NOT NULL,
    total_ht       NUMERIC(14, 3) NOT NULL DEFAULT 0,
    montant_tva    NUMERIC(14, 3) NOT NULL DEFAULT 0,
    total_ttc      NUMERIC(14, 3) NOT NULL DEFAULT 0,
    montant_paye   NUMERIC(14, 3) NOT NULL DEFAULT 0,
    reste_du       NUMERIC(14, 3) NOT NULL DEFAULT 0,
    jours_retard   INTEGER      NOT NULL DEFAULT 0,
    tranche_age    TEXT         NOT NULL
);

CREATE INDEX idx_fait_facture_client ON dwh.fait_facture (client_key);

-- Fait 6 — TACHE. Grain : une tache de projet. Porte les mesures de delai qui
-- alimentent le tableau de bord Projets.
CREATE TABLE dwh.fait_tache (
    tache_key          BIGINT       PRIMARY KEY,
    date_key           DATE         NOT NULL REFERENCES dwh.dim_date (date_key), -- date de creation
    projet_key         BIGINT       NOT NULL REFERENCES dwh.dim_projet (projet_key),
    assignee_key       BIGINT       NOT NULL REFERENCES dwh.dim_employe (employe_key),
    statut             TEXT         NOT NULL,
    urgente            BOOLEAN      NOT NULL DEFAULT false,
    date_echeance      DATE,
    date_fin_execution DATE,
    duree_prevue_jours INTEGER,
    duree_reelle_jours INTEGER,
    ecart_jours        INTEGER,
    est_terminee       SMALLINT     NOT NULL DEFAULT 0,
    est_en_retard      SMALLINT     NOT NULL DEFAULT 0,
    retard_jours       INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_fait_tache_projet   ON dwh.fait_tache (projet_key);
CREATE INDEX idx_fait_tache_assignee ON dwh.fait_tache (assignee_key);

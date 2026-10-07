# Guide Power BI — entrepôt décisionnel Antigone

Ce guide décrit comment brancher Power BI Desktop sur l'entrepôt de données
`dwh` d'Antigone et reconstruire les trois tableaux de bord.

L'entrepôt est le **même** que celui qu'utilisent les pages « Analytique » des
trois applications web. Les deux restitutions lisent les mêmes tables et
appliquent les mêmes règles d'agrégation : à période égale, elles affichent
nécessairement les mêmes chiffres. C'est le point à retenir pour la soutenance.

---

## 1. Prérequis

| Élément | Détail |
|---|---|
| Power BI Desktop | Version gratuite, à jour (le connecteur PostgreSQL est intégré depuis 2021) |
| Base | PostgreSQL contenant le schéma `dwh` |
| Entrepôt chargé | Le backend l'a construit au démarrage ; sinon voir §6 |
| Accès | Un compte PostgreSQL disposant du droit `SELECT` sur le schéma `dwh` |

Créer un compte en lecture seule dédié à Power BI est préférable à l'usage du
compte applicatif :

```sql
CREATE ROLE powerbi LOGIN PASSWORD 'un-mot-de-passe-solide';
GRANT USAGE ON SCHEMA dwh TO powerbi;
GRANT SELECT ON ALL TABLES IN SCHEMA dwh TO powerbi;
ALTER DEFAULT PRIVILEGES IN SCHEMA dwh GRANT SELECT ON TABLES TO powerbi;
```

Ce compte ne voit que l'entrepôt : il n'a aucun accès aux tables de saisie.

---

## 2. Connexion

1. **Accueil → Obtenir les données → Plus… → Base de données PostgreSQL**.
2. Serveur : `localhost:5432` (ou l'hôte distant). Base de données : `antigone_rh`.
3. Mode : **Importer**. En DirectQuery, chaque interaction avec un visuel
   déclenche une requête sur la base de production ; en Import, le rapport est
   autonome et bien plus rapide en démonstration.
4. Identifiants : compte `powerbi` créé ci-dessus.
5. Dans le navigateur, cocher les **dix tables du schéma `dwh`** listées au §3,
   puis **Charger**.
6. **Renommer les tables — étape à ne pas sauter.** Le connecteur PostgreSQL
   préfixe les tables du nom de leur schéma : elles arrivent sous le nom
   `dwh fait_presence`, `dwh dim_date`… Dans le volet **Données**, double-cliquer
   sur chaque nom et retirer le préfixe `dwh ` pour obtenir `fait_presence`,
   `dim_date`, etc.

   Sans ce renommage, toute formule DAX doit encadrer le nom de table de
   guillemets simples (`COUNTROWS ( 'dwh fait_presence' )`), puisque le nom
   contient un espace — et les mesures du §4 ci-dessous, écrites avec les noms
   courts, échouent sur « Échec de la résolution du nom ». Renommer une table ne
   casse ni les visuels ni les relations : Power BI répercute le changement.

> Si la connexion échoue avec une erreur SSL, ouvrir **Paramètres avancés** et
> décocher « Chiffrer la connexion » pour une base locale de développement.

---

## 3. Le modèle en étoile

L'entrepôt suit une **modélisation dimensionnelle** (Kimball) : quatre
dimensions partagées et six tables de faits. Chaque fait porte des mesures
numériques et des clés étrangères ; chaque dimension porte les axes d'analyse.

### Dimensions

| Table | Clé | Contenu |
|---|---|---|
| `dim_date` | `date_key` | Année, trimestre, mois, semaine ISO, jour, week-end |
| `dim_employe` | `employe_key` | Nom, poste, département, contrat, ancienneté, manager |
| `dim_client` | `client_key` | Nom, matricule fiscal, cycle de facturation |
| `dim_projet` | `projet_key` | Nom, statut, type, chef de projet, dates, durée |

`dim_employe`, `dim_client` et `dim_projet` contiennent chacune un membre de
clé **-1** (« Non affecté »). Il garantit qu'un fait dont la clé source est
nulle trouve tout de même une ligne dans sa dimension — sans lui, ces faits
disparaîtraient silencieusement des totaux.

### Faits

| Table | Grain (une ligne = …) | Mesures principales |
|---|---|---|
| `fait_presence` | un employé, un jour | heures, retard, indicateurs `est_*` |
| `fait_inactivite` | un employé, une semaine | minutes d'inactivité, déduction |
| `fait_conge` | une demande de congé | jours, jours ouvrables |
| `fait_finance` | un flux financier | montants HT / TVA / TTC |
| `fait_facture` | une facture | total, payé, reste dû, jours de retard |
| `fait_tache` | une tâche de projet | durées prévue / réelle, écart, retard |

### Relations à créer

Power BI en détecte une partie automatiquement ; vérifier et compléter dans la
vue **Modèle**. Toutes sont de type **un-à-plusieurs**, sens dimension → fait,
direction de filtre **simple**.

| Depuis (côté 1) | Vers (côté plusieurs) |
|---|---|
| `dim_date[date_key]` | `fait_presence[date_key]` |
| `dim_date[date_key]` | `fait_inactivite[date_key]` |
| `dim_date[date_key]` | `fait_conge[date_key]` |
| `dim_date[date_key]` | `fait_finance[date_key]` |
| `dim_date[date_key]` | `fait_facture[date_key]` |
| `dim_date[date_key]` | `fait_tache[date_key]` |
| `dim_employe[employe_key]` | `fait_presence[employe_key]` |
| `dim_employe[employe_key]` | `fait_inactivite[employe_key]` |
| `dim_employe[employe_key]` | `fait_conge[employe_key]` |
| `dim_employe[employe_key]` | `fait_finance[employe_key]` |
| `dim_employe[employe_key]` | `fait_tache[assignee_key]` |
| `dim_client[client_key]` | `fait_finance[client_key]` |
| `dim_client[client_key]` | `fait_facture[client_key]` |
| `dim_client[client_key]` | `dim_projet[client_key]` |
| `dim_projet[projet_key]` | `fait_tache[projet_key]` |

**Étape indispensable :** sélectionner `dim_date`, puis **Outils de table →
Marquer comme table de dates**, colonne `date_key`. Sans cela, les fonctions
de temps (`SAMEPERIODLASTYEAR`, cumuls annuels) renvoient des résultats faux.

---

## 4. Mesures DAX

Les créer dans une table dédiée (**Accueil → Entrer des données**, table vide
nommée `_Mesures`) pour qu'elles soient regroupées au lieu d'être dispersées.

### Tableau de bord 1 — Présence et productivité

```dax
Jours suivis        = COUNTROWS ( fait_presence )
Effectif suivi      = DISTINCTCOUNT ( fait_presence[employe_key] )
Taux de présence %  = DIVIDE ( SUM ( fait_presence[est_present] ), [Jours suivis] ) * 100
Taux absentéisme %  = DIVIDE ( SUM ( fait_presence[est_absent] ),  [Jours suivis] ) * 100
Part télétravail %  = DIVIDE ( SUM ( fait_presence[est_teletravail] ), [Jours suivis] ) * 100
Heures travaillées  = SUM ( fait_presence[heures_travaillees] )
Jours en retard     = SUM ( fait_presence[est_retard] )

Retard moyen (min) =
AVERAGEX (
    FILTER ( fait_presence, fait_presence[retard_minutes] > 0 ),
    fait_presence[retard_minutes]
)

Jours de congé = CALCULATE ( SUM ( fait_conge[nombre_jours] ), fait_conge[est_approuve] = 1 )

Inactivité excédentaire (h) = DIVIDE ( SUM ( fait_inactivite[inactivite_excedentaire] ), 60 )

Taux de présence N-1 =
CALCULATE ( [Taux de présence %], SAMEPERIODLASTYEAR ( dim_date[date_key] ) )
```

### Tableau de bord 2 — Finance et trésorerie

> **Le piège à ne pas manquer.** `fait_finance` contient deux lectures du même
> flux : `base = "ENGAGEMENT"` (comptable, à la date d'émission) et
> `base = "TRESORERIE"` (encaissé/décaissé réellement). Sommer la table sans
> filtrer cette colonne compte **deux fois le même dinar**. Toutes les mesures
> ci-dessous filtrent donc `base` explicitement.

```dax
Produits HT =
CALCULATE (
    SUM ( fait_finance[montant_ht] ),
    fait_finance[base] = "ENGAGEMENT",
    fait_finance[sens] = "ENTREE"
)

Charges HT =
CALCULATE (
    SUM ( fait_finance[montant_ht] ),
    fait_finance[base] = "ENGAGEMENT",
    fait_finance[sens] = "SORTIE"
)

Masse salariale = CALCULATE ( [Charges HT], fait_finance[nature] = "SALAIRE" )

Résultat = [Produits HT] - [Charges HT]
Marge %  = DIVIDE ( [Résultat], [Produits HT] ) * 100

Encaissements =
CALCULATE (
    SUM ( fait_finance[montant_ttc] ),
    fait_finance[base] = "TRESORERIE",
    fait_finance[sens] = "ENTREE"
)

Décaissements =
CALCULATE (
    SUM ( fait_finance[montant_ttc] ),
    fait_finance[base] = "TRESORERIE",
    fait_finance[sens] = "SORTIE"
)

Trésorerie nette  = [Encaissements] - [Décaissements]
Créances en cours = SUM ( fait_facture[reste_du] )
Créances en retard = CALCULATE ( SUM ( fait_facture[reste_du] ), fait_facture[jours_retard] > 0 )
Délai moyen de règlement (j) =
    CALCULATE ( AVERAGE ( fait_facture[jours_retard] ), fait_facture[jours_retard] > 0 )

Produits N-1  = CALCULATE ( [Produits HT], SAMEPERIODLASTYEAR ( dim_date[date_key] ) )
Croissance %  = DIVIDE ( [Produits HT] - [Produits N-1], [Produits N-1] ) * 100
```

### Tableau de bord 3 — Projets

```dax
Tâches             = COUNTROWS ( fait_tache )
Tâches terminées   = SUM ( fait_tache[est_terminee] )
Tâches en retard   = SUM ( fait_tache[est_en_retard] )
Taux de complétion % = DIVIDE ( [Tâches terminées], [Tâches] ) * 100

Délai moyen (j) =
CALCULATE ( AVERAGE ( fait_tache[duree_reelle_jours] ), fait_tache[est_terminee] = 1 )

Écart au prévu (j) =
CALCULATE ( AVERAGE ( fait_tache[ecart_jours] ), fait_tache[est_terminee] = 1 )

Retard moyen (j) =
CALCULATE ( AVERAGE ( fait_tache[retard_jours] ), fait_tache[est_en_retard] = 1 )

Projets en cours = CALCULATE ( COUNTROWS ( dim_projet ), dim_projet[statut] = "EN_COURS" )
Durée moyenne projet (j) = AVERAGE ( dim_projet[duree_jours] )
```

---

## 5. Construction des trois pages

Structure recommandée pour chaque page : un bandeau de **segments** en haut
(période via `dim_date`, puis un axe métier), une rangée de **cartes KPI**,
puis les visuels.

### Page 1 — Présence et productivité

| Visuel | Axe | Valeurs |
|---|---|---|
| Cartes | — | Taux de présence %, Taux absentéisme %, Heures travaillées, Retard moyen (min) |
| Courbe | `dim_date[mois_cle]` | Taux de présence %, Taux absentéisme % |
| Histogramme | `dim_date[mois_cle]` | Heures travaillées |
| Anneau | `fait_presence[statut]` | Jours suivis |
| Barres | `dim_employe[departement]` | Taux de présence % |
| Matrice | `dim_employe[nom_complet]` × `dim_date[mois_libelle]` | Jours en retard |
| Aires | `fait_inactivite` par semaine | Inactivité excédentaire (h) |

Segments : `dim_date[annee]`, `dim_employe[departement]`, `dim_employe[type_contrat]`.

### Page 2 — Finance et trésorerie

| Visuel | Axe | Valeurs |
|---|---|---|
| Cartes | — | Produits HT, Charges HT, Résultat, Marge %, Créances en retard |
| Histogramme groupé + courbe | `dim_date[mois_cle]` | Produits HT, Charges HT, puis Résultat en courbe |
| Courbe | `dim_date[mois_cle]` | Encaissements, Décaissements |
| Anneau | `fait_finance[nature]` | Charges HT |
| Barres | `fait_facture[tranche_age]` | Créances en cours |
| Barres | `dim_client[nom]` | Produits HT (10 premiers, filtre Top N) |
| Table | `fait_facture[numero]` | Client, échéance, reste dû, jours de retard |

Segments : `dim_date[annee]`, `dim_client[nom]`, `fait_finance[nature]`.

### Page 3 — Projets

| Visuel | Axe | Valeurs |
|---|---|---|
| Cartes | — | Projets en cours, Taux de complétion %, Tâches en retard, Délai moyen (j) |
| Courbe | `dim_date[mois_cle]` | Tâches, Tâches terminées, Tâches en retard |
| Anneau | `dim_projet[statut]` | Nombre de projets |
| Barres empilées | `dim_employe[nom_complet]` | Tâches terminées, Tâches en retard |
| Barres | `dim_projet[nom]` | Délai moyen (j) |
| Table | `dim_projet[nom]` | Client, statut, Tâches, Tâches en retard, Taux de complétion % |

Segments : `dim_date[annee]`, `dim_client[nom]`, `dim_projet[statut]`.

### Règles de lisibilité à respecter

Ces règles sont celles appliquées aux pages Analytique de l'application ;
les reprendre garantit une restitution cohérente entre les deux outils.

- **Jamais deux axes verticaux** sur un même visuel. Deux mesures d'unités
  différentes (un pourcentage et des heures) font deux visuels distincts.
- **Une légende dès deux séries**, et l'ordre des couleurs est fixe : une même
  entité garde sa couleur d'un visuel à l'autre.
- Palette catégorielle validée pour les déficiences de vision des couleurs :
  `#683B77` · `#eb6834` · `#2a78d6` · `#1baf7a` · `#eda100` · `#e87ba4`.
- Grille et axes discrets, pas d'étiquette de valeur sur chaque point.

---

## 6. Rafraîchissement des données

L'entrepôt est reconstruit **intégralement** (vidage puis rechargement) par la
procédure stockée `dwh.refresh_all()`. Trois déclencheurs :

| Déclencheur | Quand |
|---|---|
| Automatique | Chaque nuit à 02h30 (`app.bi.etl-cron`, réglable par `BI_ETL_CRON`) |
| Au démarrage | Le backend charge l'entrepôt s'il est vide |
| Manuel | Bouton « Recharger l'entrepôt » des pages Analytique (administrateur), ou `POST /api/analytics/refresh` |

En SQL directement :

```sql
SELECT dwh.refresh_all();          -- renvoie le nombre de lignes de faits chargées
SELECT * FROM dwh.etl_log ORDER BY id DESC LIMIT 5;   -- historique des exécutions
```

Côté Power BI, après un rechargement de l'entrepôt : **Accueil → Actualiser**.

---

## 7. Pièges connus

| Symptôme | Cause | Correction |
|---|---|---|
| Les montants sont doublés | `fait_finance` sommée sans filtrer `base` | Toujours filtrer `ENGAGEMENT` ou `TRESORERIE` |
| `SAMEPERIODLASTYEAR` renvoie vide | `dim_date` non marquée comme table de dates | Outils de table → Marquer comme table de dates |
| Un total est inférieur à la somme des lignes | Faits rattachés au membre -1 masqués par un filtre de dimension | Retirer le filtre, ou l'assumer explicitement |
| Les tables `dwh` n'apparaissent pas | Entrepôt jamais construit | Démarrer le backend, ou `SELECT dwh.refresh_all();` |
| Les chiffres diffèrent de l'application | Périodes comparées différentes | L'application filtre sur la période choisie ; aligner les segments |
| Erreur « relation dwh.xxx does not exist » | Le compte Power BI n'a pas les droits | Rejouer les `GRANT` du §1 |

---

## 8. Ce qu'il faut pouvoir dire en soutenance

- **Pourquoi un entrepôt séparé plutôt que des requêtes sur les tables métier.**
  Les tables de saisie sont normalisées pour écrire vite et sans doublon ; une
  analyse sur plusieurs années y impose des jointures coûteuses qui ralentissent
  les écrans de saisie. Le schéma en étoile dénormalise une fois par nuit ce que
  l'analyse relirait mille fois.
- **Pourquoi un rechargement complet et non incrémental.** Au volume d'une
  agence, le rechargement complet prend quelques secondes et supprime tout
  risque de désynchronisation après une correction manuelle en base. L'ETL
  incrémental serait une optimisation prématurée.
- **Pourquoi les indicateurs `est_présent`, `est_absent`… valent 0 ou 1.** Une
  moyenne sur une colonne 0/1 donne directement un taux : la mesure reste une
  ligne de DAX, et le même calcul est reproductible en SQL sans divergence.
- **Pourquoi deux restitutions (Power BI et pages Analytique).** Power BI sert
  l'exploration libre et l'export ; les pages web servent le suivi quotidien
  dans l'outil de travail, avec le cloisonnement par permission déjà en place.
  Les deux lisent le même entrepôt, donc ne peuvent pas se contredire.

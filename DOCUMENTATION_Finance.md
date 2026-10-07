# Documentation — Module Finance (Antigone)

> Ce document explique en détail le fonctionnement du module **Finance** de l'application Antigone : son architecture, chacun de ses écrans, ses formules de calcul exactes, et un guide pas-à-pas pour l'installer et l'utiliser depuis zéro.
>
> Il est écrit pour être compréhensible par quelqu'un qui découvre le projet pour la première fois, tout en restant précis pour un développeur qui doit le maintenir.

---

## Sommaire

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture globale](#2-architecture-globale)
3. [Les écrans du module Finance](#3-les-écrans-du-module-finance)
   - [3.1 Tableau de bord](#31-tableau-de-bord-dashboardpagetsx)
   - [3.2 Factures & devis](#32-factures--devis-facturespagetsx)
   - [3.3 Salaires](#33-salaires-salairespagetsx)
   - [3.4 Charges](#34-charges-chargespagetsx)
   - [3.5 CNSS trimestriel](#35-cnss-trimestriel-cnsspagetsx)
   - [3.6 Paramètres de paie](#36-paramètres-de-paie-parametrespaiepagetsx)
   - [3.7 Autres revenus](#37-autres-revenus-revenuspagetsx)
   - [3.8 Dettes](#38-dettes-dettespagetsx)
4. [Fonctionnalités et utilités par module backend](#4-fonctionnalités-et-utilités-par-module-backend)
5. [Sécurité et permissions](#5-sécurité-et-permissions)
6. [Guide pas-à-pas complet](#6-guide-pas-à-pas-complet)
7. [Pièges connus et comment les éviter](#7-pièges-connus-et-comment-les-éviter)
8. [Référence des endpoints API](#8-référence-des-endpoints-api)

---

## 1. Vue d'ensemble

### 1.1 Qu'est-ce que le module Finance ?

Antigone est un monorepo qui héberge **trois applications frontend distinctes** partageant **un seul backend** et **une seule base de données PostgreSQL** :

| App | Port dev | Public visé |
|---|---|---|
| `frontend-projects` | 3000 | Gestion de projets / médiaplans (porte l'authentification) |
| `frontend-rh` | 3001 | Ressources humaines |
| **`frontend-finance`** | **3002** | **Comptabilité / gestion financière de l'agence** |

Le module Finance couvre le cycle financier complet d'une agence :

- **Paie des employés** (calcul CNSS, IRPP, charges patronales, à la tunisienne)
- **Facturation clients** (factures, devis, paiements)
- **Autres revenus** (hors facturation)
- **Charges de l'agence** (fixes récurrentes et variables ponctuelles)
- **Dettes** de l'agence
- **Obligations fiscales** : déclaration CNSS trimestrielle, TVA mensuelle
- **Tableaux de bord** : encaissements, décaissements, résultat net

Ce module est un **portage** d'un projet historique (`Antigone_finance`, un stack Next.js + Supabase) réécrit en Spring Boot + React pour s'intégrer nativement au reste d'Antigone (même backend, même base, mêmes comptes utilisateurs).

### 1.2 Principe directeur : zéro valeur codée en dur

Toutes les valeurs qui peuvent changer avec le temps ou d'un pays à l'autre (taux de TVA, timbre fiscal, périodicités de charges, catégories de revenus/charges, barème IRPP, taux CNSS) sont stockées en base de données — pas dans le code. Un administrateur peut les modifier depuis l'interface sans redéploiement.

---

## 2. Architecture globale

```
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│  frontend-projects    │   │     frontend-rh       │   │   frontend-finance    │
│  localhost:3000       │   │  localhost:3001       │   │  localhost:3002       │
│  (porte le login)     │   │                        │   │                        │
└───────────┬───────────┘   └───────────┬───────────┘   └───────────┬───────────┘
            │                           │                           │
            └───────────────┬───────────┴───────────────┬───────────┘
                             │         HTTP (axios)      │
                             ▼                           ▼
                   ┌─────────────────────────────────────────┐
                   │        Backend Spring Boot (8080)         │
                   │        com.antigone.rh.*                  │
                   │  - Sécurité JWT + permissions             │
                   │  - Contrôleurs REST /api/finance/**       │
                   │  - Services métier (calculs)              │
                   │  - Repositories JPA                       │
                   └───────────────────┬─────────────────────┘
                                       │ JDBC
                                       ▼
                           ┌───────────────────────┐
                           │   PostgreSQL           │
                           │   antigone_rh          │
                           │   (base UNIQUE partagée │
                           │   par les 3 apps)       │
                           └───────────────────────┘
```

### 2.1 Pourquoi trois applications séparées ?

Chaque métier (Projets, RH, Finance) a son propre espace applicatif avec sa propre navigation, mais ils partagent :

- **La même base de données** — un client créé dans l'app Projets est immédiatement visible dans Finance.
- **Le même système d'authentification** — un seul login (sur `frontend-projects`), un seul JWT.
- **Les mêmes comptes/permissions** — un utilisateur a un ensemble de permissions (`VIEW_FINANCE`, `VIEW_EMPLOYES`, `VIEW_CLIENTS`...) qui déterminent quelles apps et quels écrans il peut voir.

### 2.2 Bascule entre applications

Un bouton flottant en bas à droite de chaque app (`AppSwitchButton.tsx`) permet de passer de Projets → RH → Finance sans se reconnecter. Le mécanisme :

1. Au clic, `relayAuthSnapshotForSwitch()` encode la session courante (utilisateur + token) en base64 dans `window.name`.
2. Le navigateur redirige vers l'URL de l'app cible (`VITE_FINANCE_APP_URL`, `VITE_RH_APP_URL`...).
3. Au chargement, la nouvelle app lit `window.name`, restaure la session, et l'utilisateur est déjà connecté.

Le bouton Finance n'apparaît que si l'utilisateur a la permission `VIEW_FINANCE` (ou le rôle `ADMIN`) — sinon il est masqué.

### 2.3 Package backend

Toutes les classes du module Finance vivent dans le même package que le reste de l'application RH (`com.antigone.rh`), organisées par couche (pas par métier) :

```
Backend/src/main/java/com/antigone/rh/
├── entity/         → BulletinPaie, ChargeFixe, Facture, Dette, AutreRevenu, ...
├── dto/            → FichePaieDTO, FactureDTO, ResumeChargesDTO, ...
├── repository/     → interfaces Spring Data JPA
├── service/        → PayrollCalculator, PayrollService, FacturationService, ChargesService,
│                      CnssService, TvaService, DetteService, RevenuService,
│                      DashboardFinanceService, FinanceReferentielService
├── controller/     → PayrollController, FacturationController, ChargesController,
│                      CnssController, RevenuController, DetteController,
│                      FinanceDashboardController, FinanceReferentielController,
│                      ContactClientController
├── enums/          → StatutPaie, StatutFacture, TypeDocument, TypeReferentiel
└── config/         → SecurityConfig (CORS + permissions), DataInitializer (données seed)
```

### 2.4 Frontend Finance

```
frontend-finance/src/
├── api/            → axios.ts (client HTTP), payrollService.ts, facturationService.ts,
│                      chargesService.ts, cnssService.ts, revenuService.ts, detteService.ts,
│                      dashboardService.ts, referentielService.ts, employeService.ts
├── components/
│   ├── layout/     → MainLayout.tsx, Sidebar.tsx
│   └── ui/         → AppSwitchButton.tsx, ErreurBanner.tsx
├── context/        → AuthContext.tsx (session utilisateur)
├── pages/          → DashboardPage, FacturesPage, SalairesPage, ChargesPage, CnssPage,
│                      RevenusPage, DettesPage, ParametresPaiePage, LoginPage
├── types/          → index.ts (tous les types TypeScript, miroir des DTOs backend)
└── utils/          → authStorage.ts (relais de session), apiError.ts (messages d'erreur)
```

---

## 3. Les écrans du module Finance

Cette section documente **chaque écran de l'application**, dans l'ordre du menu latéral, avec ce qu'il affiche, comment l'utiliser, et à quel(s) endpoint(s) API il correspond.

Toutes les pages "mensuelles" partagent le même sélecteur `< 2026-08 >` en haut à droite qui change le mois consulté (format interne `YYYY-MM`) ; toutes utilisent le composant `ErreurBanner` pour signaler un échec de chargement au lieu d'afficher silencieusement une liste vide.

### 3.1 Tableau de bord (`DashboardPage.tsx`)

**Route** : `/dashboard` — c'est la page d'accueil de l'app Finance.

**Objectif** : donner une vue consolidée du mois sélectionné, croisant ce qui rentre (encaissements) et ce qui sort (décaissements).

**Ce qui s'affiche** :

1. **Résultat net du mois** (bloc du haut) : `résultat net = total encaissé (factures + autres revenus) − total décaissé (salaires + charges + dettes + taxes)`. Vert si positif, rouge si négatif, avec la marge en % du chiffre encaissé.
2. **Bloc Encaissements** : total facturé, encaissé, en attente, reste à encaisser, autres revenus, total encaissé cumulé, et la répartition en % entre factures et autres revenus.
3. **Bloc Décaissements** : masse salariale (brute/nette/charges patronales/coût total), net restant à payer, **net reporté** (salaires impayés des mois antérieurs), charges fixes/variables, taxes dues (IRPP + TFP + FOPROLOS), solde restant des dettes.
4. **Déclaration TVA** : TVA collectée sur les factures (au prorata de l'encaissement réel) + sur les autres revenus, TVA déductible sur les charges, TVA nette (à reverser à l'État ou crédit en votre faveur).

**Endpoints appelés** (en parallèle au chargement) :
```
GET /api/finance/dashboard/encaissements?mois=2026-08
GET /api/finance/dashboard/decaissements?mois=2026-08
GET /api/finance/dashboard/resultat?mois=2026-08
GET /api/finance/dashboard/tva?mois=2026-08
```

**Pourquoi le tableau de bord peut afficher "0,00 DT" partout** : ces valeurs sont calculées à partir des **factures**, **bulletins de paie déjà générés** et **charges déjà enregistrées** du mois. Si aucune facture n'a été créée et qu'aucun bulletin n'a été calculé pour ce mois (voir §3.3), tout est à zéro — ce n'est pas un bug, il n'y a simplement pas encore de données pour ce mois-là.

### 3.2 Factures & devis (`FacturesPage.tsx`)

**Route** : `/factures`

**Objectif** : émettre des factures et des devis pour les clients de l'agence, et suivre leur paiement.

**Bascule Factures / Devis** : un toggle en haut change le type de document affiché. Un **devis n'a jamais de TVA ni de timbre fiscal** — c'est appliqué automatiquement par le formulaire et par le backend.

**Formulaire "Nouveau facture"** (bouton `+ Nouveau`) :

| Champ | Rôle |
|---|---|
| Client | Liste déroulante alimentée par `/api/finance/clients` — **les clients déjà créés dans l'app Projets/RH apparaissent ici automatiquement**, aucune re-saisie nécessaire |
| Émission / Échéance | Dates du document |
| Lignes de prestation | Désignation + quantité + prix unitaire, avec une case à cocher "sélectionnée" (une ligne décochée n'entre pas dans le total) |
| Total HT manuel | Un **plancher** : le HT retenu est `max(Σ lignes sélectionnées, HT manuel)` |
| TVA (%) | Pré-rempli avec la valeur par défaut du référentiel (19 % par défaut), modifiable par facture |
| Timbre fiscal | Pré-rempli avec la valeur par défaut du référentiel (1,000 DT), modifiable par facture |

Un encart d'aperçu recalcule en direct (sans appel serveur) : `Σ lignes → Total HT retenu → TVA → Timbre → Total TTC`, exactement selon la même formule que le backend (voir §4.2).

Quand un client est sélectionné, ses informations fiscales s'affichent (matricule fiscal, RNE, contact, cycle de facturation) — utile pour vérifier avant l'émission.

**Table des documents** : numéro (`FAC-2026-0001` ou `DEV-2026-0001`, généré automatiquement), client, dates, HT/TVA/timbre/TTC, montant payé, montant restant, statut.

**Actions par ligne** (uniquement pour les factures, pas les devis) :
- 💵 **Enregistrer un paiement** — saisie d'un montant, peut être partiel.
- ✅ **Solder** — marque la facture payée en totalité en une fois.
- 🗑️ **Supprimer** — supprime le document et son historique de paiements.

**Statuts** : `EN_ATTENTE` (rouge, rien payé) → `PARTIEL` (orange, paiement partiel reçu) → `PAYEE` (vert, montant payé ≥ TTC).

### 3.3 Salaires (`SalairesPage.tsx`)

**Route** : `/salaires`

**Objectif** : calculer et suivre le bulletin de paie mensuel de chaque employé.

**En-tête** : "N employés sous contrat sur YYYY-MM" — ce nombre vient de `/api/finance/employes?mois=...`, qui ne renvoie que les employés dont le contrat couvre au moins une partie du mois sélectionné (voir §4.1 pour la règle exacte). **Ce n'est pas la liste complète des employés RH** : quelqu'un embauché le mois prochain, ou parti il y a six mois, n'apparaît pas ce mois-ci.

**Cartes de synthèse** (visibles seulement après génération d'au moins un bulletin) : masse brute, masse nette, CNSS salarié, CNSS patronale, IRPP, coût total.

**Bouton "Générer tous les bulletins"** : calcule et enregistre en une fois le bulletin de chaque employé actif du mois. Peut être relancé (il met à jour les bulletins existants sans dupliquer).

**Table des employés**, une ligne par personne :

| Colonne | Contenu |
|---|---|
| Employé | Nom, prénom, matricule ; badge "Archivé le ..." si le contrat s'est terminé |
| Poste / Département | Depuis la fiche employé RH |
| Contrat | Type de contrat ; badge orange **"Exonéré"** pour CIVP / Freelance / Stage |
| Mode | Bouton bascule **BRUT** ↔ **NET** (désactivé si exonéré, où le mode n'a pas de sens) |
| Salaire saisi | Le salaire brut ou net stocké sur la fiche employé, selon le mode |
| Brut / Retenues / Net / Net à payer | Résultat du calcul, vide tant que le bulletin n'est pas généré |
| Statut | `IMPAYE` / `PARTIEL` / `PAYE` |
| Actions | 💵 acompte, ✅ marquer payé |

Si un employé n'a pas encore de bulletin ce mois-ci, un bouton **"Calculer le bulletin"** remplace les colonnes de résultat.

**Cliquer sur le nom d'un employé** ouvre une fiche détaillée avec ses champs administratifs (matricule, CIN, **numéro CNSS**, **RIB**, email, dates de contrat) et, si un bulletin existe, le détail complet des 8 étapes du calcul (voir §4.2) plus les charges patronales.

**Modale "Acompte"** : enregistre un paiement partiel du salaire à tout moment du mois — utile pour les avances sur salaire.

### 3.4 Charges (`ChargesPage.tsx`)

**Route** : `/charges`

**Objectif** : suivre les charges fixes récurrentes (loyer, abonnements...) et les charges ponctuelles de l'agence.

**Cartes de synthèse du mois** : total charges, fixes dues, fixes payées, reste à payer, cumul impayé (des mois précédents), TVA déductible.

**Section "Charges fixes"** :
- Bouton `+ Ajouter` : libellé, montant TTC, taux de TVA, **cycle** (liste déroulante alimentée par le référentiel `CYCLES_CHARGE_FIXE`, ex. 1/3/6/12/24/48 mois), jour d'échéance.
- Une charge fixe n'est **due** un mois donné que si ce mois est un multiple exact de son cycle depuis sa création (voir formule §4.3). Les charges non dues ce mois-ci apparaissent grisées.
- Colonnes : montant TTC, HT (calculé), payé, reste, **cumul impayé** (en rouge si > 0 — signale des mois passés jamais réglés), statut (Payée / Partielle / Non payée / Non due).
- Actions : ✅ payer le reste dû, 🗄️ archiver (la charge disparaît des listes futures mais garde son historique).

**Section "Charges variables — YYYY-MM"** : dépenses ponctuelles du mois (libellé, montant TTC, catégorie choisie dans le référentiel `CATEGORIE_CHARGE`). Colonnes TTC / HT / TVA calculées automatiquement.

### 3.5 CNSS trimestriel (`CnssPage.tsx`)

**Route** : `/cnss`

**Objectif** : préparer la déclaration CNSS trimestrielle à partir des bulletins de paie déjà calculés.

**Interface** : 4 cartes, une par trimestre (T1 à T4) de l'année sélectionnée, chacune affichant l'échéance légale (**le 15 du mois suivant la fin du trimestre**, ex. T1 → 15 avril).

**Bouton "Calculer"** : additionne, sur les 3 mois du trimestre, la part salariale (CNSS + contribution de solidarité) et la part patronale (CNSS employeur) de **tous les bulletins déjà générés**. Si le trimestre est en retard par rapport à l'échéance légale, une **pénalité** est calculée automatiquement (voir §4.4) et ajoutée au total.

**Bouton "Recalculer"** : ré-exécute le même calcul sur une déclaration déjà enregistrée (utile si des bulletins ont été modifiés depuis).

**Bouton vert ✓** : marque le trimestre comme payé.

**Point d'attention** : un trimestre "IMPAYE" avec des montants à 0 signifie qu'**aucun bulletin de paie n'a été généré** pour les mois de ce trimestre — retournez sur la page Salaires et générez les bulletins des mois concernés avant de calculer le trimestre.

### 3.6 Paramètres de paie (`ParametresPaiePage.tsx`)

**Route** : `/parametres`

**Objectif** : configurer les taux utilisés par **tous** les calculs de paie, et consulter le barème IRPP en vigueur.

**Bloc "Taux de cotisation"** — chaque champ affiche sa valeur en pourcentage à côté pour éviter les erreurs de saisie (`0,0918` = `9.18 %`) :

| Champ | Rôle |
|---|---|
| CNSS salarié | Retenue sur la base CNSS du salarié |
| Contribution de solidarité (CSS) | Sur le revenu net imposable (ou le salaire imposable si pas d'abattement) |
| CNSS patronale | Part employeur, sur le brut effectif |
| TFP | Taxe de formation professionnelle, sur le brut effectif |
| FOPROLOS | Fonds de promotion du logement, sur le brut effectif |
| Accidents du travail | Sur le brut effectif |
| Abattement forfaitaire | Déduction sur le salaire imposable avant IRPP |

Le bouton **Enregistrer** met à jour ces taux en base (table `parametres_paie`, une seule ligne) : **tous les futurs calculs de bulletins** utiliseront les nouvelles valeurs. Les bulletins déjà générés ne sont pas recalculés automatiquement — régénérez-les si besoin (bouton "Générer tous les bulletins" sur la page Salaires).

**Bloc "Barème IRPP progressif"** : tableau en lecture seule montrant les tranches en vigueur, avec leur date d'entrée en vigueur. Ce barème peut être versionné dans le temps (nouvelle entrée avec une `effectiveFrom` future) — le calcul utilise toujours le barème le plus récent dont la date d'entrée en vigueur est passée par rapport au mois calculé.

**Bloc "Formules appliquées"** : rappel pédagogique des 8 étapes de calcul, directement dans l'interface (repris de §4.2 ci-dessous).

### 3.7 Autres revenus (`RevenusPage.tsx`)

**Route** : `/revenus`

**Objectif** : enregistrer les revenus de l'agence qui ne passent pas par une facture (subventions, ventes ponctuelles, remboursements...).

**Formulaire** : libellé, **montant TTC**, taux de TVA, catégorie (liste déroulante alimentée par le référentiel `CATEGORIE_REVENU` : Consulting, Vente, Subvention, Remboursement, Loyer, Autre), description libre.

Le HT et la TVA contenue dans le montant sont calculés et affichés automatiquement (formule §4.5), jamais ressaisis.

### 3.8 Dettes (`DettesPage.tsx`)

**Route** : `/dettes`

**Objectif** : suivre les emprunts et dettes de l'agence.

**Formulaire** : libellé, créancier, montant total, échéance, notes.

**Table** : montant total, montant remboursé, **solde restant** (`= max(0, total − remboursé)`), avec une ligne grisée quand la dette est soldée.

**Action** : 💵 enregistrer un remboursement (partiel ou total) — met à jour le solde restant automatiquement.

---

## 4. Fonctionnalités et utilités par module backend

### 4.1 `PayrollCalculator` — le moteur de calcul de paie

Classe pure (pas d'accès base de données), responsable de :

**`estActifPourMois(employe, mois)`** — détermine si un employé doit apparaître dans la paie d'un mois. Règle :
```
actif = (date d'embauche ≤ fin du mois)
    ET  (date de fin de contrat est nulle OU ≥ début du mois)
    ET  (pas archivé avant le début du mois)
```
C'est cette méthode qui alimente le endpoint `GET /api/finance/employes?mois=...` utilisé par la page Salaires — un employé archivé en cours de mois y apparaît toujours (il a droit à un salaire au prorata), un employé embauché le mois suivant n'y apparaît pas encore.

**`estExonere(typeContrat)`** — renvoie `true` pour les types de contrat `CIVP`, `Freelance`, `Stage` : aucune retenue CNSS/IRPP ne leur est appliquée (Net = Brut). Gère explicitement le cas `typeContrat == null` (un employé sans type de contrat renseigné n'est jamais considéré exonéré).

**`joursOuvresDuMois(mois)`** — compte les jours du lundi au vendredi du mois (hors samedi/dimanche), utilisé comme diviseur pour tout prorata.

**`brutDepuisNet(netCible, taux, tranches)`** — recherche par **dichotomie** (64 itérations, tolérance 0,0005 DT) du salaire brut qui, une fois les retenues appliquées, donne exactement le net cible. Nécessaire car l'IRPP est progressif : la fonction brut→net n'est pas inversible par une simple formule.

### 4.2 `calculerFichePaie` — les 8 étapes du calcul

Pour un employé **non exonéré**, dans l'ordre exact appliqué par le code :

```
1. CNSS salarié          = base CNSS × taux CNSS salarié
2. Salaire imposable     = (brut ajusté IRPP) − CNSS salarié
3. Abattement            = salaire imposable × taux d'abattement
4. Revenu net imposable  = salaire imposable − abattement
5. Base de cotisation    = revenu net imposable (si abattement > 0) sinon salaire imposable
6. Contribution CSS      = base de cotisation × taux CSS
7. IRPP mensuel          = irppAnnuel(revenu net imposable × 12, barème) / 12
8. Net                   = brut effectif − CNSS salarié − CSS − IRPP mensuel
   Net à payer           = Net − Σ acomptes déjà versés

Charges employeur (calculées en parallèle, sur le brut effectif) :
   CNSS patronale = brut × taux CNSS patronale
   TFP            = brut × taux TFP
   FOPROLOS       = brut × taux FOPROLOS
   Accidents travail = brut × taux AT
   Coût total     = brut effectif + Σ charges employeur
```

Pour un employé **exonéré** (CIVP/Freelance/Stage) : `Net = Brut effectif`, toutes les retenues et charges patronales sont à zéro.

**Barème IRPP** (`irppAnnuel`) : calcul par tranches successives — chaque tranche du barème (ex. 0-5000 DT à 0 %, 5000-10000 DT à 15 %...) est appliquée uniquement sur la portion de revenu qui s'y trouve, jamais sur la totalité.

### 4.3 `ChargesService` — échéancier des charges fixes

**`estDueLeMois(charge, mois)`** :
```
moisEcoules = (année du mois − année de création) × 12 + (mois − mois de création)
due = (moisEcoules % cycleMois == 0)
```
Une charge créée en janvier avec un cycle de 3 mois est due en janvier, avril, juillet, octobre — jamais entre-temps.

**Cumul impayé** : pour chaque mois échu strictement avant le mois consulté, si le montant payé ce mois-là est inférieur au montant dû, la différence s'accumule dans `cumulImpaye`. Permet de repérer une charge fixe oubliée depuis plusieurs mois.

### 4.4 `CnssService` — pénalité de retard

```
échéance légale = 15 du mois suivant la fin du trimestre
si aujourd'hui > échéance :
    mois de retard = max(1, jours de retard / 30, arrondi)
    pénalité = (montant salarié + montant patronal) × mois de retard × 1 %
```

### 4.5 `RevenuService` / `ChargesService` — extraction HT/TVA

Le montant saisi partout dans le module (revenus, charges) est **toujours TTC** ; le HT en est déduit :
```
HT  = TTC / (1 + taux TVA / 100)
TVA = TTC − HT
```

### 4.6 `FacturationService` — tarification et numérotation

**Formule de tarification** (identique pour l'aperçu frontend et le calcul backend) :
```
Σ lignes    = somme(quantité × prix unitaire) des lignes cochées "sélectionnée"
Total HT    = max(Σ lignes, Total HT manuel saisi)
si devis :   TVA = 0, Timbre = 0
si facture : TVA = Total HT × taux TVA / 100
             Timbre = valeur saisie ou valeur par défaut du référentiel
Total TTC   = Total HT + TVA + Timbre
```

**Numérotation** : `genererNumero()` incrémente un compteur par type de document et par année (table `compteurs_documents`), produisant `FAC-2026-0001`, `FAC-2026-0002`, ... et `DEV-2026-0001` séparément.

**Paiements partiels** :
```
nouveau montant payé = montant payé + paiement
si nouveau ≥ TTC        → statut = PAYEE
sinon si nouveau > 0     → statut = PARTIEL
sinon                    → statut = EN_ATTENTE
```
La suppression d'un paiement fait l'opération inverse, sans jamais laisser le montant payé devenir négatif.

### 4.7 `TvaService` — déclaration TVA mensuelle

```
TVA collectée (factures) = Σ, pour chaque facture du mois,
    montant TVA × min(montant payé / total TTC, 1)
    → proportionnelle à ce qui est réellement encaissé, pas au montant facturé

TVA collectée (autres revenus) = Σ TVA extraite des revenus du mois
TVA déductible = TVA déductible des charges du mois (fixes dues + variables)
TVA nette = TVA collectée − TVA déductible
    > 0 → à reverser à l'État
    < 0 → crédit de TVA
```

### 4.8 `DashboardFinanceService` — vues consolidées

Agrège les résultats des autres services (`PayrollService`, `ChargesService`, `DetteService`, `FactureRepository`) pour produire :
- `VueEncaissementsDTO` (factures + autres revenus)
- `VueDecaissementsDTO` (salaires + charges + dettes + taxes)
- `ResultatNetDTO` = encaissements − décaissements, avec la marge en %

### 4.9 `FinanceReferentielService` — pont vers les données RH/Projets

Ce service existe pour une raison précise : les endpoints `/api/clients` et `/api/employes` du reste de l'application sont protégés par les permissions RH (`VIEW_CLIENTS`, `VIEW_EMPLOYES`), auxquelles un compte Finance n'a pas forcément accès. Il expose donc une **vue restreinte, en lecture seule**, gardée uniquement par `VIEW_FINANCE` :

- `GET /api/finance/clients` → tous les clients (`ClientRepository.findAll()`), triés par nom, avec seulement les champs utiles à la facturation.
- `GET /api/finance/employes?mois=...` → employés actifs du mois (via `estActifPourMois`), avec les champs utiles à la paie (CNSS, RIB, salaire...).
- `GET /api/finance/parametres-facturation` → TVA par défaut, timbre par défaut, cycles de charges, catégories — tout lu depuis les référentiels, jamais codé en dur côté frontend.

---

## 5. Sécurité et permissions

### 5.1 Authentification

Le login se fait exclusivement dans `frontend-projects` (`/login`). Les apps RH et Finance redirigent automatiquement vers cette page si l'utilisateur n'a pas de session valide. Le token JWT est transmis via `Authorization: Bearer <token>` sur chaque requête (`axios.ts`).

### 5.2 La permission `VIEW_FINANCE`

Déclarée dans `RoleService.PERMISSION_LABELS` :
```java
Map.entry("VIEW_FINANCE", "Finance — Gérer la paie des employés, les charges de l'agence et les déclarations CNSS")
```

Tous les endpoints `/api/finance/**` sont gardés côté backend par (`SecurityConfig.java`) :
```java
.requestMatchers("/api/finance/**").hasAnyAuthority("ROLE_ADMIN", "VIEW_FINANCE")
```

Côté frontend, `FinanceGuard` (dans `App.tsx`) bloque l'accès à toute l'app si l'utilisateur n'a ni le rôle `ADMIN` ni la permission `VIEW_FINANCE`, avec un écran "Accès refusé" et un lien de retour vers RH.

**Pour donner accès à un utilisateur** : app RH → menu **Rôles** → cocher `VIEW_FINANCE` sur le rôle concerné. Le compte `admin` par défaut (rôle ADMIN) a automatiquement accès à toutes les permissions.

### 5.3 CORS

Le backend n'autorise que les origines listées dans `app.frontend-url` (fichier `application.yml`), qui inclut par défaut `http://localhost:3000`, `3001` et `3002`. Ajouter une nouvelle app frontend se fait en modifiant cette variable, sans toucher au code Java.

---

## 6. Guide pas-à-pas complet

### Étape 1 — Prérequis

- Java 17
- Node.js 18+ et npm
- PostgreSQL 16 en local, avec une base nommée `antigone_rh`

### Étape 2 — Base de données

```bash
# Créer la base si elle n'existe pas déjà
psql -U postgres -c "CREATE DATABASE antigone_rh;"
```
Les identifiants par défaut (`postgres` / `Karim123`) sont dans `Backend/src/main/resources/application.yml` — à adapter via les variables d'environnement `DB_USERNAME` / `DB_PASSWORD` si différents sur votre machine.

### Étape 3 — Démarrer le backend

```bash
cd Backend
./mvnw spring-boot:run
```

Au premier démarrage, `DataInitializer` (exécuté automatiquement) crée :
- Les permissions (dont `VIEW_FINANCE`) et le rôle `ADMIN` avec toutes les permissions.
- Le compte `admin` / mot de passe `Admin@123` (changement obligatoire à la première connexion).
- Les référentiels financiers : `TVA_DEFAUT` (19), `TIMBRE_FISCAL_DEFAUT` (1.000), `CYCLES_CHARGE_FIXE` (1,3,6,12,24,48), les catégories de revenu et de charge.
- Le barème IRPP par défaut (tranches tunisiennes, en vigueur depuis le 2024-01-01).
- Les paramètres de paie par défaut (créés à la première consultation de la page Paramètres — pas au démarrage).

Backend disponible sur `http://localhost:8080`.

### Étape 4 — Installer les dépendances frontend (une seule fois, à la racine)

```bash
# Depuis la racine du monorepo
npm install
```
Ceci installe les dépendances des trois workspaces (`frontend-projects`, `frontend-rh`, `frontend-finance`) d'un coup.

### Étape 5 — Démarrer les frontends

Dans trois terminaux séparés (ou selon vos besoins) :
```bash
npm run dev:projects   # http://localhost:3000 — pour se connecter
npm run dev:rh         # http://localhost:3001
npm run dev:finance    # http://localhost:3002
```

### Étape 6 — Se connecter

1. Ouvrir `http://localhost:3000/login`.
2. Se connecter avec `admin` / `Admin@123` (changement de mot de passe demandé au premier login).
3. Cliquer sur le bouton flottant **💰 Finance** en bas à droite pour basculer vers `localhost:3002`.

### Étape 7 — Préparer les données de référence

1. **Vérifier les employés** : dans l'app RH, s'assurer que les employés ont bien un `type de contrat`, une `date d'embauche` et un `salaire` renseignés — indispensables au calcul de paie.
2. **Vérifier les clients** : dans l'app RH ou Projets, les clients existants sont automatiquement visibles dans Finance. Compléter leur `matricule fiscal` si vous comptez facturer avec.
3. **Ajuster les paramètres de paie** (optionnel) : app Finance → Paramètres → vérifier/modifier les taux CNSS/IRPP/CSS et enregistrer.

### Étape 8 — Premier cycle de paie

1. App Finance → **Salaires** → sélectionner le mois voulu (ex. `2026-08`).
2. Cliquer **"Générer tous les bulletins"**.
3. Vérifier les montants calculés par employé ; cliquer sur un nom pour voir le détail.
4. Marquer les salaires payés (✅) ou enregistrer des acomptes (💵) au fur et à mesure.

### Étape 9 — Facturer un client

1. App Finance → **Factures** → `+ Nouveau`.
2. Choisir le client (déjà dans la liste), ajouter les lignes de prestation, ajuster HT manuel/TVA/timbre si besoin.
3. Enregistrer → le numéro `FAC-YYYY-NNNN` est généré automatiquement.
4. Au fur et à mesure des paiements reçus, cliquer 💵 pour enregistrer un paiement partiel, ou ✅ pour solder directement.

### Étape 10 — Suivre les charges de l'agence

1. App Finance → **Charges** → `+ Ajouter` une charge fixe (ex. loyer mensuel) ou une charge variable ponctuelle.
2. Chaque mois, vérifier la colonne "Reste" et cliquer ✅ pour enregistrer le paiement du mois.

### Étape 11 — Déclarer le CNSS trimestriel

1. App Finance → **CNSS** → sélectionner l'année.
2. Sur le trimestre concerné (une fois que les 3 mois ont leurs bulletins de paie générés), cliquer **"Calculer"**.
3. Vérifier le montant (avec pénalité si en retard), puis **✓** pour marquer comme payé.

### Étape 12 — Consulter les résultats

App Finance → **Tableau de bord** → sélectionner le mois → lire le résultat net, la répartition encaissements/décaissements, et la déclaration TVA du mois.

---

## 7. Pièges connus et comment les éviter

| Symptôme | Cause | Solution |
|---|---|---|
| "0 employés sous contrat" sur Salaires | Aucun employé n'a de contrat couvrant le mois sélectionné (dates d'embauche/fin de contrat mal renseignées) | Vérifier les dates dans la fiche employé RH |
| "Aucun client enregistré en base" alors qu'un client existe | Le backend n'a pas encore les routes `/api/finance/**` chargées (JVM démarrée avant la mise à jour du code), ou blocage CORS, ou permission manquante | Redémarrer le backend ; vérifier `app.frontend-url` contient `localhost:3002` ; vérifier `VIEW_FINANCE` sur le rôle |
| `Request failed with status code 400` sans détail | Exception non gérée côté backend (le `GlobalExceptionHandler` traduit toute `RuntimeException` en 400) | Consulter les logs du backend pour la stack trace complète |
| Toutes les cartes du Dashboard affichent 0,00 DT | Aucune facture émise et/ou aucun bulletin généré pour le mois consulté | Générer les bulletins (Salaires) et créer des factures avant de consulter le Dashboard |
| Le mode BRUT/NET est grisé sur un employé | L'employé est en contrat exonéré (CIVP/Freelance/Stage) — le mode n'a aucun effet puisqu'il n'y a pas de retenue | Comportement normal, aucune action requise |
| Les charges fixes/variables ou catégories de revenu ne proposent aucune option | Les référentiels n'ont pas encore été seedés (backend jamais démarré depuis la mise à jour) | Redémarrer le backend une fois, `DataInitializer` s'exécute automatiquement |
| Régler les taux de paie n'a aucun effet sur des bulletins déjà générés | Les bulletins sont **figés** au moment du calcul (valeurs stockées, pas recalculées à la volée) | Régénérer les bulletins concernés après avoir changé les taux |
| Une déclaration CNSS trimestrielle reste à 0,00 DT après "Calculer" | Les bulletins de paie des 3 mois du trimestre n'ont pas encore été générés | Aller sur Salaires, générer les bulletins de chaque mois du trimestre, puis recalculer |

---

## 8. Référence des endpoints API

Tous les endpoints sont préfixés par `/api/finance` et protégés par `VIEW_FINANCE` (ou rôle `ADMIN`), sauf mention contraire.

### Paie (`/api/finance/paie`)
| Méthode | Route | Rôle |
|---|---|---|
| GET | `/parametres` | Lire les taux de paie |
| PUT | `/parametres` | Modifier les taux de paie |
| GET | `/baremes-irpp` | Lister les barèmes IRPP |
| POST | `/baremes-irpp` | Créer un nouveau barème (versionné) |
| POST | `/calcul` | Calculer une fiche de paie (aperçu, non enregistré) |
| POST | `/generer` | Générer/mettre à jour le bulletin d'un employé |
| POST | `/generer-tout/{mois}` | Générer tous les bulletins du mois |
| GET | `/bulletins?mois=` | Lister les bulletins d'un mois |
| GET | `/bulletins/employe/{id}` | Historique des bulletins d'un employé |
| GET | `/impayes?avant=` | Bulletins impayés avant un mois donné |
| GET | `/totaux?mois=` | Totaux agrégés du mois |
| GET | `/acomptes?employeId=&mois=` | Acomptes versés |
| POST | `/bulletins/{id}/payer` | Marquer un bulletin payé |
| POST | `/acompte` | Enregistrer un acompte |

### Charges (`/api/finance/charges`)
| Méthode | Route |
|---|---|
| GET/POST | `/fixes` |
| PUT/DELETE | `/fixes/{id}` |
| GET/POST | `/fixes/{id}/paiements` |
| GET | `/fixes/etat?mois=` |
| GET/POST | `/variables` |
| PUT/DELETE | `/variables/{id}` |
| GET | `/resume?mois=` |

### CNSS (`/api/finance/cnss`)
| Méthode | Route |
|---|---|
| GET | `?annee=` |
| GET | `/suggestion?annee=&trimestre=` |
| POST | `` (enregistrer) |
| POST | `/{id}/payer` |

### Facturation (`/api/finance/facturation`)
| Méthode | Route |
|---|---|
| GET | `/documents?type=FACTURE\|DEVIS` |
| GET | `/documents/{id}`, `/documents/client/{clientId}`, `/impayees` |
| POST/PUT/DELETE | `/documents`, `/documents/{id}` |
| POST | `/documents/{id}/payer` |
| GET/POST | `/documents/{id}/paiements` |
| DELETE | `/paiements/{id}` |
| GET/POST/PUT/DELETE | `/services`, `/templates` |
| GET/POST | `/relances` |

### Revenus, dettes, contacts, référentiels
| Module | Base |
|---|---|
| Autres revenus | `/api/finance/revenus` |
| Dettes | `/api/finance/dettes` |
| Contacts client | `/api/finance/contacts` |
| Tableaux de bord | `/api/finance/dashboard/{encaissements,decaissements,resultat,tva}` |
| Référentiels (clients, employés, paramètres) | `/api/finance/{clients,employes,parametres-facturation}` |

---

*Document généré à partir de l'état réel du code au 2026-08. En cas de divergence future entre ce document et le comportement observé, le code source (`Backend/src/main/java/com/antigone/rh/`) fait foi.*

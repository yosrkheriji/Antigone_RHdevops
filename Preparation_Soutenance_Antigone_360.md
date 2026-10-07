# Préparation à la soutenance — Antigone 360°

> Manuel construit **exclusivement** à partir de vos deux fichiers :
> - **PPT** : `Yosr Kheriji PFE.pdf` — **35 diapositives**
> - **Rapport** : `Rapport_PFE_Yosr (8).pdf` — **169 pages**, 8 chapitres
>
> Aucune fonctionnalité, technologie ou justification n'a été ajoutée. Quand une information manque dans les deux sources, c'est écrit : *« Information non explicitement documentée dans les fichiers fournis. »*

---

## ⚠️ À lire en premier — quatre points à traiter avant la soutenance

### A. Le projet n'est PAS un projet solo — et votre discours doit le refléter

Le rapport (§1.5.3, tableau 1.5) indique noir sur blanc la répartition des rôles Scrum :

| Rôle Scrum | Personne |
|---|---|
| Product Owner | **Malek Naouar** |
| Scrum Master | **Ahmed Kouki** |
| Équipe de développement | **Yosr Kheriji, Zeineb Haj Hsine** |

**Vous étiez donc deux développeuses.** C'est parfaitement normal et cela renforce la crédibilité de SCRUM (les trois rôles sont réellement tenus par trois personnes différentes). Mais cela implique deux choses :

1. **Ne dites jamais « j'ai tout fait seule ».** Le jury a le tableau sous les yeux.
2. **Préparez la question « qu'avez-vous fait, vous, précisément ? »** — c'est la question la plus probable de toute votre soutenance dès lors qu'il y a deux développeuses. Ayez une réponse nette : quels modules, quelles parties du backend/frontend/IA/BI vous avez portés. *Cette répartition n'est pas documentée dans le rapport — vous devez la préparer vous-même.*

### B. Le planning des sprints contient plusieurs erreurs internes

Le tableau 2.10 (« Planification des sprints ») du rapport est en contradiction avec vos propres chapitres :

| | Tableau 2.10 (§2.3.2) | Backlogs détaillés (ch. 3 à 8) |
|---|---|---|
| Nombre de sprints | texte : « **neuf** sprints » — mais la table liste **dix** lignes | **10** (Sprint 1 à Sprint 10) |
| Libellé de la 10ᵉ ligne | **« Sprint 9 »** (doublon) | « Sprint 10 » (ch. 8) |
| Sprint 3 | 21 pts | **17 pts** |
| Sprint 4 | 21 pts | **29 pts** |
| Sprint 5 | 22 pts | **33 pts** |
| Sprint 8 | 25 pts | **24 pts** |
| Sprint 9 (IA) | 16 pts | **32 pts** |
| **Total affiché** | **208** | somme réelle des lignes = **224** ; somme des chapitres = **254** |

**✅ Corrigez le tableau 2.10 avant la soutenance.** Les chiffres qui font foi sont ceux des backlogs détaillés : **23 · 34 · 17 · 29 · 33 · 28 · 18 · 24 · 32 · 16 = 254 points sur 10 sprints.**

Si le jury le relève : *« Vous avez raison, le tableau de synthèse du chapitre 2 n'a pas été remis à jour après l'ajout des deux dernières releases. Les chiffres exacts sont ceux des backlogs détaillés de chaque chapitre : dix sprints, 254 points au total. »*

### C. Deux identifiants « M18 » pour deux modules différents

- Backlog produit (tableau 2.9) : **M18 = Assistant Finance**
- Chapitre 8, §8.1 : *« le backlog du Sprint 10, consacré au module **M18 « Décisionnel »** »*

**✅ Renumérotez** le module décisionnel (M21 à M24 sont déjà utilisés dans le tableau 2.10 pour l'entrepôt, l'ETL, les tableaux de bord et Power BI — la cohérence serait de garder ceux-là).

### D. Orthographe du nom de votre encadrant professionnel

- Remerciements : « M. Malek **Nouar** »
- Fiche d'identité (tableau 1.1) et PPT : « Malek **Naouar** »

**✅ Harmonisez.** C'est une correction d'une minute qui évite une gêne inutile.

---

# 1. Vue globale de la soutenance

## 1.1 Le contexte

**Antigone** est une agence de stratégie et de communication digitale située à **Rades, Tunis**, **fondée en 2021** par **Malek Naouar**. Elle accompagne ses clients dans la construction de leur stratégie de marketing digital : diagnostic, définition des axes de communication, conseil, études, création et développement de sites web et d'applications mobiles.

## 1.2 La problématique

⚠️ **Attention : vos deux documents la formulent différemment.** Apprenez **celle de la PPT** (c'est elle que le jury verra projetée), et sachez que le rapport en donne une version plus large.

**PPT, diapositive 5 :**
> « Comment centraliser et automatiser les processus RH, projets et financiers d'une agence de communication digitale, tout en exploitant **l'IA et la Business Intelligence** pour offrir un suivi en temps réel, un portail client sécurisé et une meilleure aide à la décision ? »

**Rapport, §1.3.1 :**
> « Comment concevoir une plateforme centralisée, modulaire et automatisée permettant d'unifier la gestion des ressources humaines, le suivi des projets clients et la gestion financière d'une agence de communication digitale, tout en garantissant à chaque acteur, interne comme externe, une visibilité claire et en temps réel sur son périmètre d'activité ? »

**La différence :** la PPT ajoute explicitement **l'IA et la BI**. C'est cohérent avec le fait que ces deux releases ont été ajoutées en cours de projet. Si le jury note l'écart : *« La formulation de ma présentation intègre les deux dernières releases — l'assistant IA et l'informatique décisionnelle — qui ont enrichi le périmètre initial. »*

**Les constats qui la fondent** (rapport §1.3.1) :
- trois logiques de gestion — RH, projets clients, suivi financier — **rarement pilotées ensemble**, chacune dans son propre outil : un tableau Excel pour la paie, une feuille Google Sheets ou **un fil WhatsApp** pour le suivi de projet, un cahier de comptabilité à part ;
- **la même information saisie plusieurs fois** : un client créé côté commercial doit être ressaisi côté facturation → oublis, doublons, incohérences ;
- **aucun moyen autonome pour le client** de savoir où en est sa campagne ;
- **paie et charges sociales (CNSS, IRPP) calculées à la main**, sans trace claire des taux appliqués dans le temps ;
- **pas de vision consolidée** pour la direction (résultat net, trésorerie, charge réelle des équipes).

## 1.3 L'étude de l'existant — trois solutions comparées

| Solution | Forces | Faiblesses |
|---|---|---|
| **Odoo** | RH + projets + finance | Pas de paie tunisienne · portail client limité · peu adapté aux media plans |
| **BambooHR** | RH complète · ergonomique | Pas de gestion projets/finance · pas de paie tunisienne |
| **monday.com** | Gestion projets · workflows personnalisables | Pas de RH/paie · pas de finance · portail client limité |

> **La conclusion à retenir :** *« Ils couvrent bien un seul pan du métier, sans jamais réunir les trois dimensions — RH, gestion de projets clients et finance/paie localisée — dans un même espace pensé pour une agence créative. Aucun ne propose de portail client réellement dédié à l'approbation des livrables. »*

## 1.4 La solution — Antigone 360°

**Sous-titre exact de la page de garde :** « Plateforme intégrée de gestion, d'automatisation et d'aide à la décision ».

Cinq blocs (PPT, diapositive 8) :

| № | Bloc | Promesse |
|---|---|---|
| 1 | **Ressources Humaines** | Congés, pointage et validations automatisés · 12 types de demandes sans papier · tableau de bord RH en temps réel |
| 2 | **Projets & Plans médias** | Suivi de projet clair de A à Z · plans médias validés en un clic · portail dédié par client |
| 3 | **Finance** | Facturation et encaissements sans tableur · paie tunisienne automatique · déclarations sociales et fiscales en un clic |
| 4 | **Assistant IA** | Répond en langage naturel · congés, factures, paie, règlement intérieur · **réponses toujours fondées sur les vraies données, jamais inventées** |
| 5 | **BI & Décisionnel** | Tableaux de bord qui racontent une tendance, pas juste l'instant · repère absentéisme, retards de paiement, projets à risque · exploration libre pour la direction |

> **La phrase de synthèse de la diapositive 8, à connaître :** *« Là où le marché juxtapose des outils génériques et cloisonnés, Antigone 360° réunit RH, Projets, Finance, IA et BI dans une seule plateforme, cohérente de bout en bout. »*

## 1.5 Les technologies (PPT, diapositive 31)

| Catégorie | Technologies |
|---|---|
| **Frontend** | React 19 (bibliothèque interface) · TypeScript (typage statique) · Vite (bundler & dev server) |
| **Backend** | Java 17 (logique métier) · Spring Boot 3 (API REST) · Spring Data JPA (persistance) · Maven (build) |
| **Sécurité** | Spring Security (filtre d'authentification) · JWT + BCrypt (jetons & mots de passe hachés) · RBAC (permissions granulaires) |
| **Données** | PostgreSQL (base unique) · pgvector (recherche vectorielle) · Schéma `dwh` (entrepôt en étoile) |
| **IA** | GPT-4o (génération & classification) · LangChain4j (orchestration d'agents) · RAG hybride (réponses ancrées, jamais inventées) |
| **Outils** | Git / GitHub · VS Code · Postman |

**Compléments du rapport** (§2.4.2) non repris sur la diapositive : **Draw.io** (diagrammes UML), **Swagger** (documentation et test des API), **JavaScript/Electron** (AgentDesktop), **Power BI** et **DAX** (restitution décisionnelle), **Brevo** et **Google Drive** (services externes).

## 1.6 La méthodologie

**Deux niveaux de choix, à distinguer** — c'est une subtilité que le rapport soigne (§1.5) et qui fait bonne impression :

> *« Une **méthodologie** définit les principes généraux et la philosophie qui orientent le processus ; un **cadre de développement** fournit une structure concrète pour les mettre en œuvre à travers des rôles, des pratiques et des règles. »*

1. **Méthodologie retenue : Agile** — comparée à Waterfall, RUP et 2TUP.
2. **Cadre retenu : Scrum** — comparé à Kanban et XP.

**Les trois raisons affichées sur la PPT (diapositive 9) :** Déploiement modulaire · Adaptabilité aux évolutions des exigences · Détection précoce des risques.

**Le découpage réel : 10 sprints de 2 semaines, 6 releases.**

| Release | Sprints | Contenu | Points |
|---|---|---|---|
| R1 — Fondations & identités | 1–2 | Auth JWT, rôles/permissions, notifications, référentiels, comptes, employés, clients, portail | 23 + 34 = **57** |
| R2 — Organisation RH | 3–4 | Calendrier, horaires, pointage/agent, dashboard de pilotage, 12 types de congé, validation multi-niveaux | 17 + 29 = **46** |
| R3 — Projets & plans médias | 5–6 | Projets, équipes, Kanban, compétences, calendrier de production, plan média, double validation, tournage | 33 + 28 = **61** |
| R4 — Gestion financière & paie | 7–8 | Factures/devis, encaissements, charges, dettes, relances, paie tunisienne, CNSS, TVA | 18 + 24 = **42** |
| R5 — Assistant IA | 9 | Chatbot, RAG hybride, tool calling, mémoire, streaming | **32** |
| R6 — Informatique décisionnelle | 10 | Entrepôt `dwh`, chaîne ETL, tableaux de bord analytiques, Power BI | **16** |
| | | **TOTAL** | **254** |

## 1.7 Le principe directeur — la phrase qui traverse tout

> **« La logique métier sensible reste portée par les services du backend — jamais déléguée au frontend, ni au modèle de langage. »**

Sa déclinaison pour l'IA, affichée sur la diapositive 29 et à connaître mot pour mot :
> **« Jamais inventer un chiffre. »** — et, dans le rapport (§7.2.5.2) : *« les calculs métier sont réalisés côté Java et leurs résultats sont transmis au modèle comme des données à reformuler, sans effectuer de nouveaux calculs. »*

## 1.8 Déroulement de la présentation — les 35 diapositives

| # | Section | Diapositive |
|---|---|---|
| 1 | — | Page de garde |
| 2 | — | Plan |
| 3 | Introduction | Introduction |
| 4 | — | **Problématique** *(intercalaire)* |
| 5 | Problématique | La question de recherche |
| 6 | — | **Étude de l'existant** *(intercalaire)* |
| 7 | Étude de l'existant | Solutions existantes sur le marché |
| 8 | Solution proposée | Solution Proposée (5 blocs) |
| 9 | Méthodologie | La méthodologie adoptée |
| 10 | — | **Analyse des besoins** *(intercalaire)* |
| 11 | Analyse des besoins | Les acteurs du système |
| 12 | Analyse des besoins | Besoins fonctionnels : vue par module |
| 13 | Analyse des besoins | Cas d'utilisation de l'Employé |
| 14 | Analyse des besoins | Cas d'utilisation de Validateur |
| 15 | Analyse des besoins | Cas d'utilisation de Chef de Projet |
| 16 | Analyse des besoins | Cas d'utilisation de Responsable Finance |
| 17 | Analyse des besoins | Cas d'utilisation de l'Administrateur |
| 18 | Analyse des besoins | Cas d'utilisation de Client |
| 19 | Analyse des besoins | Besoins non fonctionnels |
| 20 | — | **Conception** *(intercalaire)* |
| 21 | Conception | Architecture logique |
| 22 | Conception | Architecture physique |
| 23 | Conception | Diagramme de classe |
| 24 | — | **Réalisation** *(intercalaire)* |
| 25 | Réalisation | Module Plan Média |
| 26 | Réalisation | Moteur de paie tunisien |
| 27 | Réalisation | Pointage automatisé |
| 28 | Réalisation | Informatique décisionnelle |
| 29 | Réalisation | Assistant AI |
| 30 | Réalisation | Choix du modèle de langage |
| 31 | Réalisation | Technologies utilisées |
| 32 | — | **Démonstration** *(intercalaire)* |
| 33 | Conclusion | Conclusion |
| 34 | Conclusion | Perspectives |
| 35 | — | Merci |

**Structure en quatre temps :** le *pourquoi* (3–8) · le *comment on s'est organisé* (9) · le *quoi* (10–23) · le *résultat* (24–34).

💡 **Six diapositives sont de simples intercalaires de section** (4, 6, 10, 20, 24, 32) : un titre sur fond blanc. **Ne vous y arrêtez pas** — enchaînez d'une phrase de transition. Elles servent au jury à se repérer, pas à être commentées.

---

# 2. Analyse diapositive par diapositive

---

## Diapositive 1 — Page de garde

### 1. Ce que contient la diapositive
- Logos **Antigone** et **ESPRIT** en en-tête
- Titre : **Antigone 360°**
- Sous-titre : « Plateforme intégrée de gestion, d'automatisation et d'aide à la décision »
- **Encadrant universitaire :** Hela Mejri · **Élaboré par :** Kheriji Yosr · **Encadrant professionnel :** Malek Naouar

### 2. Objectif
Poser l'identité du travail en quinze secondes. Le sous-titre annonce déjà les trois ambitions : **gérer**, **automatiser**, **aider à décider** — ce dernier mot préparant la BI et l'IA.

### 🎤 3. Ce que je dois dire à l'oral

> « Bonjour. Je m'appelle Yosr Kheriji, et je vais vous présenter mon projet de fin d'études, réalisé au sein de l'agence Antigone dans le cadre du cycle ingénieur à l'École ESPRIT.
>
> Ce projet s'intitule **Antigone 360°**. C'est une plateforme intégrée de gestion, d'automatisation et d'aide à la décision, conçue pour une agence de communication digitale.
>
> Je remercie ma encadrante universitaire, Madame Hela Mejri, ainsi que mon encadrant professionnel, Monsieur Malek Naouar, directeur de l'agence, pour leur accompagnement tout au long de ces six mois. »

*(≈ 35 secondes.)*

### ⚠️ Attention
- **Prononcez « trois-cent-soixante degrés »**, pas « trois six zéro ».
- Ne lisez pas le sous-titre mot à mot : reformulez-le, comme dans le script.
- **Vérifiez l'orthographe « Naouar »** (voir avertissement D en tête de document).

### 📖 Lien avec le rapport
Page de garde · Remerciements (p. 3) · §1.1 Cadre général.

### ➡️ Transition
> « Voici comment je vais structurer cette présentation. »

---

## Diapositive 2 — Plan

### 1. Ce que contient la diapositive
Neuf boutons répartis sur trois colonnes :
**Introduction** · **Problématique et Étude de l'existant** · **Solution proposée** · **Méthodologie adoptée** · **Analyse des besoins** · **Conception** · **Réalisation** · **Démonstration** · **Conclusion et perspectives**

### 2. Objectif
Donner au jury une carte mentale. Un jury qui sait où vous allez écoute mieux et interrompt moins.

### 🎤 3. Ce que je dois dire à l'oral

> « Ma présentation suit neuf temps, que je regrouperais en quatre blocs.
>
> D'abord **le pourquoi** : le contexte, la problématique rencontrée par l'agence, l'étude des solutions existantes, et la solution que je propose.
>
> Ensuite **la méthode** qui a permis de la construire.
>
> Puis **la conception** : l'analyse des besoins, les acteurs, les cas d'utilisation, l'architecture et le modèle de données.
>
> Enfin **le résultat** : la réalisation, avec un focus sur les modules les plus techniques — le plan média, le moteur de paie, le pointage automatisé, l'informatique décisionnelle et l'assistant IA — suivie d'une démonstration, puis du bilan et des perspectives. »

*(≈ 45 secondes.)*

### ⚠️ Attention
**Ne lisez pas les neuf boutons un par un.** Regroupez-les en blocs de sens, comme dans le script. Lire un sommaire est la façon la plus sûre de perdre l'attention d'un jury dès la deuxième minute.

### 🔗 Liens avec les autres diapositives
Ce plan structure tout le support. Les six diapositives-intercalaires (4, 6, 10, 20, 24, 32) reprennent ces intitulés.

### 📖 Lien avec le rapport
Introduction générale (annonce des huit chapitres, p. 1–2).

⚠️ **Le plan du rapport et celui de la PPT ne se superposent pas exactement :** le rapport annonce « sept chapitres » dans son introduction, mais en contient **huit** (le chapitre 8 sur le décisionnel est décrit dans la liste sans être compté). **Corrigez cette phrase.**

### ➡️ Transition
> « Commençons par le contexte. »

---

## Diapositive 3 — Introduction

### 1. Ce que contient la diapositive
Un paragraphe unique :
> « Les agences de communication digitale entrent dans une nouvelle ère de transformation, où la centralisation des processus, l'automatisation, l'intelligence artificielle et la Business Intelligence redéfinissent la gestion des ressources, le suivi des projets et la prise de décision. »

Plus le logo Antigone en grand sur la droite.

### 2. Objectif
Situer le projet dans une tendance de fond plutôt que dans un problème isolé. Les **quatre mots-clés** de la phrase — centralisation, automatisation, IA, BI — annoncent exactement les cinq blocs de la solution.

### 🎤 3. Ce que je dois dire à l'oral

> « Les agences de communication digitale entrent aujourd'hui dans une nouvelle ère de transformation. Quatre leviers y redéfinissent la façon de travailler : **la centralisation des processus**, **l'automatisation**, **l'intelligence artificielle** et **la Business Intelligence**.
>
> Ces quatre leviers touchent trois domaines qui, dans une agence, sont indissociables : la gestion des ressources humaines, le suivi des projets clients, et la prise de décision.
>
> C'est dans ce cadre que s'inscrit mon projet de fin d'études, mené au sein de l'agence Antigone — une agence de stratégie digitale située à Tunis, fondée en 2021 — sur une période de six mois. »

*(≈ 40 secondes.)*

### 🔧 4. Ce qu'il faut savoir sur l'organisme d'accueil

**À connaître par cœur** (rapport, tableau 1.1) :

| Élément | Valeur |
|---|---|
| Nom | **Antigone** |
| Fondateur | **Malek Naouar** |
| Fondation | **2021** |
| Adresse | **Rades, Tunis** |
| Site | antigoneagency.com |

**Trois domaines d'activité** (§1.2.2) :
1. **Stratégie de marketing digital** — diagnostic de la situation présente, détermination des axes de communication, accompagnement dans la construction et l'optimisation de la stratégie ;
2. **Conseil, études et accompagnement** — méthodes pragmatiques, marketing des services et marketing de l'innovation ;
3. **Création et développement digital** — sites web, applications mobiles.

⚠️ **L'effectif d'Antigone n'est pas documenté dans le rapport.** C'est pourtant une question quasi certaine (« combien de personnes ? »). **Préparez ce chiffre vous-même** — ne pas savoir l'effectif de son propre organisme d'accueil fait mauvaise impression.

### 📌 5. À retenir
- Agence de **stratégie digitale**, Tunis (Rades), **fondée 2021**, fondateur **Malek Naouar**.
- Les 4 leviers : **centralisation · automatisation · IA · BI**.
- Durée : **six mois**, cycle ingénieur ESPRIT.
- ⚠️ Effectif à préparer séparément.

### ❓ Questions du jury

**Q1 — « Que fait exactement Antigone ? »**
> « C'est une agence de stratégie digitale. Elle accompagne ses clients dans la construction de leur stratégie de marketing digital : elle commence par un diagnostic de la situation présente, puis détermine les axes de communication les plus pertinents. Elle fournit aussi du conseil et des études, et elle conçoit et développe des sites web et des applications mobiles. »

**Q2 — « Combien d'employés ? Combien de clients ? »**
> ⚠️ *Non documenté dans le rapport.* **Préparez la réponse.** À défaut : « Je préfère ne pas avancer un chiffre approximatif — ce que je peux dire, c'est que l'agence gère en parallèle plusieurs projets clients avec une équipe pluridisciplinaire, ce qui est précisément ce qui a rendu les outils dispersés insuffisants. »

**Q3 — « Pourquoi parler d'IA et de BI dès l'introduction ? »**
> « Parce que ce sont deux des cinq composantes de la solution, et surtout parce qu'elles répondent à deux besoins distincts. L'automatisation et la centralisation traitent le **présent** — faire circuler l'information sans ressaisie. L'IA et la BI traitent l'**accès à l'information** et le **passé** : l'assistant permet d'obtenir une réponse sans naviguer dans les modules, la BI permet de lire une tendance plutôt qu'un instant. »

**Q4 — « Six mois, c'est court pour ce périmètre. »**
> « Le périmètre était large, mais il a été découpé en **six releases indépendamment livrables**, sur dix sprints de deux semaines. Chaque release s'appuie sur la précédente sans la modifier — j'ai pu ajouter la finance puis l'IA sans retoucher le socle RH. Et le projet a été mené en équipe : deux développeuses, avec un Product Owner et un Scrum Master côté agence. »

**Q5 — « Le sujet vous a-t-il été imposé ou l'avez-vous proposé ? »**
> « Le besoin venait de l'agence : la mission confiée était de concevoir et développer une plateforme intégrée pour centraliser et optimiser la gestion des ressources humaines, des projets et des activités financières. »

### ⚠️ Question piège
**« L'IA et la BI ne sont-elles pas un effet de mode ajouté pour faire moderne ? »**
> « Elles répondent chacune à un constat précis de l'étude de l'existant. La BI répond à l'absence de vision consolidée pour la direction — le rapport note qu'elle n'avait accès à aucun indicateur fiable, résultat net, trésorerie ou charge réelle des équipes, au moment où elle en avait besoin. L'assistant IA, lui, répond au fait que l'information existe désormais dans la plateforme mais reste répartie entre cinq modules : poser une question en langage naturel évite d'avoir à savoir dans quel écran chercher. Ce ne sont pas des ajouts décoratifs, ce sont des réponses à deux manques identifiés. »

### 📖 Lien avec le rapport
§1.1 Cadre général · §1.2 Présentation de l'organisme d'accueil · tableau 1.1 · Introduction générale.

### 🔗 Liens avec les autres diapositives
Les quatre leviers annoncent les cinq blocs de la **diapositive 8**. L'organisme prépare la **diapositive 5** (problématique).

### ➡️ Transition
> « Voyons maintenant le problème concret auquel cette agence était confrontée. »

---

## Diapositive 4 — *Intercalaire : Problématique*

Titre seul sur fond blanc.

### 🎤 Ce que je dois dire
> « J'en viens à la problématique. »

*(3 secondes — enchaînez immédiatement sur la diapositive 5.)*

### ⚠️ Attention
**Ne vous arrêtez pas.** Une pause sur un intercalaire donne l'impression d'hésiter.

---

## Diapositive 5 — La problématique

### 1. Ce que contient la diapositive
Le logo Antigone en filigrane, et **la question de recherche** centrée, en violet :

> « Comment centraliser et automatiser les processus RH, projets et financiers d'une agence de communication digitale, tout en exploitant l'IA et la Business Intelligence pour offrir un suivi en temps réel, un portail client sécurisé et une meilleure aide à la décision ? »

### 2. Objectif
**C'est la diapositive la plus importante de votre première moitié.** Elle doit graver dans l'esprit du jury la question à laquelle tout le reste répond. Notez sa construction : elle contient **cinq livrables** — centralisation, automatisation, IA, BI, portail client — qui sont exactement les cinq blocs de la solution.

### 🎤 3. Ce que je dois dire à l'oral

> « Dans une agence de conseil en stratégie et communication digitale, la valeur produite ne dépend pas seulement de la créativité des équipes. Elle dépend tout autant de la capacité à faire cohabiter **trois logiques de gestion très différentes** : les ressources humaines internes, le pilotage des projets clients, et le suivi financier de l'activité.
>
> Or, dans la pratique, ces trois dimensions sont rarement pilotées ensemble. Chacune vit dans son propre outil : un tableau Excel pour la paie, une feuille Google Sheets ou un fil WhatsApp pour suivre l'avancement d'un projet, un cahier de comptabilité tenu à part. Ces outils n'ont jamais été pensés pour communiquer entre eux — **et la même information peut se retrouver saisie plusieurs fois, sous des formes différentes, par des personnes différentes**. Un client créé côté commercial doit souvent être ressaisi manuellement côté facturation, ce qui ouvre la porte aux oublis et aux doublons.
>
> La problématique se formule donc ainsi : **comment centraliser et automatiser les processus RH, projets et financiers d'une agence de communication digitale, tout en exploitant l'IA et la Business Intelligence pour offrir un suivi en temps réel, un portail client sécurisé et une meilleure aide à la décision ?** »

*(≈ 1 minute. **Marquez un temps d'arrêt après la question.** C'est le moment le plus important de votre introduction.)*

### 🔧 4. Explication détaillée — les quatre coûts de la fragmentation

Le rapport (§1.3.1) les nomme précisément. **À savoir citer :**

| Coût | Formulation du rapport |
|---|---|
| **Incohérence opérationnelle** | « les congés et les tâches sont suivis de façon incohérente » |
| **Communication éclatée** | « entre RH, chefs de projet, service financier et clients » |
| **Opacité pour le client** | « les clients n'ont souvent aucune manière autonome de savoir où en est leur campagne » |
| **Risque financier** | « les calculs de paie et de charges sociales (CNSS, IRPP) restent faits à la main, avec un risque d'erreur non négligeable **et aucune trace claire des taux appliqués au fil du temps** » |
| **Décision aveugle** | « l'absence de vision consolidée rend difficile l'accès à des indicateurs fiables — résultat net, trésorerie, charge de travail réelle des équipes » |

💡 **« Aucune trace claire des taux appliqués au fil du temps » est la phrase à retenir** : c'est elle qui justifie directement le **versionnement du barème IRPP** (diapositive 26). Faire ce lien devant le jury est très efficace.

**Chaque symptôme a reçu une réponse technique identifiable :**

| Symptôme | Réponse implémentée | Où |
|---|---|---|
| Ressaisie du même client | Base unique, une seule source de vérité | Architecture (diapo 21) |
| Congés suivis de façon incohérente | Entité `Demande`, historisation des statuts, circuit de validation multi-niveaux | R2 / Sprint 4 |
| Client sans visibilité | Portail dédié, accès distinct des comptes internes | R1 / Sprint 2 |
| Paie manuelle sans traçabilité des taux | Moteur de paie + **barème IRPP versionné** | R4 / Sprint 8 |
| Pas de vision consolidée | Entrepôt `dwh` + Power BI | R6 / Sprint 10 |

### 📌 5. À retenir
- **La problématique mot pour mot** (apprenez-la).
- Les **trois logiques** : RH interne · projets clients · suivi financier.
- Les outils réels cités : **Excel, Google Sheets, WhatsApp, cahier de comptabilité**.
- Le problème central : **la même information saisie plusieurs fois**.
- La phrase-clé : **« aucune trace claire des taux appliqués au fil du temps »**.

### ❓ Questions du jury

**Q1 — « Reformulez votre problématique. »**
> Récitez-la. **Question la plus probable de toute la soutenance.**

**Q2 — « Ces constats sont-ils documentés ou est-ce votre interprétation ? »**
> « Ils viennent du recueil des besoins mené au début du stage auprès des équipes de l'agence, et ils sont formalisés au chapitre 1 de mon rapport. Ils ont ensuite été traduits en besoins fonctionnels module par module au chapitre 2, chaque fonctionnalité étant rattachée à l'acteur qui la déclenche. »

**Q3 — « Quel était le problème le plus coûteux ? »**
> « En termes de **risque**, la paie : une erreur de calcul sur un salaire a des conséquences contractuelles et humaines directes, et l'absence de trace des taux appliqués rendait tout contrôle a posteriori impossible. En termes de **temps**, la double saisie : la même information — un client, une facture, une tâche — ressaisie par plusieurs personnes dans plusieurs outils. »
> ⚠️ **Ne chiffrez pas ce coût** — aucune mesure en heures ou en dinars n'existe dans le rapport.

**Q4 — « Pourquoi ne pas avoir traité un seul de ces trois domaines en profondeur ? »**
> « Parce que la valeur vient précisément de leur **articulation**. Un exemple concret : le tableau de bord de présence ne peut pas fonctionner avec le seul module de pointage — il doit croiser le pointage, le calendrier d'entreprise, les horaires paramétrés, les congés approuvés et les télétravails approuvés. Ce sont quatre sources issues de deux sprints différents. Traiter les congés seuls aurait reproduit la dispersion que le projet vise à supprimer. »

**Q5 — « Comment garantissez-vous qu'une information n'est saisie qu'une fois ? »**
> « Par une **base de données unique** partagée par les trois interfaces. Un client créé dans le module Projets est la même ligne que le client destinataire d'une facture dans le module Finance — pas une copie. C'est ce que mon rapport appelle "une seule source de vérité", et c'est aussi la raison pour laquelle j'ai écarté les microservices : ils auraient réintroduit le problème de synchronisation que je cherchais à supprimer. »

**Q6 — « Et le problème de traçabilité ? »**
> « Il est traité à trois niveaux. Les **accès** : chaque connexion, réussie ou échouée, est journalisée avec son adresse IP. Les **décisions métier** : chaque changement de statut d'une demande est historisé avec son auteur et sa date. Et les **appels de l'assistant IA** : chaque outil appelé est enregistré avec son issue. C'est d'ailleurs listé comme apport explicite de la solution dans mon rapport : "une meilleure traçabilité, qui renforce le contrôle interne". »

### ⚠️ Questions pièges

**« Un tableur bien tenu n'aurait-il pas suffi ? »**
> « Pour un seul domaine et à petite échelle, peut-être. Mais un tableur ne sait faire ni le circuit de validation multi-niveaux, ni la traçabilité horodatée de chaque décision, ni surtout le **cloisonnement** : un client ne doit voir que ses données, un employé ne doit pas accéder au module Finance sans permission explicite. Un tableur partagé n'a aucun modèle d'autorisation. Et il ne résout pas le problème central, qui n'est pas le calcul mais **la double saisie entre outils qui ne communiquent pas**. »

**« Avez-vous mesuré le gain après la mise en place ? »**
> « Non, cette mesure n'a pas été réalisée dans le cadre de mon projet, donc je préfère ne pas avancer de chiffre non vérifié. Un gain mesurable demanderait un suivi sur plusieurs mois d'exploitation, ce qui dépassait la durée du stage. Ce que je peux affirmer, c'est que les calculs sont désormais **déterministes et reproductibles**, et que la double saisie est structurellement éliminée par la base unique. »

### 📖 Lien avec le rapport
§1.3.1 Problématique (encadré p. 6) · Introduction générale.

### 🔗 Liens avec les autres diapositives
Les cinq livrables de la question sont les cinq blocs de la **diapositive 8**. La **diapositive 33** (Conclusion) y répond en miroir.

### ➡️ Transition
> « Avant de concevoir une solution, j'ai regardé ce que proposait déjà le marché. »

---

## Diapositive 6 — *Intercalaire : Étude de l'existant*

Titre seul.

### 🎤 Ce que je dois dire
> « J'ai donc mené une étude de l'existant. »

*(3 secondes.)*

---

## Diapositive 7 — Solutions existantes sur le marché

### 1. Ce que contient la diapositive
Un tableau à trois colonnes — **Solution · Forces · Faiblesses** — avec les logos :

| Solution | Forces | Faiblesses |
|---|---|---|
| **Odoo** | RH + projets + finance | Pas de paie tunisienne · portail client limité · peu adapté aux media plans |
| **BambooHR** | RH complète · ergonomique | Pas de gestion projets/finance · pas de paie tunisienne |
| **monday.com** | Gestion projets · workflows personnalisables | Pas de RH/paie · pas de finance · portail client limité |

### 2. Objectif
**Justifier le « make » plutôt que le « buy ».** C'est une étape que beaucoup de projets sautent — la traiter sérieusement est un point fort. Le message : ces outils sont matures, mais **aucun ne couvre les trois dimensions ensemble**.

### 🎤 3. Ce que je dois dire à l'oral

> « Le marché propose déjà des outils de gestion RH, de gestion de projets et de comptabilité. Il était donc légitime de regarder comment ces solutions établies répondent — ou non — aux besoins d'une agence de communication digitale, avant d'envisager de construire quoi que ce soit. J'en ai retenu trois, représentatives.
>
> **Odoo** est une suite ERP modulaire open source. C'est celle qui se rapproche le plus, sur le papier, d'une couverture RH + projets + finance, grâce à ses modules Employees, Project et Invoicing. Mais elle reste pensée pour un usage généraliste : **aucune localisation pour la paie tunisienne** — CNSS, IRPP, TFP — aucun module pensé pour les media plans, et une configuration lourde pour la faire coller au métier d'une agence créative.
>
> **BambooHR** adopte une approche plus ciblée : c'est une solution RH reconnue, très ergonomique. Mais son périmètre s'arrête strictement aux RH : aucune gestion de projet, aucun portail client, et là encore aucune paie localisée.
>
> **monday.com** est une plateforme de type *Work OS*, largement adoptée par les agences pour organiser tâches et calendriers éditoriaux, avec une bonne personnalisation des workflows. En contrepartie, elle ne propose aucun module RH ni comptable, et le partage avec les clients se limite à un accès en lecture, **sans véritable espace d'approbation**.
>
> Ces trois outils partagent la même limite : chacun couvre bien **un seul pan du métier**. Aucun ne réunit les trois dimensions dans un espace pensé pour une agence créative, et aucun ne propose de portail client réellement dédié à l'approbation des livrables. C'est cet écart qui justifie le développement d'une solution dédiée. »

*(≈ 1 min 20.)*

### 🔧 4. Le tableau complet du rapport

La PPT ne montre que trois colonnes. **Le rapport (tableau 1.2) en contient onze critères** — connaissez-les, le jury peut creuser :

| Critère | Odoo | BambooHR | monday.com |
|---|---|---|---|
| Gestion employés et congés | Oui | Oui | Non |
| Pointage / présence | Partiel | Oui | Non |
| **Paie localisée Tunisie (CNSS, IRPP)** | **Non** | **Non** | **Non** |
| Gestion de projets et tâches | Oui | Non | Oui |
| Media plans / calendrier de contenu | Non | Non | Partiel |
| **Portail client avec approbation** | **Non** | **Non** | **Limité** |
| Facturation et suivi des charges | Oui | Non | Non |
| Calendrier unifié (RH + projets) | Partiel | Non | Partiel |
| Notifications en temps réel | Oui | Oui | Oui |
| Personnalisation métier agence | Moyenne | Faible | Élevée |
| Modèle économique | Gratuit (Community) / Payant (Enterprise) | Payant (SaaS) | Payant (SaaS) |

💡 **Les deux lignes où les trois colonnes disent « Non » sont votre meilleur argument** : la **paie tunisienne** et le **portail client avec approbation**. Ce sont exactement les deux briques les plus spécifiques de votre projet.

### 📌 5. À retenir
- **Trois solutions** : Odoo (ERP généraliste) · BambooHR (RH pure) · monday.com (Work OS projets).
- **Le trou dans le marché** : aucune ne fait la **paie tunisienne**, aucune ne fait le **portail client avec approbation**.
- Chacune couvre **un seul pan** du métier.
- Le rapport compare sur **11 critères**.

### ❓ Questions du jury

**Q1 — « Avez-vous testé ces outils ou est-ce une étude documentaire ? »**
> **Réponse honnête :** « C'est une étude documentaire, appuyée sur les documentations officielles de chaque éditeur, référencées dans ma bibliographie. Je n'ai pas mené de proof of concept sur chacun — cela aurait demandé un temps que le stage ne permettait pas. La comparaison porte sur onze critères fonctionnels, définis à partir des besoins recueillis chez Antigone. »

**Q2 — « Odoo est modulaire et open source. Pourquoi ne pas l'avoir étendu plutôt que tout redévelopper ? »**
> **Question très probable — préparez-la.** « C'était l'option la plus sérieuse, et c'est pour cela qu'Odoo figure en premier dans ma comparaison. Trois raisons l'ont écartée. D'abord, **le travail de localisation de la paie** : CNSS, IRPP progressif avec barème versionné, contribution de solidarité, TFP, FOPROLOS, déclaration trimestrielle — cela représente un module de paie complet à écrire, pas une adaptation. Ensuite, **le module media plan n'existe pas** : le workflow à double validation avec réservation de créneau de tournage aurait été un développement intégral. Enfin, le rapport note qu'"une configuration lourde est nécessaire pour la faire coller au métier d'une agence créative" — on se retrouve à développer l'essentiel du métier **tout en héritant de la complexité d'un ERP généraliste**. »

**Q3 — « Pourquoi ces trois-là et pas d'autres ? »**
> « Parce qu'elles sont représentatives des trois familles d'outils que l'agence aurait pu adopter : un **ERP généraliste** couvrant plusieurs domaines, une **solution RH pure**, et une **plateforme de gestion de projets**. Elles délimitent l'espace des possibles. En ajouter d'autres aurait allongé le tableau sans changer la conclusion : le manque est structurel, pas lié à un produit particulier. »

**Q4 — « Le coût de développement est-il inférieur au coût d'une licence ? »**
> **Réponse honnête :** « Je n'ai pas réalisé d'analyse coût-bénéfice chiffrée, et je ne vais pas en inventer une. L'argument retenu dans mon rapport n'est pas financier mais fonctionnel : aucune des solutions étudiées n'apportait de réponse satisfaisante sur la paie tunisienne ni sur le portail d'approbation. Même en payant une licence, ces deux briques auraient dû être développées. »

**Q5 — « "Portail client limité" — qu'est-ce qui manque exactement ? »**
> « L'**approbation**. Sur monday.com, le partage avec le client se limite à un accès en lecture sur des tableaux. Or, dans une agence, le client ne doit pas seulement consulter son plan média : il doit pouvoir **approuver ou refuser chaque ligne**, et laisser des commentaires. C'est un circuit de décision, pas une vue partagée. C'est exactement ce que réalise mon module Plan Média avec sa double validation. »

**Q6 — « Qu'est-ce qu'un ERP, et qu'est-ce qu'un Work OS ? »**
> « Un **ERP** — *Enterprise Resource Planning* — est un progiciel de gestion intégré : il couvre plusieurs fonctions de l'entreprise (RH, ventes, comptabilité, stock) autour d'une base commune. Odoo en est un, modulaire et open source. Un **Work OS** est une catégorie plus récente : une plateforme générique de gestion du travail, où l'on compose soi-même ses tableaux et ses workflows sans que l'outil impose un métier. monday.com en est représentatif — d'où sa forte personnalisation, mais aussi son absence de modules métier comme la paie. »

### ⚠️ Questions pièges

**« Ne pourrait-on pas simplement combiner BambooHR et monday.com ? »**
> « On pourrait, mais cela reconduirait exactement le problème initial : **deux outils qui ne communiquent pas**. Le rapport décrit précisément ce symptôme — la même information saisie plusieurs fois par des personnes différentes. Et il resterait deux manques que ni l'un ni l'autre ne comble : la paie tunisienne, et le portail client avec approbation. On aurait deux abonnements, deux bases, et toujours pas de vision consolidée. »

**« Votre solution est-elle vraiment meilleure que ces produits matures ? »**
> « Non, et je ne le prétends pas. Sur leur propre terrain, ces outils sont bien plus complets et bien plus éprouvés que le mien — BambooHR fait des choses RH que je ne fais pas, monday.com a une souplesse de workflow que je n'ai pas. Mon avantage n'est pas la largeur, c'est **l'ajustement** : la paie est conforme au droit tunisien, le module media plan correspond au processus réel de l'agence, et les trois domaines partagent une base unique. C'est une solution plus légère, pensée dès le départ pour ce métier-là. »

### 📖 Lien avec le rapport
§1.3.2 Étude de l'existant · **tableau 1.2** (comparaison sur 11 critères) · références bibliographiques [1], [2], [3].

### 🔗 Liens avec les autres diapositives
Les deux manques identifiés ici — paie tunisienne et portail d'approbation — sont traités respectivement en **diapositive 26** (moteur de paie) et **diapositive 25** (module plan média). C'est le lien le plus fort de votre présentation : **le trou du marché est comblé par vos deux modules phares.**

### ➡️ Transition
> « Face à ces limites, voici la solution que nous avons conçue. »

---
## Diapositive 8 — Solution Proposée

### 1. Ce que contient la diapositive
Cinq blocs numérotés, disposés sur deux rangées, avec une flèche descendante entre la rangée 1 et la rangée 2 :

| № | Bloc | Les 3 puces affichées |
|---|---|---|
| **1** | **Ressources Humaines** | Congés, pointage et validations automatisés · 12 types de demandes gérées sans papier · Un tableau de bord RH en temps réel |
| **2** | **Projets & Plans médias** | Suivi de projet clair, de A à Z · Plans médias validés en un clic · Un portail dédié pour chaque client |
| **3** | **Finance** | Facturation et encaissements sans tableur · Paie tunisienne calculée automatiquement · Déclarations sociales et fiscales prêtes en un clic |
| **4** | **Assistant IA** | Répond en langage naturel à toute l'équipe · Congés, factures, paie, règlement intérieur — une seule question suffit · Des réponses toujours fondées sur les vraies données de l'agence, **jamais inventées** |
| **5** | **BI & Décisionnel** | Des tableaux de bord qui racontent une tendance, pas juste l'instant · Repère l'absentéisme, les retards de paiement, les projets à risque · Exploration libre pour la direction, sans solliciter le développement |

**Bandeau final :**
> ⭐ « Là où le marché juxtapose des outils génériques et cloisonnés, **Antigone 360° réunit RH, Projets, Finance, IA et BI dans une seule plateforme**, cohérente de bout en bout. »

### 2. Objectif
Répondre point par point à la diapositive 7. La flèche entre la rangée 1 et la rangée 2 porte un sens : **les trois modules métier produisent la donnée, l'IA et la BI l'exploitent.** C'est le message d'architecture de la diapositive.

### 🎤 3. Ce que je dois dire à l'oral

> « La solution retenue prend la forme d'une plateforme web unique, baptisée **Antigone 360°**, qui réunit dans un même environnement ce qui se gérait jusque-là de façon dispersée.
>
> Elle s'organise en cinq blocs. Les trois premiers sont les **modules métier**. Le module **RH** couvre tout le cycle administratif de l'employé : congés, pointage et validations automatisés, douze types de demandes gérées sans papier, et un tableau de bord RH en temps réel. Le module **Projets et plans médias** donne aux équipes créatives un suivi de projet de bout en bout, des plans médias validés en un clic, et un portail dédié pour chaque client. Le module **Finance** prend en charge la dimension comptable et fiscale : facturation et encaissements sans tableur, paie tunisienne calculée automatiquement, et déclarations sociales et fiscales prêtes en un clic.
>
> Les deux derniers blocs **exploitent** la donnée produite par les trois premiers — c'est le sens de la flèche. L'**assistant IA** répond en langage naturel à toute l'équipe : congés, factures, paie, règlement intérieur, une seule question suffit. Et le point essentiel : ses réponses sont **toujours fondées sur les vraies données de l'agence, jamais inventées**. Le **décisionnel**, lui, fournit des tableaux de bord qui racontent une tendance plutôt qu'un instant : ils repèrent l'absentéisme, les retards de paiement, les projets à risque, et permettent à la direction d'explorer librement sans solliciter le développement.
>
> En une phrase : **là où le marché juxtapose des outils génériques et cloisonnés, Antigone 360° réunit RH, Projets, Finance, IA et BI dans une seule plateforme, cohérente de bout en bout.** »

*(≈ 1 min 40. **C'est votre diapositive pivot** — prenez le temps.)*

### 🔧 4. Explication détaillée

#### a) Les huit apports listés par le rapport (§1.4.5)

La diapositive résume ; le rapport détaille. **À connaître :**

| Apport | Ce qu'il résout |
|---|---|
| **Une seule source de vérité** | Supprime les doubles saisies et les écarts constatés avec Excel, les e-mails ou WhatsApp |
| **Des processus automatisés** | Soldes de congés, paie, TVA, workflows de validation remplacent des tâches manuelles, lentes et sujettes à erreur |
| **Une visibilité en temps réel** | Tableaux de bord et notifications, là où l'agence n'avait aucun suivi centralisé |
| **Une adaptation au contexte local** | Moteur de paie conforme à la réglementation tunisienne (**CNSS, IRPP, TFP, FOPROLOS**) — point sur lequel aucune solution étudiée n'apportait de réponse |
| **Une personnalisation métier réelle** | Module Media Plans et portail d'approbation, pensés pour une agence créative plutôt que calqués sur un outil générique |
| **Une meilleure traçabilité** | Historisation des changements de statut, des validations, des paiements et **des appels aux outils de l'assistant IA** → renforce le contrôle interne |
| **Une architecture évolutive** | Conçue pour accueillir de nouveaux modules, permissions ou capacités d'assistance sans remettre en cause l'existant |
| **Une aide à la décision améliorée** | Entrepôt de données centralisant RH, finance et projets, restitué en indicateurs et tableaux de bord analytiques |
| **Une assistance intelligente et centralisée** | Assistant IA capable de générer des media plans, préparer des relances, expliquer les bulletins de paie et rechercher des informations internes, **tout en respectant les droits d'accès** |

#### b) Le détail des trois modules métier (§1.4.1 à §1.4.3)

**Module RH :** fiche employé complète (informations personnelles, matricule, CIN, contrat, rattachement hiérarchique) · demandes de congés (**douze types**), d'autorisations et de télétravail, avec calcul automatique des jours et **validation à plusieurs niveaux** · suivi du pointage avec détection des retards et des journées incomplètes, complété par l'agent **AgentDesktop** · calendrier unifié (congés, jours fériés, télétravail, horaires) et référentiels paramétrables · notifications en temps réel et assistant conversationnel s'appuyant sur le règlement intérieur.

**Module Projets :** gestion des clients et de leurs projets (statuts, échéances, équipes, chef de projet) · suivi des tâches en **vue Kanban** (À faire / En cours / Terminé) avec assignation, dates limites et niveaux d'urgence · module **Media Plans** (format, plateforme, date de publication) avec workflow d'approbation et calendrier de tournage · organisation des réunions, présentiel ou distance · intégration **Google Drive** pour les livrables.

**Module Finance :** calcul automatisé de la paie (CNSS, **IRPP progressif**, contribution de solidarité, charges patronales) avec **génération groupée** des bulletins · facturation et devis à numérotation automatique, suivi des paiements **y compris partiels** · charges fixes et variables avec échéancier et **cumul automatique des impayés** · **déclaration CNSS trimestrielle avec calcul des pénalités de retard**, et calcul mensuel de la TVA nette · tableau de bord financier consolidant encaissements, décaissements et résultat net.

**Portail Client :** accès sécurisé et **volontairement limité à la consultation et à l'approbation** — visualisation des projets et media plans en lecture seule, approbation ou refus des contenus proposés, accès direct aux fichiers Google Drive.

💡 **La formule « volontairement limité » est à reprendre telle quelle** : elle montre que la restriction est un choix de conception, pas une lacune.

### 📌 5. À retenir
- Les **5 blocs** dans l'ordre : RH · Projets & Plans médias · Finance · Assistant IA · BI & Décisionnel.
- La **flèche** : les 3 modules métier **produisent**, l'IA et la BI **exploitent**.
- **CNSS, IRPP, TFP, FOPROLOS** — les quatre sigles de la paie tunisienne.
- Le portail client est **volontairement limité** à consultation + approbation.
- La phrase du bandeau, mot pour mot.

### ❓ Questions du jury

**Q1 — « Présentez votre solution en deux minutes. »**
> Le script oral ci-dessus. **Question d'ouverture classique.**

**Q2 — « Pourquoi "360°" ? »**
> « Parce que la plateforme couvre l'activité de l'agence sous tous ses angles : les ressources humaines en interne, les projets et plans médias côté production, la finance côté gestion, plus le point de vue du client via son portail. Et avec les deux dernières releases, elle couvre aussi les deux usages de la donnée : l'assistant pour y accéder en langage naturel, le décisionnel pour l'analyser dans le temps. L'idée est qu'aucun angle de l'activité ne reste hors du système. »

**Q3 — « Quelle est la différence entre l'assistant IA et les tableaux de bord BI ? »**
> **Excellente question, préparez-la soigneusement.** « Ils répondent à deux besoins différents. L'**assistant** répond à une question **ponctuelle et individuelle**, en langage naturel : "combien de jours de congé maladie ai-je droit ?", "où en est la facture de tel client ?". Il évite de devoir savoir dans quel écran chercher. La **BI** répond à une question **analytique et historique** : "l'absentéisme du département création a-t-il augmenté depuis deux ans ?". C'est la différence entre accéder à une donnée et lire une tendance. Et techniquement, ils n'utilisent pas la même source : l'assistant interroge les tables transactionnelles via des outils, la BI lit un entrepôt dénormalisé rechargé chaque nuit. »

**Q4 — « "Jamais inventées" — comment le garantissez-vous ? »**
> « Par une séparation stricte entre le raisonnement et l'exécution, qui est un principe explicite de ma conception de prompts. Les calculs métier sont réalisés **côté Java**, et leurs résultats sont transmis au modèle comme des **données à reformuler**, sans qu'il effectue de nouveaux calculs. Concrètement, pour une explication de bulletin de paie, le modèle ne reçoit pas les paramètres — il reçoit le net déjà calculé, étape par étape. Il n'a aucun calcul à faire, donc aucune occasion de se tromper. »

**Q5 — « "Validés en un clic" pour les plans médias — n'est-ce pas exagéré ? »**
> « C'est la formulation côté utilisateur, et elle est juste de son point de vue : le client approuve son plan média en un clic depuis son portail, ou même en lot pour tout un mois. Ce qui est masqué derrière ce clic, en revanche, est un workflow complet : une double validation — manager interne puis client — et, pour une ligne marquée "shooting", la réservation d'un créneau de tournage soumis au responsable de production, qui matérialise ensuite automatiquement un projet et trois tâches. C'est justement l'objet de ma diapositive 25. »

**Q6 — « Le client peut-il modifier quelque chose ? »**
> « Très peu, et c'est volontaire. Le rapport parle d'un accès "volontairement limité à la consultation et à l'approbation". Le client peut **approuver ou refuser** un contenu, **commenter** une ligne de plan média, et **accéder** à ses fichiers Drive. Tout le reste est en lecture seule : il ne crée rien, ne modifie rien, ne supprime rien. »

**Q7 — « Que signifie "prêtes en un clic" pour les déclarations ? »**
> « Que le système **calcule et propose** la déclaration, il ne la transmet pas. Pour la CNSS trimestrielle, il parcourt les bulletins des trois mois du trimestre, somme la part salariale et la part patronale, calcule la pénalité de retard éventuelle, et propose le montant. Le comptable l'enregistre puis, après règlement effectif auprès de l'organisme, la marque comme payée. La déclaration TVA mensuelle, elle, est recalculée à chaque consultation. »

### ⚠️ Questions pièges

**« Cinq blocs en six mois, à deux développeuses. Tout est-il réellement implémenté au même niveau de finition ? »**
> **Réponse honnête et structurée :** « Non, et c'est normal — les six releases n'ont pas la même profondeur. Les trois modules métier sont les plus complets : ils représentent huit sprints sur dix et l'essentiel de la charge. L'assistant IA et le décisionnel sont les deux derniers sprints, respectivement 32 et 16 points. Le décisionnel, en particulier, est le plus léger : un entrepôt, une procédure ETL et des restitutions, mais sans la richesse fonctionnelle des modules métier. Je les présente comme des briques abouties dans leur périmètre, pas comme des produits équivalents aux trois premiers. »

**« Vous dites "une seule plateforme", mais vous avez trois applications frontend. N'est-ce pas contradictoire ? »**
> « Non, parce que l'unité est là où elle compte : **un seul backend et une seule base de données**. Les trois interfaces ne sont pas trois produits, ce sont trois vues spécialisées sur le même système — un employé qui dépose un congé n'a rien à faire dans les écrans de facturation. L'architecture N-Tiers que j'ai retenue est précisément celle qui permet à plusieurs clients de consommer la même couche métier. La cohérence des données, elle, est absolue : un client est la même ligne, qu'on le regarde depuis le module Projets ou depuis la facturation. »

### 📖 Lien avec le rapport
§1.4 Solution proposée · §1.4.1 à §1.4.4 (détail des modules) · **§1.4.5 Apports de la solution** (les neuf apports).

### 🔗 Liens avec les autres diapositives
Répond point par point à la **diapositive 7**. Les cinq blocs sont détaillés en **diapositives 25 à 29**. Le bloc « Assistant IA » se retrouve en **29**, « BI » en **28**.

### ➡️ Transition
> « Pour construire cette solution, il a fallu une méthode. »

---

## Diapositive 9 — La méthodologie adoptée

### 1. Ce que contient la diapositive
- Phrase d'introduction : « La méthodologie adoptée est **Scrum**. Elle a été retenue pour **trois raisons principales** : »
- Trois encarts avec icônes : **Déploiement modulaire** · **Adaptabilité aux évolutions des exigences** · **Détection précoce des risques**

### 2. Objectif
Montrer que le choix méthodologique est **argumenté**, pas subi. Les trois raisons affichées sont des bénéfices concrets, pas des généralités sur l'agilité.

### 🎤 3. Ce que je dois dire à l'oral

> « Avant de choisir un cadre de travail, j'ai distingué deux niveaux : la **méthodologie**, qui définit les principes généraux et la philosophie du développement, et le **cadre**, qui fournit une structure concrète pour les mettre en œuvre à travers des rôles, des pratiques et des règles.
>
> Au niveau de la méthodologie, j'ai comparé quatre approches — Waterfall, RUP, 2TUP et Agile — et retenu **l'approche Agile**, en raison de la possibilité d'évolution des besoins en cours de développement et de la nécessité de validations régulières.
>
> Au niveau du cadre, j'ai comparé **Scrum, Kanban et Extreme Programming**, et retenu **Scrum**, pour son organisation en sprints et son backlog priorisé.
>
> Trois raisons ont pesé. Le **déploiement modulaire** : chaque sprint livre un ensemble cohérent de fonctionnalités, ce qui a permis de livrer les six releases progressivement. L'**adaptabilité aux évolutions des exigences** : le périmètre a effectivement évolué en cours de projet — l'assistant IA et l'informatique décisionnelle ont été ajoutés après les quatre modules métier. Et la **détection précoce des risques** : les revues de fin de sprint ont permis de confronter régulièrement le résultat aux attentes, plutôt que de découvrir un écart en fin de parcours.
>
> Concrètement, le projet a été découpé en **dix sprints de deux semaines**, regroupés en **six releases**. »

*(≈ 1 min 20.)*

### 🔧 4. Explication détaillée

#### a) La comparaison des méthodologies (rapport, tableau 1.3)

| Méthodologie | Avantages | Inconvénients |
|---|---|---|
| **Waterfall** | Phases claires et structurées · planification simple | **Difficulté à s'adapter aux changements** · retour d'information tardif |
| **RUP** | Forte traçabilité · orienté gestion des risques | Gestion complexe · **charge documentaire importante** |
| **2TUP** | Itératif · prise en compte des risques | **Moins flexible** quand les besoins évoluent · très orienté processus |
| **Agile** ✅ | **Itératif et adaptatif** · retour continu · **bonne adaptation aux changements** | Nécessite une communication régulière · dépend de l'implication des parties prenantes |

> **Justification du rapport :** *« L'approche Agile a été retenue en raison de la possibilité d'évolution des besoins au cours du développement et de la nécessité d'effectuer des validations régulières. »*

#### b) La comparaison des cadres Agile (rapport, tableau 1.4)

| Cadre | Avantages | Inconvénients |
|---|---|---|
| **Scrum** ✅ | Organisation en **sprints** · **backlog** priorisé · suivi régulier · **rôles clairement définis** | Nécessite organisation et communication régulières · moins adapté si les tâches sont imprévisibles |
| **Kanban** | Gestion visuelle · grande flexibilité · limitation du travail en cours | **Moins structuré** · **ne définit ni rôles ni sprints fixes** |
| **XP** | Forte orientation qualité du code · tests fréquents · intégration continue | Nécessite une **forte implication** de l'équipe · pratiques difficiles à appliquer en grande équipe |

> **Justification du rapport :** *« Son organisation en sprints permet de découper le développement en plusieurs périodes successives et de livrer progressivement les fonctionnalités. L'utilisation d'un backlog facilite la gestion et la priorisation des User Stories. »*

#### c) Les trois rôles Scrum — **à maîtriser absolument**

| Rôle | Définition (rapport §1.5.3) | Qui, chez Antigone |
|---|---|---|
| **Product Owner** | « Définit les besoins, en priorise les fonctionnalités et maximise la valeur du produit » | **Malek Naouar** |
| **Scrum Master** | « Facilitateur : suit la bonne application de la méthode, anime les réunions, encourage les bonnes pratiques » | **Ahmed Kouki** |
| **Équipe de développement** | « Profils techniques, responsables de la réalisation des tâches planifiées, de l'analyse à la livraison » | **Yosr Kheriji, Zeineb Haj Hsine** |

🚨 **C'est le point le plus sensible de cette diapositive.** Voir l'avertissement A en tête de document : **vous étiez deux développeuses.** Préparez impérativement la réponse à « qu'avez-vous fait, vous ? ».

### 📌 5. À retenir
- La distinction **méthodologie** (principes) vs **cadre** (structure concrète).
- Agile retenue contre **Waterfall, RUP, 2TUP**.
- Scrum retenu contre **Kanban, XP**.
- Les **3 raisons** : déploiement modulaire · adaptabilité · détection précoce des risques.
- Les **3 rôles** et **qui les tenait** — PO Malek Naouar, SM Ahmed Kouki, dev Yosr Kheriji + Zeineb Haj Hsine.
- **10 sprints de 2 semaines · 6 releases · 254 points.**

### ❓ Questions du jury

**Q1 — « Quelle est la différence entre une méthodologie et un cadre de développement ? »**
> Voir §4 — c'est la première phrase de votre section 1.5. « Une méthodologie définit les principes généraux et la philosophie qui orientent le processus ; un cadre fournit une structure concrète pour les mettre en œuvre à travers des rôles, des pratiques et des règles clairement définis. » **Question très probable puisque vous l'affichez.**

**Q2 — « Pourquoi Scrum plutôt que Kanban ? »**
> « Parce que Kanban est **moins structuré** : il ne définit ni rôles, ni sprints fixes. Il est excellent pour un flux continu de tâches imprévisibles — un support, une maintenance — mais mon projet avait un backlog connu à l'avance et un besoin de livraisons jalonnées. Scrum apportait trois choses que Kanban n'apporte pas : l'organisation en sprints, qui a permis de livrer six releases progressivement ; un backlog priorisé, qui a permis d'arbitrer ce qui passait en premier ; et des rôles clairement définis, ce qui comptait puisque nous étions cinq personnes impliquées, avec un Product Owner et un Scrum Master côté agence. »

**Q3 — « Pourquoi pas XP ? »**
> « XP est très orienté qualité de code — tests fréquents, intégration continue, programmation en binôme. Ce sont de bonnes pratiques, mais XP exige une **forte implication continue de toute l'équipe** sur des rituels techniques très exigeants, et certaines de ses pratiques sont difficiles à tenir. Scrum m'apportait la structure de pilotage dont j'avais besoin. Cela dit, rien n'empêche d'emprunter à XP : l'intégration continue via GitHub et les tests automatisés relèvent de cet esprit. »

**Q4 — « Pourquoi pas le cycle en V ou Waterfall ? »**
> « Parce que le périmètre a effectivement évolué en cours de projet. Le backlog initial ne contenait ni l'assistant IA au niveau de maturité finalement atteint, ni l'informatique décisionnelle — ces deux releases ont été ajoutées une fois les modules métier stabilisés. Avec une planification en cascade, cet ajout aurait imposé de rouvrir toute la phase de spécification. Le rapport résume la limite de Waterfall en deux points : difficulté à s'adapter aux changements, et retour d'information tardif dans le processus. »

**Q5 — « Qui était le Product Owner et quel était son rôle réel ? »**
> « Monsieur Malek Naouar, le directeur de l'agence. Son rôle était de définir les besoins, de prioriser les fonctionnalités et de maximiser la valeur du produit. Concrètement, c'est lui qui arbitrait ce qui passait dans le sprint suivant, et qui validait le résultat en revue de fin de sprint. Avoir le directeur comme PO a été un avantage : les décisions de périmètre étaient prises rapidement, sans remontée hiérarchique. »

**Q6 — « Vous étiez deux développeuses. Comment vous répartissiez-vous le travail ? »**
> 🚨 **Question quasi certaine — préparez-la vous-même**, cette information n'est pas dans le rapport. Structurez votre réponse par modules ou par couches, et soyez précise sur ce que **vous** avez porté.

**Q7 — « Combien de temps duraient vos sprints et que faisiez-vous en fin de sprint ? »**
> « Deux semaines. Chaque sprint se refermait par une **revue** avec l'encadrement, permettant de confronter le résultat livré aux attentes, et par une **rétrospective** destinée à ajuster le rythme du sprint suivant. C'est ce qui correspond à la troisième raison affichée : la détection précoce des risques. »

**Q8 — « Comment estimiez-vous vos user stories ? »**
> « À deux niveaux. Dans le **backlog produit**, chaque user story porte une complexité qualitative — Faible, Moyenne ou Élevée. Dans les **backlogs de sprint**, chaque user story est décomposée en tâches de développement, et chaque tâche estimée à un point. Une story de cinq tâches vaut donc cinq points. Les sprints vont de 16 à 34 points, pour un total de 254. »

### ⚠️ Questions pièges

**« Neuf ou dix sprints ? Votre rapport dit les deux. »**
> 🚨 **Question très probable — voir l'avertissement B.** « Vous avez raison, et c'est une erreur de mise à jour que je corrigerai. Le découpage réel est de **dix sprints**, comme le montrent les backlogs détaillés des chapitres 3 à 8 : Sprint 1 à Sprint 10. Le texte du chapitre 2 annonce neuf sprints et le tableau de synthèse comporte deux lignes libellées "Sprint 9" — la seconde correspond en réalité au Sprint 10, consacré à l'informatique décisionnelle. »

**« Dix sprints de deux semaines font vingt semaines, soit cinq mois. Votre stage en dure six. »**
> « Exact. Les vingt semaines de sprints ne couvrent pas les six mois complets : s'y ajoutent la phase de cadrage initiale — recueil des besoins, étude de l'existant, choix technologiques et mise en place de l'environnement — ainsi que la stabilisation finale et la rédaction du rapport. »

**« Les charges de vos sprints ne correspondent pas entre le chapitre 2 et les chapitres suivants. »**
> Voir l'avertissement B. **Reconnaissez immédiatement**, donnez les bons chiffres (23 · 34 · 17 · 29 · 33 · 28 · 18 · 24 · 32 · 16 = 254), et précisez que ceux qui font foi sont les backlogs détaillés.

**« Scrum avec seulement deux développeuses, est-ce vraiment pertinent ? »**
> « C'est une équipe réduite, mais les trois rôles Scrum étaient réellement tenus par des personnes distinctes — un Product Owner, un Scrum Master, et une équipe de développement — ce qui n'est pas toujours le cas dans un projet étudiant. Ce qui aurait été discutable, c'est de revendiquer Scrum en travaillant seule, avec les trois casquettes. Ici, les rituels avaient un sens réel : la revue confrontait le travail au Product Owner, et la rétrospective était animée par le Scrum Master. »

### 📖 Lien avec le rapport
§1.5 Étude des méthodologies · **tableau 1.3** (méthodologies) · **tableau 1.4** (cadres Agile) · §1.5.3 et **tableau 1.5** (rôles Scrum) · figure 1.2 (fonctionnement du cadre Scrum) · §2.3 Pilotage du projet avec SCRUM · **tableau 2.9** (backlog produit) · **tableau 2.10** (planification des sprints ⚠️).

### 🔗 Liens avec les autres diapositives
Les six releases structurent les **diapositives 25 à 29**. Le backlog produit détaille les besoins de la **diapositive 12**.

### ➡️ Transition
> « Voyons maintenant sur quels besoins concrets ce backlog a été construit. »

---

## Diapositive 10 — *Intercalaire : Analyse des besoins*

Titre seul.

### 🎤 Ce que je dois dire
> « J'en viens à l'analyse des besoins. »

*(3 secondes.)*

---

## Diapositive 11 — Les acteurs du système

### 1. Ce que contient la diapositive

**Acteurs principaux (humains)** — six encarts avec icônes :

| Acteur | Description affichée |
|---|---|
| **Administrateur** | Configure, supervise les 4 modules, arbitre en dernier recours |
| **Employé** | Dépose ses demandes, pointe ses horaires, suit ses tâches |
| **Validateur** | Intervient dans le circuit d'approbation des demandes |
| **Chef de projet** | Pilote projets, équipes, tâches et plans médias |
| **Responsable Finance** | Facturation, charges, paie, déclarations CNSS/TVA |
| **Client** | Suit ses projets, ses plans médias et ses livrables |

**Acteurs secondaires (systèmes externes)** — trois encarts :

| Acteur | Description affichée |
|---|---|
| **Agent de présence** | Recoupe le pointage (heartbeat) |
| **Google Drive** | Livrables & justificatifs |
| **Assistant IA** | Réponses en langage naturel |

### 2. Objectif
Montrer que la conception part des **utilisateurs**, pas des écrans. Et justifier par avance le découpage en interfaces distinctes : six profils aux besoins très différents ne partagent pas une interface unique.

### 🎤 3. Ce que je dois dire à l'oral

> « La plateforme met en interaction plusieurs acteurs. J'en ai identifié **six principaux, humains**.
>
> L'**administrateur** détient l'ensemble des permissions : il configure les référentiels, gère les comptes et les rôles, supervise les quatre modules et arbitre en dernier recours. L'**employé** est l'utilisateur du quotidien : il consulte son profil, dépose ses demandes, pointe ses horaires et suit ses tâches. Le **validateur** n'est pas un profil distinct — c'est **un employé investi d'un rôle de validation**, qui intervient dans le circuit d'approbation des demandes de son équipe. Le **chef de projet** pilote les projets, constitue les équipes, répartit les tâches et planifie les plans médias. Le **responsable Finance** accède au module financier — facturation, charges, paie, déclarations — et ce périmètre est **protégé par une permission dédiée, distincte des droits RH**. Enfin le **client** est le seul acteur externe à l'entreprise : il se connecte via un accès dédié pour suivre ses projets et récupérer ses livrables.
>
> À côté, **trois acteurs secondaires**, qui sont des systèmes. L'**agent de présence** est un petit programme installé sur le poste de l'employé, qui envoie des signaux de présence — des *heartbeats* — au serveur pour recouper automatiquement les pointages. **Google Drive** stocke et restitue les fichiers liés aux projets et aux clients. Et l'**assistant conversationnel** permet aux utilisateurs d'interagir en langage naturel avec la plateforme, **tout en respectant leurs droits d'accès**. »

*(≈ 1 min 20.)*

### 🔧 4. Explication détaillée

#### a) La définition UML à savoir donner
> **Concept général :** un **acteur**, au sens UML, représente un **rôle** joué par une entité externe — humaine ou non — qui interagit avec le système. Un acteur n'est pas une personne : une même personne peut incarner plusieurs acteurs.
>
> **Dans mon projet :** c'est exactement le cas du **Validateur**. Le rapport le définit comme *« un employé investi d'un rôle de validation »*. Ce n'est pas un compte distinct, c'est un employé dont le rôle porte la permission adéquate.

La distinction **principal / secondaire** : un acteur principal **initie** des actions et attend un résultat ; un acteur secondaire **soutient** le système sans le piloter.

#### b) L'agent de présence — l'acteur qui suscite le plus de questions

- **Nature :** une application de bureau **Electron**, installée sur le poste de chaque employé, s'exécutant **en arrière-plan** (§2.2.2.1).
- **Ce qu'elle fait :** « détection automatique de la connexion au réseau de l'entreprise et envoi d'un signal de présence au serveur, **sans intervention manuelle de l'utilisateur** ».
- **Son rôle dans le système :** *recouper* le pointage déclaré — il ne le remplace pas.

#### c) Pourquoi l'assistant IA est-il un acteur ?
Le rapport le classe en acteur **secondaire** : *« composant intelligent permettant aux utilisateurs d'interagir en langage naturel avec la plateforme pour obtenir des informations contextualisées et accéder à différentes fonctionnalités RH, projets et finance, tout en respectant leurs droits d'accès »*. Il **soutient** le système sans le piloter — il ne déclenche jamais d'action métier de sa propre initiative.

### ⚠️ 5. Un écart à connaître

**Le service de messagerie (Brevo) n'apparaît pas parmi les acteurs secondaires** de la diapositive 11 ni de la liste du §2.1.1. **Pourtant il figure explicitement dans vos diagrammes de cas d'utilisation** (diapositives 13 et 17) sous le libellé « Service de messagerie `<<système>>` », et l'architecture physique (diapositive 22) le mentionne sous le nom **Brevo**.

**Si le jury le relève :** *« Vous avez raison, le service de messagerie transactionnelle — Brevo — est un acteur secondaire à part entière : il achemine les e-mails de création de compte et de réinitialisation de mot de passe. Il apparaît dans mes diagrammes de cas d'utilisation et dans mon architecture physique, mais il a été omis de la liste de synthèse. C'est une omission que je corrigerai. »*

### 📌 6. À retenir
- **6 acteurs principaux** humains, **3 secondaires** (+ Brevo, omis).
- Un acteur = un **rôle**, pas une personne.
- **Validateur = un employé investi d'un rôle de validation**, pas un profil distinct.
- **Client = seul acteur externe** à l'entreprise.
- Le périmètre Finance est **protégé par une permission dédiée, distincte des droits RH**.
- Agent de présence = application **Electron**, en arrière-plan, **sans intervention de l'utilisateur**.

### ❓ Questions du jury

**Q1 — « Différence entre un acteur et un rôle dans votre système ? »**
> Voir §4a. Insistez sur l'exemple du Validateur.

**Q2 — « Un employé peut-il être à la fois chef de projet et validateur ? »**
> « Oui, et c'est le cas normal. Un compte porte plusieurs rôles, et chaque rôle porte plusieurs permissions — c'est une relation plusieurs-à-plusieurs dans mon modèle de données. Le système ne raisonne jamais sur "quel est le rôle de cette personne" mais sur "cette personne a-t-elle cette permission". C'est ce qui rend le modèle combinable. »

**Q3 — « Comment le client se connecte-t-il ? Même mécanisme que les employés ? »**
> « Non, et c'est délibéré. C'est un **accès dédié, distinct des comptes internes** — c'est même un besoin fonctionnel explicite du Portail Client dans mon chapitre 2. Le cloisonnement est un besoin non fonctionnel : "séparation stricte entre l'espace client et l'espace interne". Un client n'a pas de compte employé et ne peut donc jamais atteindre un endpoint interne. »

**Q4 — « L'agent de présence ne pose-t-il pas un problème de vie privée ? »**
> « C'est une question légitime. Ce que l'agent remonte est volontairement limité : la **détection de la connexion au réseau de l'entreprise** et un signal de présence. Il ne capture ni le contenu de l'écran, ni les applications utilisées, ni les frappes clavier. Et son usage est borné : toute déduction sur salaire issue d'un rapport d'inactivité passe par une **validation explicite de l'administrateur** — le système propose, il ne décide pas. Cela dit, je n'ai pas traité la conformité réglementaire du traitement — information des salariés, base légale, durée de conservation — et c'est un préalable que je signalerais avant tout déploiement réel. »

**Q5 — « Que se passe-t-il si l'agent n'est pas installé ? »**
> « Le système continue de fonctionner en mode dégradé. L'agent **recoupe** le pointage, il n'en est pas la condition. Sans lui, l'indicateur d'activité n'est pas calculé, mais le statut de présence reste déterminé par la cascade de décision que je présente en diapositive 27. »

**Q6 — « Pourquoi Electron pour l'agent et pas une application native ? »**
> « Electron permet de développer une application de bureau en **JavaScript**, la même famille de langages que les interfaces web du projet. Pour une équipe réduite, cela évite d'introduire une quatrième technologie et un quatrième écosystème d'outils. Le rapport cite d'ailleurs JavaScript/Electron comme langage à part entière dans le tableau des technologies. »

**Q7 — « L'assistant IA respecte les droits d'accès : comment ? »**
> « Chaque outil métier exposé au modèle **vérifie les droits de l'utilisateur avant d'effectuer une opération** — c'est un rôle explicite de ma couche d'action. Et ces appels sont enregistrés pour assurer leur traçabilité. Le contrôle d'accès n'est donc jamais confié au prompt : il reste du code Java, exécuté avant toute lecture de données. »

### ⚠️ Questions pièges

**« Six acteurs, mais combien d'interfaces ? »**
> « **Quatre interfaces clientes** : trois applications web React — `frontend-rh`, `frontend-projects` et `frontend-finance` — plus l'application de bureau AgentDesktop. La répartition n'est pas un pour un : `frontend-rh` sert l'employé, le validateur et l'administrateur ; `frontend-projects` sert le chef de projet ; `frontend-finance` sert le responsable Finance **et porte le portail client**. »
> ⚠️ **Attention, votre rapport contient ici une ambiguïté** : le §2.2.2.1 écrit que les trois interfaces sont destinées « aux responsables RH, aux chefs de projet et employés, ainsi qu'aux responsables finance, **cette dernière portant également le portail dédié aux clients externes** ». Vérifiez dans votre code où se trouve réellement le portail client et soyez cohérente.

**« Un client peut-il voir les données d'un autre client ? »**
> « Non. C'est le tout premier besoin non fonctionnel de mon tableau, sous Sécurité / Confidentialité : *"Un client ne doit jamais pouvoir accéder aux données d'un autre client."* Le cloisonnement est appliqué côté serveur, par filtrage sur l'identifiant du client authentifié — jamais par masquage d'interface. »

### 📖 Lien avec le rapport
**§2.1.1 Identification des acteurs** (les six principaux et les trois secondaires, repris quasi mot pour mot sur la diapositive).

### 🔗 Liens avec les autres diapositives
Les six acteurs sont déclinés un par un en **diapositives 13 à 18**. L'agent de présence est développé en **diapositive 27**. L'assistant IA en **29**.

### ➡️ Transition
> « À partir de ces acteurs, j'ai recensé les besoins fonctionnels, regroupés par module. »

---

## Diapositive 12 — Besoins fonctionnels : vue par module

### 1. Ce que contient la diapositive
Six encarts avec icônes, trois puces chacun :

| Module | Puces affichées |
|---|---|
| **Authentification & Admin** | Connexion sécurisée (JWT) · Rôles & permissions (RBAC) · Gestion des comptes utilisateurs |
| **Ressources Humaines** | 12 types de demandes de congé · Pointage & agent de présence · Circuit de validation multi-niveaux |
| **Projets & Plans médias** | Projets, équipes, tâches Kanban · Plan média & double validation · Réservation de créneaux de tournage |
| **Portail Client** | Accès dédié, distinct des comptes internes · Suivi de projets & plans médias · Espace Drive de livrables |
| **Finance** | Facturation & devis, encaissements · Charges, dettes, relances · Paie tunisienne, CNSS, TVA |
| **Transverse** | Référentiels paramétrables · Tableaux de bord par rôle · Assistant conversationnel (chatbot) |

### 2. Objectif
Donner le **périmètre fonctionnel complet** en une image, en montrant qu'il est structuré et non accumulé. Le découpage en six encarts reflète le découpage réel de la plateforme et celui du backlog.

### 🎤 3. Ce que je dois dire à l'oral

> « J'ai regroupé les besoins fonctionnels par module, ce qui reflète la manière dont la plateforme est elle-même découpée. Dans mon rapport, chaque fonctionnalité est rattachée à **l'acteur qui la déclenche**, pour garder le lien direct avec l'identification précédente.
>
> Je ne vais pas les énumérer tous, mais souligner ceux qui ont demandé le plus de conception.
>
> Côté **RH** : **douze types de demandes de congé**, chacun avec ses règles propres, et un circuit de validation multi-niveaux.
>
> Côté **Projets** : le plan média avec sa **double validation** — manager interne puis client — et la réservation de créneaux de tournage.
>
> Côté **Finance** : la chaîne complète, de la facture jusqu'aux déclarations sociales et fiscales tunisiennes.
>
> Et en **transverse** : des référentiels entièrement paramétrables, pour que l'agence puisse faire évoluer ses listes de valeurs sans qu'une ligne de code change. »

*(≈ 1 minute. **Ne lisez pas les dix-huit puces.**)*

### 🔧 4. Explication détaillée

#### a) Les 12 types de congé
**Apprenez-les.** C'est une question à faible coût pour le jury et à fort effet.

**Moyen mnémotechnique — trois familles :**
- **Classiques (3)** : payé · sans solde · exceptionnel
- **Médicaux & familiaux (6)** : maladie · maternité · paternité · règles · décès d'un proche · décès en famille
- **Professionnels (3)** : formation · récupération · administratif

Chaque type porte des règles propres, **revérifiées côté serveur** à la soumission : plafonds de jours, justificatif obligatoire, restriction de genre pour certains types, délai de prévenance, contrôle de chevauchement, contrôle de solde.

#### b) Le calcul intelligent des jours
Le rapport parle de « calcul automatique des jours ». Le principe : le décompte n'est pas une simple différence de dates. Il combine **l'horaire affecté à l'employé** (quels jours de la semaine sont travaillés) et **le calendrier d'entreprise** (jours fériés et jours spéciaux).

#### c) Les référentiels paramétrables — le besoin d'évolutivité
> **Concept général :** plutôt que de coder en dur les listes de valeurs (départements, postes, types de contrat, formats et plateformes de plan média), on les stocke en base dans une table générique typée.
>
> **Dans mon projet :** l'entité `Référentiel` porte un **type**, un **libellé** et une **valeur** (visible sur le diagramme de classe, diapositive 23). L'administrateur peut créer, modifier ou **désactiver** une valeur sans redéploiement.
>
> **Le besoin non fonctionnel correspondant** (tableau 2.7, Maintenabilité / Évolutivité) : *« Référentiels paramétrables plutôt que valeurs codées en dur, afin d'absorber l'évolution des besoins métier sans refonte du code. »*

💡 **Nuance honnête à donner** si on creuse : le référentiel rend la **valeur** paramétrable, pas la **règle métier** qui s'y attache. Ajouter un type de congé est immédiat ; lui donner un plafond spécifique demande du code.

### 📌 5. À retenir
- **6 modules** : Auth & Admin · RH · Projets & Plans médias · Portail Client · Finance · Transverse.
- **12 types de congé** (les trois familles mnémotechniques).
- **Double validation** du plan média : manager interne, puis client.
- **Référentiels paramétrables** = évolutivité sans redéploiement.
- Chaque besoin est rattaché dans le rapport à **l'acteur qui le déclenche**.

### ❓ Questions du jury

**Q1 — « Citez-moi les douze types de congé. »**
> Les trois familles ci-dessus. 💡 **Apprenez-les par cœur** : ne pas savoir énumérer ce que votre propre diapositive annonce est très pénalisant.

**Q2 — « Qu'est-ce qu'un circuit de validation multi-niveaux ? »**
> « C'est un circuit où plusieurs validateurs interviennent successivement sur la même demande, chacun à un rang donné. La règle est asymétrique : la demande n'est approuvée que lorsque **toutes** les étapes le sont, mais **un seul refus suffit à la refuser immédiatement** — on n'attend pas les étapes suivantes, cela n'aurait pas de sens de solliciter un troisième validateur sur une demande déjà refusée. Et chaque décision est historisée avec son auteur et sa date. »

**Q3 — « Comment ajouter un nouveau type de congé ? Faut-il redéployer ? »**
> Voir §4c, avec la nuance. **Excellente occasion de montrer que vous distinguez ce qui est réellement paramétrable.**

**Q4 — « "Double validation" : qui valide quoi ? »**
> « Deux circuits distincts et successifs. Premier niveau, **interne** : un manager approuve ou désapprouve la ligne de plan média ; une ligne désapprouvée peut être corrigée et renvoyée. Second niveau, **client** : le social media manager soumet la ligne à validation, et le client l'approuve ou la refuse depuis son portail. Ce n'est qu'après ces deux étapes que la ligne produit ses effets. »

**Q5 — « Pourquoi un module "Transverse" ? »**
> « Parce que trois besoins ne relèvent d'aucun domaine métier en particulier mais les servent tous : les **référentiels**, qui alimentent les listes de valeurs de tous les modules ; les **tableaux de bord par rôle**, qui agrègent selon le profil de l'utilisateur ; et l'**assistant conversationnel**, accessible depuis toutes les applications. Les regrouper évite de les dupliquer dans chaque module. »

**Q6 — « "Tableaux de bord par rôle" — qu'est-ce que cela change concrètement ? »**
> « Que le tableau de bord affiché dépend des permissions du compte connecté, pas d'un choix de l'utilisateur. Un administrateur voit le pilotage global — effectifs, présence, demandes en cours ; un responsable Finance voit les indicateurs financiers ; un chef de projet voit l'avancement de ses projets. C'est le besoin fonctionnel transverse "Consulter un tableau de bord adapté à son rôle", rattaché à tous les utilisateurs internes. »

**Q7 — « Le portail client est-il un module ou une application ? »**
> « C'est un **espace applicatif** avec son propre mécanisme d'authentification, distinct des comptes internes. Fonctionnellement, il constitue un module à part entière dans mon analyse des besoins — avec ses quatre fonctionnalités propres : connexion dédiée, consultation de l'avancement des projets, consultation des plans médias, et accès à l'espace Google Drive. Techniquement, il est hébergé dans l'une des applications React plutôt que dans une quatrième application. »

### ⚠️ Questions pièges

**« Avez-vous réellement implémenté les douze types, ou seulement la liste ? »**
> « Les douze sont implémentés avec leurs règles propres : plafonds de jours différents selon le type, justificatif obligatoire pour certains, restriction de genre pour d'autres, et pour le congé payé un contrôle de solde, un contrôle de chevauchement et un délai de prévenance. Le calcul du nombre de jours décomptés tient compte de l'horaire de l'employé et des jours fériés du calendrier d'entreprise. »

**« Ce périmètre est très large. N'auriez-vous pas dû en faire moins mais mieux ? »**
> « C'est un arbitrage assumé, et il découle de la problématique : le problème de l'agence n'était pas qu'un domaine soit mal outillé, c'est qu'aucun ne communiquait avec les autres. Faire un seul module excellent aurait laissé la double saisie intacte. Cela dit, je reconnais que la profondeur n'est pas uniforme — les trois modules métier représentent huit sprints sur dix, l'assistant et le décisionnel un chacun. »

### 📖 Lien avec le rapport
**§2.1.2 Spécification des besoins** · tableaux **2.1** (Auth & Admin), **2.2** (RH), **2.3** (Projets & Plans médias), **2.4** (Portail Client), **2.5** (Finance), **2.6** (Transverse) · **tableau 2.9** (backlog produit, modules M1 à M24).

### 🔗 Liens avec les autres diapositives
Les six modules se retrouvent dans les cas d'utilisation des **diapositives 13 à 18** et dans les réalisations des **diapositives 25 à 29**.

### ➡️ Transition
> « Ces besoins ont été formalisés en diagrammes de cas d'utilisation, un par acteur. »

---
## 🎯 Préambule aux diapositives 13 à 18 — les six diagrammes de cas d'utilisation

Ces six diapositives forment un bloc. **Ne les traitez pas comme six exposés indépendants** : annoncez le bloc, puis parcourez-le en accélérant.

### 🎤 Phrase d'annonce (à dire avant la diapositive 13)
> « J'ai formalisé les besoins par un diagramme de cas d'utilisation **par acteur**, plutôt qu'un diagramme unique qui serait devenu illisible. Je vais les parcourir rapidement, en m'arrêtant sur les relations les plus significatives. Un point général d'abord : **tous les cas d'utilisation ont l'authentification pour pré-condition.** »

### ⚠️ Gestion du temps — le piège n°1 de votre présentation
Six diagrammes à 1 minute chacun, c'est **6 minutes** sur un exposé qui en compte peut-être 20. **C'est trop.** Répartition recommandée :

| Diapositive | Temps cible | Traitement |
|---|---|---|
| 13 — Employé | **60 s** | Détaillée : c'est l'acteur central |
| 14 — Validateur | **25 s** | Rapide : diagramme simple |
| 15 — Chef de projet | **50 s** | Détaillée : c'est le plus riche |
| 16 — Resp. Finance | **40 s** | Moyenne |
| 17 — Administrateur | **40 s** | Moyenne : insister sur l'héritage |
| 18 — Client | **25 s** | Rapide : insister sur la lecture seule |
| | **≈ 4 min** | |

### 📚 Rappel UML à maîtriser — vous l'utilisez massivement

> **`include`** : le cas inclus est **toujours** exécuté. Dépendance obligatoire, souvent une factorisation.
> **`extend`** : le cas étendant s'exécute **sous condition**, à un point d'extension. Comportement optionnel.
> **Généralisation d'acteur** : un acteur **hérite** de tous les cas d'utilisation d'un autre et en porte de nouveaux.

**Vos exemples réels, à savoir citer immédiatement :**

| Relation | Type | Diapo |
|---|---|---|
| S'authentifier et gérer son compte → Se connecter | `include` | 13 |
| S'authentifier et gérer son compte → Modifier ses informations personnelles | **`extend`** | 13 |
| Soumettre et suivre ses demandes RH → Soumettre une demande | `include` | 13 |
| Soumettre et suivre ses demandes RH → Annuler une demande | **`extend`** | 13 |
| Gérer le pointage → Pointer l'entrée / Pointer la sortie | `include` | 13 |
| Gérer le pointage → Consulter l'historique de pointage | **`extend`** | 13 |
| Suivre ses tâches → Mettre à jour le statut d'une tâche | **`extend`** | 13 |
| Refuser une demande → **Saisir un motif de refus** | `include` | 14 |
| Gérer les Media Plans → Faire approuver un plan média | `include` | 15 |
| Gérer la facturation → Relancer un impayé | `include` | 16 |
| Gérer les comptes utilisateurs → Réinitialiser un mot de passe | `include` | 17 |
| **Validateur hérite de Employé** | généralisation | 14 |
| **Administrateur hérite de Validateur, Chef de Projet et Responsable Finance** | généralisation | 17 |

💡 **« Refuser une demande *include* Saisir un motif de refus » est votre meilleur exemple d'`include`** : il est obligatoire par règle métier (le motif de refus est requis), pas par commodité technique.

---

## Diapositive 13 — Cas d'utilisation de l'Employé

### 1. Ce que contient la diapositive
Un diagramme centré sur l'acteur **Employé**, relié à **cinq paquets** de cas d'utilisation (un code couleur par paquet), avec **trois acteurs système** sur la droite.

| Paquet | Cas inclus / étendus |
|---|---|
| 🔵 **S'authentifier et gérer son compte** | `include` Se connecter · `include` Modifier le mot de passe · `extend` Modifier ses informations personnelles |
| 🟣 **Soumettre et suivre ses demandes RH** | `include` Soumettre une demande (congé / autorisation / télétravail) · `include` Consulter le statut d'une demande · `extend` Annuler une demande |
| 🟢 **Gérer le pointage (entrée / sortie)** | `include` Pointer l'entrée · `include` Pointer la sortie · `extend` Consulter l'historique de pointage |
| 🔴 **Obtenir une assistance RH avec l'IA** | `include` Poser une question · `include` Consulter les réponses suggérées · `extend` Consulter l'historique des échanges |
| 🟠 **Suivre ses tâches et media plans assignés** | `include` Consulter un Media Plan · `extend` Mettre à jour le statut d'une tâche |

**Trois acteurs système, avec leur annotation :**
- **Service de messagerie `<<système>>`** — « Envoi de l'email de login et de réinitialisation de mot de passe »
- **Agent de présence (Agent Desktop) `<<système>>`** — « Envoi du heartbeat réseau — recoupement automatique du pointage déclaré »
- **Assistant conversationnel (Chatbot IA) `<<système>>`**

### 2. Objectif
C'est **l'acteur central** : celui que tous les autres spécialisent. Le message est que l'employé a un périmètre complet et autonome — il ne dépend de personne pour ses actions quotidiennes.

### 🎤 3. Ce que je dois dire à l'oral

> « Voici le diagramme de l'**employé**, l'utilisateur du quotidien. Il s'organise en cinq groupes.
>
> Il **gère son compte** : il se connecte, modifie son mot de passe — le service de messagerie intervenant ici pour l'envoi des identifiants et la réinitialisation — et peut modifier ses informations personnelles.
>
> Il **soumet et suit ses demandes RH** : congé, autorisation ou télétravail, consulte leur statut, et peut les annuler tant qu'elles n'ont pas été traitées.
>
> Il **gère son pointage**, entrée et sortie, et consulte son historique. Vous noterez ici l'acteur **Agent de présence** : il envoie un heartbeat réseau qui permet le recoupement automatique du pointage déclaré.
>
> Il peut **obtenir une assistance RH avec l'IA** : poser une question, consulter les réponses suggérées, et retrouver l'historique de ses échanges.
>
> Enfin il **suit ses tâches et les media plans qui lui sont assignés**, et met à jour le statut de ses tâches. »

*(≈ 60 secondes.)*

### 🔧 4. Points techniques

**La distinction `include` / `extend` sur ce diagramme est pédagogique :**
- « Soumettre une demande » et « Consulter le statut » sont en **`include`** : on ne peut pas gérer ses demandes sans faire l'un ou l'autre.
- « Annuler une demande » est en **`extend`** : c'est conditionnel — on n'annule que si l'on a changé d'avis, et seulement tant que la demande est en attente.

**Le trio d'acteurs système** montre que l'employé interagit avec trois automatismes différents : un pour la communication (messagerie), un pour la mesure (agent), un pour l'assistance (chatbot).

### 📌 5. À retenir
- **5 paquets** : compte · demandes RH · pointage · assistance IA · tâches et media plans.
- **3 acteurs système** : messagerie, agent de présence, chatbot.
- `include` = obligatoire · `extend` = conditionnel — et vous avez un exemple de chaque dans **chaque** paquet.
- **Tous les cas d'utilisation ont l'authentification pour pré-condition.**

### ❓ Questions du jury

**Q1 — « Pourquoi "Annuler une demande" est-il en `extend` et pas en `include` ? »**
> « Parce que l'annulation est **conditionnelle** : elle ne se produit que si l'employé change d'avis, et uniquement tant que la demande n'a pas été traitée. On peut parfaitement gérer ses demandes sans jamais en annuler une. À l'inverse, "Soumettre une demande" est en `include` : sans elle, le cas parent n'a pas de sens. »

**Q2 — « L'employé pointe-t-il manuellement ou est-ce automatique ? »**
> « Les deux coexistent, et c'est le sens du lien avec l'agent de présence. L'employé peut pointer son entrée et sa sortie, et l'agent envoie en parallèle un heartbeat réseau qui permet le **recoupement automatique du pointage déclaré**. L'agent ne remplace pas le pointage, il le fiabilise. »

**Q3 — « Que peut faire l'employé avec l'assistant IA ? »**
> « Trois choses sur ce diagramme : poser une question, consulter les réponses suggérées, et consulter l'historique de ses échanges. Concrètement, dans le périmètre RH, il peut interroger le règlement intérieur et obtenir l'explication de son bulletin de paie. »

**Q4 — « Un employé peut-il consulter le media plan d'un client dont il ne s'occupe pas ? »**
> « Non — le diagramme précise "media plans **assignés**". L'assignation employé/client est d'ailleurs une fonctionnalité à part entière du module Media Plan, gérée par l'administrateur. »

**Q5 — « Pourquoi le service de messagerie est-il relié à "Se connecter" ? »**
> « Parce qu'il intervient sur deux moments du cycle de vie du compte : l'**envoi de l'e-mail de login** à la création du compte, et la **réinitialisation de mot de passe**. C'est un acteur secondaire : il soutient le cas d'utilisation sans le déclencher. »

### ⚠️ Question piège
**« Votre acteur "Service de messagerie" n'apparaît pas dans votre liste d'acteurs secondaires. »**
> Voir diapositive 11, §5. **Reconnaissez l'omission** et précisez qu'il s'agit de Brevo, visible dans l'architecture physique.

### 📖 Lien avec le rapport
**§2.1.3.1** et **figure 2.1** — Cas d'utilisation de l'Employé.

### ➡️ Transition
> « Un employé peut par ailleurs être investi d'un rôle de validation. »

---

## Diapositive 14 — Cas d'utilisation de Validateur

### 1. Ce que contient la diapositive
Un diagramme simple centré sur **Responsable hiérarchique / Validateur** :

- Cas racine : **Gérer les demandes RH et validations**
  - `include` → **Consulter les demandes de son équipe**
  - `include` → **Valider une demande**
  - `include` → **Refuser une demande** → `include` → **Saisir un motif de refus**

**Une note UML attachée à l'acteur :**
> « Un employé investi d'un rôle de validation : **hérite de tous les cas d'utilisation Employé**. »

### 2. Objectif
Montrer deux choses en un diagramme minimal : la **généralisation d'acteur** (le validateur *est* un employé), et une **règle métier traduite en UML** (le motif de refus est obligatoire).

### 🎤 3. Ce que je dois dire à l'oral

> « Le **validateur** n'est pas un profil distinct : c'est un employé investi d'un rôle de validation, et la note du diagramme le précise — **il hérite de tous les cas d'utilisation de l'employé**.
>
> Son périmètre propre est volontairement réduit à trois actions : consulter les demandes de son équipe, en valider une, ou la refuser. Et vous noterez une relation `include` significative : **refuser une demande inclut obligatoirement la saisie d'un motif**. Ce n'est pas une option d'interface, c'est une règle métier — un refus sans justification n'est pas acceptable dans un circuit RH traçable. »

*(≈ 25 secondes.)*

### 🔧 4. Le point technique de ce diagramme

**Pourquoi le motif de refus est-il en `include` et non en `extend` ?**
> Parce qu'il est **toujours** exécuté quand on refuse. Si c'était optionnel, ce serait un `extend`. En le modélisant en `include`, je traduis en UML une contrainte métier : la traçabilité exige qu'un refus soit motivé. C'est cohérent avec le besoin non fonctionnel de traçabilité — *« chaque décision est enregistrée avec l'auteur et la date, afin de faciliter les contrôles ultérieurs »*.

**Pourquoi "Valider" n'a pas d'`include` équivalent ?**
> Parce qu'une approbation n'a pas besoin d'être justifiée — elle est l'issue attendue. L'asymétrie entre validation et refus est intentionnelle.

### 📌 5. À retenir
- **Généralisation** : le validateur hérite de tous les cas d'utilisation Employé.
- **`include` obligatoire : Refuser → Saisir un motif.**
- L'asymétrie valider / refuser est **voulue**.
- Le validateur ne voit que **les demandes de son équipe**.

### ❓ Questions du jury

**Q1 — « Comment un employé devient-il validateur ? »**
> « Par l'attribution d'une **permission** via le module de gestion des rôles. Ce n'est pas un compte séparé ni un rôle codé en dur : l'administrateur crée un rôle, lui associe la permission de validation, et l'attribue au compte concerné. C'est l'application directe du RBAC granulaire. »

**Q2 — « Le validateur voit-il toutes les demandes ? »**
> « Non, uniquement **celles de son équipe** — c'est écrit dans le cas d'utilisation. Le périmètre est déterminé par le rattachement hiérarchique manager/subordonné, qui fait partie de la fiche employé. »

**Q3 — « Pourquoi le motif de refus est-il obligatoire ? »**
> Voir §4. Reliez-le au besoin non fonctionnel de traçabilité.

**Q4 — « Et si le validateur ne traite pas la demande ? »**
> **Réponse honnête :** « Le diagramme ne modélise pas de mécanisme d'escalade ou de relance automatique du validateur, et je ne vais pas prétendre le contraire. La demande reste en attente et l'employé peut en consulter le statut à tout moment. Une escalade après un délai serait une évolution naturelle. »

**Q5 — « Un validateur peut-il valider sa propre demande ? »**
> ⚠️ **Information non explicitement documentée dans les fichiers fournis.** « Le diagramme précise qu'il traite les demandes **de son équipe**, ce qui exclut logiquement la sienne puisqu'il n'est pas son propre subordonné. Je préfère vérifier le comportement exact dans le code plutôt que de l'affirmer. » — 💡 **Vérifiez-le avant la soutenance**, c'est une question classique de séparation des pouvoirs.

### 📖 Lien avec le rapport
**§2.1.3.2** et **figure 2.2**.

### ➡️ Transition
> « Passons au chef de projet, dont le périmètre est nettement plus large. »

---

## Diapositive 15 — Cas d'utilisation de Chef de Projet

### 1. Ce que contient la diapositive
Le diagramme le plus riche de la série — **sept paquets** :

| Paquet | Cas inclus |
|---|---|
| 🔵 **Gérer les projets, tâches et équipes** | Créer et suivre un projet · Constituer et gérer l'équipe · Répartir et suivre les tâches (Kanban) |
| 🟣 **Gérer le calendrier de production** | Planifier une production · Assigner des ressources · **Réserver un créneau de tournage** |
| 🟢 **Gérer les Media Plans** | Construire un plan média · **Faire approuver un plan média (interne / client)** · Échanger via le fil de commentaires |
| 🔴 **Gérer les clients** | Ajouter un client · Modifier les informations client · Consulter l'historique client |
| 🟠 **Planifier des réunions de projet** | *(sans sous-cas)* |
| 🩵 **Suivre les compétences de l'équipe** | *(sans sous-cas)* |
| 🩷 **Consulter les rapports et tableaux de bord** | Générer un rapport de projet · Consulter les KPI · Exporter les données |

**Un acteur système :** **Google Drive `<<système>>`**, relié aux cas « Construire un plan média », « Faire approuver un plan média » et « Modifier les informations client ».

### 2. Objectif
Montrer l'ampleur du module Projets & Plans médias — le plus lourd du projet (**61 points**, Sprints 5 et 6). C'est la diapositive qui justifie que ce module soit un des trois piliers.

### 🎤 3. Ce que je dois dire à l'oral

> « Le **chef de projet** est l'acteur au périmètre le plus large. Son diagramme s'organise en sept groupes.
>
> Il **gère les projets, les tâches et les équipes** : il crée et suit un projet, constitue son équipe, et répartit les tâches sur un tableau **Kanban**.
>
> Il **gère le calendrier de production** : il planifie une production, assigne des ressources, et **réserve un créneau de tournage** — c'est le point d'entrée du workflow que je détaillerai en diapositive 25.
>
> Il **gère les Media Plans** : il les construit, les fait approuver — en interne puis côté client — et échange avec le client via un fil de commentaires.
>
> Il **gère les clients**, **planifie les réunions de projet**, **suit les compétences de son équipe**, et **consulte les rapports et tableaux de bord** : rapport de projet, KPI, export des données.
>
> Vous noterez l'acteur **Google Drive**, qui intervient sur trois cas — la construction et l'approbation d'un plan média, et la modification d'un client — puisque c'est lui qui stocke les livrables et les justificatifs. »

*(≈ 50 secondes.)*

### 🔧 4. Points techniques

**Pourquoi Google Drive est-il relié à « Modifier les informations client » ?**
> Parce que la fiche client porte des fichiers — logo, documents, dossier partagé. Le rapport le confirme : Google Drive « stocke et restitue les fichiers liés aux projets **et aux clients** (livrables, justificatifs, dossiers partagés) ».

**Deux paquets sans sous-cas** — « Planifier des réunions » et « Suivre les compétences » — sont des cas atomiques : ils ne se décomposent pas. C'est normal et cohérent, inutile de forcer une décomposition artificielle.

**Le chef de projet gère aussi les clients**, ce qui peut surprendre. C'est cohérent avec le métier : dans une agence, c'est le chef de projet qui est en relation avec le client, pas l'administrateur.

### 📌 5. À retenir
- **7 paquets**, le périmètre le plus large.
- « Réserver un créneau de tournage » = point d'entrée du **workflow de la diapositive 25**.
- **Google Drive** intervient sur les plans médias **et** les clients.
- Le chef de projet **gère les clients** — logique métier d'agence.
- Kanban = **À faire / En cours / Terminé**.

### ❓ Questions du jury

**Q1 — « Qu'est-ce qu'un Media Plan ? »**
> **Question très probable** — le jury peut ne pas connaître le terme. « C'est le **calendrier éditorial** d'un client : la planification des contenus qui seront publiés pour lui sur une période donnée. Chaque ligne porte un contenu, un **format**, une **plateforme** de publication et une **date**. Pour une agence de communication, c'est le livrable central : c'est ce que le client valide chaque mois avant que la production ne démarre. »

**Q2 — « Pourquoi le chef de projet gère-t-il les clients et pas l'administrateur ? »**
> « Parce que c'est lui qui est en relation avec le client au quotidien — c'est le fonctionnement réel d'une agence. L'administrateur, lui, conserve la supervision globale : il hérite d'ailleurs des cas d'utilisation du chef de projet, comme le montre la diapositive 17. »

**Q3 — « Qu'est-ce qu'un KPI dans votre contexte ? »**
> « Un *Key Performance Indicator*, un indicateur clé de performance. Pour un projet, il s'agit typiquement du taux de complétion des tâches, du nombre de tâches en retard, ou des délais de réalisation. Ces indicateurs sont consultables dans les tableaux de bord, et sont repris de façon historique dans la partie décisionnelle. »

**Q4 — « Qu'est-ce qu'un tableau Kanban ? »**
> « Une représentation visuelle de l'avancement où chaque tâche est une carte, et chaque colonne un statut : **À faire, En cours, Terminé**. Le chef de projet répartit les tâches, les assigne avec une date limite et un niveau d'urgence ; l'employé fait avancer les siennes en changeant leur statut. C'est directement inspiré du cadre Kanban que j'ai étudié dans ma comparaison des cadres Agile — même si, comme cadre de pilotage de projet, j'ai retenu Scrum. »

**Q5 — « "Assigner des ressources" — quelles ressources ? »**
> ⚠️ *Le rapport ne détaille pas ce cas d'utilisation.* « Dans le contexte du calendrier de production, il s'agit d'affecter les personnes nécessaires à une production planifiée. Le suivi des compétences de l'équipe, qui figure sur le même diagramme, sert précisément à savoir qui mobiliser. »

**Q6 — « L'export de données, dans quel format ? »**
> ⚠️ **Information non explicitement documentée dans les fichiers fournis.** 💡 **Vérifiez dans votre code.**

### ⚠️ Question piège
**« Ce diagramme est très chargé. Un chef de projet fait-il vraiment tout cela ? »**
> « Le diagramme présente le **périmètre maximal** du rôle, pas la charge quotidienne d'une personne. Et il reflète la réalité d'une agence de cette taille : le chef de projet y porte à la fois la relation client, le pilotage de la production et le suivi de son équipe. C'est précisément parce que ces responsabilités étaient réparties entre plusieurs outils non reliés qu'elles paraissaient moins nombreuses auparavant. »

### 📖 Lien avec le rapport
**§2.1.3.3** et **figure 2.3**.

### ➡️ Transition
> « Le responsable finance, lui, a un périmètre plus étroit mais plus technique. »

---

## Diapositive 16 — Cas d'utilisation de Responsable Finance

### 1. Ce que contient la diapositive
Quatre paquets :

| Paquet | Cas inclus |
|---|---|
| 🔵 **Gérer la facturation et les devis** | Créer un devis · Générer une facture · Suivre les paiements · Relancer un impayé |
| 🟣 **Gérer la paie, charges et dettes** | Générer les bulletins de paie · Suivre les charges de l'agence · Suivre les dettes et échéanciers |
| 🟢 **Gérer les déclarations CNSS et TVA** | Vérifier les cotisations · **Soumettre la déclaration CNSS trimestrielle** · **Consulter la déclaration TVA mensuelle** |
| 🟠 **Consulter le tableau de bord financier** | Consulter les indicateurs financiers · Filtrer par période · Exporter un rapport financier |

### 2. Objectif
Montrer que le module Finance couvre **toute la chaîne**, de la vente (devis) jusqu'aux obligations légales (déclarations). C'est le module qui comble le trou identifié en diapositive 7.

### 🎤 3. Ce que je dois dire à l'oral

> « Le **responsable Finance** couvre quatre domaines.
>
> La **facturation et les devis** : il crée un devis, génère une facture, suit les paiements — y compris partiels — et relance les impayés.
>
> La **paie, les charges et les dettes** : il génère les bulletins de paie, suit les charges de l'agence et ses dettes avec leurs échéanciers.
>
> Les **déclarations CNSS et TVA** : il vérifie les cotisations, soumet la déclaration CNSS trimestrielle et consulte la déclaration TVA mensuelle.
>
> Et le **tableau de bord financier** : indicateurs, filtrage par période, export de rapport.
>
> Un point important : ce périmètre est **protégé par une permission dédiée, distincte des droits RH**. Un responsable RH n'accède pas à la finance, et inversement. »

*(≈ 40 secondes.)*

### 🔧 4. Points techniques — les sigles à savoir expliquer

| Sigle | Signification | Rôle dans le projet |
|---|---|---|
| **CNSS** | Caisse Nationale de Sécurité Sociale (Tunisie) | Cotisation **salariale** (retenue sur le salaire) et **patronale** (à la charge de l'employeur). Déclaration **trimestrielle**, avec calcul des **pénalités de retard** |
| **IRPP** | Impôt sur le Revenu des Personnes Physiques | Retenue fiscale sur salaire, calculée sur un **barème progressif versionné** |
| **TVA** | Taxe sur la Valeur Ajoutée | Déclaration **mensuelle**, calcul de la TVA nette |
| **TFP** | Taxe de Formation Professionnelle | Charge patronale |
| **FOPROLOS** | Fonds de Promotion du Logement pour les Salariés | Charge patronale |

💡 **Apprenez au moins CNSS, IRPP et TVA** — le jury tunisien les connaît, et ne pas savoir les développer serait gênant.

**La différence trimestriel / mensuel est significative :** la CNSS est déclarée par trimestre, la TVA par mois. Ce n'est pas un choix technique, c'est la réglementation.

### 📌 5. À retenir
- **4 paquets** : facturation & devis · paie, charges & dettes · déclarations CNSS & TVA · tableau de bord.
- **CNSS = trimestrielle** · **TVA = mensuelle**.
- Les cinq sigles : CNSS, IRPP, TVA, TFP, FOPROLOS.
- Périmètre **protégé par une permission dédiée, distincte des droits RH**.
- Paiements **partiels** suivis.

### ❓ Questions du jury

**Q1 — « Que signifient CNSS et IRPP ? »**
> Voir le tableau §4. **Question quasi certaine devant un jury tunisien.**

**Q2 — « Quelle est la différence entre un devis et une facture ? »**
> « Un **devis** est une proposition commerciale : il n'engage pas comptablement et n'est pas un document fiscal. Une **facture** constate une prestation réalisée et déclenche l'obligation de paiement. Dans mon système, les deux sont gérés par le même module mais avec une numérotation et un traitement distincts. »

**Q3 — « Comment suivez-vous un paiement partiel ? »**
> « Chaque paiement est enregistré individuellement et vient s'imputer sur le montant dû. Le statut de la facture est **recalculé automatiquement** à chaque mouvement : impayée tant que rien n'est reçu, partiellement payée dès qu'un montant est reçu, payée quand le cumul atteint le total. C'est le même mécanisme de recalcul de statut que j'applique aux bulletins de paie. »

**Q4 — « Qu'est-ce qu'une pénalité de retard CNSS ? »**
> « Une majoration appliquée lorsque la déclaration trimestrielle est soumise après l'échéance légale. Mon système la **calcule automatiquement** et l'intègre au montant suggéré, pour que le comptable connaisse le montant réel à régler. »

**Q5 — « Le responsable Finance voit-il les salaires de tout le monde ? »**
> « Oui, c'est nécessaire à sa fonction : il génère les bulletins de paie de l'ensemble des employés. En revanche, ce périmètre est strictement cloisonné vis-à-vis des autres profils — c'est un besoin non fonctionnel explicite : "un employé ne doit pas pouvoir consulter le module Finance sans permission explicite". »

**Q6 — « Qui peut consulter le tableau de bord financier ? »**
> « Le responsable Finance **et l'administrateur** — c'est précisé dans le tableau des besoins fonctionnels du module Finance. L'administrateur hérite d'ailleurs des cas d'utilisation du responsable Finance, comme le montre la note de la diapositive 17. »

### ⚠️ Question piège
**« Votre moteur de paie est-il conforme au droit tunisien ? Qui l'a validé ? »**
> **Réponse honnête et nuancée :** « Le moteur reproduit la mécanique de calcul de la paie tunisienne — CNSS salariale et patronale, IRPP progressif, contribution de solidarité, TFP, FOPROLOS. Les taux ne sont pas codés en dur : ils sont **paramétrables**, et le barème IRPP est **versionné**, ce qui permet de recalculer un mois ancien avec les taux en vigueur à l'époque. En revanche, je n'ai pas fait valider le calcul par un expert-comptable dans le cadre de ce projet, et je ne prétendrai pas à une conformité juridique certifiée. Ce que je peux affirmer, c'est que le calcul est déterministe, paramétrable et traçable. »

### 📖 Lien avec le rapport
**§2.1.3.4** et **figure 2.4** · chapitre 6 (Release 4).

### ➡️ Transition
> « L'administrateur, lui, supervise l'ensemble. »

---

## Diapositive 17 — Cas d'utilisation de l'Administrateur

### 1. Ce que contient la diapositive
**Sept paquets**, et surtout une **note UML capitale** :

> « **Hérite via Validateur, Chef de Projet et Responsable Finance de tous les cas d'utilisation des panneaux précédents.** »

| Paquet | Cas inclus |
|---|---|
| 🔵 **Gérer les employés et l'organigramme** | Ajouter un employé · Modifier une fiche employé · Archiver un employé · Consulter l'organigramme |
| 🟣 **Configurer le calendrier et les horaires** | Configurer le calendrier d'entreprise (jours fériés) · Configurer les horaires de travail · **Gérer les restrictions de dates de congé** |
| 🟢 **Gérer les comptes utilisateurs** | Créer un compte · Modifier / activer / désactiver un compte · Réinitialiser un mot de passe |
| 🔴 **Gérer les rôles et permissions** | Créer un rôle · Modifier un rôle · **Attribuer des permissions** |
| 🟠 **Paramétrer les référentiels** | Créer / modifier une valeur de référentiel · **Désactiver** une valeur de référentiel |
| 🩵 **Suivre la présence et l'activité** | Consulter le rapport de présence · Détecter les absences · Générer des statistiques |
| 🩷 **Superviser l'activité de l'agence** | Consulter le tableau de bord global · Consulter les indicateurs par projet · Consulter les alertes |

**Un acteur système :** **Service de messagerie `<<système>>`**, relié à « Gérer les comptes utilisateurs ».

### 2. Objectif
Montrer la **généralisation d'acteurs** — le concept UML le plus valorisant de cette série — et la portée de la supervision.

### 🎤 3. Ce que je dois dire à l'oral

> « L'**administrateur** détient l'ensemble des permissions. Le point le plus important de ce diagramme est la note : **il hérite, via le Validateur, le Chef de Projet et le Responsable Finance, de tous les cas d'utilisation des panneaux précédents.** C'est une relation de généralisation d'acteurs : plutôt que de redessiner tous leurs cas, je les hérite.
>
> Son périmètre propre couvre sept domaines. Il **gère les employés et l'organigramme** — ajout, modification, archivage. Il **configure le calendrier et les horaires**, y compris les restrictions de dates de congé. Il **gère les comptes utilisateurs** — création, activation, désactivation, réinitialisation de mot de passe, le service de messagerie intervenant pour l'envoi des identifiants. Il **gère les rôles et les permissions**. Il **paramètre les référentiels**, avec la possibilité de désactiver une valeur plutôt que de la supprimer. Il **suit la présence et l'activité**. Et il **supervise l'activité de l'agence** via un tableau de bord global, des indicateurs par projet et des alertes. »

*(≈ 40 secondes.)*

### 🔧 4. Points techniques

#### a) La généralisation d'acteurs — **votre meilleur point UML**
> **Concept général :** un acteur peut **hériter** d'un autre. L'acteur enfant dispose de tous les cas d'utilisation du parent, plus les siens propres.
>
> **Dans mon projet :** l'Administrateur hérite de **trois** acteurs simultanément — Validateur, Chef de Projet et Responsable Finance. Et comme le Validateur hérite lui-même de l'Employé, l'Administrateur hérite transitivement de l'Employé aussi.
>
> **Le bénéfice :** sans cette relation, il faudrait redessiner sur le diagramme de l'administrateur tous les cas des quatre autres acteurs — le diagramme deviendrait illisible et, surtout, toute modification devrait être répercutée à deux endroits.

#### b) « Désactiver » plutôt que « supprimer »
Le paquet Référentiels propose explicitement **« Désactiver une valeur de référentiel »**. C'est un choix de conception à défendre :
> « Désactiver plutôt que supprimer **préserve l'historique** : les anciennes lignes qui référencent une valeur désactivée restent lisibles. Supprimer un type de congé rendrait illisibles toutes les demandes passées qui l'utilisaient. On retrouve la même logique côté employés avec l'**archivage**. »

#### c) « Gérer les restrictions de dates de congé »
Cas d'utilisation spécifique à connaître : l'administrateur peut interdire la pose de congés sur certaines périodes — typiquement une période de forte charge pour l'agence.

### 📌 5. À retenir
- **La note de généralisation** : hérite de Validateur + Chef de Projet + Responsable Finance (et transitivement d'Employé).
- **7 paquets** propres.
- **Désactiver ≠ supprimer** → préservation de l'historique (référentiels) · **archiver** (employés).
- « Gérer les restrictions de dates de congé » — cas propre à l'administrateur.

### ❓ Questions du jury

**Q1 — « Expliquez la relation de généralisation entre vos acteurs. »**
> Voir §4a. **Excellente question à espérer** — c'est le concept UML le plus valorisant de votre série.

**Q2 — « L'administrateur peut-il déposer une demande de congé ? »**
> « Oui. Il hérite du Validateur, qui hérite lui-même de l'Employé — donc transitivement de tous les cas d'utilisation de l'employé, dont le dépôt de demande. C'est la traduction UML du fait qu'un administrateur reste un employé de l'agence. »

**Q3 — « Pourquoi archiver un employé au lieu de le supprimer ? »**
> « Pour préserver l'historique. Supprimer un employé ferait disparaître ses demandes, ses pointages et ses bulletins de paie — or ce sont des données que l'agence doit pouvoir consulter après son départ, notamment pour des raisons comptables et légales. L'archivage coupe l'accès sans détruire la donnée, et il est réversible. »

**Q4 — « Qu'est-ce qu'une "restriction de dates de congé" ? »**
> « C'est la possibilité pour l'administrateur d'interdire la pose de congés sur certaines périodes — typiquement une période de forte activité pour l'agence, ou une fermeture annuelle. Cela évite d'avoir à refuser les demandes une par une après coup. »

**Q5 — « Comment attribuez-vous les permissions ? »**
> « Par le RBAC. L'administrateur crée un **rôle**, lui **attribue des permissions**, puis associe ce rôle à un ou plusieurs comptes. Le système ne teste jamais "cet utilisateur est-il administrateur" mais "cet utilisateur a-t-il cette permission" — ce qui rend le modèle combinable et évolutif sans modification de code. »

**Q6 — « Que contient le tableau de bord global ? »**
> « Trois types d'information selon le diagramme : le tableau de bord global lui-même, les indicateurs par projet, et les alertes. Il agrège les signaux des différents modules pour donner à l'administrateur une vue de supervision. »

### ⚠️ Question piège
**« L'administrateur peut tout faire. N'est-ce pas un risque de sécurité ? »**
> « C'est un risque réel et connu, celui du super-utilisateur. Deux éléments l'atténuent dans mon système. D'abord la **traçabilité** : chaque décision est enregistrée avec son auteur et sa date, et chaque connexion est journalisée avec son adresse IP — une action d'administrateur est donc auditable. Ensuite, le modèle RBAC permet de **ne pas donner toutes les permissions** à un compte : le rôle "Administrateur" est un rôle comme un autre, composé de permissions. On pourrait parfaitement créer des rôles d'administration partielle. Cela dit, je reconnais que le principe du moindre privilège pourrait être appliqué plus finement. »

### 📖 Lien avec le rapport
**§2.1.3.5** et **figure 2.5**.

### ➡️ Transition
> « Reste le seul acteur externe : le client. »

---

## Diapositive 18 — Cas d'utilisation de Client

### 1. Ce que contient la diapositive
Un diagramme volontairement **plat** — pas de paquets, pas de relations `include` ou `extend` — avec cinq cas reliés directement à l'acteur **Client** :

- 🔵 Se connecter au portail client
- 🟣 Consulter l'avancement de ses projets
- 🟢 Consulter ses media plans
- 🔴 Commenter un plan media
- 🟡 Accéder à son espace Google Drive dédié

**Un acteur système :** **Google Drive `<<système>>`**, relié au dernier cas.

### 2. Objectif
Montrer par **la forme même du diagramme** que le périmètre client est restreint : cinq cas, aucune décomposition, aucune action destructrice.

### 🎤 3. Ce que je dois dire à l'oral

> « Le **client** est le seul acteur externe à l'entreprise, et son diagramme est volontairement le plus simple de la série : cinq cas d'utilisation, sans décomposition.
>
> Il se **connecte au portail client** via un accès dédié, distinct des comptes internes. Il **consulte l'avancement de ses projets** et **ses media plans**. Il peut **commenter un plan média** — c'est sa seule action d'écriture, en dehors de l'approbation. Et il **accède à son espace Google Drive dédié** pour récupérer ses livrables.
>
> La simplicité de ce diagramme est un message en soi : l'accès client est **volontairement limité à la consultation et à l'approbation**. Il ne crée rien, ne modifie rien, ne supprime rien. »

*(≈ 25 secondes.)*

### 🔧 4. Points techniques

**Le cloisonnement, à trois niveaux :**
1. **Authentification séparée** — un accès dédié, distinct des comptes internes.
2. **Périmètre de données** — « **ses** projets », « **ses** media plans », « **son** espace Drive » : chaque cas est filtré sur le client authentifié.
3. **Nature des actions** — consultation, commentaire, approbation. Aucune action destructrice.

**Le besoin non fonctionnel correspondant :** *« Un client ne doit jamais pouvoir accéder aux données d'un autre client »* (Sécurité / Confidentialité) et *« séparation stricte entre l'espace client et l'espace interne »* (Sécurité / Contrôle d'accès).

### ⚠️ 5. Un écart à connaître

**Le diagramme ne montre pas le cas « Approuver / refuser un plan média »**, alors que cette fonctionnalité est centrale dans votre solution — elle figure sur la diapositive 8 (« Un portail dédié pour chaque client »), sur la diapositive 25 (« Puis le client (portail) » dans la double validation) et dans votre backlog produit (M12.7 : *« En tant que client, je souhaite approuver ou désapprouver un media plan »*).

**Si le jury le relève :** *« Vous avez raison, l'approbation côté client devrait figurer explicitement sur ce diagramme — elle est présente dans mon backlog produit sous la référence M12.7 et elle est au cœur du workflow de double validation. Le diagramme montre la consultation et le commentaire, mais l'action de validation aurait mérité d'y apparaître. »*

💡 **Mieux : ajoutez ce cas au diagramme avant la soutenance.** C'est une correction de cinq minutes qui supprime une question embarrassante sur votre fonctionnalité phare.

### 📌 6. À retenir
- **5 cas, aucune décomposition** — la simplicité est le message.
- Accès **dédié, distinct des comptes internes**.
- Filtrage sur **« ses »** projets / media plans / espace Drive.
- Seule action d'écriture visible : **commenter**.
- ⚠️ **L'approbation côté client manque sur le diagramme** — à corriger.

### ❓ Questions du jury

**Q1 — « Le client peut-il approuver un plan média ? »**
> Voir §5. **Reconnaissez l'omission du diagramme** et confirmez que la fonctionnalité existe (M12.7 du backlog).

**Q2 — « Comment le client se connecte-t-il ? »**
> « Par un **accès dédié, distinct des comptes internes** — c'est un besoin fonctionnel explicite du module Portail Client. Ses identifiants ne sont pas gérés comme ceux des employés, et il n'existe pas dans la table des comptes internes. »

**Q3 — « Le client peut-il voir les tâches internes d'un projet ? »**
> « Non. Il consulte **l'avancement** de ses projets, pas leur détail opérationnel. La répartition des tâches entre les collaborateurs de l'agence relève du chef de projet et n'a pas vocation à être exposée au client. »

**Q4 — « L'espace Google Drive est-il en lecture seule ? »**
> « C'est un espace **dédié** au client, où il récupère ses livrables. Le rapport parle d'"accès direct aux fichiers partagés via Google Drive" dans un cadre "volontairement limité à la consultation et à l'approbation". »
> ⚠️ *Le niveau exact de droit Drive côté client n'est pas détaillé dans le rapport* — **vérifiez-le dans votre code** avant de l'affirmer.

**Q5 — « Pourquoi le client peut-il commenter mais pas modifier ? »**
> « Parce que le commentaire est un **retour**, pas une modification. Dans le fonctionnement d'une agence, le client exprime des demandes de correction, mais c'est l'agence qui les applique — elle reste responsable du contenu produit. Techniquement, cela garde aussi une trace claire de qui a demandé quoi, dans le fil de commentaires. »

### ⚠️ Question piège
**« Si un client devine l'URL d'un autre client, que se passe-t-il ? »**
> « Il est bloqué côté serveur. Le filtrage se fait sur l'identité authentifiée, pas sur le paramètre d'URL : les données retournées sont celles du client connecté, quel que soit l'identifiant demandé. C'est le principe que j'applique dans tout le projet — le frontend collecte la saisie, le backend décide. Une restriction qui ne reposerait que sur l'interface ne serait pas une sécurité. »

### 📖 Lien avec le rapport
**§2.1.3.6** et **figure 2.6** · §1.4.4 Portail Client.

### ➡️ Transition
> « Au-delà de ce que le système doit faire, il fallait définir comment il devait le faire. »

---

## Diapositive 19 — Besoins non fonctionnels

### 1. Ce que contient la diapositive
Quatre cercles avec icônes, sans texte explicatif :
**Sécurité** · **Performance** · **Compatibilité** · **Fiabilité**

### 2. Objectif
Poser les quatre piliers de qualité. ⚠️ **La diapositive étant purement visuelle, tout le contenu repose sur votre oral** — c'est la diapositive où le décalage entre ce qui est projeté et ce que vous devez dire est le plus grand. **Préparez-la soigneusement.**

### 🎤 3. Ce que je dois dire à l'oral

> « Les besoins non fonctionnels définissent les contraintes et les qualités attendues du système. Mon rapport les organise en sept caractéristiques, déclinées en sous-caractéristiques ; j'en retiens ici les quatre principales.
>
> La **sécurité** est la plus travaillée, avec quatre dimensions. La **confidentialité** : un client ne doit jamais pouvoir accéder aux données d'un autre client, et un employé ne doit pas consulter le module Finance sans permission explicite. L'**authentification** : connexion par jeton JWT, mots de passe hachés avec BCrypt, et changement obligatoire du mot de passe à la première connexion. Le **contrôle d'accès** : des autorisations fines par permission — du RBAC — plutôt qu'un simple rôle binaire. Et la **traçabilité** : chaque décision est enregistrée avec son auteur et sa date.
>
> La **fiabilité** couvre la disponibilité et la tolérance aux fautes : une demande, une fois soumise, **ne peut pas se perdre** ; chaque changement de statut est historisé et reste consultable même en cas d'erreur ultérieure.
>
> La **performance** porte sur deux points : les listes doivent rester fluides même avec un volume croissant, et surtout **les calculs sensibles — solde de congé, jours ouvrés, bulletin de paie — s'exécutent côté serveur**, pour garantir un résultat unique quel que soit le frontend utilisé.
>
> Enfin la **compatibilité** : le backend fournit **une API REST unique** utilisée par les trois interfaces et permettant de communiquer avec les services externes. »

*(≈ 1 min 10.)*

### 🔧 4. Le tableau complet du rapport (tableau 2.7)

**La diapositive montre 4 piliers ; le rapport en documente 7 caractéristiques et 14 sous-caractéristiques.** Connaissez-les — le jury peut demander celles qui ne sont pas affichées.

| Caractéristique | Sous-caractéristique | Traduction technique |
|---|---|---|
| **Sécurité** | Confidentialité | Cloisonnement client ↔ client ; Finance derrière permission explicite |
| **Sécurité** | Authentification | JWT · BCrypt · changement obligatoire à la première connexion |
| **Sécurité** | Contrôle d'accès | RBAC par permission ; séparation stricte espace client / espace interne |
| **Sécurité** | Traçabilité / Auditabilité | Chaque décision enregistrée avec auteur et date |
| **Fiabilité** | Disponibilité | Disponible en permanence pour le dépôt et le traitement des demandes |
| **Fiabilité** | Tolérance aux fautes | Une demande soumise ne se perd pas ; chaque statut historisé |
| **Performance** | Comportement temporel | Listes fluides malgré un volume croissant |
| **Performance** | Utilisation des ressources | **Calculs sensibles côté serveur** → résultat unique quel que soit le frontend |
| **Utilisabilité** | Ergonomie | Vocabulaire métier compréhensible par des non-techniciens |
| **Utilisabilité** | Accessibilité | Interfaces responsives, poste fixe et mobile |
| **Compatibilité** | Interopérabilité | API REST unique pour les 3 interfaces + services externes |
| **Portabilité** | Adaptabilité | React/TypeScript, tout navigateur récent, indépendamment de l'OS |
| **Maintenabilité** | Modularité | Architecture en couches (contrôleur / service / repository) |
| **Maintenabilité** | Évolutivité | Référentiels paramétrables plutôt que valeurs codées en dur |

⚠️ **Trois caractéristiques du rapport n'apparaissent pas sur la diapositive :** Utilisabilité, Portabilité et Maintenabilité. Si le jury demande « et la maintenabilité ? », répondez sans hésiter — elle est bien traitée dans le rapport (modularité + évolutivité).

### 🚨 5. Le piège absolu de cette diapositive : la performance

**Le rapport ne contient AUCUNE métrique de performance mesurée.** Pas de temps de réponse, pas de test de charge, pas de nombre d'utilisateurs simultanés. La formulation est qualitative : *« Les listes doivent rester fluides même en cas de volume de données croissant. »*

**🚫 N'annoncez JAMAIS un temps de réponse chiffré.** La question suivante serait « comment l'avez-vous mesuré ? » et vous n'auriez pas de réponse.

**La formulation à utiliser si le jury insiste :**
> « Je n'ai pas réalisé de campagne de mesure de performance ni de test de charge dans le cadre de ce projet, donc je préfère ne pas avancer de chiffre non vérifié. Ce que je peux décrire, ce sont les **dispositions de conception** prises : les calculs sensibles ne sont jamais rejoués côté client, les montants d'un bulletin sont **figés** à la génération plutôt que recalculés à chaque lecture, et surtout l'analyse historique ne touche plus les tables de saisie — elle lit un **entrepôt dénormalisé** rechargé une fois par nuit. »

💡 **Cette réponse est excellente** : elle refuse d'inventer tout en démontrant que la performance a été *pensée* au niveau de la conception.

### 📌 6. À retenir
- **4 piliers affichés** : Sécurité · Performance · Compatibilité · Fiabilité.
- **7 caractéristiques / 14 sous-caractéristiques** dans le rapport.
- **Aucune métrique de performance mesurée** — ne jamais chiffrer.
- **« Les calculs sensibles s'exécutent côté serveur »** — la phrase la plus importante.
- **« Une demande soumise ne peut pas se perdre »** — la promesse de fiabilité.
- JWT · BCrypt · RBAC · traçabilité horodatée.

### ❓ Questions du jury

**Q1 — « Quel temps de réponse garantissez-vous ? »**
> Voir §5. **Aucun chiffre.**

**Q2 — « Pourquoi les calculs sensibles côté serveur ? Donnez un exemple. »**
> « Pour deux raisons. La **cohérence** : trois interfaces différentes ne doivent pas pouvoir produire trois résultats différents pour le même calcul — c'est exactement la formulation de mon besoin non fonctionnel, "un résultat unique et cohérent, quel que soit le frontend utilisé". Et la **sécurité** : un calcul effectué côté client est modifiable par l'utilisateur.
>
> L'exemple le plus parlant est le calcul du nombre de jours d'un congé : il combine l'horaire affecté à l'employé et le calendrier d'entreprise. Si je le faisais côté client, un utilisateur pourrait manipuler le décompte. Le même principe vaut pour le solde de congé et pour le bulletin de paie. »

**Q3 — « Comment garantissez-vous qu'une demande ne se perd pas ? »**
> « Par la persistance immédiate et l'historisation systématique. La demande est écrite en base au moment de la soumission, et **tout** changement de statut donne lieu à un enregistrement horodaté avec son auteur. Le rapport le formule ainsi : "chaque changement de statut est historisé et reste consultable **même en cas d'erreur ultérieure**". On peut donc toujours reconstituer le parcours complet d'une demande. »

**Q4 — « Comment avez-vous testé la sécurité ? »**
> **Réponse honnête :** « Je n'ai pas mené d'audit de sécurité ni de test d'intrusion, et je ne vais pas prétendre le contraire. Ce que j'ai mis en place, ce sont des mécanismes de conception : le filtre JWT intercepte **toutes** les requêtes entrantes avant qu'elles n'atteignent les contrôleurs, et les annotations `@PreAuthorize` vérifient la permission requise. Le rapport est explicite : "ce filtrage garantit qu'aucune fonctionnalité métier n'est accessible sans authentification préalable". »

**Q5 — « Vos interfaces sont-elles réellement responsives ? »**
> « C'est un besoin déclaré sous Utilisabilité / Accessibilité, et les technologies choisies le supportent. Je n'ai pas mené de campagne de tests multi-appareils formalisée, donc je ne prétendrai pas à une compatibilité exhaustive sur toutes les tailles d'écran. »

**Q6 — « Qu'entendez-vous par "disponibilité en permanence" ? Avez-vous un SLA ? »**
> « Non, et c'est une limite que j'assume. La disponibilité est déclarée comme un **besoin** — le système doit rester accessible pour permettre le dépôt et le traitement des demandes sans interruption — mais je n'ai ni engagement de niveau de service, ni taux de disponibilité mesuré, ni architecture redondante. »

**Q7 — « Pourquoi ISO ou une autre norme n'apparaît-elle pas ? »**
> « Mon tableau est structuré selon des caractéristiques de qualité logicielle classiques — sécurité, fiabilité, performance, utilisabilité, compatibilité, portabilité, maintenabilité — qui correspondent au modèle de qualité produit couramment utilisé. Je ne revendique pas une conformité formelle à une norme : c'est une grille d'analyse, qui m'a servi à ne pas oublier de dimension. »

### ⚠️ Questions pièges

**« Avez-vous mesuré la qualité de votre code ? Couverture de tests, dette technique ? »**
> **Réponse honnête :** « Je n'ai pas mis en place d'outil d'analyse statique, donc je n'ai ni taux de couverture ni indice de dette technique à vous donner, et je ne veux pas en inventer. Ce que je peux documenter, c'est que chaque user story de mes backlogs de sprint comporte une tâche "Tester la fonctionnalité", et que les services REST ont été testés via **Postman**, documentés avec **Swagger**. C'est du test manuel outillé, pas une couverture automatisée mesurée. »
>
> 💡 **Ne cédez jamais à la tentation d'annoncer un pourcentage de couverture.** C'est la question de suivi la plus facile à poser et la plus embarrassante à ne pas savoir répondre.

**« Votre sécurité repose sur JWT. Que se passe-t-il si un jeton est volé ? »**
> « Il reste utilisable jusqu'à son expiration. C'est la contrepartie assumée d'une authentification **sans état** : il n'y a pas de liste de révocation côté serveur. Les parades en place sont la durée de vie limitée du jeton et le transport chiffré. Une révocation immédiate demanderait de maintenir une liste noire côté serveur, ce qui réintroduirait de l'état et le compromis inverse. »

### 📖 Lien avec le rapport
**§2.1.2, Besoins non fonctionnels** · **tableau 2.7** (les 14 sous-caractéristiques).

### 🔗 Liens avec les autres diapositives
Les quatre piliers sont tenus par l'architecture de la **diapositive 21** (sécurité : filtre JWT + `@PreAuthorize` ; compatibilité : API REST unique) et par les mécanismes des **diapositives 26 et 27** (calculs côté serveur).

### ➡️ Transition
> « Ces exigences se traduisent maintenant en choix de conception. »

---
## Diapositive 20 — *Intercalaire : Conception*

Titre seul.

### 🎤 Ce que je dois dire
> « J'en viens à la conception. »

*(3 secondes.)*

---

## Diapositive 21 — Architecture logique

### 1. Ce que contient la diapositive
Un schéma en **trois tiers**, reliés par des flèches annotées.

**TIER 1 · COUCHE PRÉSENTATION** — quatre encarts :

| Composant | Port | Détail |
|---|---|---|
| `frontend-rh` | **3001** | SPA React · Axios (HTTP) |
| `frontend-projects` | **3000** | **Porte le login** · Axios (HTTP) |
| `frontend-finance` | **3002** | SPA React · Axios (HTTP) |
| `AgentDesktop` | **TRAY APP** | Heartbeat · Réseau local · **Electron** |

**Flèche → annotée : 🔒 HTTPS · REST (JSON)**

**TIER 2 · COUCHE MÉTIER** — empilement :
- 🍃 **Spring Boot | API REST — PORT : 8080**
- 🔒 **Middleware — Spring Security (Filtre JWT + Permissions `@PreAuthorize`)**
- ⚙️ **Contrôleurs REST & Services — API Endpoints**
- Quatre domaines en pied : **RH & Employés** (employés, congés, validations) · **Projets & Clients** (projets, tâches, media plan) · **Finance** (paie, factures, CNSS) · **Notifications & IA** (alertes, assistant RH virtuel) — chacun annoté *Hibernate · JDBC*

**Flèche → annotée : JDBC · OAuth2 / REST**

**TIER 3 · COUCHE DONNÉES** — deux encarts :
- 🐘 **PostgreSQL — PORT : 5432** — Base `antigone_rh` (relationnelle)
- ☁️ **Google Drive API — OAUTH2** — Fichiers clients & justificatifs

### 2. Objectif
C'est **la diapositive d'architecture la plus importante**. Elle démontre que vous maîtrisez la séparation des responsabilités et que la sécurité est une couche transverse, pas un ajout.

### 🎤 3. Ce que je dois dire à l'oral

> « L'architecture retenue est une architecture **client–serveur à trois niveaux**, ou N-Tiers. Je l'ai choisie après avoir comparé trois approches — monolithique, N-Tiers et microservices — et j'y reviendrai si vous le souhaitez.
>
> Le **premier tier**, la couche présentation, regroupe **quatre interfaces clientes**. Trois applications web React : `frontend-rh` pour les employés et les congés, `frontend-projects` pour les projets et les media plans — c'est elle qui porte le login — et `frontend-finance` pour la facturation et la paie. Chacune communique avec l'API via **Axios**. S'y ajoute **AgentDesktop**, une application de bureau développée en **Electron**, qui s'exécute dans la barre système et envoie les heartbeats de présence en détectant le réseau local.
>
> Ces quatre clients dialoguent avec le **deuxième tier** en **HTTPS, en REST, au format JSON**.
>
> Le deuxième tier est la couche métier : une application **Spring Boot** exposant une **API REST** sur le port 8080. Le point que je veux souligner, c'est le **middleware de sécurité** : **Spring Security**, avec un **filtre JWT** qui intercepte l'ensemble des requêtes entrantes **avant qu'elles n'atteignent les contrôleurs**, et des annotations **`@PreAuthorize`** qui vérifient que l'utilisateur dispose bien de la permission requise. Ce filtrage garantit qu'**aucune fonctionnalité métier n'est accessible sans authentification préalable**.
>
> En dessous, les contrôleurs REST et les services métier, organisés en quatre domaines : RH et employés, projets et clients, finance, et notifications et IA.
>
> Le **troisième tier** est la couche données : **PostgreSQL**, sur le port 5432, base `antigone_rh`, accédée en **JDBC** via Hibernate. Et **l'API Google Drive**, accédée en **OAuth2**, utilisée en complément pour le stockage des fichiers volumineux — documents clients, justificatifs, visuels de media plans. »

*(≈ 1 min 50. **Prenez le temps.**)*

### 🔧 4. Explication détaillée

#### a) Pourquoi N-Tiers ? — la comparaison du rapport (tableau 2.8)

**Question quasi certaine. Voici les trois options et leur arbitrage :**

| Architecture | Avantages | Limites |
|---|---|---|
| **Monolithique** | Simplicité de développement et de déploiement · débogage facilité (processus unique) · pas de latence réseau interne · **cohérence transactionnelle native** | Évolutivité et maintenabilité limitées quand l'application grossit · un défaut dans un module peut affecter tout le système · **mise à l'échelle uniquement globale** |
| **Client–Serveur N-Tiers** ✅ | **Séparation claire des responsabilités** · meilleure maintenabilité · **plusieurs clients peuvent consommer la même couche métier** · sécurité centralisée au niveau du serveur · mise à l'échelle horizontale de la couche serveur | Configuration plus complexe qu'un monolithe simple · le serveur d'application reste une unité de déploiement unique |
| **Microservices** | Scalabilité fine service par service · résilience (isolation des pannes) · liberté technologique par service · déploiements indépendants | **Forte complexité opérationnelle** (orchestration, découverte de service, supervision distribuée) · latence réseau · **gestion difficile de la cohérence des données distribuées** · coût d'infrastructure et d'expertise DevOps élevé |

> **La justification exacte du rapport :** *« L'architecture client–serveur à trois niveaux a été retenue puisqu'**un serveur d'application centralisé** fournit les services à **plusieurs interfaces clientes** — `frontend-rh`, `frontend-projects`, `frontend-finance` et `AgentDesktop` — tout en utilisant une **base de données PostgreSQL commune**. Les microservices ont été écartés en raison de leur complexité, tandis que l'architecture monolithique répond moins efficacement au besoin de plusieurs clients distincts. »*

💡 **Le critère décisif, à énoncer clairement : vous avez QUATRE clients distincts.** C'est exactement le cas d'usage du N-Tiers — et c'est ce qui disqualifie le monolithe, qui suppose une interface unique.

#### b) Le rôle de chaque couche (§2.2.2.1)

| Couche | Composants | Responsabilité |
|---|---|---|
| **Présentation** | 3 SPA React (routeur React Router, composants réutilisables, service **Axios**) + AgentDesktop (Electron) | Interactions utilisateur |
| **Métier** | API Spring Boot structurée en **contrôleurs REST, services métier et repositories** | Traitement, logique métier, sécurisation, exposition des services |
| **Sécurité (middleware)** | **Spring Security + JWT**, filtre `JwtAuthenticationFilter`, annotations `@PreAuthorize` | Intercepte toute requête avant les contrôleurs |
| **Données** | **PostgreSQL** + **Google Drive API** (OAuth2) | Persistance |

#### c) Le trajet complet d'une requête — **à savoir dérouler**

```
1.  Utilisateur        → clic dans l'interface React
2.  Axios              → requête HTTPS REST/JSON vers le port 8080
                         avec le jeton JWT dans l'en-tête
3.  JwtAuthenticationFilter
                       → intercepte AVANT le contrôleur
                       → valide le jeton, reconstitue identité + permissions
4.  @PreAuthorize      → vérifie que l'utilisateur a la permission requise
5.  Contrôleur REST    → reçoit la requête, délègue (aucune règle métier)
6.  Service métier     → applique les règles de gestion et les calculs
7.  Repository (JPA)   → Hibernate traduit en SQL
8.  PostgreSQL (5432)  → persistance dans la base antigone_rh
   ↑ la réponse remonte : Service → Contrôleur → JSON → Axios → composant React
```

💡 **C'est la réponse à toute question du type « que se passe-t-il quand un utilisateur clique sur… ? ».** Elle se décline pour n'importe quelle fonctionnalité.

#### d) Pourquoi Google Drive en complément de PostgreSQL ?
> « PostgreSQL stocke les **données structurées** — employés, clients, projets, tâches, media plans, paie, factures. Google Drive prend en charge les **fichiers volumineux** : documents clients, justificatifs, visuels de media plans. Stocker des fichiers binaires en base alourdirait les sauvegardes et les requêtes, sans bénéfice. Et Drive apporte un avantage métier : le client accède directement à son dossier partagé. »

### 📌 5. À retenir
- **Architecture N-Tiers** (3 tiers), retenue contre monolithique et microservices.
- **Le critère décisif : quatre clients distincts** pour une couche métier unique.
- Ports : **3000** (projects, porte le login) · **3001** (rh) · **3002** (finance) · **8080** (backend) · **5432** (PostgreSQL).
- Base : **`antigone_rh`**.
- **Le filtre JWT intercepte AVANT les contrôleurs** · `@PreAuthorize` vérifie la permission.
- Communication : **HTTPS / REST / JSON** en haut · **JDBC** et **OAuth2** en bas.
- Les 4 domaines métier : RH & Employés · Projets & Clients · Finance · Notifications & IA.

### ❓ Questions du jury

**Q1 — « Pourquoi une architecture N-Tiers et pas des microservices ? »**
> Voir §4a. **Question la plus probable de cette diapositive.** Insistez sur : cohérence des données + complexité opérationnelle disproportionnée + quatre clients pour une même couche métier.

**Q2 — « Pourquoi pas un monolithe, plus simple ? »**
> « Parce qu'un monolithe suppose que présentation, logique métier et accès aux données soient regroupés en une seule unité — or j'ai **quatre interfaces clientes distinctes**, dont une application de bureau en Electron. Le rapport le formule ainsi : "l'architecture monolithique répond moins efficacement au besoin de plusieurs clients distincts". Le N-Tiers permet précisément à plusieurs clients de consommer la même couche métier. »

**Q3 — « Décrivez le trajet d'une requête, du clic à la base. »**
> Déroulez les 8 étapes du §4c. **Question très probable.**

**Q4 — « Comment la sécurité est-elle appliquée ? »**
> « À deux niveaux, tous deux côté serveur. D'abord un **filtre**, `JwtAuthenticationFilter`, qui intercepte **l'ensemble des requêtes entrantes avant qu'elles n'atteignent les contrôleurs** : il valide le jeton et reconstitue l'identité et les permissions. Ensuite des annotations **`@PreAuthorize`** sur les méthodes, qui vérifient que l'utilisateur dispose bien de la permission requise pour la ressource demandée. Le rapport conclut : "ce filtrage garantit qu'aucune fonctionnalité métier n'est accessible sans authentification préalable". »

**Q5 — « Pourquoi trois frontends et pas un seul ? »**
> « Parce que les profils ont des besoins disjoints : un employé qui dépose un congé n'a rien à faire dans les écrans de facturation, et un comptable n'a aucune raison de voir le tableau Kanban. Séparer les applications évite une interface fourre-tout où l'on masquerait des menus selon les droits. Et cela renforce le cloisonnement du module Finance, qui est servi par une application distincte. »

**Q6 — « Qu'est-ce qu'une SPA ? »**
> « *Single Page Application* : l'application charge une seule page HTML, et la navigation ainsi que les mises à jour de contenu se font côté client, sans rechargement complet. C'est React Router qui gère la navigation. L'avantage est la fluidité : chaque action ne recharge que les données nécessaires, via Axios. »

**Q7 — « Qu'est-ce qu'un middleware ? »**
> « Un composant qui s'intercale entre la requête entrante et son traitement, pour appliquer un traitement transverse. Ici, le middleware de sécurité — Spring Security avec le filtre JWT — s'applique à **toutes** les requêtes sans que chaque contrôleur ait à le redemander. C'est ce qui garantit qu'on ne peut pas oublier de sécuriser un endpoint par inadvertance. »

**Q8 — « Pourquoi OAuth2 pour Google Drive et JWT pour vos utilisateurs ? »**
> « Ce sont deux besoins différents. **JWT** authentifie **mes utilisateurs auprès de mon API** : c'est moi qui émets et signe le jeton. **OAuth2** est le protocole par lequel **mon application s'authentifie auprès d'un service tiers**, Google, pour agir sur son API. Je ne choisis pas OAuth2 : c'est le protocole imposé par Google. »

**Q9 — « Que contient le domaine "Notifications & IA" ? »**
> « Il regroupe deux fonctions transverses : les **alertes et notifications** envoyées aux utilisateurs lors des décisions sur leurs demandes, et l'**assistant RH virtuel**. Ce sont les deux composants qui ne relèvent d'aucun domaine métier particulier mais les servent tous. »

### ⚠️ Questions pièges

**« Votre backend unique n'est-il pas un point de défaillance unique ? »**
> « Si, et c'est la limite que le rapport reconnaît lui-même pour le N-Tiers : "le serveur d'application reste une unité de déploiement unique". C'est le compromis assumé face aux microservices, qui auraient apporté l'isolation des pannes mais au prix d'une complexité opérationnelle disproportionnée pour ce projet. Cela dit, le N-Tiers autorise la **mise à l'échelle horizontale de la couche serveur** — c'est même listé comme un de ses avantages — et l'authentification JWT étant sans état, plusieurs instances pourraient servir indifféremment n'importe quelle requête. »

**« Si un utilisateur de `frontend-rh` appelle directement un endpoint Finance, que se passe-t-il ? »**
> « Il est bloqué **côté serveur**. Le fait que l'appel vienne d'une autre application ne change rien : le filtre JWT reconstitue ses permissions, et l'annotation `@PreAuthorize` sur l'endpoint Finance rejette la requête s'il ne dispose pas de la permission requise. La séparation en trois applications est une séparation **d'usage**, pas une frontière de sécurité — la frontière de sécurité est dans le middleware. »
> 💡 **Réponse forte** : elle montre que vous ne confondez pas ergonomie et sécurité.

**« Pourquoi `frontend-projects` porte-t-il le login ? »**
> ⚠️ *Le rapport n'explique pas ce choix.* « C'est l'application qui sert de point d'entrée : le login y est centralisé, puis l'utilisateur est orienté selon ses permissions. Je préfère ne pas inventer une justification théorique — c'est un choix d'organisation du code que je peux détailler en vous montrant l'implémentation. »
> 💡 **Vérifiez ce point avant la soutenance** : c'est visible sur votre diapositive, donc questionnable.

### 📖 Lien avec le rapport
**§2.2.2 Architecture globale** · **tableau 2.8** (comparaison des trois architectures) · **§2.2.2.1** et **figure 2.8** (architecture logique).

### 🔗 Liens avec les autres diapositives
Les technologies sont détaillées en **diapositive 31**. Le déploiement en **22**. La sécurité annoncée ici tient les promesses de la **diapositive 19**.

### ➡️ Transition
> « Voyons maintenant comment ces composants sont concrètement déployés. »

---

## Diapositive 22 — Architecture physique

### 1. Ce que contient la diapositive
Un schéma de déploiement en **quatre zones**.

**① POSTE DE DÉVELOPPEMENT (Clients)**
- Navigateur Web (`frontend-projects`) — **localhost:3000**
- Navigateur Web (`frontend-rh`) — **localhost:3001**
- Navigateur Web (`frontend-finance`) — **localhost:3002**
- **AgentDesktop** (Electron, **mode dev non empaqueté**)

**② SERVEURS DE DÉVELOPPEMENT FRONTEND** — reliés par *HTTP (HMR)*
- **Vite** (port 3000) — `npm run dev:projects`
- **Vite** (port 3001) — `npm run dev:rh`
- **Vite** (port 3002) — `npm run dev:finance`
- **`@antigone/ai-chat-widget`** — *package npm workspace, **partagé par les 3 SPA*** (relié aux trois par *import workspace*)

**③ BACKEND (PROCESSUS LOCAL)** — relié par *HTTPS / REST (JSON) + JWT*
- **Application Spring Boot** — `./mvnw spring-boot:run` — **Port : 8080** — *(**PAS de conteneur Docker en local**)*
- Encart ℹ️ : « **Sans OPENAI_API_KEY valide, le backend démarre quand même : seuls les endpoints `/api/v1/**` (IA) répondent 503, le reste de l'application reste fonctionnel.** »

**④ BASE DE DONNÉES (LOCALE)** — reliée par *JDBC*
- **PostgreSQL** — localhost:5432 — Base : `antigone_rh`
- Encart ℹ️ : « Créée localement via `createdb antigone_rh`. **Même moteur (PostgreSQL) qu'en production — pas de base allégée (H2) utilisée en dev.** »

**⑤ SERVICES EXTERNES RÉELS** — *(même en local, pas de mock)*
- **OpenAI API** (chat + embeddings) · **Brevo** (envoi d'e-mails) · **Google Drive API**

**L'AgentDesktop appelle directement le backend** en *HTTPS / REST (JSON), appel direct*.

### 2. Objectif
Montrer le déploiement réel. ⚠️ **Point important : ce schéma décrit l'environnement de DÉVELOPPEMENT** (localhost, `npm run dev`, Vite, mode dev non empaqueté), pas un déploiement en production. **Soyez claire là-dessus** — c'est la première chose que le jury remarquera.

### 🎤 3. Ce que je dois dire à l'oral

> « Voici l'architecture physique, telle qu'elle a été mise en œuvre sur l'environnement de développement.
>
> Sur le **poste**, quatre clients : trois navigateurs pointant sur les ports 3000, 3001 et 3002, et AgentDesktop en Electron.
>
> Les trois applications frontend sont servies par **Vite**, le serveur de développement, avec rechargement à chaud. Un point notable : le widget de chat IA, **`@antigone/ai-chat-widget`**, est un **package npm de workspace partagé par les trois applications** — il n'est pas dupliqué, il est importé.
>
> Le **backend** est un processus Spring Boot local sur le port 8080, lancé par Maven. Deux précisions figurent volontairement sur ce schéma. D'abord, **il n'y a pas de conteneur Docker en local**. Ensuite, et c'est un choix de conception que je tiens à souligner : **sans clé API OpenAI valide, le backend démarre quand même** — seuls les endpoints de l'assistant IA répondent 503, et tout le reste de l'application reste pleinement fonctionnel. L'IA est un module **optionnel**, pas une dépendance bloquante.
>
> La **base de données** est un PostgreSQL local, base `antigone_rh`. Là aussi un choix assumé : **le même moteur qu'en production, pas de base allégée type H2 en développement** — pour que le comportement observé en développement soit celui de la production.
>
> Enfin, les **services externes sont réels, même en local** : OpenAI pour le chat et les embeddings, Brevo pour les e-mails, Google Drive pour les fichiers. Aucun n'est simulé. »

*(≈ 1 min 30.)*

### 🔧 4. Les trois choix de conception affichés — **vos meilleurs arguments**

Ce schéma porte trois décisions explicites. **Ce sont trois excellents points à défendre :**

| Choix | Pourquoi c'est un bon choix |
|---|---|
| **Sans clé API, le backend démarre quand même** | L'assistant IA est un module **optionnel et isolé**. Un défaut de configuration externe ne fait pas tomber toute l'application. Seuls les endpoints `/api/v1/**` répondent 503 — un code explicite, *Service Unavailable* — le reste reste opérationnel. |
| **Même moteur PostgreSQL en dev et en production** | Évite la classe de bugs la plus insidieuse : un comportement qui diffère entre une base allégée (H2) et la base réelle. Particulièrement critique ici, puisque le projet utilise des fonctionnalités **spécifiques à PostgreSQL** — notamment **pgvector** pour la recherche vectorielle, qu'H2 ne connaît pas. |
| **Services externes réels, pas de mock** | Ce qui est testé est le comportement réel, y compris les latences et les cas d'erreur. Un mock donnerait une fausse confiance. |

💡 **Le premier point est le plus valorisant** : il montre que vous avez pensé la **résilience** et le **couplage faible** avec un service tiers.

### ⚠️ 5. Le point à ne pas manquer : c'est un environnement de développement

**Assumez-le d'emblée plutôt que d'attendre la question.**

> « Ce schéma décrit l'environnement de développement — vous voyez les ports localhost, Vite et le mode dev non empaqueté de l'agent. Le déploiement en production n'est pas documenté dans mon rapport. »

**Si le jury demande « et en production ? » :**
> « L'architecture est conçue pour être déployable : le backend est une application Spring Boot autonome, les frontends sont des applications React qui se compilent en fichiers statiques, et la base est un PostgreSQL standard. Mais je n'ai pas réalisé de déploiement en production dans le cadre de ce projet, et je ne vais pas décrire une cible que je n'ai pas mise en œuvre. »

🚫 **N'inventez surtout pas un déploiement cloud, Docker ou CI/CD** s'il n'a pas eu lieu — c'est vérifiable en trois questions.

### 📌 6. À retenir
- **C'est l'environnement de développement** — assumez-le.
- Ports : **3000 / 3001 / 3002** (Vite) · **8080** (Spring Boot) · **5432** (PostgreSQL).
- **`@antigone/ai-chat-widget`** = package npm workspace **partagé par les 3 SPA**.
- **Sans clé OpenAI → 503 sur `/api/v1/**` seulement**, le reste fonctionne.
- **Même PostgreSQL en dev qu'en prod**, pas de H2.
- **Services externes réels** : OpenAI, Brevo, Google Drive — pas de mock.
- **Pas de Docker en local.**
- Lancement : `./mvnw spring-boot:run` · `npm run dev:*` · `createdb antigone_rh`.

### ❓ Questions du jury

**Q1 — « Est-ce l'architecture de production ou de développement ? »**
> Voir §5. **Répondez franchement : développement.**

**Q2 — « Pourquoi ne pas utiliser H2 en développement ? C'est plus rapide à démarrer. »**
> **Excellente question — vous avez une très bonne réponse.** « Parce que le comportement observé en développement doit être celui de la production. H2 n'implémente pas les fonctionnalités spécifiques de PostgreSQL que le projet utilise — en particulier **pgvector**, l'extension de recherche vectorielle sur laquelle repose le RAG de l'assistant IA. Avec H2, mes tests passeraient sur un comportement qui n'existe pas en production, ce qui est pire que pas de test du tout. Le coût est un démarrage un peu plus lourd ; le bénéfice est la fidélité. »

**Q3 — « Que se passe-t-il sans clé API OpenAI ? »**
> Voir §4. **Le point le plus valorisant de la diapositive** — développez la notion de module optionnel.

**Q4 — « Pourquoi 503 et pas 500 ou 404 ? »**
> « Parce que **503 signifie *Service Unavailable*** : le service existe mais n'est temporairement pas disponible. C'est sémantiquement exact — l'assistant est bien implémenté, c'est sa dépendance externe qui manque. Un 500 laisserait croire à une erreur interne non maîtrisée, et un 404 à une fonctionnalité inexistante. »

**Q5 — « Qu'est-ce qu'un npm workspace ? »**
> « C'est un mécanisme de monorepo : plusieurs packages coexistent dans un même dépôt et peuvent s'importer entre eux sans être publiés sur un registre. Dans mon cas, `@antigone/ai-chat-widget` est un package interne **importé par les trois applications React**. L'intérêt est qu'une correction sur le widget bénéficie immédiatement aux trois interfaces — je n'ai pas trois copies à maintenir. »

**Q6 — « Qu'est-ce que le HMR ? »**
> « *Hot Module Replacement* — le rechargement à chaud. Quand je modifie un composant React, Vite remplace uniquement ce module dans le navigateur, sans recharger la page ni perdre l'état de l'application. C'est ce qui rend le cycle de développement frontend très rapide. »

**Q7 — « Pourquoi Vite plutôt que Webpack ou Create React App ? »**
> « Principalement pour la rapidité du cycle de développement : Vite sert les modules ES natifs sans bundling en développement, ce qui rend le démarrage et le rechargement quasi instantanés. Avec trois applications frontend à faire tourner en parallèle sur un même poste, c'est un gain de confort réel. »

**Q8 — « L'AgentDesktop appelle directement le backend, sans passer par les frontends ? »**
> « Oui, c'est le sens de la flèche "appel direct". C'est logique : l'agent n'a pas d'interface web, il s'exécute en arrière-plan dans la barre système et envoie ses heartbeats directement à l'API en HTTPS/REST. Il est un client de l'API au même titre que les applications web. »

**Q9 — « Pourquoi pas de Docker ? »**
> **Réponse honnête :** « Le schéma le précise explicitement : pas de conteneur Docker en local. Le backend tourne comme processus Maven et la base comme instance PostgreSQL locale. Docker aurait apporté une reproductibilité d'environnement entre les deux développeuses, et c'est une amélioration que je retiendrais. Ce n'était pas un besoin bloquant sur ce projet. »

### ⚠️ Questions pièges

**« Vous n'avez donc jamais déployé en production ? »**
> « Le déploiement en production n'est pas documenté dans mon rapport et je ne vais pas décrire une mise en production que je n'ai pas réalisée. Ce que je peux dire, c'est que l'architecture est conçue pour l'être : le backend est une application Spring Boot autonome, les frontends se compilent en fichiers statiques, et la base est un PostgreSQL standard. Et le déploiement sur le cloud figure d'ailleurs explicitement dans mes perspectives, en diapositive 34. »
> 💡 **Le fait que « Déploiement sur le Cloud » soit dans vos perspectives est votre meilleure défense** : cela montre que l'absence est identifiée et assumée, pas ignorée.

**« Vos services externes sont réels même en local. Ne consommez-vous pas des crédits API à chaque test ? »**
> « Si, et c'est le coût assumé de ce choix. Le bénéfice est que je teste le comportement réel — y compris les latences et les cas d'erreur — plutôt qu'un mock qui donnerait une fausse confiance. C'est particulièrement important pour l'assistant IA, dont la qualité des réponses ne peut pas être simulée. »

### 📖 Lien avec le rapport
**§2.2.2.2 Architecture physique** et **figure 2.9** · §2.4.1 (environnement matériel).

### 🔗 Liens avec les autres diapositives
Complète la **diapositive 21** (le *où* après le *quoi*). Les services externes annoncent l'assistant IA (**29**) et Google Drive (**15, 18**). Le « Déploiement sur le Cloud » est une perspective de la **diapositive 34**.

### ➡️ Transition
> « Voyons enfin le modèle de données qui structure l'ensemble. »

---

## Diapositive 23 — Diagramme de classe

### 1. Ce que contient la diapositive
Un diagramme de classes complet. **Les classes visibles, avec leurs attributs principaux :**

| Classe | Attributs affichés |
|---|---|
| **Role** | id, nom |
| **Permission** | id, code, libelle |
| **Compte** | id, username, passwordHash, enabled |
| **Employé** | id, matricule, nom, email, poste |
| **Demande** *(classe mère)* | id, type, dateCreation, statut |
| └ **Congé** | id, dateDebut, dateFin, nombreJours |
| └ **Autorisation** | id, date, heureDebut, heureFin |
| └ **Teletravail** | id, dateDebut, dateFin |
| **Validation** | id, decision, dateValidation |
| **Pointage** | id, datePointage, heureEntree, heureSortie, statut |
| **HoraireTravail** | id, nom, heureDebut, heureFin, joursTravail |
| **AffectationHoraire** | id, dateDebut, dateFin |
| **Notification** | id, titre, message, lu |
| **Projet** | id, nom, statut, dateDebut, dateFin |
| **Tache** | id, nom |
| **Client** | id, nom, email, telephone |
| **ContactClient** | id, date, poste |
| **MediaPlan** | id, titre, datePublication, format, statut |
| **MediaPlanComment** | id, content, createdAt |
| **Facture** | id, numero, type, dateEmission, totalTtc, statut |
| **LigneDocument** | id, designation, quantite, prixUnitaire |
| **PaiementFacture** | id, montant, datePaiement |
| **BulletinPaie** | id, mois, salaireBrut, netAPayer |
| **Référentiel** | id, libelle, valeur, type |

**Les relations annotées visibles :** Role `regroupe` Permission (0..\* ↔ 0..\*) · Role `est attribué` Compte · Compte `possède` Employé (0..1 → 1) · Employé `soumet` Demande (1 → 0..\*) · **Héritage** Demande ▷ Congé / Autorisation / Teletravail · Congé `suit` Validation · Employé `pointe` Pointage · Employé `a un` AffectationHoraire → HoraireTravail `applique` · Employé `dirige` Projet · Employé `participe` Projet · Projet `organise` Tache · Client `commande` Projet · Client `concerne` MediaPlan · MediaPlan `reçoit` MediaPlanComment · Client `perçoit` Facture · Facture `contient` LigneDocument · Facture `encaisse` PaiementFacture · Employé `génère`/`reçoit` BulletinPaie et Notification · **Référentiel `utilise`** (liens pointillés vers Facture, BulletinPaie, MediaPlan)

### 2. Objectif
Montrer la structure statique du système. C'est la diapositive qui prouve que le modèle de données est **pensé**, pas accumulé.

### 🎤 3. Ce que je dois dire à l'oral

> « Voici le diagramme de classes d'analyse, qui met en évidence les principales entités métier et leurs relations. Je ne vais pas le parcourir classe par classe, mais souligner **quatre points de conception**.
>
> Premièrement, la **gestion des accès**, en haut : un `Compte` est attribué à un ou plusieurs `Role`, et chaque `Role` **regroupe** des `Permission` — en relation plusieurs-à-plusieurs. C'est la traduction du RBAC : le système ne teste pas un rôle, il teste une permission.
>
> Deuxièmement, l'**héritage sur les demandes** : une classe mère `Demande`, qui porte le type, la date de création et le statut, et trois spécialisations — `Congé`, `Autorisation`, `Télétravail`. Elles partagent le même cycle de vie mais portent des attributs propres : un congé a des dates et un nombre de jours, une autorisation a une heure de début et de fin.
>
> Troisièmement, la **centralité de l'employé** : il soumet des demandes, pointe, dirige des projets, participe à des projets, reçoit des notifications et perçoit des bulletins de paie. C'est le pivot du modèle.
>
> Et quatrièmement, le **`Référentiel`** en bas : une entité générique portant un libellé, une valeur et un type, reliée par des liens pointillés `utilise` à plusieurs entités. C'est ce qui rend les listes de valeurs paramétrables sans modification de code. »

*(≈ 1 min 20. **Ne lisez jamais toutes les classes** — le jury a le schéma sous les yeux.)*

### 🔧 4. Explication détaillée

#### a) L'héritage sur `Demande` — **le point le plus interrogé**

> **Concept général :** JPA propose trois stratégies de mapping d'un héritage :
> - **`SINGLE_TABLE`** — une table pour toute la hiérarchie, avec colonne discriminante. Rapide, mais colonnes nulles pour les attributs spécifiques.
> - **`JOINED`** — une table par classe, reliées par clé primaire partagée. Normalisé, mais impose des jointures.
> - **`TABLE_PER_CLASS`** — une table complète par classe concrète. Pas de jointure, mais duplication des colonnes communes.
>
> **Dans mon projet :** la classe `Demande` porte les attributs communs — type, date de création, statut — et les trois sous-classes portent leurs attributs propres.

⚠️ **La stratégie `@Inheritance` précise n'est pas documentée dans le rapport.** **Vérifiez l'annotation dans votre code** et sachez la nommer.

**Ce que vous pouvez affirmer sans risque :** l'intérêt de l'héritage est que le **circuit de validation** et **l'historisation des statuts** sont écrits une fois et fonctionnent pour les trois types. `Validation` se rattache au niveau de la demande, pas de chaque sous-classe.

#### b) Les relations à connaître

| Relation | Cardinalité | Signification |
|---|---|---|
| `Role` ↔ `Permission` | **0..\* ↔ 0..\*** | Un rôle regroupe plusieurs permissions, une permission peut être dans plusieurs rôles |
| `Compte` → `Employé` | **0..1 → 1** | Un employé a **au plus un** compte |
| `Employé` → `Demande` | 1 → 0..\* | Un employé soumet plusieurs demandes |
| `Congé` → `Validation` | 1 → 0..\* | Les étapes du circuit de validation |
| `Employé` ↔ `Projet` | **dirige** (0..\*) et **participe** (0..\* ↔ 0..\*) | **Deux relations distinctes** : chef de projet vs membre |
| `Employé` → `AffectationHoraire` → `HoraireTravail` | 1 → 0..\* → 1 | L'horaire est **affecté sur une période** (dateDebut, dateFin) |
| `Facture` → `LigneDocument` | 1 → 0..\* | Composition |
| `Facture` → `PaiementFacture` | 1 → 0..\* | **Plusieurs paiements par facture → paiements partiels** |

💡 **Deux relations méritent d'être soulignées spontanément :**

1. **`Employé` a deux relations vers `Projet` — `dirige` et `participe`.** C'est ce qui distingue le chef de projet d'un membre d'équipe sur le même projet.
2. **`AffectationHoraire` est une classe d'association porteuse de dates.** Un employé n'a pas *un* horaire figé : il a un horaire **sur une période**. C'est ce qui permet de changer d'horaire (horaire d'été, par exemple) sans réécrire l'historique.

#### c) Le `Référentiel` générique
Une seule entité — id, libellé, valeur, **type** — reliée par des liens pointillés `utilise` à `Facture`, `BulletinPaie` et `MediaPlan`. Le champ `type` permet de distinguer les familles de valeurs (départements, postes, formats, plateformes…) dans une même table.

#### d) `Facture` porte un champ `type`
C'est ainsi que **devis et factures cohabitent** dans la même entité, distingués par ce champ — cohérent avec la diapositive 16 où les deux sont gérés par le même paquet.

### 📌 5. À retenir
- **RBAC** : `Compte` → `Role` → `Permission`, en plusieurs-à-plusieurs.
- **Héritage** : `Demande` ▷ `Congé` / `Autorisation` / `Teletravail`.
- **`Employé` = pivot** du modèle.
- **Deux relations Employé↔Projet** : `dirige` et `participe`.
- **`AffectationHoraire`** porte des dates → horaire **sur une période**.
- **`Facture.type`** distingue devis et facture.
- **Plusieurs `PaiementFacture` par facture** → paiements partiels.
- **`Référentiel`** générique avec un champ `type`.
- ⚠️ Vérifier la stratégie `@Inheritance`.

### ❓ Questions du jury

**Q1 — « Pourquoi l'héritage pour les demandes ? »**
> « Parce que les trois types partagent un cycle de vie identique — soumission, validation, historisation — tout en portant des attributs propres. Sans héritage, j'aurais dupliqué trois fois la logique de validation, avec le risque classique que les trois implémentations divergent. Avec l'héritage, la `Validation` se rattache au niveau de la `Demande` et le circuit fonctionne pour les trois types sans code spécifique. »

**Q2 — « Quelle stratégie de mapping JPA ? »**
> ⚠️ **Vérifiez avant la soutenance.** À défaut : « Je préfère vérifier plutôt que d'affirmer de mémoire. Ce que je peux donner, c'est le critère de choix : `SINGLE_TABLE` est le plus rapide en lecture mais laisse des colonnes nulles ; `JOINED` est le plus normalisé mais impose une jointure à chaque lecture. »

**Q3 — « Pourquoi deux relations entre Employé et Projet ? »**
> « Parce qu'un employé peut avoir **deux rapports différents** à un projet : il peut le **diriger**, en tant que chef de projet, ou y **participer**, en tant que membre d'équipe. Ce sont deux sémantiques distinctes — le chef de projet a des droits que le membre n'a pas — donc deux relations. Les fusionner en une seule aurait demandé un attribut de rôle sur l'association, ce qui serait moins lisible. »

**Q4 — « À quoi sert AffectationHoraire ? Pourquoi pas un lien direct ? »**
> **Excellente question, très bonne réponse à donner.** « Parce qu'un employé n'a pas un horaire figé pour toujours : il a un horaire **sur une période**. `AffectationHoraire` porte une date de début et une date de fin. Cela permet de changer d'horaire — par exemple passer à un horaire d'été — sans écraser l'historique. Si je calcule le statut de présence d'une journée passée, je dois savoir quel horaire s'appliquait **à cette date-là**, pas aujourd'hui. Un lien direct ne le permettrait pas. »

**Q5 — « Comment gérez-vous les paiements partiels d'une facture ? »**
> « Par la cardinalité : une `Facture` peut avoir **plusieurs** `PaiementFacture`, chacun portant un montant et une date. Le statut de la facture est ensuite recalculé à partir du cumul des paiements. C'est cette relation un-à-plusieurs qui rend le paiement partiel possible — avec un simple champ "montant payé" sur la facture, on perdrait l'historique des versements. »

**Q6 — « Comment devis et factures cohabitent-ils ? »**
> « Par un champ `type` sur l'entité `Facture`. Les deux partagent la même structure — un numéro, une date d'émission, des lignes de document, un total — mais leur traitement diffère : un devis n'est pas un document fiscal et n'a pas le même circuit. Les distinguer par un champ plutôt que par deux entités évite de dupliquer toute la gestion des lignes. »

**Q7 — « Que contient le Référentiel ? »**
> « Trois champs utiles : un `libelle`, une `valeur` et un **`type`**. C'est le `type` qui permet de faire cohabiter toutes les listes paramétrables dans une même table : départements, postes, types de contrat, formats de publication, plateformes. Les liens pointillés `utilise` montrent quelles entités s'en servent — Facture, BulletinPaie, MediaPlan. »

**Q8 — « Un employé peut-il avoir plusieurs comptes ? »**
> « Non — la cardinalité est **0..1** côté compte : un employé a **au plus un** compte. Le "0" est important : un employé peut exister sans compte, par exemple s'il n'a pas encore été activé sur la plateforme. »

**Q9 — « Où sont les entités de l'assistant IA et du décisionnel ? »**
> « Elles ne figurent pas sur ce diagramme, qui est le **diagramme de classes d'analyse du domaine métier**. L'assistant IA a son propre diagramme de classes, présenté au chapitre 7 de mon rapport. Quant au décisionnel, il ne définit pas de classes métier : il définit un **schéma en étoile**, avec des dimensions et des tables de faits, présenté au chapitre 8. »

### ⚠️ Questions pièges

**« Ce diagramme est-il le modèle physique de votre base ? »**
> « Non, c'est un **diagramme de classes d'analyse** : il représente les concepts métier et leurs relations, sans les détails d'implémentation. Le modèle physique est plus riche — il comporte notamment les tables d'historisation des statuts, les logs d'accès, et les entités propres à l'assistant IA et à l'entrepôt décisionnel. »

**« Je ne vois pas d'entité pour l'historisation des statuts, alors que vous en parlez beaucoup. »**
> « Vous avez raison, elle n'apparaît pas sur ce diagramme d'analyse, alors que la traçabilité est un de mes besoins non fonctionnels majeurs — "chaque décision est enregistrée avec l'auteur et la date". Le diagramme montre l'entité `Validation`, qui porte la décision et sa date pour les demandes, mais l'historisation complète des changements de statut mériterait d'y figurer. C'est une omission du diagramme d'analyse, pas du système. »

**« Les attributs affichés sont très peu nombreux. Une facture n'a que cinq champs ? »**
> « Non — c'est un diagramme d'**analyse**, qui ne retient que les attributs structurants pour la compréhension du domaine. Une facture réelle porte aussi les montants hors taxe, la TVA, le timbre fiscal, les dates d'échéance et de règlement. Les faire figurer aurait rendu le diagramme illisible sans rien ajouter à la compréhension des relations. »

### 📖 Lien avec le rapport
**§2.2.1 Diagramme de classes d'analyse** et **figure 2.7**.

### 🔗 Liens avec les autres diapositives
Le RBAC visible ici est ce qui implémente les permissions de la **diapositive 19**. `MediaPlan` et `Projet` sont au cœur de la **diapositive 25**, `BulletinPaie` de la **26**, `Pointage` et `HoraireTravail` de la **27**.

### ➡️ Transition
> « La conception étant posée, passons à la réalisation. »

---

## Diapositive 24 — *Intercalaire : Réalisation*

Titre seul.

### 🎤 Ce que je dois dire
> « J'en viens à la réalisation. Plutôt que de parcourir les six releases une à une, j'ai choisi de vous présenter **cinq modules représentatifs**, ceux qui ont demandé le plus de conception. »

*(8 secondes — cette transition est utile : elle annonce la logique des diapositives 25 à 29.)*

---
## 🎯 Préambule aux diapositives 25 à 29 — les cinq modules phares

Ces cinq diapositives partagent **exactement la même structure visuelle** : cinq colonnes numérotées, une colonne orange centrale mise en évidence (le cœur technique), et un bandeau violet de synthèse en trois mots. **Exploitez cette régularité** : une fois la première expliquée, les suivantes s'enchaînent très vite.

| Diapo | Module | Colonne centrale (le cœur) | Bandeau de synthèse |
|---|---|---|---|
| 25 | Plan Média | **Créneau Head Prod** | Automatique · Sans ressaisie · Traçable |
| 26 | Moteur de paie tunisien | **Conversion NET ↔ BRUT** | Conforme · Traçable · Auditable |
| 27 | Pointage automatisé | **Cascade de décision** | Automatique · Fiable · Sans ressaisie |
| 28 | Informatique décisionnelle | **Modèle en étoile** | Cohérent · Sécurisé · À jour |
| 29 | Assistant AI | **Index hybride RAG** | Sécurisé · Contrôlé · Traçable |

💡 **Les trois mots du bandeau sont votre conclusion de chaque module.** Terminez chaque diapositive en les prononçant.

⏱️ **Budget de temps : environ 1 min 30 par module, soit ≈ 7 min 30 pour les cinq.**

---

## Diapositive 25 — Module Plan Média

### 1. Ce que contient la diapositive
Cinq étapes numérotées :

| № | Étape | Contenu |
|---|---|---|
| **1** | **Plan média** | Contenu, format, plateforme · Date de tournage proposée · **Statut : en attente** |
| **2** | **Double validation** | Manager interne · Puis le client (portail) · **Renvoi possible si refus** |
| **3** | **Réservation** 🟠 | **Créneau Head Prod** — *Contrôle de disponibilité avant toute réservation* · *Aucun conflit de date toléré* |
| **4** | **Décision Head Prod** | **Validation → créneau confirmé** · **Rejet → ligne désapprouvée** · Répercussion automatique |
| **5** | **Matérialisation** | **1 projet créé** · 3 tâches par défaut · **Shooting · Post-prod · Publication** |

**Bandeau :** « Automatique · Sans ressaisie · Traçable »
**Légende :** « Workflow Plan Média → Tournage → Projet »

### 2. Objectif
Démontrer le **processus métier le plus élaboré du système**. C'est la brique qui comble le trou identifié en diapositive 7 (« portail client avec approbation : Non / Non / Limité »).

### 🎤 3. Ce que je dois dire à l'oral

> « Le module Plan Média est le plus élaboré du système. Il enchaîne cinq étapes, de la proposition de contenu jusqu'à la création automatique du projet.
>
> **Un**, le social media manager construit la ligne de plan média : le contenu, le format, la plateforme, et une date de tournage proposée. Elle part au statut *en attente*.
>
> **Deux**, elle traverse une **double validation** : d'abord un manager interne, ensuite le client depuis son portail. Si l'un des deux refuse, la ligne peut être corrigée et **renvoyée** — le circuit est réversible.
>
> **Trois**, et c'est le cœur du workflow : la **réservation d'un créneau auprès du Head Prod**, le responsable de production. Le système effectue un **contrôle de disponibilité avant toute réservation** — **aucun conflit de date n'est toléré**.
>
> **Quatre**, le Head Prod décide. S'il **valide**, le créneau est confirmé. S'il **rejette**, la ligne de plan média redescend automatiquement au statut *désapprouvée* — c'est la répercussion automatique.
>
> **Cinq**, la matérialisation : dès que le créneau est confirmé, le système crée **automatiquement un projet et trois tâches par défaut** — Shooting, Post-production, et Publication.
>
> Le résultat, c'est ce que résume le bandeau : **automatique, sans ressaisie, traçable**. Là où l'agence recréait manuellement un projet après chaque tournage validé, l'enchaînement est désormais déclenché par la validation elle-même. »

*(≈ 1 min 30.)*

### 🔧 4. Explication détaillée

#### a) Pourquoi une double validation ?
> Deux niveaux de risque différents. La validation **interne** vérifie la **faisabilité et la cohérence éditoriale** : le contenu est-il pertinent, réalisable, cohérent avec la stratégie du client ? La validation **client** vérifie l'**accord commercial** : le client approuve ce qui sera publié en son nom. Ce sont deux décisions distinctes, prises par des personnes différentes, sur des critères différents. Les fusionner reviendrait à soumettre au client des propositions non validées en interne.

#### b) Le contrôle de disponibilité — **le point technique central**
> **Le problème :** le Head Prod est une **ressource unique**. Si deux plans médias proposent la même date de tournage, l'un des deux ne pourra pas être honoré.
>
> **La solution :** le contrôle est effectué **avant** la réservation, pas après. On ne crée jamais un créneau en conflit qu'il faudrait ensuite arbitrer. La diapositive l'affiche explicitement : *« Contrôle de disponibilité avant toute réservation — aucun conflit de date toléré. »*

#### c) La répercussion automatique du rejet
> **Le point de conception :** un rejet du Head Prod ne laisse pas la ligne dans un état intermédiaire flou. Le statut de la ligne **redescend automatiquement** à *désapprouvée*, exactement comme si le manager l'avait refusée. Le social media manager retrouve la ligne dans le même état qu'après un refus classique, et peut la corriger et la renvoyer.
>
> **Pourquoi c'est important :** sans cette répercussion, une ligne approuvée en interne et côté client mais rejetée côté production resterait « approuvée » alors qu'elle ne peut pas être produite. L'état du système mentirait.

#### d) Les trois tâches par défaut
`Shooting` · `Post-production` · `Validation & Publication` — ce sont les trois phases systématiques d'une production vidéo. Les créer automatiquement évite une ressaisie qui serait identique à chaque fois.

### 📌 5. À retenir
- Les **5 étapes** dans l'ordre.
- **Double validation** : manager interne **puis** client.
- **Contrôle de disponibilité AVANT réservation** — aucun conflit toléré.
- **Rejet → répercussion automatique** sur le statut de la ligne.
- **1 projet + 3 tâches** : Shooting · Post-prod · Publication.
- **Renvoi possible** après refus → circuit réversible.
- Bandeau : **Automatique · Sans ressaisie · Traçable**.

### ❓ Questions du jury

**Q1 — « Déroulez-moi ce workflow de bout en bout. »**
> Le script oral. **Question très probable : c'est votre processus le plus impressionnant.**

**Q2 — « Qui est le Head Prod ? »**
> « Le responsable de production — la personne qui pilote les tournages. C'est une **ressource unique** dans l'agence, d'où la nécessité du contrôle de disponibilité : deux tournages ne peuvent pas être planifiés le même jour. »

**Q3 — « Que se passe-t-il si la date demandée n'est pas disponible ? »**
> « La réservation est refusée. Le contrôle est fait **avant** toute réservation, donc on ne crée jamais un créneau en conflit. Le social media manager doit proposer une autre date — et il peut le faire via le mécanisme de renvoi, puisque la ligne redevient modifiable. »

**Q4 — « Pourquoi créer automatiquement le projet ? »**
> « Parce que c'est exactement ce que l'équipe faisait manuellement, et que la ressaisie est une source d'oubli. Une fois la date de tournage confirmée, il **faut** un projet pour suivre ce qui en découle : le tournage lui-même, la post-production, puis la validation et la publication. Ces trois tâches sont systématiques. Les créer automatiquement garantit que le suivi démarre au bon moment, sans dépendre de la vigilance de quelqu'un. »

**Q5 — « Et si le client refuse après la validation interne ? »**
> « La ligne passe en *désapprouvée* et peut être corrigée puis renvoyée — c'est le "renvoi possible si refus" de l'étape 2. Le circuit est réversible autant de fois que nécessaire. Aucun créneau n'est réservé à ce stade, puisque la réservation n'intervient qu'à l'étape 3. »

**Q6 — « Peut-on annuler un créneau déjà confirmé ? »**
> ⚠️ *Non documenté dans les fichiers fournis.* « Le workflow décrit le chemin nominal et le rejet. L'annulation d'un créneau déjà confirmé, avec la question du projet déjà créé, n'est pas modélisée dans mon rapport. C'est un cas que je traiterais en priorité dans une évolution. »
> 💡 **Question très probable — préparez votre réponse selon ce que fait réellement votre code.**

**Q7 — « "Traçable" — qu'est-ce qui est tracé exactement ? »**
> « Chaque décision du circuit : l'approbation ou le refus du manager, l'approbation ou le refus du client, la décision du Head Prod. Cela correspond au besoin non fonctionnel de traçabilité de mon chapitre 2 : "chaque décision est enregistrée avec l'auteur et la date, afin de faciliter les contrôles ultérieurs". »

**Q8 — « Que se passe-t-il pour une ligne de plan média sans tournage ? »**
> « Elle suit la double validation mais ne déclenche ni réservation de créneau ni création de projet — les étapes 3 à 5 sont propres aux lignes nécessitant un shooting. Toutes les publications ne demandent pas un tournage : un visuel ou un carrousel n'en a pas besoin. »

### ⚠️ Questions pièges

**« Que se passe-t-il si le Head Prod ne répond jamais ? »**
> **Réponse honnête :** « Le créneau reste en attente et la ligne ne se matérialise pas en projet. Il n'y a pas de mécanisme de relance automatique ni d'escalade après un délai — c'est une limite que je reconnais. Ce que le système offre, c'est la **visibilité** : le social media manager voit l'état d'attente et peut relancer humainement. Une relance automatisée après un délai paramétrable serait l'évolution naturelle. »

**« Trois tâches par défaut, n'est-ce pas trop rigide ? »**
> « C'est un choix assumé : ces trois phases sont systématiques pour une production vidéo, et les créer automatiquement supprime une ressaisie identique à chaque fois. Rien n'empêche ensuite le chef de projet d'ajouter, modifier ou supprimer des tâches — le projet créé est un projet ordinaire, avec son tableau Kanban. C'est un point de départ, pas un carcan. »

**« Ce workflow est-il réellement utilisé ou est-ce une démonstration ? »**
> Répondez factuellement selon la réalité. Ce que vous pouvez affirmer : « Il a été conçu à partir du processus réel de l'agence, décrit lors du recueil des besoins — c'est précisément le manque que l'étude de l'existant avait identifié : aucun outil du marché ne propose de portail client avec approbation. »

### 📖 Lien avec le rapport
**Chapitre 5, §5.9.1** — Cas d'utilisation « Planifier un tournage à partir d'un plan média » · **figures 5.10 et 5.11** (diagrammes de séquence système et objet) · **tableau 5.6** · §5.10 Réalisation du sprint 6 · **figure 5.12** (Planification mensuelle du Media Plan).

### 🔗 Liens avec les autres diapositives
Point d'entrée : « Réserver un créneau de tournage » (**diapositive 15**). Comble le manque « portail client avec approbation » de la **diapositive 7**. Utilise `MediaPlan`, `Projet` et `Tache` de la **diapositive 23**.

### ➡️ Transition
> « Passons à la brique la plus calculatoire du système : le moteur de paie. »

---

## Diapositive 26 — Moteur de paie tunisien

### 1. Ce que contient la diapositive
Cinq étapes :

| № | Étape | Contenu |
|---|---|---|
| **1** | **Paramètres & barème** | Taux CNSS, solidarité, TFP · **Barème IRPP versionné** · Abattement forfaitaire |
| **2** | **Calcul du salaire** | Prorata jours ouvrés · Primes, absences, acomptes · **Brut effectif** |
| **3** | **Moteur IRPP** 🟠 | **Conversion NET ↔ BRUT** — *Recherche dichotomique — l'IRPP progressif interdit l'inversion analytique* · *Exonération CIVP · Freelance · Stage* |
| **4** | **Bulletin figé** | Instantané employé + mois · **Statut recalculé** · **Impayé → Partiel → Payé** |
| **5** | **Déclarations** | CNSS trimestrielle suggérée · **Pénalité de retard calculée** · TVA mensuelle recalculée |

**Bandeau :** « Conforme · Traçable · Auditable »

### 2. Objectif
Démontrer la brique **la plus technique** du projet, et celle qui comble le second trou du marché (« paie localisée Tunisie : Non / Non / Non »). **C'est votre meilleur argument « travail d'ingénieur ».**

### 🎤 3. Ce que je dois dire à l'oral

> « Le moteur de paie est la brique la plus calculatoire du système, et celle qui répond au manque le plus net identifié dans l'étude de l'existant : aucune des trois solutions du marché ne propose de paie localisée pour la Tunisie.
>
> **Un**, les **paramètres et le barème**. Les taux — CNSS, contribution de solidarité, TFP — et l'abattement forfaitaire sont **paramétrables**, pas codés en dur. Et le **barème IRPP est versionné** : je conserve l'historique des barèmes avec leur date d'entrée en vigueur. C'est ce qui permet de recalculer un mois ancien avec les taux qui s'appliquaient à l'époque — et cela répond directement à un constat de ma problématique : *« aucune trace claire des taux appliqués au fil du temps »*.
>
> **Deux**, le **calcul du salaire** : prorata sur les jours ouvrés si l'employé arrive ou part en cours de mois, puis primes, absences et acomptes, pour obtenir le **brut effectif**.
>
> **Trois**, et c'est le cœur : le **moteur IRPP**. L'impôt sur le revenu tunisien suit un **barème progressif**, par tranches. Cela pose un problème quand un employé est payé « en net » : il faut retrouver le brut correspondant. Or **l'IRPP progressif interdit l'inversion analytique** — on ne peut pas écrire une formule `brut = f(net)`, parce que le taux change selon la tranche. Je résous donc l'inversion par **recherche dichotomique** : on encadre le brut, on calcule le net correspondant au milieu de l'intervalle, et on réduit l'intervalle de moitié jusqu'à converger. Le module gère aussi les **exonérations** pour les contrats CIVP, Freelance et Stage.
>
> **Quatre**, le **bulletin figé** : une fois généré, il constitue un instantané pour un employé et un mois donnés. Son statut est recalculé automatiquement à chaque versement : **impayé, partiel, puis payé**.
>
> **Cinq**, les **déclarations** : la CNSS trimestrielle est suggérée automatiquement à partir des bulletins, **pénalité de retard comprise**, et la TVA mensuelle est recalculée.
>
> Le bandeau résume l'ambition : **conforme, traçable, auditable**. »

*(≈ 1 min 50. **C'est votre diapositive la plus valorisante — ne la précipitez pas.**)*

### 🔧 4. Explication détaillée

#### a) La conversion NET → BRUT par dichotomie — **votre meilleur argument technique**

> **Le problème, à énoncer clairement :** l'IRPP est un impôt **progressif par tranches**. Le taux marginal change selon le niveau de revenu. Conséquence mathématique : la fonction qui va du brut au net est **définie par morceaux**, et son inverse n'a **pas de forme fermée**. On ne peut donc pas écrire directement `brut = f(net)`.
>
> **La solution : la recherche dichotomique** (ou recherche par bissection).
> 1. On encadre le brut cherché entre une borne basse et une borne haute.
> 2. On calcule le net correspondant au milieu de l'intervalle.
> 3. Si ce net est supérieur au net visé, le brut est trop élevé → on garde la moitié basse. Sinon, la moitié haute.
> 4. On répète : à chaque itération, l'intervalle est **divisé par deux**.
>
> **Pourquoi ça marche :** la fonction brut → net est **monotone croissante** (plus le brut est élevé, plus le net l'est). C'est la condition qui rend la dichotomie applicable. 💡 **Mentionnez la monotonie** : c'est ce qui distingue une réponse comprise d'une réponse récitée.
>
> **La limite assumée :** la dichotomie converge vers une **approximation**, pas vers une valeur exacte. La précision dépend du nombre d'itérations. En pratique elle est largement suffisante pour un montant en dinars, mais elle n'est pas garantie analytiquement.

#### b) Le versionnement du barème IRPP

| Sans versionnement | Avec versionnement |
|---|---|
| Régénérer un bulletin de mars après un changement de barème en juin appliquerait les **nouveaux** taux | Le barème **effectif à la date du bulletin** est appliqué |
| Le bulletin recalculé **diffère** de l'original → il est faux | Le bulletin recalculé est **identique** à l'original |
| Aucune trace des taux appliqués | **Traçabilité complète** des taux dans le temps |

💡 **Reliez-le explicitement à votre problématique** : *« aucune trace claire des taux appliqués au fil du temps »*. C'est le lien le plus fort entre votre problème et votre solution.

#### c) Le bulletin « figé » — pourquoi ?
> « Un bulletin de paie est un **document contractuel** remis à l'employé. Il ne doit pas changer de contenu a posteriori. Le figer à la génération, sous forme d'instantané pour un couple employé + mois, garantit que ce qui a été remis reste ce qui est stocké. Si les paramètres de paie changent le mois suivant, les bulletins passés ne sont pas réécrits. »

#### d) Le recalcul de statut — un mécanisme partagé
`Impayé → Partiel → Payé` : c'est **exactement le même mécanisme** que pour les factures (diapositive 16). Un versement est enregistré, le cumul est comparé au montant dû, le statut est recalculé. 💡 **Signaler cette réutilisation** montre une conception cohérente.

#### e) Les sigles et leur rôle

| Sigle | Développé | Qui paie |
|---|---|---|
| **CNSS** | Caisse Nationale de Sécurité Sociale | Part **salariale** (retenue) + part **patronale** |
| **IRPP** | Impôt sur le Revenu des Personnes Physiques | Salarié (retenue à la source) |
| **Contribution de solidarité** | — | Salarié |
| **TFP** | Taxe de Formation Professionnelle | **Employeur** |
| **FOPROLOS** | Fonds de Promotion du Logement pour les Salariés | **Employeur** |

**Abattement forfaitaire :** réduction appliquée au salaire imposable avant le calcul de l'IRPP.

### 📌 5. À retenir
- **L'IRPP progressif interdit l'inversion analytique** → **recherche dichotomique**.
- La **monotonie** de la fonction brut→net rend la dichotomie applicable.
- **Barème IRPP versionné** → recalcul fidèle d'un mois ancien.
- **Bulletin figé** = instantané employé + mois, document contractuel.
- Statuts : **Impayé → Partiel → Payé** (même mécanisme que les factures).
- **Exonérations : CIVP, Freelance, Stage.**
- CNSS **trimestrielle** + **pénalité de retard** · TVA **mensuelle**.
- Bandeau : **Conforme · Traçable · Auditable**.

### ❓ Questions du jury

**Q1 — « Expliquez la conversion NET vers BRUT. »**
> Voir §4a. **C'est LA question de cette diapositive.** Énoncez d'abord *pourquoi* c'est impossible analytiquement, puis la dichotomie, puis la monotonie.

**Q2 — « Pourquoi ne pas résoudre l'équation directement ? »**
> « Parce qu'il n'y a pas une équation, mais une fonction **définie par morceaux** : le taux d'imposition change à chaque tranche du barème. Inverser une telle fonction demanderait de déterminer d'abord dans quelle tranche se situe le résultat — or c'est précisément ce qu'on cherche. On pourrait tester tranche par tranche, mais la dichotomie est plus simple, plus robuste, et elle reste valide si le barème change de structure. »

**Q3 — « Combien d'itérations faut-il ? »**
> « Chaque itération divise l'intervalle par deux, donc la convergence est logarithmique : une vingtaine d'itérations suffisent largement à atteindre une précision au millime sur un salaire. Le nombre exact d'itérations de mon implémentation n'est pas documenté dans le rapport — je peux vous le montrer dans le code. »
> ⚠️ **Vérifiez-le avant la soutenance** si vous voulez donner un chiffre.

**Q4 — « Pourquoi versionner le barème IRPP ? »**
> Voir §4b, avec le lien vers la problématique. **Excellent moment pour boucler la boucle.**

**Q5 — « Pourquoi figer le bulletin ? »**
> Voir §4c.

**Q6 — « Que signifie "prorata jours ouvrés" ? »**
> « Quand un employé est embauché ou part en cours de mois, il ne perçoit pas un mois complet. Le salaire est calculé au prorata des **jours effectivement ouvrés** sur la période où son contrat était actif. Le calcul s'appuie sur le calendrier d'entreprise et sur son horaire de travail — ce sont les mêmes données que celles du module de pointage. »

**Q7 — « Qu'est-ce qu'un contrat CIVP ? »**
> « Un Contrat d'Initiation à la Vie Professionnelle — un dispositif tunisien d'insertion des jeunes diplômés. Avec le Freelance et le Stage, il fait partie des statuts **exonérés** de cotisations dans mon moteur : le calcul ne leur applique pas les retenues sociales et fiscales habituelles. »

**Q8 — « Comment la pénalité de retard CNSS est-elle calculée ? »**
> « Elle est calculée automatiquement en fonction du retard par rapport à l'échéance légale de la déclaration trimestrielle, et intégrée au montant suggéré — de sorte que le comptable connaisse le montant réel à régler, pas seulement les cotisations dues. »

**Q9 — « Les taux sont-ils codés en dur ? »**
> « Non, et c'est explicite sur la diapositive : les taux CNSS, la contribution de solidarité, la TFP et l'abattement sont des **paramètres**. C'est nécessaire : ces taux évoluent par décision réglementaire, et il serait absurde de devoir redéployer l'application à chaque changement. Et le barème IRPP va plus loin : il est **versionné**, donc l'historique est conservé. »

**Q10 — « Pourquoi la CNSS est-elle trimestrielle et la TVA mensuelle ? »**
> « Ce n'est pas un choix technique, c'est la **réglementation tunisienne** : la déclaration CNSS est trimestrielle, la déclaration TVA est mensuelle. Le système s'y conforme. »

### ⚠️ Questions pièges

**« Votre calcul est-il juridiquement conforme ? Qui l'a validé ? »**
> **Réponse honnête et nuancée :** « Le moteur reproduit la mécanique de calcul de la paie tunisienne — CNSS salariale et patronale, IRPP progressif, contribution de solidarité, abattement, TFP, FOPROLOS. Mais je n'ai pas fait valider le calcul par un expert-comptable dans le cadre de ce projet, et je ne prétendrai pas à une conformité juridique certifiée. Ce que je peux affirmer, c'est que le calcul est **déterministe** — mêmes entrées, même résultat — **paramétrable**, et **traçable** grâce au versionnement du barème. Les trois mots du bandeau — conforme, traçable, auditable — décrivent une intention de conception, pas une certification. »

**« La dichotomie donne une approximation. Un salaire, ça ne s'approxime pas. »**
> **Très bonne question, excellente réponse possible :** « Vous avez raison sur le principe, et c'est la limite que j'assume. Deux éléments la rendent acceptable. D'abord, la convergence est très rapide : chaque itération divise l'intervalle par deux, donc on atteint une précision bien inférieure au millime en quelques dizaines d'itérations — très en dessous de l'arrondi comptable. Ensuite, le sens de l'usage : la conversion NET→BRUT sert à **déterminer le brut contractuel** à partir d'un net négocié. Une fois ce brut fixé, tous les calculs de bulletin repartent du brut, de façon exacte. L'approximation n'intervient qu'une fois, en amont, pas à chaque paie. »

**« Et si le barème IRPP change de structure, pas seulement de taux ? »**
> « Le versionnement le permet : un barème est un objet complet avec ses tranches et sa date d'entrée en vigueur, pas seulement une liste de taux. Si la structure change — nouvelle tranche, nouveau seuil — on crée une nouvelle version. Et la dichotomie reste valide, précisément parce qu'elle ne fait aucune hypothèse sur la forme du barème : elle ne suppose que la monotonie. »
> 💡 **Très bonne réponse** : elle montre que votre choix algorithmique est robuste au changement réglementaire.

### 📖 Lien avec le rapport
**Chapitre 6, §6.9.1** — Cas d'utilisation « Calculer et générer un bulletin de paie, verser un acompte, puis établir la déclaration CNSS du trimestre » · **figures 6.7 et 6.8** · **tableau 6.6** · §6.10 Réalisation du Sprint 8 · **figures 6.9 à 6.12** (gestion des salaires, déclarations CNSS, paramétrage de la paie).

### 🔗 Liens avec les autres diapositives
Comble le manque « paie localisée Tunisie » de la **diapositive 7**. Répond au constat « aucune trace des taux » de la **diapositive 5**. Utilise `BulletinPaie` de la **diapositive 23**. Périmètre du responsable Finance (**diapositive 16**).

### ➡️ Transition
> « Troisième module : le pointage automatisé. »

---

## Diapositive 27 — Pointage automatisé

### 1. Ce que contient la diapositive
Cinq étapes :

| № | Étape | Contenu |
|---|---|---|
| **1** | **Capture** | Agent de bureau (desktop) · Battement de vie (heartbeat) · **Entrée / sortie détectées** |
| **2** | **Règles** | Calendrier d'entreprise · Horaires (standard, été, télétravail) · **Congés & autorisations** |
| **3** | **Moteur de statut** 🟠 | **Cascade de décision** — *Férié → Horaire → Congé → Télétravail → Retard / Présent* · *Calculée en continu, côté serveur* |
| **4** | **Restitution** | Tableau de présence en direct · Historique par employé / période · **Code couleur par statut** |
| **5** | **Décision** | Rapport d'inactivité · **Déduction sur salaire** · Validation par l'admin. |

**Bandeau :** « Automatique · Fiable · Sans ressaisie »

### 2. Objectif
Montrer un **algorithme de décision** et le principe « calculé côté serveur ». C'est aussi la diapositive où la dimension éthique doit être anticipée.

### 🎤 3. Ce que je dois dire à l'oral

> « Le pointage automatisé repose sur cinq étapes.
>
> **Un**, la **capture** : un agent de bureau installé sur le poste de l'employé envoie un **battement de vie** au serveur et détecte automatiquement les entrées et sorties, en s'appuyant sur la connexion au réseau de l'entreprise — **sans intervention manuelle**.
>
> **Deux**, les **règles** applicables : le calendrier d'entreprise avec ses jours fériés, les horaires de travail — standard, été, télétravail — et les congés et autorisations approuvés.
>
> **Trois**, le cœur du module : le **moteur de statut**, qui applique une **cascade de décision** dans un ordre précis. Le système vérifie d'abord si le jour est **férié**. Sinon, si c'est un jour travaillé selon l'**horaire** affecté à l'employé. Sinon, s'il existe un **congé** approuvé. Sinon, un **télétravail** approuvé. Et seulement alors, il compare l'heure d'entrée pointée pour déterminer si l'employé est **en retard ou présent**. Cette cascade est **calculée en continu, côté serveur**.
>
> **Quatre**, la **restitution** : un tableau de présence en direct, un historique par employé et par période, avec un code couleur par statut.
>
> **Cinq**, la **décision** : le système génère des rapports d'inactivité et peut proposer une déduction sur salaire — mais **cette déduction est validée par l'administrateur**. Le système propose, l'humain décide. »

*(≈ 1 min 30.)*

### 🔧 4. Explication détaillée

#### a) Pourquoi l'ordre de la cascade est-il une décision de conception ?

```
1. Jour FÉRIÉ ?              → oui : statut = jour férié, ON S'ARRÊTE
2. Jour travaillé (HORAIRE)? → non : statut = jour non travaillé, ON S'ARRÊTE
3. CONGÉ approuvé ?          → oui : statut = en congé, ON S'ARRÊTE
4. TÉLÉTRAVAIL approuvé ?    → oui : statut = télétravail, ON S'ARRÊTE
5. Comparer le POINTAGE      → statut = présent ou en retard
```

💡 **L'argument à donner :** *« Chaque étape est plus "forte" que la suivante. Un jour férié n'a pas à être testé pour le retard. Un employé en congé approuvé ne doit jamais apparaître absent. Si j'inversais l'ordre — en testant le pointage avant le congé — un employé en congé validé serait compté absent, ce qui serait à la fois faux et vexant. **L'ordre de la cascade est lui-même le cœur de l'algorithme.** »*

#### b) « Calculée en continu, côté serveur » — deux affirmations
1. **En continu** : le statut n'est pas figé à un instant donné, il reflète l'état courant.
2. **Côté serveur** : c'est l'application directe du besoin non fonctionnel — *« les calculs sensibles s'exécutent côté serveur afin de garantir un résultat unique et cohérent, quel que soit le frontend utilisé »*.

#### c) Le rôle de l'agent — **recouper, pas remplacer**
Le rapport est précis : l'agent envoie des signaux de présence « **utilisés pour recouper automatiquement les pointages** ». Et le diagramme de la diapositive 13 l'annote : « recoupement automatique du pointage déclaré ». **L'agent fiabilise une donnée déclarée, il ne s'y substitue pas.**

#### d) La chaîne de décision sur la déduction
`Rapport d'inactivité` → `Proposition de déduction` → **`Validation par l'administrateur`**
> **Le point à souligner :** aucune déduction n'est appliquée automatiquement. C'est un garde-fou volontaire sur une décision qui touche la rémunération.

### 📌 5. À retenir
- **La cascade dans l'ordre** : Férié → Horaire → Congé → Télétravail → Retard/Présent.
- **L'ordre EST l'algorithme** — chaque étape prime sur la suivante.
- **Calculée en continu, côté serveur.**
- L'agent **recoupe**, il ne remplace pas.
- Trois types d'horaires : **standard, été, télétravail**.
- **Déduction proposée, validée par l'administrateur** — jamais automatique.
- Bandeau : **Automatique · Fiable · Sans ressaisie**.

### ❓ Questions du jury

**Q1 — « Décrivez votre cascade de décision. »**
> Les 5 étapes dans l'ordre, puis l'argument du §4a. **Question très probable.**

**Q2 — « Pourquoi cet ordre précisément ? »**
> Voir §4a, avec le contre-exemple : « si je testais le pointage avant le congé, un employé en congé validé serait compté absent ». **Le contre-exemple est ce qui convainc.**

**Q3 — « Comment l'agent détecte-t-il l'entrée et la sortie ? »**
> « Par la **détection automatique de la connexion au réseau de l'entreprise**. L'agent s'exécute en arrière-plan sur le poste et envoie un battement de vie au serveur : quand la connexion au réseau apparaît, c'est une arrivée ; quand elle disparaît durablement, c'est un départ. Le tout **sans intervention manuelle de l'utilisateur**. »

**Q4 — « Et si l'employé est en déplacement, ou en télétravail non déclaré ? »**
> « S'il est en **télétravail déclaré et approuvé**, la cascade le détecte à l'étape 4 et lui attribue le statut télétravail — il n'apparaît pas absent. S'il est en déplacement sans déclaration, l'agent ne détecte pas le réseau de l'entreprise et il apparaîtra absent. C'est une limite réelle : le système repose sur la déclaration préalable. »

**Q5 — « Un employé peut-il contourner l'agent ? »**
> **Réponse honnête :** « Techniquement, un agent installé sur le poste de l'utilisateur n'est jamais inviolable — c'est vrai de tout dispositif de ce type. C'est d'ailleurs pourquoi l'agent **recoupe** le pointage plutôt que de le remplacer : la donnée déclarée reste la référence, l'agent l'objective. Et surtout, toute conséquence — une déduction sur salaire — passe par une **validation humaine explicite**, jamais par une application automatique d'une mesure technique. »

**Q6 — « Qu'est-ce qu'un "horaire été" ? »**
> « Un horaire de travail spécifique appliqué sur une période donnée — pratique courante en Tunisie pendant les mois d'été. C'est précisément ce que permet l'entité `AffectationHoraire` de mon diagramme de classe : elle porte une date de début et une date de fin, donc un employé peut avoir un horaire différent selon la période, sans que cela réécrive l'historique. »
> 💡 **Excellent moment pour relier deux diapositives** (23 et 27).

**Q7 — « Comment un retard est-il déterminé ? »**
> « Par comparaison entre l'heure d'entrée effectivement pointée et l'heure de début prévue par l'horaire affecté à l'employé **à cette date**. Et cette comparaison n'intervient qu'à la dernière étape de la cascade : si le jour est férié, non travaillé, en congé ou en télétravail, la question du retard ne se pose même pas. »

**Q8 — « Le code couleur, quels statuts ? »**
> « Les statuts issus de la cascade : jour férié, jour non travaillé, en congé, en télétravail, présent, en retard, absent. Le code couleur permet à l'administrateur de lire le tableau de présence d'un coup d'œil, sans avoir à interpréter un libellé pour chaque ligne. »

### ⚠️ Questions pièges

**« N'est-ce pas de la surveillance des employés ? »**
> **Question très probable — préparez-la soigneusement.** « C'est une question légitime et je l'ai prise au sérieux. Trois éléments cadrent l'usage. D'abord, **ce qui est capté est limité** : la détection de la connexion au réseau de l'entreprise et un signal de présence. L'agent ne capture ni le contenu de l'écran, ni les applications utilisées, ni les frappes clavier. Ensuite, **la cascade protège l'employé** : un jour férié, un congé ou un télétravail approuvé sont détectés **avant** toute évaluation de présence — le système ne peut pas compter absent quelqu'un qui est légitimement absent. Enfin, **aucune conséquence n'est automatique** : le rapport d'inactivité propose, l'administrateur valide.
>
> Cela dit, je dois être honnête : je n'ai pas traité la conformité réglementaire de ce traitement — information des salariés, base légale, durée de conservation. C'est un préalable que je signalerais avant tout déploiement réel. »
> 💡 **Reconnaître la limite réglementaire est plus fort que de prétendre l'avoir traitée.**

**« "Automatique, fiable" — mais que se passe-t-il si l'agent plante ? »**
> « Le système fonctionne en mode dégradé : sans battement de vie, l'indicateur d'activité n'est pas alimenté, mais la cascade de décision continue de s'appliquer sur les autres sources — calendrier, horaires, congés, télétravail. L'agent **enrichit** la donnée de présence, il n'en est pas la condition. »

### 📖 Lien avec le rapport
**Chapitre 4, §4.4.1** — Cas d'utilisation « Consulter le tableau de bord de pilotage RH » · **figures 4.2 et 4.3** · **tableau 4.3** · §4.5 Réalisation du sprint 3 · **figures 4.6 à 4.12** (première connexion de l'agent, confirmation de présence, agent en arrière-plan, suivi des présences en temps réel, rapports d'inactivité, historique).

### 🔗 Liens avec les autres diapositives
L'agent est un acteur de la **diapositive 11** et intervient dans la **13**. Les horaires viennent de `AffectationHoraire` (**diapositive 23**). Les données produites alimentent l'entrepôt (**diapositive 28**).

### ➡️ Transition
> « Quatrième module : l'informatique décisionnelle. »

---

## Diapositive 28 — Informatique décisionnelle

### 1. Ce que contient la diapositive
Cinq étapes :

| № | Étape | Contenu |
|---|---|---|
| **1** | **Sources** | Tables transactionnelles · RH · Projets · Finance · **Jamais modifiées** |
| **2** | **Transformation** | Procédure ETL nocturne · **02h30 chaque nuit** · Rechargement complet |
| **3** | **Entrepôt** 🟠 | **Modèle en étoile** — *4 dimensions · 6 tables de faits* · *Schéma `dwh` dédié* |
| **4** | **Restitution** | Pages Analytique (RH·Fin·Proj) · **Power BI (lecture seule)** · **Mêmes chiffres, 2 vues** |
| **5** | **Gouvernance** | Permissions déjà existantes · **VIEW_MONITORING / FINANCE** · Fraîcheur affichée |

**Bandeau :** « Cohérent · Sécurisé · À jour »

### 2. Objectif
Montrer que vous distinguez **opérationnel (OLTP)** et **analytique (OLAP)** — une distinction d'architecture que tous les candidats ne maîtrisent pas.

### 🎤 3. Ce que je dois dire à l'oral

> « L'informatique décisionnelle répond à un besoin que les modules opérationnels ne couvrent pas : **lire une tendance plutôt qu'un instant**.
>
> **Un**, les **sources** : les tables transactionnelles des trois domaines — RH, projets, finance. Point important : **elles ne sont jamais modifiées**. L'entrepôt lit, il n'écrit pas dans l'opérationnel.
>
> **Deux**, la **transformation** : une procédure ETL s'exécute **chaque nuit à 2h30**, en rechargement complet.
>
> **Trois**, l'**entrepôt** : un schéma `dwh` dédié, modélisé **en étoile**, avec **quatre dimensions et six tables de faits**.
>
> **Quatre**, la **restitution**, et c'est le point que je veux souligner : elle est **double**. D'un côté des pages Analytique intégrées aux applications — RH, Finance, Projets. De l'autre un rapport **Power BI en lecture seule**. Et surtout : **les mêmes chiffres, deux vues**. Les deux restitutions lisent le même entrepôt, donc elles ne peuvent pas se contredire — c'est décisif quand un dirigeant compare un chiffre affiché à l'écran avec un chiffre exporté.
>
> **Cinq**, la **gouvernance** : la release n'introduit **aucune nouvelle permission**. Elle réutilise celles déjà en place — `VIEW_MONITORING`, `VIEW_FINANCE` — de sorte qu'un utilisateur autorisé à consulter un module opérationnel l'est aussi à en consulter l'analyse. Et chaque page affiche la **fraîcheur des données**. »

*(≈ 1 min 40.)*

### 🔧 4. Explication détaillée

#### a) OLTP vs OLAP — **la distinction fondamentale**

| | **OLTP** (tables transactionnelles) | **OLAP** (schéma `dwh`) |
|---|---|---|
| Optimisé pour | **Écrire** vite, sans doublon | **Analyser** l'historique |
| Forme | Normalisé, structuré par entité | **Dénormalisé**, modèle en étoile |
| Question type | « Combien de projets en cours ce mois ? » | « L'absentéisme a-t-il augmenté depuis deux ans ? » |
| Alimentation | Temps réel, par l'application | **Rechargement nocturne** |
| Contenu | **Source de vérité** | **Données dérivées**, reconstructibles |

> **L'argument à donner :** *« Un schéma transactionnel est normalisé pour écrire vite, sans doublon. Il est structuré autour de l'entité, pas autour de l'analyse. L'interroger sur l'historique complet impose des jointures profondes et des agrégations répétées, dont le coût retombe sur la base de production — donc sur les écrans de saisie des utilisateurs. L'entrepôt résout cela en dénormalisant une fois par nuit. »*

#### b) Le modèle en étoile
> **Concept général :** un modèle en étoile oppose deux familles de tables. Les **dimensions** portent les **axes d'analyse** — qui, quoi, quand. Les **tables de faits** portent les **mesures numériques** et référencent les dimensions. Une table de faits au centre, ses dimensions en rayons : d'où le nom.
>
> **Dans mon projet : 4 dimensions et 6 tables de faits**, dans un schéma `dwh` dédié.

⚠️ **Le détail des dimensions et des faits n'est pas affiché sur la diapositive.** Il figure dans la figure 8.2 du rapport. **Sachez les nommer** — préparez-les depuis votre chapitre 8.

#### c) « Jamais modifiées » et « rechargement complet » — deux choix liés

| Choix | Justification |
|---|---|
| **Sources jamais modifiées** | L'entrepôt est en **lecture seule** sur l'opérationnel. Aucun risque qu'une erreur d'ETL corrompe les données de production. |
| **Rechargement complet** | Plus simple qu'un chargement incrémental, qui exigerait de tracer les suppressions et les modifications rétroactives. **Supprime toute classe de bug de désynchronisation.** Au volume d'une agence, c'est tenable. |

⚠️ **La limite à assumer :** le rechargement complet ne passerait pas à l'échelle d'un volume très supérieur. Dites-le spontanément.

#### d) « Mêmes chiffres, 2 vues » — **votre meilleur argument**
> « Les pages Analytique et le rapport Power BI **lisent le même entrepôt**. Ils ne recalculent rien indépendamment l'un de l'autre. Conséquence : à période égale, ils affichent nécessairement les mêmes chiffres. C'est la condition pour que l'un puisse servir à vérifier l'autre — et c'est décisif pour la confiance de la direction. »

#### e) « Fraîcheur affichée » — pourquoi c'est important
> « Un tableau de bord décisionnel qui ne dit pas de quand datent ses chiffres invite à la méprise : l'utilisateur d'une application transactionnelle est habitué à des données instantanées. Comme l'entrepôt est rechargé chaque nuit, ses chiffres datent au maximum de la veille — il faut donc l'afficher explicitement. »

### 📌 5. À retenir
- **OLTP** (écrire) vs **OLAP** (analyser).
- **Modèle en étoile** : **4 dimensions · 6 tables de faits**, schéma `dwh`.
- **ETL nocturne à 02h30**, **rechargement complet**.
- **Sources jamais modifiées** — l'entrepôt lit seulement.
- **Double restitution, mêmes chiffres** : pages Analytique + Power BI (lecture seule).
- **Aucune nouvelle permission** : `VIEW_MONITORING`, `VIEW_FINANCE`.
- **Fraîcheur affichée**.
- Bandeau : **Cohérent · Sécurisé · À jour**.

### ❓ Questions du jury

**Q1 — « Pourquoi un entrepôt séparé ? »**
> Voir §4a. **L'opposition des deux questions types est la meilleure entrée en matière.**

**Q2 — « Qu'est-ce qu'un modèle en étoile ? Pourquoi pas un flocon ? »**
> « Un modèle en étoile oppose les **dimensions**, qui portent les axes d'analyse, aux **tables de faits**, qui portent les mesures et référencent les dimensions. Le **flocon** normaliserait les dimensions en sous-tables. Je ne l'ai pas fait, et c'est volontaire : la dénormalisation des dimensions économise des jointures à chaque requête d'analyse, et le coût en redondance est négligeable, une dimension étant petite par nature. »

**Q3 — « Que sont vos 4 dimensions et vos 6 tables de faits ? »**
> ⚠️ **Préparez cette réponse depuis la figure 8.2 de votre rapport.** C'est une question quasi certaine dès lors que vous affichez ces chiffres.

**Q4 — « Qu'est-ce qu'un ETL ? »**
> « *Extract, Transform, Load* : extraire les données des systèmes sources, les transformer — agrégation, dénormalisation, mise en conformité avec le modèle cible — puis les charger dans l'entrepôt. Dans mon cas, c'est une procédure qui s'exécute chaque nuit à 2h30, en rechargement complet. »

**Q5 — « Pourquoi 2h30 du matin ? »**
> « Parce que c'est une plage où l'agence n'utilise pas la plateforme. Le rechargement consomme des ressources sur la base : le faire en heures ouvrées dégraderait l'expérience des utilisateurs sur les écrans de saisie — ce qui serait exactement le problème que l'entrepôt vise à éviter. »

**Q6 — « Pourquoi un rechargement complet et non incrémental ? »**
> Voir §4c. **Assumez la limite d'échelle.**

**Q7 — « Pourquoi Power BI en plus des pages Analytique ? »**
> « Ils répondent à des usages distincts. Les **pages Analytique** servent le suivi quotidien **dans l'outil de travail** : elles héritent du cloisonnement par permission et de l'identité visuelle de l'application. Le **rapport Power BI** sert **l'exploration libre** : il permet à la direction de croiser des axes que l'application n'a pas prévus et de construire ses propres mesures. C'est d'ailleurs la formulation de ma diapositive 8 : "exploration libre pour la direction, sans solliciter le développement". Et comme ils lisent le même entrepôt, ils ne peuvent pas diverger. »

**Q8 — « Power BI en lecture seule — pourquoi ? »**
> « Parce que Power BI est un outil de **restitution**, pas de saisie. Lui donner un accès en écriture sur l'entrepôt n'aurait aucun sens et ouvrirait un risque : une manipulation pourrait altérer des données analytiques. L'accès en lecture seule garantit que l'entrepôt ne peut être modifié que par la procédure ETL. »

**Q9 — « Pourquoi ne pas avoir créé de nouvelles permissions ? »**
> « C'est un choix délibéré : un utilisateur autorisé à consulter un module opérationnel l'est aussi à en consulter l'analyse, et l'inverse serait difficile à justifier. Réutiliser `VIEW_MONITORING` et `VIEW_FINANCE` garantit que le cloisonnement analytique est **exactement** le même que le cloisonnement opérationnel — sans risque de divergence entre deux modèles de droits. »
> 💡 **Excellente réponse** : elle montre que vous évitez une duplication de logique de sécurité.

**Q10 — « Qu'est-ce que DAX ? »**
> « Le langage de formules de Power BI, utilisé pour définir les mesures et calculer les indicateurs analytiques — présence, absentéisme, produits, charges, avancement des projets. Il figure dans le tableau des outils décisionnels de mon rapport. »

### ⚠️ Questions pièges

**« Votre entrepôt est-il dans la même base PostgreSQL ? »**
> « Oui, dans un **schéma dédié** `dwh`. La séparation est donc **logique** plutôt que physique, et je l'assume. Ce qui compte pour la séparation OLTP/OLAP, c'est que les requêtes analytiques ne rejouent plus de jointures profondes sur les tables de saisie — et c'est acquis, puisque les restitutions lisent exclusivement le schéma `dwh`. Une instance physiquement séparée aurait ajouté un coût d'infrastructure que le volume de l'agence ne justifie pas. »

**« Vos chiffres datent de la veille. N'est-ce pas un problème ? »**
> « Pour un usage analytique, non — on analyse une tendance sur plusieurs mois, pas la minute écoulée. Et c'est précisément pour éviter la méprise que la **fraîcheur est affichée** sur chaque page. Si un utilisateur a besoin d'une donnée à la seconde, ce n'est pas au décisionnel qu'il doit s'adresser mais aux tableaux de bord opérationnels, qui lisent directement les tables transactionnelles. Les deux coexistent et répondent à deux questions différentes. »

**« Le décisionnel ne représente que 16 points sur 254. N'est-ce pas anecdotique ? »**
> « C'est la release la plus légère en charge, c'est exact, et je ne prétendrai pas le contraire. Mais elle repose sur tout le reste : sans les quatre modules métier, il n'y aurait aucune donnée à analyser. Sa faible charge s'explique aussi par sa nature — un schéma, une procédure ETL et des restitutions, sans la richesse fonctionnelle et les règles métier d'un module opérationnel. »

### 📖 Lien avec le rapport
**Chapitre 8** — Release 6 : Informatique décisionnelle · **§8.3.1** Le modèle en étoile et **figure 8.2** · **§8.3.2** La chaîne ETL et **tableau 8.2** · **§8.3.3** La double restitution · **§8.5** Cas d'utilisation « Consulter l'analyse de présence et recharger l'entrepôt » · **figures 8.5 à 8.8** (tableaux de bord RH, Projets & Tâches, financier, schéma du Data Warehouse).

### 🔗 Liens avec les autres diapositives
Les sources sont les données produites par les **diapositives 25, 26 et 27**. Les permissions réutilisées viennent du RBAC de la **diapositive 23**. Power BI figure dans les technologies du rapport (§2.4.2.3).

### ➡️ Transition
> « Cinquième et dernier module : l'assistant IA. »

---

## Diapositive 29 — Assistant AI

### 1. Ce que contient la diapositive
**Sept blocs** — cinq en ligne, deux en dessous :

| № | Bloc | Contenu |
|---|---|---|
| **1** | **Raisonnement** | Agent LangChain4j · Classification d'intention · **Jamais inventer un chiffre** |
| **2** | **Récupération** | Recherche sémantique (pgvector) · Recherche lexicale (PostgreSQL) · **Fusion RRF** |
| **3** | **Base de connaissances** 🟠 | **Index hybride RAG** — *Marques · Projets · Media plans · Règlement intérieur* · *Réindexé chaque jour* |
| **4** | **Action** | **15 outils métier** · Lire un tableau de bord · Rédiger une relance |
| **5** | **Livraison** | **Streaming mot par mot** · Activité des outils en direct |
| **6** | **Mémoire** | Fenêtre glissante + résumé cumulatif |
| **7** | **Tâches de fond** | Réindexation quotidienne |

**Bandeau :** « Sécurisé · Contrôlé · Traçable »

### 2. Objectif
C'est **votre diapositive de différenciation technique**. Elle doit faire passer un message avant tout autre : vous n'avez pas « branché ChatGPT » sur une application — vous avez conçu une architecture qui **garantit l'exactitude et le cloisonnement**.

### 🎤 3. Ce que je dois dire à l'oral

> « L'assistant IA est organisé en sept composants, que je regrouperais en cinq fonctions.
>
> Le **raisonnement**, d'abord : un agent construit avec **LangChain4j**, qui classifie l'intention de la demande de l'utilisateur. Et le principe qui structure tout le module : **jamais inventer un chiffre**. Les calculs métier sont réalisés côté Java, et leurs résultats sont transmis au modèle comme des **données à reformuler** — il n'effectue aucun nouveau calcul.
>
> La **récupération** : c'est le RAG hybride. Deux recherches indépendantes sont lancées pour chaque question — une **recherche sémantique** via pgvector, qui trouve par le sens, et une **recherche lexicale** native PostgreSQL, qui trouve par les mots exacts. Leurs résultats sont fusionnés par **RRF**, la fusion par rang réciproque.
>
> La **base de connaissances** : un index hybride couvrant les marques, les projets, les media plans et le règlement intérieur, **réindexé chaque jour**.
>
> L'**action** : **quinze outils métier** que le modèle peut appeler — lire un tableau de bord, rédiger une relance. Chaque outil **vérifie les droits d'accès de l'utilisateur avant d'effectuer une opération**, et chaque appel est enregistré.
>
> La **livraison** : un **streaming mot par mot**, qui affiche la réponse au fil de l'eau, ainsi que l'activité des outils en direct — l'utilisateur voit ce que fait le système.
>
> Enfin la **mémoire** — une fenêtre glissante des vingt derniers messages plus un résumé cumulatif — et les **tâches de fond**, avec la réindexation quotidienne.
>
> Le bandeau résume les trois garanties : **sécurisé, contrôlé, traçable**. »

*(≈ 1 min 50.)*

### 🔧 4. Explication détaillée — les concepts à savoir définir

#### a) LLM
> **Simplement :** un modèle statistique entraîné à prédire le mot suivant. Il **ne possède pas de base de données** et **ne sait rien** des clients d'Antigone, des factures ou des salaires.
>
> **Conséquence :** toute donnée métier doit lui être **fournie dans le prompt**.
> 💡 **Phrase à retenir :** *« Un LLM est un excellent rédacteur mais une très mauvaise base de données. »*

#### b) RAG (Retrieval-Augmented Generation)
> **Simplement :** « génération augmentée par la récupération ». On **cherche** d'abord les documents pertinents, on les **injecte** dans le prompt, puis le modèle **répond en s'appuyant dessus**.
>
> **L'exemple :** *« Sans RAG, si je demande "combien de jours de congé maladie ?", le modèle invente une réponse plausible mais fausse. Avec RAG, je lui envoie l'article exact du règlement intérieur, et il cite le vrai chiffre. »*

#### c) Embedding et recherche sémantique
> **Simplement :** un embedding traduit un texte en **vecteur de nombres** — ici **1536 dimensions**. Deux textes de sens proche produisent des vecteurs proches, **même sans mot commun**.
>
> **pgvector** est l'extension PostgreSQL qui stocke ces vecteurs et permet de rechercher les plus proches.

#### d) Pourquoi « hybride » ? — **le point clé**

| Type de requête | Recherche sémantique seule | Recherche lexicale seule |
|---|---|---|
| « contenu sur le lancement produit » | ✅ trouve « teasing nouvelle collection » | ❌ aucun mot commun |
| « facture FA-2026-0147 » | ❌ le numéro est **dilué** dans le vecteur | ✅ correspondance exacte |
| « règles sur les retards » | ✅ trouve « ponctualité » | ❌ |

> **L'argument :** *« Les embeddings diluent les correspondances exactes. Un numéro de facture ou un nom de marque remonte mal en recherche sémantique — le vecteur capture le sens général, pas la chaîne exacte. Inversement, une recherche par mots-clés rate la proximité de sens. Les deux branches sont complémentaires, d'où l'exécution des deux pour chaque requête. »*

#### e) La fusion RRF
> **Le problème :** les deux branches produisent des scores sur des **échelles incomparables**. Les additionner n'a aucun sens mathématique.
>
> **La solution : RRF — *Reciprocal Rank Fusion*** — fusionne les **rangs**, pas les scores. Chaque document reçoit une contribution inversement proportionnelle à son rang dans chaque branche, et les contributions s'additionnent.
>
> **L'effet recherché :** un document trouvé par **les deux** branches remonte devant un document trouvé par une seule. *« La convergence de deux méthodes indépendantes est un signal de pertinence plus fort qu'un bon score dans une seule. »*

#### f) Le tool calling
> **Simplement :** le mécanisme par lequel le modèle, au lieu de répondre directement, **demande l'exécution d'une fonction**. On lui décrit les fonctions disponibles, il choisit laquelle appeler.
>
> **Dans mon projet :** des méthodes Java annotées **`@Tool`** — quinze au total — qui récupèrent les informations sur les marques, projets, media plans, factures, employés ou politiques internes.
>
> **La garantie de sécurité :** *« chaque outil vérifie les droits d'accès de l'utilisateur avant d'effectuer une opération »*, et *« les appels aux outils sont enregistrés afin d'assurer leur traçabilité »*.

#### g) La mémoire
> **Le problème :** un LLM est **sans état** — il ne conserve pas l'historique entre les appels.
>
> **La solution, en deux mécanismes :** une **fenêtre glissante des 20 derniers messages** conservés en clair, plus un **résumé cumulatif** des échanges plus anciens. Le résumé est régénéré **tous les 6 messages évincés**.
>
> **Et la mémoire est persistée en base** : une conversation survit à un redémarrage du service.

#### h) Le streaming SSE
> **Server-Sent Events** — un flux HTTP unidirectionnel du serveur vers le client. Il transmet progressivement les réponses, les événements liés aux outils et les résultats structurés.
>
> **Pourquoi :** *« particulièrement adapté aux traitements longs, tels que la génération d'un Media Plan »*. Sans streaming, l'utilisateur regarderait un indicateur figé sans savoir si le système fonctionne.

#### i) Les trois principes de prompting (§7.2.5.2)

| Principe | Contenu |
|---|---|
| **Séparation raisonnement / exécution** | Les calculs métier sont faits côté Java ; leurs résultats sont transmis comme **données à reformuler** |
| **Méthode de travail structurée** | Certaines fonctionnalités imposent un **ordre précis** d'utilisation des outils — notamment la génération des media plans |
| **Protection contre les injections de prompt** | Les règles d'accès sont appliquées **indépendamment du modèle**, et les données provenant des outils sont traitées comme des **informations, non comme des instructions** |

### 📌 5. À retenir
- **Les 7 blocs** : Raisonnement · Récupération · Base de connaissances · Action · Livraison · Mémoire · Tâches de fond.
- **« Jamais inventer un chiffre »** — le principe directeur.
- **RAG hybride** = sémantique (pgvector) + lexicale (PostgreSQL) → **fusion RRF**.
- **RRF fusionne les RANGS**, pas les scores (échelles incomparables).
- **15 outils métier**, chacun **vérifiant les droits avant toute opération**.
- **Mémoire : 20 messages + résumé tous les 6 évincés**, persistée en base.
- **Streaming SSE** pour les traitements longs.
- **Réindexation quotidienne.**
- **3 principes de prompting**, dont la protection contre l'injection.
- Bandeau : **Sécurisé · Contrôlé · Traçable**.

### ❓ Questions du jury

**Q1 — « Comment garantissez-vous que l'IA n'invente pas de chiffres ? »**
> **LA question.** « Par la **séparation stricte entre raisonnement et exécution**, qui est un principe explicite de ma conception de prompts : les calculs métier sont réalisés côté Java et leurs résultats sont transmis au modèle comme des **données à reformuler, sans qu'il effectue de nouveaux calculs**. Concrètement, pour expliquer un bulletin de paie, le modèle ne reçoit pas les paramètres — il reçoit le net déjà calculé. Il n'a aucun calcul à faire, donc aucune occasion de se tromper. Et le RAG complète ce dispositif en **ancrant les réponses dans les données réelles** plutôt que dans la mémoire d'entraînement du modèle. »

**Q2 — « Qu'est-ce que le RAG ? »**
> Voir §4b, avec l'exemple du congé maladie.

**Q3 — « Pourquoi une recherche hybride et pas seulement vectorielle ? »**
> Le tableau du §4d. **Question très probable.**

**Q4 — « Qu'est-ce que la fusion RRF ? »**
> Voir §4e. Insistez sur : **les échelles sont incomparables, donc on fusionne les rangs**.

**Q5 — « Qu'est-ce qu'un embedding ? »**
> Voir §4c, avec l'exemple « lancement produit » / « teasing nouvelle collection ».

**Q6 — « Vous n'avez pas entraîné de modèle. Où est le travail d'IA ? »**
> « Entraîner un modèle n'aurait pas eu de sens ici, pour trois raisons. **Volumétrie** : un entraînement utile demande des milliers d'exemples annotés. **Fraîcheur** : un modèle entraîné fige la connaissance — une facture payée hier doit être invisible aujourd'hui, ce que le RAG garantit par construction. **Sécurité** : un modèle entraîné sur les bulletins de paie de toute l'entreprise pourrait les restituer à n'importe qui, alors que chaque outil vérifie les droits avant toute lecture.
>
> Le travail d'ingénierie porte sur l'architecture : la conception du RAG hybride avec fusion RRF, le contrôle d'accès dans un système à outils, l'ingénierie de prompt avec ses trois principes, la gestion de la mémoire longue et du streaming. »

**Q7 — « Qu'est-ce que le tool calling ? »**
> Voir §4f, avec la garantie de sécurité.

**Q8 — « Comment gérez-vous une conversation longue ? »**
> Voir §4g. Fenêtre de 20 + résumé tous les 6 évincés + persistance en base.

**Q9 — « Que fait exactement l'assistant ? »**
> « Quatre choses, selon mon backlog : **discuter en temps réel** avec l'utilisateur, **rédiger une relance client** pour le comptable, **expliquer un bulletin de paie** à un employé, et **générer un media plan mensuel** pour le social media manager. Plus une fonction d'administration : **réindexer la base de connaissances**. »

**Q10 — « Pourquoi du streaming ? »**
> Voir §4h.

**Q11 — « Comment vous protégez-vous de l'injection de prompt ? »**
> « L'injection de prompt consiste à faire dévier le modèle par le texte qu'on lui soumet. Ma défense principale n'est pas dans le prompt : **les règles d'accès sont appliquées indépendamment du modèle**. Un prompt ne peut pas contourner un contrôle de droits écrit en Java, puisque chaque outil vérifie les droits avant d'effectuer une opération. En complément, les données renvoyées par un outil sont traitées comme des **informations, pas comme des instructions à exécuter** — c'est nécessaire, car ces données peuvent contenir du texte saisi par un tiers. »
> 💡 **Le point fort : la sécurité ne repose pas sur le prompt.**

### ⚠️ Questions pièges

**« Qu'est-ce qui différencie votre assistant d'un ChatGPT avec copier-coller ? »**
>
> | Critère | ChatGPT + copier-coller | L'assistant d'Antigone |
> |---|---|---|
> | Sélection du contexte | Manuelle, par l'utilisateur | **Automatique**, par pertinence hybride |
> | Fraîcheur | Ce que l'utilisateur a copié | **L'état actuel de la base** |
> | Cloisonnement | Aucun | **Droits vérifiés par chaque outil** |
> | Traçabilité | Aucune | **Chaque appel d'outil enregistré** |
> | Exactitude numérique | Le modèle recalcule | **Calculs en Java**, le modèle reformule |

**« Un employé peut-il obtenir le salaire d'un collègue via l'assistant ? »**
> « Non. Chaque outil **vérifie les droits d'accès de l'utilisateur avant d'effectuer une opération** — indépendamment de ce que le modèle demande ou de la façon dont la question est formulée. Une reformulation habile ne contourne rien, parce que le contrôle est une vérification en Java exécutée avant toute lecture de données, pas une consigne dans le prompt. Et la tentative est enregistrée dans le journal des appels d'outils. »

**« Que se passe-t-il si OpenAI est indisponible ? »**
> « L'assistant ne répond plus, mais **le reste de l'application continue de fonctionner**. C'est même affiché sur mon architecture physique : sans clé API valide, le backend démarre quand même et seuls les endpoints `/api/v1/**` répondent **503**. L'IA est un module **optionnel**, pas une dépendance bloquante. »
> 💡 **Excellent moment pour relier les diapositives 22 et 29.**

**« Quelle est la limite de votre assistant ? »**
> « Plusieurs, que j'assume. D'abord la **dépendance à un fournisseur externe** — atténuée par l'usage de LangChain4j comme couche d'abstraction, qui rend le changement de modèle possible par configuration, mais réelle. Ensuite, **je n'ai pas d'évaluation automatisée de la qualité des réponses** : je n'ai ni taux de pertinence ni jeu de test de référence, et je ne vais pas en inventer. Enfin, la réindexation est **quotidienne** : une donnée créée dans la journée n'est pas immédiatement dans l'index de recherche, même si les outils métier, eux, lisent la base en direct. »

### 📖 Lien avec le rapport
**Chapitre 7** — Release 5 : L'Assistant IA · **§7.1** Architecture de l'IA (les cinq couches) et **figure 7.1** · **§7.2.5** Conception des prompts · **§7.5** Diagramme de classe · **§7.7.1** Cas d'utilisation « Interroger l'assistant » · **figures 7.6 à 7.11** (assistant règlement intérieur, profil de marque, proposition et génération de media plan, relances, envoi d'e-mail).

### 🔗 Liens avec les autres diapositives
Le choix du modèle est justifié en **diapositive 30**. pgvector et LangChain4j figurent en **31**. Le widget partagé apparaît en **22**. L'assistant est un acteur de la **diapositive 11**.

### ➡️ Transition
> « Justement, comment ai-je choisi ce modèle de langage ? »

---

## Diapositive 30 — Choix du modèle de langage (LLM)

### 1. Ce que contient la diapositive
Un tableau comparatif à quatre colonnes — **Modèle · Points forts · Limites · Décision** :

| Modèle | Points forts | Limites | Décision |
|---|---|---|---|
| **OpenAI GPT-4o** | Appel d'outils performant, streaming, support multilingue, intégration avec LangChain4j | Dépendance à une API externe | **Sélectionné** |
| **Claude** | Raisonnement performant et utilisation des outils | Nécessite un fournisseur supplémentaire pour les embeddings | Non retenu |
| **Gemini** | Contexte long et support multilingue | Intégration plus complexe avec l'environnement Java | Non retenu |
| **Llama** | Possibilité d'auto-hébergement | Besoins élevés en infrastructure | Non retenu |

### 2. Objectif
Montrer que le choix du modèle est **argumenté** et non subi. C'est la diapositive qui prouve que vous avez évalué des alternatives.

### 🎤 3. Ce que je dois dire à l'oral

> « Le choix du modèle de langage a été guidé par les besoins spécifiques de l'assistant, plutôt que par un choix générique. Quatre critères ont pesé.
>
> D'abord les **sorties structurées** : trois fonctionnalités — la génération de media plans, les relances clients et les données de paie — produisent un résultat directement exploitable par l'application. Il fallait donc un format **JSON contraint par un schéma**.
>
> Ensuite la **fiabilité du tool calling** : certaines fonctionnalités nécessitent l'appel successif de plusieurs outils métier dans un ordre cohérent avec le processus.
>
> Puis la **qualité de génération en français**, puisque les réponses sont lues par des humains — un client qui reçoit une relance, un employé qui lit l'explication de sa paie.
>
> Et enfin une **fenêtre de contexte suffisamment large**, pour traiter simultanément les informations de la marque, du projet, l'historique des media plans et les résultats du RAG.
>
> J'ai comparé quatre modèles. **Claude** a de bonnes capacités de raisonnement, mais l'intégration des embeddings aurait nécessité le recours à un **autre fournisseur** — ce qui aurait ajouté une dépendance. **Gemini** offre une grande fenêtre de contexte, mais son intégration avec l'environnement Java est plus complexe. **Llama** permettrait l'auto-hébergement, donc la souveraineté, mais avec des besoins en infrastructure trop importants pour ce projet.
>
> J'ai retenu **GPT-4o**, pour la combinaison de ces quatre critères et pour son **intégration avec LangChain4j**. Sa limite est réelle et je l'assume : la **dépendance à une API externe**.
>
> Un cinquième critère, transverse, a d'ailleurs orienté l'architecture plus que le choix du modèle lui-même : la **réversibilité**. C'est précisément pour cela que je passe par **LangChain4j** comme couche d'abstraction, plutôt que par un appel HTTP direct — changer de modèle, y compris de fournisseur, ne demande de modifier **qu'un paramètre de configuration**, jamais le code métier. »

*(≈ 1 min 30. **Le dernier paragraphe est votre meilleur argument** — ne l'oubliez pas.)*

### ⚠️ 4. Une différence à connaître entre votre PPT et votre rapport

**Votre rapport (tableau 7.1) compare CINQ modèles, la PPT n'en montre que QUATRE.**

| Modèle | Rapport | PPT | Décision (rapport) |
|---|---|---|---|
| GPT-4o | ✅ | ✅ | **Sélectionné** |
| Claude | ✅ | ✅ | Rejeté |
| Gemini | ✅ | ✅ | Rejeté |
| **Mistral Large** | ✅ | ❌ **absent** | **Alternative** |
| Llama | ✅ | ✅ | Rejeté |

**Mistral Large** est décrit dans le rapport comme : *« Bonne prise en charge du français et possibilité d'hébergement en Europe »*, avec pour limite *« Intégration moins mature avec la pile technologique du projet »*, et **classé "Alternative"** — ni sélectionné, ni rejeté.

💡 **C'est en réalité une bonne nouvelle : Mistral est votre meilleure réponse à la question sur la souveraineté des données.** Si le jury vous interroge sur l'hébergement hors Europe, citez-le spontanément : *« Mon rapport identifie d'ailleurs Mistral Large comme une alternative crédible, précisément pour sa possibilité d'hébergement en Europe. »*

**Si le jury note l'absence :** *« Mon rapport compare cinq modèles ; la diapositive n'en reprend que quatre par souci de lisibilité. Le cinquième est Mistral Large, classé "alternative" plutôt que rejeté — c'est la piste que je retiendrais si la souveraineté des données devenait une exigence. »*

### 🔧 5. Les quatre critères de sélection (§7.2.1) — à connaître

| Critère | Pourquoi il compte dans ce projet |
|---|---|
| **Sorties structurées** | Media plans, relances et données de paie doivent être **directement exploitables par l'application** — un JSON contraint par un schéma, pas du texte libre à reparser |
| **Fiabilité du tool calling** | Certaines fonctionnalités enchaînent **plusieurs outils** dans un ordre imposé par le processus métier |
| **Qualité en français** | Les livrables sont lus par des humains : un client qui reçoit une relance, un employé qui lit sa paie |
| **Fenêtre de contexte** | Il faut traiter **simultanément** identité de la marque, données du projet, historique des media plans, référentiels et résultats du RAG |
| **+ Réversibilité** *(transverse)* | LangChain4j comme couche d'abstraction → changer de modèle = **un paramètre de configuration** |

### 📌 6. À retenir
- **4 modèles sur la PPT, 5 dans le rapport** (Mistral Large manque).
- **GPT-4o sélectionné** · Claude, Gemini, Llama rejetés · **Mistral Large = alternative**.
- Les **raisons de rejet** : Claude → embeddings chez un autre fournisseur · Gemini → intégration Java complexe · Llama → infrastructure.
- **La limite de GPT-4o : dépendance à une API externe** — assumez-la.
- Les **4 critères** + la **réversibilité** via LangChain4j.

### ❓ Questions du jury

**Q1 — « Pourquoi GPT-4o ? »**
> Les quatre critères + l'intégration LangChain4j. **Question certaine.**

**Q2 — « Pourquoi avoir rejeté Claude ? »**
> « Pour une raison précise et purement architecturale : **l'intégration des embeddings aurait nécessité le recours à un fournisseur supplémentaire**. Le RAG hybride a besoin d'un modèle d'embedding pour la recherche sémantique. Avec GPT-4o, chat et embeddings viennent du même fournisseur, donc une seule intégration et une seule clé. Avec Claude, il aurait fallu ajouter un second fournisseur — donc une dépendance de plus. »

**Q3 — « Pourquoi pas Llama en auto-hébergé ? Cela règlerait la dépendance externe. »**
> « C'est exact, et c'est le seul avantage que je lui reconnais : la possibilité d'auto-hébergement, donc la souveraineté. Mais les **besoins en infrastructure sont élevés** — il faut du GPU et une disponibilité à assurer. Pour une agence de cette taille et un volume d'usage modéré, c'est disproportionné. C'est un arbitrage explicite, pas un oubli. »

**Q4 — « Vous envoyez des données RH et financières à un service américain. Est-ce acceptable ? »**
> **Question sérieuse — préparez-la.** « C'est une question légitime. Trois éléments de réponse. D'abord, ce qui **transite est limité** : le modèle ne reçoit jamais la base, mais un contexte déjà filtré par le contrôle d'accès et déjà calculé côté Java. Ensuite, l'architecture est **réversible** : c'est précisément pour cela que je passe par LangChain4j — changer de fournisseur ne demande de modifier qu'un paramètre de configuration. Enfin, mon rapport identifie explicitement **Mistral Large** comme alternative, avec pour argument la possibilité d'**hébergement en Europe**. Si la souveraineté devenait une exigence, le chemin technique existe et il est court.
>
> Cela dit, je n'ai pas traité la conformité réglementaire du traitement — c'est un préalable que je signalerais avant tout déploiement avec des données de production. »

**Q5 — « Qu'est-ce que LangChain4j ? »**
> « Une bibliothèque Java qui facilite l'intégration des modèles de langage et des systèmes RAG. Elle joue deux rôles dans mon projet. D'abord, elle fournit les briques d'orchestration — agents, appel d'outils, gestion de la mémoire. Ensuite, et c'est le plus important, elle sert de **couche d'abstraction** : mon code métier ne parle jamais directement à l'API d'OpenAI, il parle à LangChain4j. C'est ce qui rend le fournisseur substituable. »

**Q6 — « Qu'est-ce qu'une "sortie structurée" ? »**
> « Au lieu de demander au modèle du texte libre qu'il faudrait ensuite analyser, on lui impose un **schéma JSON**. Le résultat est directement exploitable par l'application. C'est essentiel pour la génération d'un media plan : je dois obtenir des lignes avec un contenu, un format, une plateforme et une date, pas un paragraphe de prose qu'il faudrait découper. »

**Q7 — « Que signifie "fenêtre de contexte" ? »**
> « La quantité de texte que le modèle peut prendre en compte en une seule fois — prompt et réponse compris. Elle compte ici parce que le contexte d'une génération de media plan est volumineux : l'identité de la marque, les données du projet, l'historique des media plans précédents, les référentiels, et les résultats de la recherche hybride. Une fenêtre trop étroite obligerait à tronquer, donc à dégrader la qualité. »

### ⚠️ Questions pièges

**« Avez-vous mesuré la qualité des réponses de chaque modèle ? »**
> **Réponse honnête :** « Non, je n'ai pas mené de benchmark comparatif quantitatif, et je ne vais pas prétendre le contraire. Ma comparaison est **qualitative**, fondée sur les caractéristiques techniques de chaque modèle par rapport aux quatre critères de mon projet — support des sorties structurées, fiabilité du tool calling, qualité en français, fenêtre de contexte — et sur la compatibilité avec ma pile technologique. Un benchmark chiffré aurait demandé un jeu de test de référence que je n'ai pas construit ; c'est d'ailleurs une limite que je reconnais aussi sur l'évaluation de mon RAG. »

**« GPT-4o évolue vite. Votre choix sera-t-il encore valide dans six mois ? »**
> « Probablement pas, et c'est exactement pour cela que la **réversibilité** est mon cinquième critère — celui qui a orienté l'architecture plus que le choix du modèle lui-même. En passant par LangChain4j comme couche d'abstraction plutôt que par un appel HTTP direct, changer de modèle, y compris de fournisseur, ne demande de modifier **qu'un paramètre de configuration, jamais le code métier**. Le choix du modèle est donc volontairement le maillon le moins structurant de mon architecture. »
> 💡 **Excellente réponse** : elle transforme une fragilité apparente en décision de conception.

### 📖 Lien avec le rapport
**§7.2.1** Choix du modèle de langage (les 4 critères + la réversibilité) · **§7.2.2 et tableau 7.1** — Comparaison des modèles candidats (**5 modèles**) · **§7.2.3** Modèle choisi · **§7.2.4 et tableau 7.2** — Configuration du modèle.

### 🔗 Liens avec les autres diapositives
LangChain4j figure sur la **diapositive 31**. L'architecture qu'il orchestre est celle de la **diapositive 29**. Le comportement sans clé API est visible sur la **22**.

### ➡️ Transition
> « Voici l'ensemble de la pile technologique du projet. »

---

## Diapositive 31 — Technologies utilisées

### 1. Ce que contient la diapositive
Six catégories numérotées, chaque technologie avec son rôle :

| № | Catégorie | Technologies et rôles |
|---|---|---|
| **1** | **Frontend** | **React 19** (bibliothèque interface) · **TypeScript** (typage statique) · **Vite** (bundler & dev server) |
| **2** | **Backend** | **Java 17** (logique métier) · **Spring Boot 3** (API REST) · **Spring Data JPA** (persistance) · **Maven** (build) |
| **3** | **Sécurité** | **Spring Security** (filtre d'authentification) · **JWT + BCrypt** (jetons & mots de passe hachés) · **RBAC** (permissions granulaires) |
| **4** | **Données** | **PostgreSQL** (base unique) · **pgvector** (recherche vectorielle) · **Schéma dwh** (entrepôt en étoile) |
| **5** | **IA** | **GPT-4o** (génération & classification) · **LangChain4j** (orchestration d'agents) · **RAG hybride** (réponses ancrées, jamais inventées) |
| **6** | **Outils de développement** | **Git / GitHub** (gestion de versions) · **VS Code** (éditeur de code) · **Postman** (tests des points d'entrée API) |

**Bandeau :** « De l'interface à l'intelligence »

### 2. Objectif
Poser la pile complète en une image. **À passer vite** — mais chaque ligne peut déclencher une question, donc chaque ligne doit être défendable.

### 🎤 3. Ce que je dois dire à l'oral

> « Voici la pile technologique, organisée en six catégories.
>
> Côté **frontend** : React 19 pour l'interface, TypeScript pour le typage statique, et Vite comme bundler et serveur de développement.
>
> Côté **backend** : Java 17 pour la logique métier, Spring Boot 3 pour l'API REST, Spring Data JPA pour la persistance, et Maven pour le build.
>
> Pour la **sécurité** : Spring Security comme filtre d'authentification, JWT et BCrypt pour les jetons et les mots de passe hachés, et le RBAC pour les permissions granulaires.
>
> Pour les **données** : PostgreSQL comme base unique, l'extension pgvector pour la recherche vectorielle, et le schéma `dwh` pour l'entrepôt en étoile. **Une seule base couvre les trois usages** : transactionnel, vectoriel et analytique.
>
> Pour l'**IA** : GPT-4o pour la génération et la classification, LangChain4j pour l'orchestration des agents, et le RAG hybride pour des réponses ancrées et jamais inventées.
>
> Et côté **outils** : Git et GitHub, VS Code, et Postman pour tester les points d'entrée de l'API.
>
> Le fil conducteur, c'est ce que résume le bandeau : **de l'interface à l'intelligence**. »

*(≈ 1 minute. **Ne vous attardez pas.**)*

### 🔧 4. Tableau de défense — une phrase par technologie

| Technologie | Pourquoi ce choix | Alternative | Pourquoi écartée |
|---|---|---|---|
| **React 19** | Composants réutilisables, interfaces réactives, large écosystème | Angular, Vue | *Non comparées dans le rapport* — n'inventez pas d'étude comparative |
| **TypeScript** | **Typage statique** → erreurs détectées à la compilation ; contrats d'API typés entre 3 interfaces | JavaScript | Sur trois applications consommant la même API, le typage évite les erreurs de contrat |
| **Vite** | Build rapide, **HMR** quasi instantané | Webpack, CRA | Temps de rebuild sensiblement plus long |
| **Java 17** | Version **LTS**, socle de Spring Boot 3, typage fort adapté à des règles métier complexes | Node.js, .NET | *Non comparées* |
| **Spring Boot 3** | Socle complet : configuration, injection de dépendances, sécurité, persistance, ordonnancement | Assemblage manuel | Intégration de plusieurs briques au lieu d'un socle cohérent |
| **Spring Data JPA** | Génère les requêtes standards, réduit le code répétitif | JDBC manuel | Volume de code répétitif considérable |
| **Maven** | Standard Java, gestion des dépendances et du cycle de build | Gradle | *Non comparé* |
| **Spring Security** | Filtre d'authentification intercepté **avant les contrôleurs** | Sécurité maison | Réimplémenter une chaîne de filtres est une source classique de failles |
| **JWT** | Authentification **sans état**, adaptée à 4 clients distincts | Session serveur | État serveur à maintenir, cookies inter-domaines |
| **BCrypt** | **Hachage** à sens unique, avec sel et coût élevé | Stockage en clair, MD5/SHA simple | Réversible ou trop rapide à casser |
| **RBAC** | Permissions **granulaires** plutôt que rôle binaire | Rôles en dur | Ne permet pas de créer un rôle sans modifier le code |
| **PostgreSQL** | **Une seule base** pour le relationnel, le vectoriel (pgvector) et l'analytique (schéma `dwh`) | MySQL | Pas de recherche vectorielle native → base vectorielle séparée nécessaire |
| **pgvector** | Recherche vectorielle **dans PostgreSQL** | Pinecone, Qdrant | Un service de plus à déployer, sécuriser et synchroniser |
| **LangChain4j** | Orchestration d'agents **et couche d'abstraction** → fournisseur substituable | Appel HTTP direct | Il faudrait réimplémenter la boucle d'outils, la mémoire, le streaming — et le code serait couplé au fournisseur |
| **Postman** | Test manuel outillé des endpoints REST | — | Complété par **Swagger** pour la documentation (cité dans le rapport) |

### ⚠️ 5. Les technologies du rapport absentes de la diapositive

Le rapport (§2.4.2) en cite plusieurs de plus. **Sachez répondre si le jury demande « où est… ? »**

| Technologie | Rôle | Où dans le rapport |
|---|---|---|
| **Draw.io** | Diagrammes UML et schémas fonctionnels | Tableau 2.11 |
| **Swagger** | **Documentation et test des services RESTful** | Tableau 2.11 |
| **JavaScript / Electron** | **AgentDesktop** (agent de bureau) | Tableau 2.12 |
| **SQL** | Modélisation et requêtes PostgreSQL | Tableau 2.12 |
| **Power BI** | Exploitation de l'entrepôt, tableaux de bord interactifs | Tableau 2.14 |
| **DAX** | Langage de mesures de Power BI | Tableau 2.14 |
| **Axios** | Communication HTTP des SPA avec l'API | §2.2.2.1 |
| **React Router** | Navigation dans les SPA | §2.2.2.1 |
| **Brevo** | E-mails transactionnels | Figure 2.9 |
| **Google Drive API** | Stockage des fichiers volumineux (OAuth2) | §2.2.2.1 |

💡 **Les trois omissions les plus notables sont Power BI, Electron et Swagger** — ce sont des technologies visibles ailleurs dans votre présentation (diapositives 22 et 28). Si le jury demande « je ne vois pas Power BI dans vos technologies », répondez : *« Cette diapositive reprend la pile principale ; Power BI et DAX figurent dans mon rapport, au tableau des outils de l'informatique décisionnelle, et le résultat est visible sur ma diapositive 28. »*

### 📌 6. À retenir
- **6 catégories** : Frontend · Backend · Sécurité · Données · IA · Outils.
- **PostgreSQL = base unique** pour trois usages : relationnel, vectoriel (pgvector), analytique (`dwh`).
- **BCrypt = hachage**, jamais « cryptage ».
- **LangChain4j = orchestration ET abstraction** du fournisseur.
- ⚠️ **Power BI, Electron, Swagger, Axios, Brevo** sont dans le rapport mais pas sur la diapositive.

### ❓ Questions du jury

**Q1 — « Pourquoi PostgreSQL et pas MySQL ? »**
> « La raison décisive est qu'il m'a permis de tout faire avec **un seul système de gestion de base de données**. PostgreSQL porte trois choses que j'aurais sinon dû aller chercher ailleurs : le **relationnel** classique, la **recherche vectorielle** avec l'extension pgvector — indispensable au RAG — et l'**entrepôt analytique** dans un schéma dédié. Avec MySQL, il m'aurait fallu une base vectorielle séparée : un service de plus à déployer, sécuriser et synchroniser. »

**Q2 — « Différence entre hachage et chiffrement ? »**
> « Le chiffrement est **réversible** : avec la clé, on retrouve la donnée. Le hachage est **à sens unique** : on ne peut pas remonter au mot de passe depuis son empreinte. C'est exactement ce qu'on veut pour un mot de passe — même l'administrateur de la base ne peut pas les lire. À la connexion, on ne déchiffre rien : on rehache la saisie et on compare les deux empreintes. »
> ⚠️ **Ne dites JAMAIS « les mots de passe sont cryptés ».** Dites **« hachés »**.

**Q3 — « Qu'apporte TypeScript par rapport à JavaScript ? »**
> « Le typage statique, donc la détection d'erreurs à la compilation plutôt qu'à l'exécution. C'est particulièrement utile ici : **trois applications consomment la même API**. Si je modifie la forme d'une donnée côté backend, le typage me signale immédiatement les appels devenus incohérents dans les trois interfaces, plutôt qu'une erreur en production sur une propriété indéfinie. »

**Q4 — « Qu'est-ce que le RBAC ? »**
> « *Role-Based Access Control* — le contrôle d'accès fondé sur les rôles. Dans mon implémentation, un compte porte des rôles, et chaque rôle **regroupe des permissions**. Le point important : le système ne teste jamais "cet utilisateur est-il administrateur", mais "cet utilisateur a-t-il cette permission". C'est ce qui permet à l'administrateur de créer un nouveau rôle et de lui attribuer des permissions **sans qu'une ligne de code change**. »

**Q5 — « Qu'est-ce que Spring Data JPA ? »**
> « Une couche au-dessus de JPA qui génère automatiquement les requêtes à partir du nom des méthodes de mes interfaces de repository. Je déclare une interface, Spring en génère l'implémentation — je n'écris pas le SQL pour les cas standards. JPA est la spécification Java de la correspondance objet-relationnel, Hibernate en est l'implémentation. »

**Q6 — « Java 17, pourquoi pas une version plus récente ? »**
> « Parce que c'est une version **LTS**, à support long terme, et que c'est la version sur laquelle repose Spring Boot 3. Pour un projet destiné à être repris par une autre équipe, le critère n'est pas d'avoir la version la plus récente mais une version stable et supportée durablement. »

**Q7 — « Avez-vous utilisé des tests automatisés ? »**
> **Réponse honnête :** « Chaque user story de mes backlogs de sprint comporte une tâche "Tester la fonctionnalité", et les services REST ont été testés avec **Postman**, documentés avec **Swagger**. Il s'agit principalement de **test manuel outillé**. Je n'ai pas mis en place de suite de tests automatisés avec mesure de couverture, et je ne vais pas prétendre le contraire — c'est une limite que je reconnais. »

**Q8 — « Où est Power BI dans vos technologies ? »**
> Voir §5.

### ⚠️ Questions pièges

**« Maîtrisiez-vous toutes ces technologies avant le projet ? »**
> « Une partie oui — Java, Spring Boot et React étaient dans ma formation. D'autres non : **LangChain4j, le RAG, pgvector et la modélisation dimensionnelle** ont été apprises pendant le projet. C'est d'ailleurs sur ces parties que le travail d'ingénierie a été le plus dense, parce qu'il a fallu comprendre le mécanisme avant de pouvoir l'adapter — par exemple comprendre pourquoi on ne peut pas additionner les scores de deux moteurs de recherche différents, ce qui m'a conduite à la fusion par rang réciproque. »

**« Cette pile n'est-elle pas surdimensionnée pour une agence de cette taille ? »**
> « Les briques ajoutées le sont chacune pour une raison fonctionnelle précise, pas par effet de mode. **pgvector** n'est là que parce que le RAG a besoin de recherche vectorielle — et il évite justement d'ajouter une base de données dédiée. Le **schéma `dwh`** n'est là que parce que l'absence de vision consolidée était un constat explicite de l'étude de l'existant — et il vit dans la même instance PostgreSQL. Ce qui aurait été surdimensionné, ce serait une architecture microservices ou une base vectorielle séparée : je les ai écartées toutes les deux. »
> 💡 **Réponse forte** : elle montre que vous avez arbitré la complexité à chaque étape.

### 📖 Lien avec le rapport
**§2.4.2 Environnement logiciel** · **tableau 2.11** (outils) · **tableau 2.12** (langages) · **tableau 2.13** (frameworks) · **tableau 2.14** (outils décisionnels) · §2.4.1 (environnement matériel).

### 🔗 Liens avec les autres diapositives
Détaille la pile des architectures des **diapositives 21 et 22**. GPT-4o et LangChain4j sont justifiés en **30**. pgvector sert le RAG de la **29**. Le schéma `dwh` est celui de la **28**.

### ➡️ Transition
> « Je vous propose maintenant de voir le système fonctionner. »

---
## Diapositive 32 — *Intercalaire : Démonstration*

Titre seul.

### 🎤 Ce que je dois dire
> « Je vous propose maintenant une démonstration. »

### ⚠️ Préparation de la démonstration — **le moment le plus risqué de votre soutenance**

**Votre PPT ne détaille pas le déroulé de la démonstration.** Vous devez donc le préparer et le chronométrer vous-même.

#### Déroulé recommandé — suivez l'ordre de vos modules phares

| № | Parcours | Durée cible | Ce qu'il faut absolument montrer |
|---|---|---|---|
| 1 | **Parcours employé** | 1 min 30 | Connexion · dépôt d'une demande de congé · **le calcul automatique du nombre de jours** · validation par le responsable · **le solde qui diminue** |
| 2 | **Plan média** | 1 min 30 | Création d'une ligne · validation interne · **validation client depuis le portail** · **la création automatique du projet et des 3 tâches** |
| 3 | **Chaîne financière** | 1 min 30 | Création d'une facture · **encaissement partiel → statut "Partiel"** · solde → **"Payé"** · génération d'un bulletin de paie |
| 4 | **Assistant IA** | 1 min 30 | Question sur le règlement intérieur → **la réponse cite l'article** · puis explication d'un bulletin de paie |
| 5 | **Power BI** *(si le temps le permet)* | 1 min | Un tableau de bord analytique |

💡 **Les trois moments à ne surtout pas manquer** — ce sont vos meilleurs effets :
1. **La création automatique du projet + 3 tâches** à la validation du créneau → c'est votre workflow le plus impressionnant.
2. **Le statut de facture qui passe de Partiel à Payé** → démontre le recalcul automatique.
3. **L'assistant qui cite un article exact du règlement** → preuve visible que le RAG fonctionne.

#### Checklist la veille

- [ ] **Backend démarré** (`./mvnw spring-boot:run`, port 8080) et testé
- [ ] **Les trois serveurs Vite lancés** (ports 3000, 3001, 3002)
- [ ] **PostgreSQL local démarré**, base `antigone_rh` peuplée de données réalistes
- [ ] **Clé API OpenAI valide et quota disponible** — sinon l'assistant répondra **503** en direct
- [ ] **Index RAG réindexé** la veille
- [ ] Comptes de démonstration testés : **employé, validateur, chef de projet, comptable, administrateur, client**
- [ ] **Mots de passe notés** — vous ne devez pas chercher un mot de passe devant le jury
- [ ] **Onglets pré-ouverts et pré-connectés** : un par application
- [ ] **Captures d'écran ou vidéo de secours** prêtes en deux clics

#### 🚨 Le plan B — préparez-le vraiment

> **Si le backend ne répond pas, si l'API est indisponible, si le réseau tombe :**
> « La démonstration en direct n'est pas disponible, je vais donc vous présenter le parcours à partir de captures d'écran. »
>
> **Puis enchaînez sans vous excuser davantage.** Un jury ne pénalise pas une panne technique ; il pénalise une candidate qui perd ses moyens pendant trois minutes.

💡 **Une vidéo d'écran enregistrée à l'avance pour chaque parcours est le meilleur investissement possible.** Votre rapport contient déjà toutes les captures (figures 3.4 à 3.29, 4.4 à 4.18, 5.4 à 5.12, 6.4 à 6.12, 7.6 à 7.11, 8.5 à 8.8) — vous avez donc déjà un support de secours complet.

#### Trois erreurs à éviter

| ⚠️ Erreur | Conséquence |
|---|---|
| **Commenter chaque clic** (« je clique ici, puis là… ») | Le jury voit vos clics. Commentez le **résultat métier** : « le solde est passé de 18 à 15 jours, la déduction a été faite côté serveur à l'approbation » |
| **Improviser une fonctionnalité non répétée** | Si le jury demande « montrez-moi X » et que X n'est pas dans votre déroulé : « Je peux vous le montrer, laissez-moi un instant » — **ne lancez pas une navigation hasardeuse** |
| **Dépasser le temps** | La démonstration mord sur vos questions. **Chronométrez-la au moins deux fois en conditions réelles.** |

### ❓ Questions probables pendant la démonstration

**« Montrez-moi ce qui se passe avec une donnée invalide. »**
> Montrez un cas d'erreur métier maîtrisé : un congé dépassant le solde, ou un refus de demande sans motif. **Le message d'erreur vient du serveur** — dites-le.

**« Le chatbot peut-il me donner le salaire d'un autre employé ? »**
> **Faites-le en direct** si vous êtes connectée avec un compte employé non administrateur. C'est la démonstration la plus convaincante de votre projet. ⚠️ **Répétez-la avant** et vérifiez le message exact retourné.

**« Où voit-on que le chiffre n'est pas inventé ? »**
> Montrez la **cohérence** entre l'écran Finance et l'explication de l'assistant : le même net à payer, au centime. Puis expliquez que les calculs sont faits en Java et transmis au modèle comme données à reformuler.

---

## Diapositive 33 — Conclusion

### 1. Ce que contient la diapositive
Trois affirmations :
- **Plateforme intégrée de gestion pour une agence digitale**
- **Fusion entre gestion RH, projets, finance, IA et BI**
- **Automatisation des processus et pilotage intelligent par la donnée**

### 2. Objectif
Laisser au jury **une seule idée forte** plutôt qu'un inventaire. C'est la dernière chose qu'il entendra avant les questions.

### 🎤 3. Ce que je dois dire à l'oral

> « Pour conclure, je retiendrais trois choses de ce projet.
>
> D'abord, ce qui a été livré : une **plateforme intégrée de gestion pour une agence digitale**, couvrant l'intégralité du périmètre initial — les identités, l'organisation RH, les projets et plans médias, la chaîne financière, l'assistant IA et l'informatique décisionnelle.
>
> Ensuite, ce qui en fait la spécificité : la **fusion** entre gestion RH, projets, finance, IA et BI. Là où le marché juxtapose des outils cloisonnés — et l'étude de l'existant l'a montré : Odoo, BambooHR et monday.com couvrent chacun un seul pan du métier — Antigone 360° les réunit dans une seule plateforme, sur une base de données unique. C'est cette unité qui supprime la double saisie et qui rend possible aussi bien l'assistant que l'analyse décisionnelle.
>
> Enfin, ce que cela change au quotidien : l'**automatisation des processus** — le calcul des soldes de congés, la paie, la TVA, les workflows de validation remplacent des tâches auparavant faites à la main — et le **pilotage intelligent par la donnée**, avec des tableaux de bord qui donnent enfin à la direction une vision consolidée de l'activité.
>
> Et un principe a traversé l'ensemble du projet : **la logique métier sensible reste portée par le backend — jamais déléguée au frontend, ni au modèle de langage.** Que ce soit le calcul du solde de congé, la cascade de détermination d'un statut de présence, le barème IRPP ou l'explication d'un bulletin par l'assistant, l'interface et le modèle ne font que restituer un résultat déjà calculé et déjà validé. »

*(≈ 1 min 20. **Ralentissez sur la dernière phrase** — c'est le moment où vous devez être la plus posée.)*

### 🔧 4. Comment défendre le principe directeur

**Si le jury demande « donnez-moi trois exemples concrets » :**

| Module | Ce qui reste côté serveur | Ce que ferait un système mal conçu |
|---|---|---|
| **Congés** | Le calcul du nombre de jours combine horaire et calendrier, **côté serveur** | Faire confiance à un décompte calculé côté client, donc modifiable |
| **Pointage** | La cascade de décision est **calculée en continu, côté serveur** | Laisser l'interface interpréter les statuts, avec un risque de divergence entre les trois applications |
| **Assistant IA** | Les calculs sont faits en Java, transmis comme **données à reformuler** | Laisser le modèle recalculer un net à payer — donc accepter qu'il puisse se tromper |

💡 **Le contre-exemple est plus parlant que l'exemple** : dire ce qu'un système mal conçu ferait montre que vous avez compris l'enjeu, pas seulement appliqué une règle.

### 📌 5. À retenir
- Les **trois affirmations** de la diapositive.
- **La phrase du principe directeur.**
- Les **trois exemples** et leurs contre-exemples.
- Le lien avec l'étude de l'existant : **la fusion est ce qu'aucun outil du marché ne propose**.

### ❓ Questions du jury

**Q1 — « Quel est l'apport principal de ce projet ? »**
> « Pour l'agence, c'est la **fin de la double saisie** : les trois domaines partagent une base unique, donc un client créé côté projet est le même que celui destinataire d'une facture. C'était le constat central de la problématique. S'y ajoutent deux automatisations coûteuses à faire à la main : le calcul des jours de congé et la paie tunisienne.
>
> Sur le plan technique, l'apport que je retiens est d'avoir conçu une architecture d'assistant IA qui **garantit structurellement l'exactitude des chiffres** — en séparant le calcul, fait en Java, de la rédaction, faite par le modèle. »

**Q2 — « Qu'avez-vous appris de ce projet ? »**
> **Question très fréquente — personnalisez-la.** Pistes : un domaine technique nouveau (RAG, pgvector, modélisation dimensionnelle) · une leçon méthodologique (le découpage en releases qui ne se retouchent pas) · une leçon de rigueur (documenter ses propres limites plutôt que les laisser découvrir).

**Q3 — « Si vous deviez recommencer, que feriez-vous différemment ? »**
> **Réponse à préparer honnêtement.** Pistes tirées de vos propres sources : mettre en place une **stratégie de tests automatisés** dès le départ, plutôt que du test manuel via Postman · **conteneuriser l'environnement** avec Docker pour garantir la reproductibilité entre les deux développeuses · construire un **jeu de test de référence pour évaluer le RAG**, dont je n'ai aujourd'hui aucune mesure objective.

**Q4 — « Le projet est-il réellement utilisé par l'agence ? »**
> Répondez **factuellement** selon la réalité. Si l'exploitation en conditions réelles n'a pas commencé, dites-le plutôt que de laisser entendre le contraire — c'est vérifiable en une question de suivi.

**Q5 — « Votre principe directeur a-t-il eu un coût ? »**
> **Excellente question à saisir.** « Oui. Recalculer côté serveur plutôt que de faire confiance au client ajoute des allers-retours réseau — négligeable à l'échelle d'une action humaine, mais réel. Et pour l'assistant, préparer un contexte entièrement pré-calculé en Java demande plus de code que de laisser le modèle interroger librement. C'est un arbitrage assumé : j'ai choisi la solution plus coûteuse en développement parce qu'une erreur arithmétique sur un salaire n'est pas acceptable. »

### ⚠️ Questions pièges

**« Vous dites couvrir tout le périmètre initial. L'IA et la BI en faisaient-elles partie dès le départ ? »**
> **Réponse honnête :** « Le backlog produit initial comportait bien des modules d'assistant, mais leur maturité finale dépasse ce qui était prévu au cadrage, et l'informatique décisionnelle a été ajoutée une fois les modules métier stabilisés — c'est-à-dire une fois les données disponibles pour l'analyse. C'est précisément ce que la méthode agile permet : le backlog reste un document vivant, et la capacité libérée par les livraisons en temps a été réinvestie. »

**« Six releases en six mois à deux développeuses, tout est-il au même niveau de finition ? »**
> « Non, et les chiffres le montrent : les quatre modules métier représentent **huit sprints sur dix**, soit 205 points sur 254. L'assistant IA pèse 32 points et le décisionnel 16. Ce sont des briques abouties dans leur périmètre, mais moins profondes fonctionnellement que les modules métier. Je les présente comme telles. »

### 📖 Lien avec le rapport
**Conclusion et perspectives** (p. 155) · §1.4.5 Apports de la solution.

### 🔗 Liens avec les autres diapositives
Répond à la problématique de la **diapositive 5** et au constat de la **diapositive 7**. Reprend les cinq blocs de la **diapositive 8**.

### ➡️ Transition
> « Ce travail ouvre plusieurs pistes d'évolution. »

---

## Diapositive 34 — Perspectives

### 1. Ce que contient la diapositive
Trois perspectives :
- **Application mobile**
- **Tableaux de bord consolidés**
- **Déploiement sur le Cloud**

### 2. Objectif
Montrer que vous avez une **vision au-delà du livrable**, et que vous savez distinguer ce qui est fait de ce qui reste à faire.

### 🎤 3. Ce que je dois dire à l'oral

> « Le projet livré constitue une base fonctionnelle complète, mais trois directions permettraient de le faire grandir.
>
> Une **application mobile**, d'abord. Ce serait le prolongement le plus naturel : elle donnerait aux employés en déplacement un accès direct au pointage, aux demandes de congé et aux notifications, et permettrait aux responsables de valider une demande sans être devant un poste. L'API REST existante serait consommée telle quelle — c'est le bénéfice d'une architecture N-Tiers avec une couche métier indépendante de ses clients.
>
> Des **tableaux de bord consolidés**, ensuite. Aujourd'hui, les pages Analytique sont une par domaine — RH, Finance, Projets. Une vue unique croisant les trois donnerait à la direction une lecture d'ensemble de l'agence : charge des équipes, avancement des projets et santé financière dans un même écran. L'entrepôt le rend possible, puisque les dimensions sont partagées ; c'est la restitution qui reste à construire.
>
> Enfin, le **déploiement sur le Cloud**. C'est la perspective la plus immédiate, puisque l'architecture physique que je vous ai présentée est un environnement de développement. Passer en production demanderait de conteneuriser le backend, de compiler les frontends en fichiers statiques et d'héberger la base sur une instance managée. »

*(≈ 1 min 10.)*

### 🔧 4. Savoir chiffrer l'effort de chaque perspective

**Le jury demandera probablement « par laquelle commenceriez-vous ? » ou « quelle difficulté ? ». Préparez une évaluation :**

| Perspective | Ce qui existe déjà | Ce qu'il faudrait construire | Difficulté principale |
|---|---|---|---|
| **Mobile** | **L'API REST complète** — un client mobile la consommerait telle quelle | Une application native ou hybride | Le **pointage** : l'agent actuel détecte le **réseau de l'entreprise**. Sur mobile, ce mécanisme ne s'applique pas — il faudrait une autre règle, par exemple une géolocalisation, ce qui pose à son tour des questions de vie privée. **C'est une révision de la règle métier, pas un simple portage.** |
| **Tableaux de bord consolidés** | **L'entrepôt `dwh`** avec ses dimensions partagées entre les six tables de faits | Une page transverse croisant les trois domaines | Le **modèle de sécurité** : une vue consolidée croise RH, Finance et Projets, alors que chaque domaine a aujourd'hui sa propre permission (`VIEW_MONITORING`, `VIEW_FINANCE`). Il faudrait décider **qui a droit à la vue croisée** |
| **Déploiement Cloud** | Backend Spring Boot autonome · frontends compilables en statique · PostgreSQL standard | Conteneurisation, hébergement, CI/CD, gestion des secrets | La **gestion des secrets** — clés OpenAI, identifiants Brevo et Google Drive — et la **configuration multi-environnements**. Moins un problème technique qu'un travail d'industrialisation |

💡 **Savoir dire laquelle est la plus simple et laquelle demande de revoir une règle métier est ce qui distingue une vraie perspective d'une liste d'intentions.**

### 📌 5. À retenir
- Les **3 perspectives** : mobile · tableaux de bord consolidés · Cloud.
- **L'API REST serait réutilisée telle quelle** par le mobile.
- **Le pointage mobile demanderait de revoir la règle métier**, pas seulement le code.
- Les tableaux de bord consolidés posent une **question de permissions**.
- Le Cloud est **la plus immédiate** puisque l'architecture actuelle est un environnement de développement.

### ❓ Questions du jury

**Q1 — « Par laquelle commenceriez-vous ? »**
> « Par le **déploiement Cloud**, pour une raison simple : c'est celle qui conditionne les autres. Tant que la plateforme tourne en environnement de développement, ni l'application mobile ni les tableaux de bord consolidés ne peuvent servir à qui que ce soit. C'est aussi la moins risquée fonctionnellement — l'architecture est conçue pour être déployable : backend autonome, frontends compilables en statique, base PostgreSQL standard.
>
> Cela dit, avant même les perspectives, je traiterais deux points de dette : mettre en place des **tests automatisés**, et construire un **jeu de test pour évaluer la qualité du RAG**, dont je n'ai aujourd'hui aucune mesure objective. »
> 💡 **Commencer par la dette technique plutôt que par les nouvelles fonctionnalités est une excellente réponse d'ingénieur.**

**Q2 — « L'application mobile : quelle technologie ? »**
> **Réponse honnête :** « Je n'ai pas arrêté de choix technologique — mon rapport présente cette perspective sur le plan fonctionnel, et je ne vais pas inventer une décision que je n'ai pas prise. Ce que je peux affirmer, c'est que **l'API REST existante serait consommée telle quelle**, sans modification : c'est précisément le bénéfice de l'architecture N-Tiers, où plusieurs clients consomment la même couche métier. Le choix se poserait entre du natif et une approche multiplateforme. »

**Q3 — « Le pointage fonctionnerait-il sur mobile ? »**
> Voir §4. **Excellente occasion de montrer que vous voyez la difficulté réelle** : l'agent actuel repose sur la détection du réseau de l'entreprise, ce qui ne se transpose pas sur mobile.

**Q4 — « Qu'entendez-vous par "tableaux de bord consolidés" ? Vous avez déjà des tableaux de bord. »**
> **Question probable — préparez-la.** « Ce sont deux choses différentes. Les pages Analytique livrées sont **une par domaine** : présence pour les RH, indicateurs financiers pour la finance, avancement pour les projets — chacune cloisonnée par sa permission. Ce que je vise en perspective, c'est une **vue unique croisant les trois** pour la direction : la charge des équipes, l'avancement des projets et la santé financière dans un même écran. L'entrepôt le rend possible puisque les dimensions sont partagées entre les tables de faits ; c'est la restitution et le modèle de droits qui restent à construire. »

**Q5 — « Pourquoi n'avez-vous pas déployé sur le Cloud pendant le projet ? »**
> **Réponse honnête :** « Le périmètre fonctionnel était déjà large — six releases sur dix sprints — et la priorité a été donnée à la couverture métier. Le déploiement en production relève de l'industrialisation : conteneurisation, CI/CD, gestion des secrets, configuration multi-environnements. C'est un chantier à part entière, que j'ai identifié et placé en perspective plutôt que de le traiter à moitié. »

**Q6 — « Ces trois perspectives suffisent-elles ? »**
> « Ce sont les trois que ma présentation retient. J'en ajouterais deux, qui relèvent plutôt de la dette technique que de l'évolution fonctionnelle : une **stratégie de tests automatisés** — aujourd'hui les tests sont manuels via Postman — et une **évaluation objective de la qualité du RAG**, pour laquelle il faudrait un jeu de questions-réponses de référence. »

### ⚠️ Questions pièges

**« Vous placez le déploiement Cloud en perspective. Votre projet n'est donc pas en production ? »**
> « Exact, et c'est ce que montre honnêtement mon architecture physique : des ports localhost, Vite en serveur de développement, l'agent en mode dev non empaqueté. Je ne prétends pas à une mise en production que je n'ai pas réalisée. L'architecture est conçue pour l'être — backend Spring Boot autonome, frontends compilables en statique, PostgreSQL standard — et c'est précisément pour cela que je place ce chantier en première perspective. »

**« Votre liste de perspectives ne mentionne aucune amélioration de l'IA ou de la BI. Sont-elles terminées ? »**
> « Non, et c'est une remarque juste. Côté IA, deux évolutions s'imposeraient : une **évaluation automatisée de la pertinence des réponses**, que je n'ai pas aujourd'hui, et une **indexation plus fréquente** — elle est quotidienne, donc une donnée créée dans la journée n'est pas immédiatement dans l'index de recherche. Côté BI, le **rechargement complet** de l'entrepôt ne passerait pas à l'échelle d'un volume très supérieur ; il faudrait alors passer à un chargement incrémental. Ce sont des limites que j'assume plutôt que des perspectives affichées. »

### 📖 Lien avec le rapport
**Conclusion et perspectives** (p. 155).

### 🔗 Liens avec les autres diapositives
Le Cloud répond à la nature « environnement de développement » de la **diapositive 22**. Les tableaux de bord consolidés s'appuient sur l'entrepôt de la **diapositive 28**. Le mobile réutiliserait l'API de la **diapositive 21**.

### ➡️ Transition
> « Je vous remercie de votre attention. »

---

## Diapositive 35 — Merci de votre attention

### 1. Ce que contient la diapositive
« Merci de votre Attention »

### 2. Objectif
Clore proprement et **laisser à l'écran une diapositive neutre** pendant toute la séance de questions — que le jury regardera potentiellement pendant vingt minutes.

### 🎤 3. Ce que je dois dire à l'oral
> « Je vous remercie de votre attention. Je suis à votre disposition pour répondre à vos questions. »

*(10 secondes. **N'ajoutez rien.** Ne résumez pas une nouvelle fois, ne vous excusez pas d'avoir été longue, ne commentez pas votre propre présentation.)*

### ⚠️ 4. Comment se comporter pendant la séance de questions

**C'est ici que se joue une part décisive de votre note. Six règles :**

| Règle | Détail |
|---|---|
| **1. Écoutez la question entière** | N'anticipez pas. Un jury interrompu reformule, et vous perdez du temps |
| **2. Reformulez si besoin** | « Si je comprends bien, vous me demandez comment je garantis que… ? » — cela vous donne deux secondes de réflexion et évite de répondre à côté |
| **3. Répondez d'abord, développez ensuite** | Commencez par la réponse directe en une phrase, puis développez. Ne construisez pas un raisonnement de trois minutes avant de conclure |
| **4. Revenez toujours à VOTRE projet** | Une question théorique appelle une réponse théorique **courte**, suivie de « dans mon projet, concrètement… » |
| **5. Dites « je ne sais pas » quand c'est le cas** | Voir la section 5 de la partie finale. C'est **toujours** mieux qu'une invention |
| **6. Ne vous excusez pas des limites** | Présentez-les comme des décisions ou des constats, pas comme des fautes |

**Si vous ne comprenez pas :** « Pourriez-vous préciser ce que vous entendez par… ? » — parfaitement acceptable.
**Si le jury vous contredit à tort :** restez factuelle. « Je comprends la remarque. Dans mon implémentation, c'est effectivement [X], et je peux vous montrer où cela se joue. »
**Si le jury vous contredit à raison :** reconnaissez immédiatement. « Vous avez raison, je n'avais pas vu cela sous cet angle. » C'est valorisant, pas dévalorisant.

### 📌 5. Les cinq phrases à avoir en tête pendant toute la séance

1. **« La logique métier sensible reste portée par le backend — jamais déléguée au frontend ni au modèle de langage. »**
2. **« Le modèle reformule un calcul déjà fait : il n'invente jamais un chiffre. »**
3. **« Chaque outil vérifie les droits d'accès avant toute opération — le contrôle est en Java, pas dans le prompt. »**
4. **« Cette partie n'a pas été mesurée dans le cadre de mon projet, donc je préfère ne pas avancer de chiffre non vérifié. »**
5. **« C'est une limite que j'ai identifiée et que j'assume. »**

---

# Partie finale — Préparation générale au jury

---

## 1. Questions générales sur le projet

### Contexte et problématique

**Q1 — « Présentez votre projet en deux minutes. »**
> « **Antigone 360°** est une plateforme intégrée développée pour Antigone, une agence de stratégie et de communication digitale basée à Tunis, fondée en 2021. L'agence gérait trois logiques très différentes — ses ressources humaines, ses projets clients et son suivi financier — dans des outils qui ne communiquaient pas : un tableau Excel pour la paie, une feuille Google Sheets ou un fil WhatsApp pour le suivi de projet, un cahier de comptabilité à part. La même information y était saisie plusieurs fois.
>
> La plateforme réunit en un seul système cinq blocs : les **ressources humaines** — congés, pointage, validations, douze types de demandes ; les **projets et plans médias**, avec un portail dédié par client ; la **finance**, avec une paie tunisienne automatisée et les déclarations CNSS et TVA ; un **assistant IA** qui répond en langage naturel sans jamais inventer un chiffre ; et une couche **décisionnelle** qui permet de lire des tendances.
>
> Techniquement, c'est une architecture **N-Tiers** : quatre interfaces clientes — trois applications React et un agent de bureau Electron — un backend Spring Boot exposant une API REST sécurisée par JWT et RBAC, et une base PostgreSQL unique qui porte à la fois le transactionnel, la recherche vectorielle via pgvector, et l'entrepôt analytique.
>
> Le projet a été mené sur six mois en **Scrum**, en dix sprints de deux semaines regroupés en six releases, avec un Product Owner et un Scrum Master côté agence et une équipe de deux développeuses. »

**Q2 — « Reformulez votre problématique. »**
> Celle de la PPT (diapositive 5). **Apprenez-la mot pour mot.**

**Q3 — « Quels étaient vos objectifs mesurables ? »**
> **Réponse honnête :** « Les objectifs étaient fonctionnels plutôt que chiffrés : couvrir le backlog produit sur les six releases prévues. Je n'avais pas d'objectif quantifié de type "réduire de X % le temps de traitement", et je n'ai donc pas de mesure d'atteinte à présenter. Ce que je peux affirmer, c'est que les six releases ont été livrées, et que le périmètre a été étendu en cours de projet avec l'informatique décisionnelle. »

**Q4 — « Qu'est-ce qui distingue votre solution d'un ERP du marché ? »**
> « Deux briques qu'aucune des trois solutions étudiées ne couvre — et mon tableau comparatif le montre avec trois "Non" alignés sur chacune. D'abord la **paie localisée pour la Tunisie** : CNSS, IRPP progressif avec barème versionné, contribution de solidarité, TFP, FOPROLOS, déclaration trimestrielle. Ensuite le **portail client avec approbation** : pas un simple accès en lecture, mais un circuit de décision où le client approuve ou refuse chaque ligne de plan média. »

### Architecture et conception

**Q5 — « Décrivez votre architecture. »**
> Voir diapositive 21. Les trois tiers, puis le rôle de chaque couche.

**Q6 — « Pourquoi N-Tiers et pas microservices ? »**
> Voir diapositive 21, §4a. **Le critère décisif : quatre clients pour une même couche métier**, et la complexité opérationnelle des microservices.

**Q7 — « Pourquoi pas un monolithe ? »**
> « Parce qu'un monolithe regroupe présentation, logique métier et données en une seule unité — or j'ai **quatre interfaces clientes distinctes**, dont une application de bureau. Mon rapport le formule ainsi : "l'architecture monolithique répond moins efficacement au besoin de plusieurs clients distincts". »

**Q8 — « Décrivez votre modèle de données. »**
> Voir diapositive 23. Les quatre points : RBAC, héritage sur `Demande`, centralité de l'`Employé`, `Référentiel` générique.

**Q9 — « Comment garantissez-vous l'intégrité des données ? »**
> « À trois niveaux. **Structurellement** : une base unique, donc pas de synchronisation entre copies — un client est la même ligne côté projet et côté facturation. **Par les contraintes** : cardinalités du modèle, un employé a au plus un compte, une facture appartient à un client. **Par les transactions** : les opérations composites — approuver un congé, déduire le solde, historiser la décision — forment une unité atomique. »

### Sécurité

**Q10 — « Comment sécurisez-vous votre application ? »**
> « Par quatre mécanismes, tous côté serveur. **Authentification** : jeton JWT, mots de passe hachés en BCrypt, changement obligatoire à la première connexion. **Filtrage systématique** : `JwtAuthenticationFilter` intercepte l'ensemble des requêtes **avant qu'elles n'atteignent les contrôleurs** — mon rapport précise que "ce filtrage garantit qu'aucune fonctionnalité métier n'est accessible sans authentification préalable". **Autorisation** : annotations `@PreAuthorize` vérifiant la permission requise, dans un modèle RBAC granulaire. **Traçabilité** : chaque décision enregistrée avec son auteur et sa date, chaque appel d'outil de l'assistant journalisé. »

**Q11 — « Quelles sont les faiblesses de sécurité de votre application ? »**
> **Donnez-les vous-même :** « Trois que j'identifie. D'abord, un **JWT volé reste valable jusqu'à son expiration** — il n'y a pas de liste de révocation, c'est la contrepartie assumée du sans-état. Ensuite, je **n'ai pas mené d'audit de sécurité ni de test d'intrusion** : je peux décrire mes mécanismes, pas prouver leur résistance. Enfin, la **conformité réglementaire du traitement** — notamment pour l'agent de présence et pour l'envoi de contexte à un service externe — n'a pas été traitée ; c'est un préalable avant tout déploiement réel. »

### Méthodologie

**Q12 — « Pourquoi Scrum ? »** · **Q13 — « Pourquoi pas Waterfall ? »** · **Q14 — « Pourquoi pas Kanban ou XP ? »**
> Voir diapositive 9, §4.

**Q15 — « Comment vous répartissiez-vous le travail avec l'autre développeuse ? »**
> 🚨 **Question quasi certaine — préparez-la vous-même**, cette information n'est pas dans le rapport.

### Fonctionnalités

**Q16 — « Quelle est la fonctionnalité la plus complexe ? »**
> « Deux candidates, selon le critère. Sur le plan **algorithmique**, le moteur de paie : l'IRPP progressif interdit l'inversion analytique, ce qui m'a conduite à résoudre la conversion NET→BRUT par recherche dichotomique. Sur le plan de la **conception**, le workflow du plan média : une double validation, puis une réservation de créneau avec contrôle de disponibilité, puis une répercussion automatique du rejet, et enfin la création automatique d'un projet et de trois tâches. »

**Q17 — « Quelle fonctionnalité vous a posé le plus de difficultés ? »**
> **Réponse à personnaliser.** Une piste défendable : le RAG hybride, parce qu'il a fallu comprendre pourquoi deux moteurs de recherche produisent des scores incomparables avant de pouvoir les fusionner correctement par RRF.

### Tests et performance

**Q18 — « Quelles sont les performances de votre système ? »**
> ⚠️ **Aucun chiffre.** Voir diapositive 19, §5.

**Q19 — « Comment avez-vous testé votre application ? »**
> **Réponse honnête :** « Chaque user story de mes backlogs comporte une tâche "Tester la fonctionnalité". Les services REST ont été testés avec **Postman** et documentés avec **Swagger**. Il s'agit de **test manuel outillé**. Je n'ai pas de suite de tests automatisés avec mesure de couverture, et je ne vais pas prétendre le contraire — c'est la première dette que je traiterais. »

**Q20 — « Comment avez-vous validé que la paie est correcte ? »**
> **Réponse honnête :** « Par des tests manuels sur des cas représentatifs, en comparant les résultats du moteur aux calculs attendus. Je n'ai pas fait valider le moteur par un expert-comptable, et je ne revendique pas de conformité juridique certifiée. Ce que je garantis, c'est le **déterminisme** : mêmes entrées, mêmes paramètres, même barème → même résultat, reproductible. »

### IA et BI

**Q21 — « Où est l'intelligence artificielle dans votre projet ? »**
> « Elle intervient à quatre niveaux, tous encadrés. **Classer** : le modèle détermine l'intention de la demande. **Récupérer** : il appelle des outils métier Java pour obtenir des faits. **Structurer** : il produit un JSON conforme à un schéma pour les media plans, les relances et la paie. **Rédiger** : il formule la réponse en français. Ce qu'il ne fait jamais, c'est **calculer** ou **décider d'un accès**. »

**Q22 — « Comment garantissez-vous que l'IA n'invente rien ? »**
> Voir diapositive 29, Q1.

**Q23 — « Pourquoi un entrepôt de données séparé ? »**
> Voir diapositive 28, §4a.

### Limites et perspectives

**Q24 — « Quelles sont les limites de votre solution ? »**
> **Réponse structurée :**
> - **Déploiement** : l'architecture physique documentée est un environnement de développement ; la mise en production reste à faire.
> - **Tests** : test manuel outillé, pas de couverture automatisée mesurée.
> - **Performance** : aucune mesure, aucun test de charge.
> - **Sécurité** : pas d'audit ; pas de révocation de JWT ; conformité réglementaire non traitée.
> - **Métier** : la conversion NET→BRUT est une approximation par dichotomie ; pas de validation par un expert-comptable.
> - **IA** : dépendance à un fournisseur externe ; pas d'évaluation automatisée de la qualité ; index réindexé quotidiennement, donc pas immédiat.
> - **BI** : rechargement complet non scalable à très grand volume.
> - **Workflow** : pas de relance automatique si le Head Prod ne répond pas.

**Q25 — « Que feriez-vous avec six mois de plus ? »**
> « Dans l'ordre : d'abord la **dette technique** — tests automatisés, puis déploiement Cloud avec CI/CD et gestion des secrets. Ensuite l'**évaluation du RAG**, avec un jeu de questions-réponses de référence, pour pouvoir mesurer la pertinence plutôt que de l'estimer. Et enfin, côté fonctionnel, les **tableaux de bord consolidés**, puisque l'entrepôt les rend désormais possibles. »

---

## 2. Questions techniques transversales

### Backend

**« Qu'apporte Spring Boot concrètement ? »**
> Configuration automatique, injection de dépendances, serveur embarqué, et un écosystème couvrant exactement mes besoins sans intégration manuelle : Spring Security pour l'authentification, Spring Data JPA pour la persistance, et l'ordonnancement pour les tâches planifiées — notamment l'ETL nocturne et la réindexation quotidienne.

**« Qu'est-ce qu'une API REST ? »**
> **Concept général :** un style d'architecture où des **ressources** sont identifiées par des URL et manipulées par les **verbes HTTP** — GET pour lire, POST pour créer, PUT/PATCH pour modifier, DELETE pour supprimer — de façon **sans état**, chaque requête portant toute l'information nécessaire.
> **Dans mon projet :** une API unique consommée par quatre clients distincts, sécurisée par JWT — c'est le sans-état du REST qui rend le JWT naturel ici.

**« Expliquez l'architecture en couches de votre backend. »**
> « Trois couches, avec une responsabilité unique chacune. Les **contrôleurs REST** reçoivent la requête, valident le format et délèguent — aucune règle métier. Les **services métier** portent toute la logique : règles de gestion, calculs, validations, transactions. Les **repositories** assurent la persistance via Spring Data JPA. Le bénéfice concret : un même service peut être appelé par plusieurs points d'entrée, et je peux le tester sans monter de contexte HTTP. »

**« Qu'est-ce qu'un ORM ? JPA ? Hibernate ? »**
> « Un **ORM** — *Object-Relational Mapping* — fait correspondre des objets Java à des lignes de tables relationnelles. **JPA** est la spécification Java. **Hibernate** en est l'implémentation. **Spring Data JPA** ajoute par-dessus la génération automatique des requêtes à partir du nom des méthodes. »

**« Comment gérez-vous les exceptions ? »**
> ⚠️ *Non détaillé dans le rapport.* « Les services lèvent des exceptions métier porteuses d'un message, et les contrôleurs les traduisent en codes HTTP appropriés. Je peux vous montrer l'implémentation. » — 💡 **Vérifiez si vous avez un gestionnaire global d'exceptions.**

### Frontend

**« Comment React communique-t-il avec l'API ? »**
> « Via **Axios**, avec le jeton JWT placé dans l'en-tête d'autorisation de chaque requête. Chaque SPA est structurée autour d'un routeur **React Router**, de composants réutilisables et d'un service Axios assurant la communication avec l'API en HTTP. »

**« Qu'est-ce qu'une SPA ? »**
> Voir diapositive 21, Q6.

**« Comment gérez-vous l'état dans React ? »**
> ⚠️ **Non documenté dans le rapport.** « Mon rapport cite React, React Router et Axios. Je m'appuie sur les mécanismes natifs de React pour l'état local et le contexte pour ce qui est transverse. » — 💡 **Vérifiez dans votre code** si vous utilisez une bibliothèque dédiée.

**« Comment protégez-vous les routes du frontend ? »**
> « Par des routes protégées qui vérifient la présence et la validité du jeton. ⚠️ **Mais c'est une protection d'ergonomie, pas de sécurité.** La vraie protection est côté serveur : le filtre JWT et les annotations `@PreAuthorize`. Un utilisateur qui appellerait directement l'API serait bloqué indépendamment de ce que fait l'interface. Un frontend ne sécurise jamais rien. »

### Base de données

**« Qu'est-ce qu'une transaction ? Les propriétés ACID ? »**
> « **Atomicité** — tout ou rien ; **Cohérence** — les contraintes restent respectées ; **Isolation** — les transactions concurrentes ne se perturbent pas ; **Durabilité** — une transaction validée survit à une panne.
> Dans mon projet : approuver un congé regroupe la déduction du solde, le changement de statut et l'historisation — c'est atomique. »

**« Comment gérez-vous les relations plusieurs-à-plusieurs ? »**
> « Par une table de jointure générée par JPA. L'exemple le plus visible est `Role` ↔ `Permission`, en 0..\* des deux côtés : un rôle regroupe plusieurs permissions, une permission peut appartenir à plusieurs rôles. C'est ce qui rend le RBAC combinable. »

**« Avez-vous des index ? »**
> **Réponse honnête :** « Je n'ai pas mené de campagne d'optimisation guidée par des mesures, donc je ne prétendrai pas à une stratégie d'indexation raisonnée. Les clés primaires et étrangères sont indexées par défaut. Là où des index sont **structurellement nécessaires**, ils existent : la recherche vectorielle de pgvector et la recherche plein texte du RAG en dépendent. »

### Sécurité

**« JWT : structure et contenu ? »**
> « Un JSON Web Token comporte trois parties : un **en-tête**, une **charge utile** — l'identité et les permissions — et une **signature** calculée par le serveur. Sa propriété clé est d'être **sans état** : le serveur ne conserve aucune session, il vérifie la signature et l'expiration à chaque requête.
> ⚠️ **Point important : le contenu est signé, pas chiffré.** La charge utile est lisible par quiconque possède le jeton. C'est pourquoi je n'y mets aucune donnée sensible : uniquement l'identité et les droits. »

**« Qu'est-ce qui empêche un utilisateur de modifier son JWT pour devenir administrateur ? »**
> **Question très fréquente.** « La **signature**. Le jeton est signé par le serveur avec une clé secrète que le client ne possède pas. S'il modifie ne serait-ce qu'un caractère de la charge utile, la signature ne correspond plus, et le filtre rejette la requête avant même qu'elle n'atteigne le contrôleur. La signature garantit l'**intégrité** : le client peut lire le jeton, il ne peut pas le falsifier. »

**« BCrypt : pourquoi ? »**
> « C'est une fonction de **hachage** — à sens unique — avec deux propriétés essentielles : un **sel** aléatoire unique par mot de passe, qui rend les tables pré-calculées inutilisables, et un **coût de calcul volontairement élevé**, qui ralentit les attaques par force brute. Le mot de passe n'est jamais stocké en clair, et la vérification consiste à rehacher la saisie et comparer. »

**« Qu'est-ce que le RBAC ? »**
> Voir diapositive 31, Q4.

### BI

**« OLTP vs OLAP ? »** · **« Modèle en étoile vs flocon ? »** · **« Qu'est-ce qu'un ETL ? »**
> Voir diapositive 28, §4.

**« Qu'est-ce qu'une table de faits ? Une dimension ? »**
> « Une **dimension** porte les **axes d'analyse** : qui, quoi, quand — un employé, un client, une date. Une **table de faits** porte les **mesures numériques** et référence les dimensions. Dans mon entrepôt : quatre dimensions et six tables de faits. »

**« Qu'est-ce que DAX ? »**
> « Le langage de formules de Power BI, utilisé pour définir les mesures et calculer les indicateurs analytiques — présence, absentéisme, produits, charges, avancement des projets. »

### IA

**« LLM, RAG, embedding, pgvector, RRF, tool calling, SSE »**
> Voir diapositive 29, §4, et le **glossaire** en fin de document.

**« Différence entre fine-tuning et RAG ? »**
> « Le **fine-tuning** réentraîne le modèle sur des données spécifiques : la connaissance est figée dans les poids. Le **RAG** laisse le modèle intact et lui fournit les documents pertinents à chaque requête. J'ai choisi le RAG pour trois raisons : la **volumétrie** — un entraînement utile demande des milliers d'exemples ; la **fraîcheur** — une facture payée hier doit être invisible aujourd'hui ; et la **sécurité** — un modèle entraîné sur tous les bulletins de paie pourrait les restituer à n'importe qui, alors que chaque outil vérifie les droits avant toute lecture. »

**« Qu'est-ce qu'une hallucination et comment la combattez-vous ? »**
> « Une hallucination est une information plausible mais fausse, produite par le modèle. Je la combats à deux niveaux. **Architecturalement** : le modèle ne calcule rien — les résultats lui sont transmis comme données à reformuler. Et **par le RAG** : ses réponses sont ancrées dans les données réelles de l'application plutôt que dans sa mémoire d'entraînement. Sur les chiffres, c'est structurellement empêché. Sur la formulation, une imprécision reste possible — je ne prétendrai pas le contraire. »

---
## 3. Questions sur les choix et les alternatives

**Format à appliquer systématiquement :** *pourquoi ce choix* → *quel problème il résout* → *quelles alternatives* → *pourquoi écartées*.

| Question | Réponse condensée |
|---|---|
| **Pourquoi Agile et pas Waterfall / RUP / 2TUP ?** | Possibilité d'évolution des besoins en cours de développement + nécessité de validations régulières. Waterfall : difficulté à s'adapter aux changements et retour d'information tardif. RUP : charge documentaire importante. 2TUP : moins flexible quand les besoins évoluent. |
| **Pourquoi Scrum et pas Kanban / XP ?** | Organisation en **sprints**, **backlog** priorisé, **rôles clairement définis**. Kanban est moins structuré et ne définit ni rôles ni sprints. XP exige une implication continue sur des pratiques techniques très exigeantes. |
| **Pourquoi N-Tiers ?** | **Quatre clients distincts** consomment la même couche métier. Séparation claire des responsabilités, sécurité centralisée au niveau du serveur. |
| **Pourquoi pas microservices ?** | Complexité opérationnelle (orchestration, découverte de service, supervision distribuée), latence réseau, **gestion difficile de la cohérence des données distribuées**, coût DevOps élevé — disproportionné pour ce projet. |
| **Pourquoi pas un monolithe ?** | Il répond moins efficacement au besoin de **plusieurs clients distincts**, et sa mise à l'échelle est uniquement globale. |
| **Pourquoi PostgreSQL ?** | **Une seule base** couvre trois usages : relationnel, **vectoriel** (pgvector, indispensable au RAG), et **analytique** (schéma `dwh`). MySQL aurait imposé une base vectorielle séparée. |
| **Pourquoi JWT ?** | **Sans état** — adapté à quatre clients distincts, dont une application de bureau. Une session serveur imposerait un état à maintenir et des cookies inter-domaines. Contrepartie assumée : pas de révocation immédiate. |
| **Pourquoi BCrypt ?** | Hachage à sens unique, **sel** unique par mot de passe, **coût de calcul élevé**. Le mot de passe n'est jamais stocké ni transmis en clair. |
| **Pourquoi RBAC et pas des rôles en dur ?** | Permet à l'administrateur de **créer un rôle et de lui attribuer des permissions sans qu'une ligne de code change**. Le système teste une permission, jamais un nom de rôle. |
| **Pourquoi React ?** | Composants réutilisables, interfaces réactives, large écosystème. ⚠️ **Angular et Vue ne sont pas comparés dans le rapport** — ne prétendez pas à une étude comparative. |
| **Pourquoi TypeScript ?** | Typage statique → erreurs détectées à la compilation. Décisif avec **trois interfaces consommant la même API**. |
| **Pourquoi Vite ?** | Build rapide et **HMR** quasi instantané — avec trois applications à faire tourner en parallèle, le gain est réel. |
| **Pourquoi Java 17 ?** | Version **LTS**, socle de Spring Boot 3. Le critère est la stabilité et le support durable, pas la nouveauté. |
| **Pourquoi Electron pour l'agent ?** | Développer une application de bureau en **JavaScript**, même famille que les interfaces web — évite d'introduire une quatrième technologie pour une équipe réduite. |
| **Pourquoi GPT-4o ?** | Sorties structurées, **fiabilité du tool calling**, qualité en français, fenêtre de contexte large, et **intégration avec LangChain4j**. |
| **Pourquoi pas Claude ?** | L'intégration des **embeddings aurait nécessité un fournisseur supplémentaire** — une dépendance de plus. |
| **Pourquoi pas Gemini ?** | **Intégration plus complexe** avec l'environnement Java. |
| **Pourquoi pas Llama auto-hébergé ?** | **Besoins en infrastructure élevés** (GPU, disponibilité), disproportionnés pour le volume d'usage d'une agence de cette taille. |
| **Et Mistral Large ?** | 💡 **Classé "alternative" dans mon rapport**, pas rejeté — bonne prise en charge du français et **possibilité d'hébergement en Europe**. C'est la piste à retenir si la souveraineté devenait une exigence. |
| **Pourquoi LangChain4j ?** | Orchestration d'agents **et couche d'abstraction** : changer de modèle, y compris de fournisseur, ne demande de modifier **qu'un paramètre de configuration, jamais le code métier**. |
| **Pourquoi pgvector et pas une base vectorielle dédiée ?** | Recherche vectorielle **dans la base déjà utilisée** — pas de service supplémentaire à déployer, sécuriser et synchroniser. |
| **Pourquoi le RAG et pas le fine-tuning ?** | Volumétrie insuffisante · besoin de **fraîcheur** · et surtout **sécurité** : le RAG applique les droits à chaque requête, un modèle entraîné restituerait tout à tous. |
| **Pourquoi une recherche hybride ?** | Les embeddings **diluent les correspondances exactes** (numéro de facture, nom de marque) ; les mots-clés ratent la proximité de sens. Les deux branches sont **complémentaires**. |
| **Pourquoi RRF plutôt qu'une somme de scores ?** | Les deux moteurs produisent des scores sur des **échelles incomparables**. RRF fusionne les **rangs**, ce qui est mathématiquement valide. |
| **Pourquoi la dichotomie pour NET→BRUT ?** | **L'IRPP progressif interdit l'inversion analytique** : la fonction est définie par morceaux et n'a pas de forme fermée. La dichotomie est applicable parce que la fonction brut→net est **monotone croissante**. |
| **Pourquoi versionner le barème IRPP ?** | Pour que le recalcul d'un mois ancien applique **les taux de l'époque** — et pour répondre au constat « aucune trace claire des taux appliqués au fil du temps ». |
| **Pourquoi un DWH ?** | Un schéma transactionnel est normalisé pour **écrire**. L'interroger sur l'historique impose des jointures profondes dont le coût retombe sur les écrans de saisie. |
| **Pourquoi un modèle en étoile et pas un flocon ?** | La **dénormalisation des dimensions est volontaire** : elle économise des jointures, et le coût en redondance est négligeable, une dimension étant petite. |
| **Pourquoi un rechargement complet ?** | Plus simple qu'un incrémental, qui exigerait de tracer les suppressions et les modifications rétroactives. **Supprime toute classe de bug de désynchronisation.** Limite assumée : ne passe pas à très grand volume. |
| **Pourquoi Power BI ?** | **Exploration libre** pour la direction, en complément des pages Analytique fermées. ⚠️ Aucune comparaison avec Tableau ou Metabase dans le rapport. |
| **Pourquoi PostgreSQL en dev et pas H2 ?** | Le comportement observé en développement doit être celui de la production. **H2 n'implémente pas pgvector**, sur lequel repose le RAG. |
| **Pourquoi des services externes réels et pas des mocks ?** | Ce qui est testé est le **comportement réel**, y compris latences et cas d'erreur. Un mock donnerait une fausse confiance. |

---

## 4. Questions difficiles de soutenance

### D1 — « Où est le travail d'ingénieur ? Ce n'est pas simplement du développement d'application de gestion ? »

**🎤 Version courte :**
> « La gestion est le support, pas la contribution. Je citerais trois points où la solution évidente ne marchait pas. **Un** : l'inversion d'un barème IRPP progressif n'admet pas de forme analytique — d'où une recherche dichotomique. **Deux** : deux moteurs de recherche produisent des scores sur des échelles incomparables — d'où la fusion par rang réciproque, qui fusionne les rangs et non les scores. **Trois** : la cascade de détermination d'un statut de présence, où l'ordre des tests **est** l'algorithme — inverser deux étapes ferait apparaître absent un employé en congé validé. Aucun des trois n'est du CRUD. »

**Version approfondie :** développez l'un des trois. Le premier est le plus impressionnant ; le troisième est le plus facile à faire comprendre à un jury non spécialiste.

**Raisonnement :** le jury cherche à distinguer l'assemblage de briques de la résolution de problèmes. **Donnez-lui trois problèmes, pas trois technologies.**

---

### D2 — « Vous avez utilisé GPT-4o. N'est-ce pas juste un appel d'API ? »

**🎤 Version courte :**
> « L'appel d'API est la partie triviale. Le travail est dans tout ce qui l'entoure : garantir que les chiffres sont exacts, que le cloisonnement tient, et qu'une génération longue reste utilisable. Concrètement : le modèle **ne calcule rien** — il reçoit des résultats déjà calculés en Java ; le contrôle d'accès est **dans chaque outil**, pas dans le prompt ; et le RAG hybride ancre les réponses dans les données réelles. »

**Version approfondie :** développez les sept blocs de la diapositive 29, puis les trois principes de prompting, puis le fait qu'un prompt manipulé ne contourne pas une vérification en Java.

**Raisonnement :** retournez la question — c'est précisément **parce que** l'appel d'API est facile que toute la valeur est dans l'architecture qui l'encadre.

---

### D3 — « Vous n'avez quasiment pas de tests automatisés. Comment garantissez-vous que ça fonctionne ? »

**🎤 Version courte :**
> « Vous avez raison, et c'est la dette que je reconnais le plus volontiers. Les tests sont **manuels et outillés** : chaque user story porte une tâche de test, les endpoints REST sont testés via Postman et documentés avec Swagger. Je n'ai pas de taux de couverture et je n'en inventerai pas. »

**Version approfondie :**
> « Ce que je peux dire, c'est **où** j'investirais en priorité si je devais automatiser : sur les règles métier à forte densité, précisément celles où un test manuel ne couvre qu'une fraction des cas. Le calcul du nombre de jours d'un congé, qui combine horaire et calendrier ; la cascade de détermination du statut de présence, qui a cinq branches ; et le moteur de paie, avec ses exonérations et sa conversion NET→BRUT. Ce sont trois endroits où une régression serait invisible à l'œil nu. »

**Raisonnement :** ne vous défendez pas, **expliquez le critère** qui guiderait l'investissement. Cela transforme une lacune en priorisation lucide.

---

### D4 — « Vous n'avez mesuré aucune performance. Comment savez-vous que le système tient ? »

**🎤 Version courte :**
> « Je ne le sais pas, et je ne vais pas prétendre le contraire : aucune campagne de mesure ni test de charge n'a été réalisé. Ce que je peux décrire, ce sont les **dispositions de conception** prises. »

**Version approfondie :**
> « Trois dispositions. Les **bulletins sont figés** à la génération plutôt que recalculés à chaque lecture. L'**analyse historique ne touche plus les tables de saisie** : elle lit un entrepôt dénormalisé rechargé une fois par nuit — c'est même la raison d'être de la release décisionnelle. Et les **calculs sensibles sont centralisés côté serveur**, donc écrits une fois.
>
> Et je sais **où** je chercherais en premier si un problème apparaissait : la cascade de statut de présence, qui croise calendrier, horaires, congés et télétravail pour chaque employé — c'est le calcul le plus dense du système. »

**Raisonnement :** un candidat qui sait **où** son système cassera est plus convaincant qu'un candidat qui affirme qu'il ne cassera pas.

---

### D5 — « Votre architecture physique montre un environnement de développement. Le projet n'est donc pas terminé ? »

**🎤 Version courte :**
> « Le **développement** est terminé : les six releases sont livrées et fonctionnelles. C'est la **mise en production** qui reste à faire, et je l'ai identifiée comme première perspective. Je préfère l'afficher honnêtement plutôt que de décrire un déploiement que je n'ai pas réalisé. »

**Version approfondie :**
> « L'architecture est conçue pour être déployée : le backend est une application Spring Boot autonome, les frontends se compilent en fichiers statiques, la base est un PostgreSQL standard. Ce qui manque relève de l'industrialisation — conteneurisation, CI/CD, gestion des secrets, configuration multi-environnements. C'est un chantier à part entière, que j'ai préféré identifier plutôt que traiter à moitié dans les dernières semaines. »

**Raisonnement :** transformez l'aveu en preuve de lucidité, et rappelez que c'est **explicitement dans vos perspectives**.

---

### D6 — « Vous envoyez des données RH et financières à un service américain. Est-ce acceptable ? »

**🎤 Version courte :**
> « Question légitime. Ce qui **transite est limité** : le modèle ne reçoit jamais la base, mais un contexte déjà filtré par le contrôle d'accès et déjà calculé côté Java. Et l'architecture est **réversible** : LangChain4j sert de couche d'abstraction, donc changer de fournisseur ne demande qu'un paramètre de configuration. »

**Version approfondie :**
> « Mon rapport identifie d'ailleurs explicitement **Mistral Large comme alternative**, précisément pour sa possibilité d'**hébergement en Europe** — il est classé "alternative", pas "rejeté". Si la souveraineté devenait une exigence, le chemin technique existe et il est court.
>
> Cela dit, je dois être honnête : je n'ai pas traité la conformité réglementaire du traitement — base légale, information des personnes, durée de conservation. C'est un préalable avant tout déploiement avec des données de production. »

**Raisonnement :** ne minimisez pas. Montrez que le choix a été **évalué**, que le chemin de sortie **existe**, et reconnaissez ce qui n'a pas été traité.

---

### D7 — « Votre planning contient des erreurs : neuf ou dix sprints ? Des charges qui ne correspondent pas ? »

**🎤 Version courte :**
> « Vous avez raison, et je l'ai identifié. Le découpage réel est de **dix sprints** — c'est ce que montrent les backlogs détaillés des chapitres 3 à 8. Le tableau de synthèse du chapitre 2 n'a pas été mis à jour après l'ajout des deux dernières releases : il annonce neuf sprints et comporte deux lignes libellées "Sprint 9". »

**Version approfondie :** donnez les chiffres exacts — **23 · 34 · 17 · 29 · 33 · 28 · 18 · 24 · 32 · 16 = 254 points**.

**Raisonnement :** reconnaître immédiatement désamorce. **Mieux : corrigez le tableau avant la soutenance.**

---

### D8 — « Vous étiez deux développeuses. Qu'avez-vous fait, vous ? »

🚨 **Question quasi certaine. Cette information n'est pas dans votre rapport — vous DEVEZ la préparer.**

**Structure de réponse recommandée :**
1. **Nommez la répartition** : par modules, par couches, ou mixte.
2. **Citez précisément ce que vous avez porté** — deux ou trois modules ou briques, nommément.
3. **Assumez ce que vous n'avez pas fait**, tout en montrant que vous le comprenez : *« Cette partie a été développée par ma binôme ; je peux vous en expliquer le fonctionnement, mais je ne revendique pas de l'avoir écrite. »*

⚠️ **Le piège absolu :** revendiquer l'ensemble du projet. Le tableau 1.5 de votre rapport est sous les yeux du jury.
⚠️ **Le piège inverse :** minimiser votre contribution. Soyez factuelle et précise.

---

### D9 — « Quelle est la partie dont vous êtes la moins satisfaite ? »

**🎤 Version courte :**
> « L'absence de tests automatisés. Les tests sont manuels et outillés, ce qui suffit pour valider une fonctionnalité au moment où on l'écrit, mais ne protège pas contre les régressions — et sur des règles métier aussi denses que le calcul des jours de congé ou le moteur de paie, c'est une vraie fragilité. »

**Version approfondie :**
> « J'en citerais une seconde : je n'ai **aucune mesure objective de la qualité des réponses de l'assistant**. Je constate qu'il répond bien, mais je ne peux pas le prouver. Il faudrait un jeu de questions-réponses de référence.
>
> Ces deux points ont la même cause : j'ai priorisé la **couverture fonctionnelle** sur l'outillage de validation. C'est un arbitrage défendable dans un stage de six mois, mais c'est le premier que je reverrais. »

**Raisonnement :** une réponse qui remonte du symptôme à la **cause méthodologique** vaut bien plus qu'une liste de regrets.

---

### D10 — « Votre assistant IA apporte-t-il vraiment de la valeur, ou est-ce une démonstration technique ? »

**🎤 Version courte :**
> « Les quatre capacités répondent à des besoins identifiés : générer un media plan mensuel, rédiger une relance client, expliquer un bulletin de paie à un employé, et retrouver un article du règlement intérieur sans le relire en entier. Ce sont quatre tâches qui prennent du temps aujourd'hui. »

**Version approfondie :**
> « Je distinguerais deux niveaux de valeur. **L'explication de bulletin de paie** crée une valeur nouvelle : c'est le seul moyen pour un employé d'obtenir le détail de son net, puisque le module Finance lui est fermé par permission. Les trois autres **accélèrent** des tâches qui étaient faisables autrement.
>
> Et je reconnais une limite : je n'ai pas mesuré l'usage réel ni le gain de temps, donc je ne vais pas avancer de bénéfice chiffré. »

**Raisonnement :** distinguer ce qui crée de la **valeur nouvelle** de ce qui **accélère l'existant** est une nuance que peu de candidats font.

---

## 5. Sujets sur lesquels je ne dois JAMAIS improviser

> **La formulation de repli, à connaître par cœur :**
> **« Cette partie n'a pas été mesurée dans le cadre de mon projet, donc je préfère ne pas avancer de chiffre non vérifié. »**
>
> Puis **enchaînez sur ce que vous POUVEZ dire** : les dispositions de conception prises, ou la méthode que vous emploieriez pour le mesurer. **Ne restez jamais sur un « je ne sais pas » nu.**

### 🚫 Métriques jamais mesurées

| Sujet | Ce qu'il ne faut PAS dire | Ce qu'il faut dire |
|---|---|---|
| **Temps de réponse** | Tout chiffre | « Non mesuré. Les dispositions prises sont : bulletins figés, entrepôt dénormalisé pour l'analyse, calculs centralisés côté serveur. » |
| **Charge supportée** | Tout nombre d'utilisateurs simultanés | « Aucun test de charge. Je sais où je chercherais : la cascade de statut de présence. » |
| **Couverture de tests** | Tout pourcentage | « Test manuel outillé via Postman et Swagger. Pas de couverture automatisée mesurée. » |
| **Gain de temps pour l'agence** | Toute durée ou proportion | « Non mesuré ; cela demanderait un suivi sur plusieurs mois d'exploitation. » |
| **Taux d'erreur avant/après** | Tout pourcentage | « Constat rapporté par l'agence, pas une mesure. Ce que je garantis, c'est le déterminisme du calcul. » |
| **Coût d'infrastructure ou d'API** | Tout montant | « Pas de chiffrage réalisé. » |
| **Qualité / pertinence du RAG** | Tout taux de rappel ou de précision | « Aucune évaluation automatisée. Il faudrait un jeu de questions-réponses de référence. » |
| **Nombre de lignes de code** | Toute estimation | « Non consolidé dans mon rapport. » |
| **Nombre d'entités / de contrôleurs** | Tout chiffre non vérifié | 💡 **Comptez-les avant la soutenance** si vous voulez pouvoir répondre. |
| **Précision exacte de la dichotomie** | Un nombre d'itérations inventé | 💡 **Vérifiez-le dans votre code.** |

### 🚫 Technologies NON utilisées — ne pas laisser croire le contraire

| Ne pas revendiquer | Réalité |
|---|---|
| **Docker** | Le schéma dit explicitement « **PAS de conteneur Docker en local** » |
| **CI/CD, GitHub Actions** | Non documenté — Git/GitHub sont cités pour la **gestion de versions** uniquement |
| **Déploiement Cloud** | **En perspective**, pas réalisé |
| **Redis, Kubernetes, message broker** | Aucun n'est utilisé |
| **Base vectorielle dédiée** (Pinecone, Qdrant) | Non utilisée — **pgvector** |
| **Fine-tuning** | **Non utilisé**, et volontairement écarté |
| **Redux, Zustand** | Non documentés — ⚠️ **vérifiez votre code** |
| **SonarQube ou analyse statique** | Non utilisé — donc pas d'indice de dette technique |
| **Tests unitaires automatisés** | ⚠️ **Vérifiez votre code.** Le rapport ne documente que le test manuel via Postman |

### 🚫 Sujets non traités dans le projet

| Sujet | Réponse honnête |
|---|---|
| **Conformité RGPD / réglementaire** | « Non traitée. C'est un préalable que je signalerais avant tout déploiement réel — notamment pour l'agent de présence et pour l'envoi de contexte à un service externe. » |
| **Audit de sécurité / test d'intrusion** | « Non réalisé. Je peux décrire mes mécanismes, pas prouver leur résistance. » |
| **Sauvegarde et reprise** | « Non documentée dans mon rapport. » |
| **Accessibilité (WCAG)** | « L'accessibilité est déclarée comme besoin non fonctionnel, mais aucune campagne de vérification n'a été menée. » |
| **Internationalisation** | Non traitée — l'application est en français. |
| **Validation du moteur de paie par un expert-comptable** | « Non réalisée. Je garantis le déterminisme, pas une conformité certifiée. » |
| **Effectif d'Antigone** | ⚠️ **Non documenté — préparez-le vous-même.** |
| **Répartition du travail entre les deux développeuses** | 🚨 **Non documentée — préparez-la impérativement.** |

### ✅ Ce que vous POUVEZ affirmer sans risque

- **Fiche d'identité d'Antigone** : Malek Naouar, 2021, Rades/Tunis, antigoneagency.com.
- **Rôles Scrum** : PO Malek Naouar · SM Ahmed Kouki · Dev Yosr Kheriji + Zeineb Haj Hsine.
- **Chiffres du projet** : 10 sprints · 6 releases · 254 points (23·34·17·29·33·28·18·24·32·16).
- **Ports** : 3000 / 3001 / 3002 / 8080 / 5432 · base `antigone_rh`.
- **Config IA** : GPT-4o · température **0,7** (conversation) et **0,4** (structuré) · **schéma JSON strict activé** · streaming activé · **max 10 allers-retours d'outils** · **2 tentatives max** · **fenêtre 20 messages** · **résumé tous les 6 messages évincés** · `text-embedding-3-small` · **1536 dimensions** · **top-k dense 12** · **top-k lexical 12**.
- **15 outils métier** · **réindexation quotidienne**.
- **BI** : **4 dimensions · 6 tables de faits** · schéma `dwh` · ETL **02h30** · rechargement complet · Power BI **lecture seule**.
- **Comparaisons documentées** : 3 solutions du marché (11 critères) · 4 méthodologies · 3 cadres Agile · 3 architectures · **5 modèles de langage**.
- **Environnement matériel** : Asus Vivobook · Windows 10 64 bits · Intel Core i7-10870H · **16 Go RAM** · 215 Go SSD + 1 To HDD.

---

## 6. Fiche de révision finale

### 🏢 Contexte
- **Antigone** · agence de **stratégie digitale** · **Rades, Tunis** · fondée **2021** · fondateur **Malek Naouar**.
- **3 domaines** : stratégie de marketing digital · conseil et études · création et développement digital.
- **ESPRIT**, cycle ingénieur, **6 mois**.
- Encadrante universitaire **Hela Mejri** · encadrant professionnel **Malek Naouar**.

### 🎯 Problématique
> « Comment centraliser et automatiser les processus RH, projets et financiers d'une agence de communication digitale, tout en exploitant l'IA et la Business Intelligence pour offrir un suivi en temps réel, un portail client sécurisé et une meilleure aide à la décision ? »

**Les constats :** trois logiques jamais pilotées ensemble · Excel, Google Sheets, WhatsApp, cahier de comptabilité · **la même information saisie plusieurs fois** · **aucune trace claire des taux appliqués au fil du temps** · pas de vision consolidée.

### 🔍 Étude de l'existant
**Odoo** (RH+projets+finance, mais pas de paie tunisienne) · **BambooHR** (RH pure) · **monday.com** (projets).
**Les deux trous du marché : paie tunisienne (3 × Non) et portail client avec approbation (Non/Non/Limité).**

### 📋 Méthodologie
- **Méthodologie Agile** (vs Waterfall, RUP, 2TUP) → **Cadre Scrum** (vs Kanban, XP).
- **3 raisons** : déploiement modulaire · adaptabilité aux évolutions · détection précoce des risques.
- **3 rôles** : PO **Malek Naouar** · SM **Ahmed Kouki** · Dev **Yosr Kheriji + Zeineb Haj Hsine**.
- **10 sprints de 2 semaines · 6 releases · 254 points.**

### 👥 Acteurs
**6 principaux** : Administrateur · Employé · Validateur · Chef de projet · Responsable Finance · Client.
**3 secondaires** : Agent de présence · Google Drive · Assistant IA *(+ Brevo, omis de la liste)*.
**Généralisations** : Validateur ◁ Employé · Administrateur ◁ Validateur + Chef de projet + Responsable Finance.

### 🏗️ Architecture
- **N-Tiers** (3 tiers), retenue contre monolithique et microservices. **Critère décisif : 4 clients distincts.**
- **Tier 1** : `frontend-rh` (3001) · `frontend-projects` (3000, **porte le login**) · `frontend-finance` (3002) · **AgentDesktop** (Electron).
- **Tier 2** : Spring Boot, **port 8080** · **filtre JWT AVANT les contrôleurs** · `@PreAuthorize` · 4 domaines (RH, Projets, Finance, Notifications & IA).
- **Tier 3** : PostgreSQL **5432**, base `antigone_rh` · Google Drive API (**OAuth2**).
- Communication : **HTTPS/REST/JSON** en haut · **JDBC** en bas.
- **Physique = environnement de DÉVELOPPEMENT** · Vite · **pas de Docker** · **PostgreSQL et pas H2** · **services externes réels** · **sans clé API → 503 sur `/api/v1/**` seulement**.

### 🗄️ Modèle de données
- **RBAC** : `Compte` → `Role` → `Permission` (0..\* ↔ 0..\*).
- **Héritage** : `Demande` ▷ `Congé` / `Autorisation` / `Teletravail`.
- **`Employé` = pivot** · **deux relations vers `Projet`** : `dirige` et `participe`.
- **`AffectationHoraire`** porte des **dates** → horaire sur une période.
- **`Facture.type`** distingue devis et facture · **plusieurs `PaiementFacture`** → paiements partiels.
- **`Référentiel`** générique avec un champ `type`.

### 🔐 Sécurité
- **JWT** : sans état, **signé mais PAS chiffré**.
- **BCrypt** : **hachage** (jamais « cryptage »), sel + coût élevé.
- **RBAC par permission**, jamais par nom de rôle.
- **Filtre avant les contrôleurs** + **`@PreAuthorize`**.
- Traçabilité : décisions horodatées avec auteur · appels d'outils IA enregistrés.
- ⚠️ Limites : pas de révocation JWT · pas d'audit de sécurité · conformité réglementaire non traitée.

### 📊 Les 5 modules phares

| Module | Cœur technique | Les 3 mots |
|---|---|---|
| **Plan Média** | Créneau Head Prod · contrôle de disponibilité **avant** réservation · **1 projet + 3 tâches auto** | Automatique · Sans ressaisie · Traçable |
| **Paie tunisienne** | **NET↔BRUT par dichotomie** (IRPP progressif = pas d'inversion analytique) · **barème versionné** · bulletin figé | Conforme · Traçable · Auditable |
| **Pointage** | **Cascade** : Férié → Horaire → Congé → Télétravail → Retard/Présent · **côté serveur** | Automatique · Fiable · Sans ressaisie |
| **Décisionnel** | **Modèle en étoile** : 4 dimensions + 6 faits · ETL **02h30** · **mêmes chiffres, 2 vues** | Cohérent · Sécurisé · À jour |
| **Assistant IA** | **RAG hybride** : sémantique (pgvector) + lexicale (PostgreSQL) → **fusion RRF** · **15 outils** | Sécurisé · Contrôlé · Traçable |

### 🤖 IA — les paramètres
GPT-4o · **T=0,7** conversation / **T=0,4** structuré · **schéma JSON strict** · streaming · **10 allers-retours max** · **2 tentatives** · **mémoire 20 messages** + **résumé tous les 6 évincés** · `text-embedding-3-small` · **1536 dim.** · **top-k 12 / 12** · réindexation quotidienne.
**5 modèles comparés** : GPT-4o (sélectionné) · Claude, Gemini, Llama (rejetés) · **Mistral Large (alternative)**.

### 🎓 Les phrases à connaître
1. **« La logique métier sensible reste portée par le backend — jamais déléguée au frontend ni au modèle de langage. »**
2. **« Les calculs métier sont réalisés côté Java et transmis au modèle comme des données à reformuler. »**
3. **« Chaque outil vérifie les droits d'accès avant d'effectuer une opération. »**
4. **« L'IRPP progressif interdit l'inversion analytique. »**
5. **« RRF fusionne les rangs, pas les scores — les échelles sont incomparables. »**
6. **« L'ordre de la cascade EST l'algorithme. »**
7. **« Mêmes chiffres, deux vues. »**

---

## 7. Glossaire technique

> **Format : définition simple → rôle dans mon projet.**

| Terme | Définition simple | Dans Antigone 360° |
|---|---|---|
| **API REST** | Ressources identifiées par des URL, manipulées par les verbes HTTP, sans état | Une API unique consommée par **4 clients distincts** |
| **N-Tiers** | Architecture en couches séparées : présentation, métier, données | Le choix retenu, contre monolithique et microservices |
| **SPA** | Application chargée en une page, navigation côté client | Les 3 applications React |
| **JWT** | Jeton signé portant identité et droits, vérifiable sans session | Authentification **sans état** · **signé, pas chiffré** |
| **BCrypt** | Hachage **à sens unique**, avec sel et coût élevé | Stockage des mots de passe — jamais en clair |
| **RBAC** | Contrôle d'accès fondé sur les rôles | `Compte` → `Role` → `Permission` · on teste **une permission**, pas un rôle |
| **`@PreAuthorize`** | Annotation Spring vérifiant une permission avant d'exécuter une méthode | Second niveau de sécurité, après le filtre JWT |
| **Middleware** | Composant intercalé entre la requête et son traitement | Spring Security + filtre JWT, appliqué à **toutes** les requêtes |
| **ORM** | Correspondance entre objets et tables relationnelles | Évite d'écrire le SQL standard |
| **JPA** | La **spécification** Java de l'ORM | Contrat standard |
| **Hibernate** | L'**implémentation** de JPA | Traduit les entités en SQL PostgreSQL |
| **Spring Data JPA** | Génère les requêtes depuis le nom des méthodes | Réduit le code répétitif de persistance |
| **`include` (UML)** | Le cas inclus est **toujours** exécuté | « Refuser une demande » *include* « Saisir un motif » |
| **`extend` (UML)** | Le cas étendant s'exécute **sous condition** | « Gérer ses demandes » *extend* « Annuler une demande » |
| **Généralisation d'acteur** | Un acteur **hérite** des cas d'utilisation d'un autre | Administrateur ◁ Validateur + Chef de projet + Resp. Finance |
| **Electron** | Framework d'applications de bureau en JavaScript | **AgentDesktop**, exécuté en arrière-plan |
| **Heartbeat** | Signal périodique attestant qu'un système est actif | Envoyé par l'agent pour **recouper** le pointage |
| **HMR** | *Hot Module Replacement* — rechargement à chaud | Vite remplace un module sans recharger la page |
| **npm workspace** | Monorepo : plusieurs packages, imports internes | `@antigone/ai-chat-widget` **partagé par les 3 SPA** |
| **OAuth2** | Protocole d'autorisation auprès d'un service tiers | Accès à l'API Google Drive |
| **CNSS** | Caisse Nationale de Sécurité Sociale (Tunisie) | Part **salariale** + **patronale** · déclaration **trimestrielle** |
| **IRPP** | Impôt sur le Revenu des Personnes Physiques | **Barème progressif versionné** |
| **TVA** | Taxe sur la Valeur Ajoutée | Déclaration **mensuelle**, recalculée |
| **TFP** | Taxe de Formation Professionnelle | Charge **patronale** |
| **FOPROLOS** | Fonds de Promotion du Logement pour les Salariés | Charge **patronale** |
| **Abattement forfaitaire** | Réduction appliquée au salaire imposable | Étape du calcul avant l'IRPP |
| **CIVP** | Contrat d'Initiation à la Vie Professionnelle | **Exonéré** de cotisations dans le moteur |
| **Recherche dichotomique** | Diviser l'intervalle par deux à chaque itération | Conversion **NET→BRUT** — applicable car la fonction est **monotone** |
| **OLTP / OLAP** | Transactionnel (écrire) / analytique (analyser) | Tables métier vs schéma `dwh` |
| **DWH** | *Data Warehouse* — entrepôt de données | Schéma `dwh`, rechargé chaque nuit |
| **Modèle en étoile** | Une table de faits au centre, ses dimensions en rayons | **4 dimensions · 6 tables de faits** |
| **Dimension** | Axe d'analyse : qui, quoi, quand | Les 4 dimensions de l'entrepôt |
| **Table de faits** | Mesures numériques référençant les dimensions | Les 6 tables de faits |
| **ETL** | *Extract, Transform, Load* | Procédure nocturne à **02h30**, rechargement complet |
| **KPI** | Indicateur clé de performance | Taux de complétion, tâches en retard, résultat net |
| **Power BI** | Outil de visualisation et d'exploration | Branché sur `dwh` en **lecture seule** |
| **DAX** | Langage de mesures de Power BI | Définit les indicateurs analytiques |
| **LLM** | Modèle entraîné à prédire le mot suivant | GPT-4o — *« excellent rédacteur, très mauvaise base de données »* |
| **RAG** | Chercher les documents pertinents, puis générer à partir d'eux | **Ancre les réponses dans les données réelles** |
| **Embedding** | Traduction d'un texte en vecteur de nombres | **1536 dimensions** · `text-embedding-3-small` |
| **pgvector** | Extension PostgreSQL de recherche vectorielle | La branche **sémantique** du RAG |
| **Recherche lexicale** | Recherche par correspondance de mots | La branche **exacte** du RAG — trouve « FA-2026-0147 » |
| **RAG hybride** | Combiner recherche sémantique **et** lexicale | Les deux branches sont **complémentaires** |
| **RRF** | *Reciprocal Rank Fusion* — fusion par rang réciproque | Fusionne les **rangs**, car les scores sont **incomparables** |
| **Tool calling** | Le modèle demande l'exécution d'une fonction | **15 outils** `@Tool`, chacun **vérifiant les droits** |
| **Sortie structurée** | Réponse contrainte à un schéma JSON | Media plans, relances, données de paie |
| **Température** | Paramètre d'aléatoire du modèle | **0,7** conversation · **0,4** structuré |
| **Fenêtre de contexte** | Quantité de texte traitée en une fois | Marque + projet + historique + résultats RAG |
| **Mémoire conversationnelle** | Conserver le fil d'un échange | **20 messages** + **résumé tous les 6 évincés**, persistée en base |
| **SSE** | *Server-Sent Events* — flux HTTP serveur → client | **Streaming mot par mot**, adapté aux traitements longs |
| **LangChain4j** | Bibliothèque Java d'intégration des LLM et du RAG | Orchestration **et** couche d'abstraction du fournisseur |
| **Hallucination** | Information plausible mais fausse | Combattue par : calculs en Java + ancrage RAG |
| **Injection de prompt** | Détourner le modèle par le texte soumis | Défense : **les droits sont en Java, pas dans le prompt** |
| **Fine-tuning** | Réentraîner un modèle sur des données spécifiques | **Non utilisé** — voir « RAG vs fine-tuning » |

---

## 8. ⚠️ Incohérences détectées entre la PPT et le rapport

> Les connaître, c'est pouvoir y répondre calmement. **Idéalement, corrigez-les avant la soutenance.**

### ⚠️ N° 1 — Neuf ou dix sprints ? *(le plus exposé)*

| Source | Ce qui est affirmé |
|---|---|
| Rapport §2.3.2, texte | « réparti en **neuf** sprints de deux semaines » |
| Rapport, tableau 2.10 | **Dix lignes**, dont **deux libellées « Sprint 9 »** |
| Rapport, chapitre 8 | « le backlog du **Sprint 10** » |

**✅ Correction :** renommer la dernière ligne du tableau 2.10 en « Sprint 10 » et corriger le texte en « dix sprints ».
**Réponse si relevé :** voir question difficile **D7**.

---

### ⚠️ N° 2 — Les charges de sprint ne concordent pas

| Sprint | Tableau 2.10 | Backlog détaillé (chapitres 3–8) |
|---|---|---|
| 1 | 23 | ✅ 23 |
| 2 | 34 | ✅ 34 |
| **3** | **21** | **17** ❌ |
| **4** | **21** | **29** ❌ |
| **5** | **22** | **33** ❌ |
| 6 | 28 | ✅ 28 |
| 7 | 18 | ✅ 18 |
| **8** | **25** | **24** ❌ |
| **9 (IA)** | **16** | **32** ❌ |
| 10 (BI) | 16 | ✅ 16 |
| **Total** | **affiché 208**, somme réelle **224** | **254** |

**Le total affiché (208) ne correspond ni à la somme de ses propres lignes (224), ni aux backlogs détaillés (254).**

**✅ Correction :** aligner le tableau 2.10 sur les backlogs détaillés et recalculer le total à **254**.
**Chiffres à retenir : 23 · 34 · 17 · 29 · 33 · 28 · 18 · 24 · 32 · 16 = 254.**

---

### ⚠️ N° 3 — Deux modules « M18 »

| Source | Affirmation |
|---|---|
| Tableau 2.9 (backlog produit) | **M18 = Assistant Finance** |
| Chapitre 8, §8.1 | « le module **M18 « Décisionnel »** » |
| Tableau 2.10 | Le décisionnel y est décrit comme **M21 à M24** |

**✅ Correction :** dans le chapitre 8, remplacer « M18 » par **M21–M24**, cohérent avec le tableau 2.10.

---

### ⚠️ N° 4 — « Sept chapitres » annoncés, huit présents

L'Introduction générale annonce : *« Notre rapport est composé d'une introduction, de **sept chapitres** et une conclusion »* — puis **énumère huit chapitres**, le huitième étant l'informatique décisionnelle.

**✅ Correction :** remplacer « sept » par « **huit** ».

---

### ⚠️ N° 5 — Deux formulations de la problématique

| Source | Formulation |
|---|---|
| **PPT, diapositive 5** | « …tout en exploitant **l'IA et la Business Intelligence** pour offrir un suivi en temps réel, un portail client sécurisé et une meilleure aide à la décision ? » |
| **Rapport, §1.3.1** | « …plateforme **centralisée, modulaire et automatisée**… tout en garantissant à chaque acteur, interne comme externe, une visibilité claire et en temps réel sur son périmètre d'activité ? » |

**Ce n'est pas une contradiction** — la version PPT intègre les deux dernières releases. **Apprenez celle de la PPT** (c'est elle que le jury verra), et sachez expliquer l'écart.

---

### ⚠️ N° 6 — Mistral Large absent de la PPT

Le rapport (tableau 7.1) compare **5 modèles** ; la diapositive 30 n'en montre que **4**. **Mistral Large**, classé **« alternative »**, manque.

💡 **C'est en réalité un atout** : c'est votre meilleure réponse sur la souveraineté des données. **Citez-le spontanément.**

---

### ⚠️ N° 7 — Brevo absent de la liste des acteurs

Le service de messagerie **n'apparaît pas** dans les acteurs secondaires (§2.1.1 et diapositive 11), alors qu'il figure dans les **diagrammes de cas d'utilisation** (diapositives 13 et 17, « Service de messagerie `<<système>>` ») et dans l'**architecture physique** (diapositive 22, « Brevo »).

**✅ Correction :** l'ajouter à la liste des acteurs secondaires.

---

### ⚠️ N° 8 — L'approbation client absente du diagramme Client

Le diagramme de la diapositive 18 ne montre pas « Approuver / refuser un plan média », alors que c'est une fonctionnalité centrale (M12.7 du backlog, diapositives 8 et 25).

**✅ Correction :** ajouter ce cas d'utilisation au diagramme. **Correction de 5 minutes qui supprime une question embarrassante sur votre fonctionnalité phare.**

---

### ⚠️ N° 9 — Orthographe « Nouar » / « Naouar »

Remerciements : « M. Malek **Nouar** » · Tableau 1.1 et PPT : « Malek **Naouar** ».
**✅ Harmonisez.**

---

### ⚠️ N° 10 — Technologies du rapport absentes de la PPT

**Power BI, DAX, Electron, Swagger, Draw.io, Axios, React Router, Brevo, Google Drive API** figurent dans le rapport (§2.4.2) mais pas sur la diapositive 31.
**Ce n'est pas une contradiction** — la diapositive est une synthèse. Mais sachez répondre si le jury demande « où est Power BI ? » (voir diapositive 31, §5).

---

### ✅ Points parfaitement cohérents — aucun risque

| Élément | Vérifié |
|---|---|
| Les 6 acteurs principaux et leurs descriptions | ✅ PPT = rapport, mot pour mot |
| Les 6 modules de besoins fonctionnels et leurs puces | ✅ identiques |
| Odoo / BambooHR / monday.com et leurs forces-faiblesses | ✅ identiques |
| Les 3 raisons du choix de Scrum | ✅ identiques |
| Architecture N-Tiers, 4 clients, ports, base `antigone_rh` | ✅ identiques |
| GPT-4o sélectionné + raisons de rejet de Claude, Gemini, Llama | ✅ identiques |
| Config IA : 20 messages, résumé tous les 6, 1536 dim., top-k 12/12 | ✅ identiques |
| BI : 4 dimensions + 6 tables de faits, ETL 02h30 | ✅ identiques |
| 15 outils · réindexation quotidienne · fusion RRF | ✅ identiques |

---

## 9. ✅ Checklist finale

### Corrections sur les livrables
- [ ] **Tableau 2.10** : renommer la 2ᵉ « Sprint 9 » en « Sprint 10 », aligner les charges sur les backlogs (17·29·33·24·32), corriger le total à **254**
- [ ] **Texte §2.3.2** : « neuf sprints » → « **dix sprints** »
- [ ] **Chapitre 8** : « module M18 » → « **M21–M24** »
- [ ] **Introduction générale** : « sept chapitres » → « **huit chapitres** »
- [ ] **Harmoniser** « Nouar » / « **Naouar** »
- [ ] **Diagramme Client (diapo 18)** : ajouter « Approuver / refuser un plan média »
- [ ] **Acteurs secondaires** : ajouter **Brevo / Service de messagerie**
- [ ] *(Optionnel)* Ajouter **Mistral Large** à la diapositive 30

### Vérifications dans le code
- [ ] **Stratégie `@Inheritance`** sur l'entité `Demande`
- [ ] **Où est stocké le JWT** côté frontend
- [ ] **Gestion d'état React** : bibliothèque ou mécanismes natifs ?
- [ ] **Gestionnaire global d'exceptions** : existe-t-il ?
- [ ] **Nombre d'itérations** de la dichotomie NET→BRUT
- [ ] **Un validateur peut-il valider sa propre demande ?**
- [ ] **Peut-on annuler un créneau de tournage confirmé ?**
- [ ] **Droits Drive côté portail client** : lecture seule ?
- [ ] **Format d'export** des données projet
- [ ] **Pourquoi `frontend-projects` porte le login**
- [ ] **Tests unitaires automatisés** : en existe-t-il ?
- [ ] **Nombre d'entités / de contrôleurs**, si vous voulez pouvoir donner un chiffre

### À préparer séparément *(non documenté)*
- [ ] 🚨 **La répartition du travail entre vous et Zeineb Haj Hsine** — question quasi certaine
- [ ] **L'effectif d'Antigone** et son nombre de clients
- [ ] **Les 4 dimensions et les 6 tables de faits** de l'entrepôt (figure 8.2)
- [ ] **Les 12 types de congé**, appris par cœur
- [ ] **La problématique de la PPT**, mot pour mot
- [ ] **La cascade de pointage** dans l'ordre
- [ ] **Le workflow Plan Média** en 5 étapes
- [ ] **L'explication de la dichotomie** NET→BRUT

### Démonstration
- [ ] Backend démarré et testé · 3 serveurs Vite lancés · PostgreSQL peuplé
- [ ] **Clé API OpenAI valide et quota disponible**
- [ ] Index RAG réindexé la veille
- [ ] Comptes de démonstration testés, mots de passe notés
- [ ] Onglets pré-ouverts et pré-connectés
- [ ] **Captures ou vidéo de secours** prêtes en deux clics
- [ ] Déroulé **chronométré deux fois**
- [ ] Les 3 moments-clés répétés : création auto du projet + 3 tâches · statut de facture Partiel → Payé · assistant citant un article

### Les 5 phrases à avoir en tête
1. **« La logique métier sensible reste portée par le backend — jamais déléguée au frontend ni au modèle de langage. »**
2. **« Le modèle reformule un calcul déjà fait : il n'invente jamais un chiffre. »**
3. **« Chaque outil vérifie les droits d'accès avant toute opération — le contrôle est en Java, pas dans le prompt. »**
4. **« Cette partie n'a pas été mesurée dans le cadre de mon projet, donc je préfère ne pas avancer de chiffre non vérifié. »**
5. **« C'est une limite que j'ai identifiée et que j'assume. »**

---

**Bonne soutenance.**


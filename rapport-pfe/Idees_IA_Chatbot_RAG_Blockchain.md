# Idées IA (Chatbot / RAG) et Blockchain pour Antigone

Ce document liste des idées de fonctionnalités intelligentes qu'on pourrait ajouter à la plateforme Antigone, expliquées simplement, module par module. L'objectif n'est pas de tout faire, mais de choisir 1 ou 2 idées réalistes selon le temps disponible.

---

## C'est quoi le RAG, en simple ?

RAG veut dire *Retrieval-Augmented Generation*. En clair : au lieu de laisser une IA répondre "de mémoire" (avec le risque qu'elle invente des choses fausses), on lui donne d'abord les bonnes informations à lire — extraites de notre propre base de données ou de nos documents — puis on lui demande de répondre **en se basant uniquement sur ça**.

Exemple concret : un employé demande *"combien de jours de congé me reste-t-il ?"*. Le chatbot va d'abord chercher le vrai solde de cet employé dans la base de données, puis reformule la réponse en langage naturel. Il ne devine pas, il lit la vraie donnée avant de répondre.

On distingue deux façons de faire du RAG dans ce projet :

- **RAG documentaire** : l'IA cherche dans des textes (règlement intérieur, description des types de congé, politiques internes) pour répondre à des questions générales.
- **RAG structuré** (aussi appelé *function calling*) : l'IA interroge directement la base de données (via nos services existants) pour répondre à des questions personnelles ou chiffrées.

Antigone a déjà un `ChatbotController` de base — ces idées consistent à le rendre vraiment intelligent en le connectant aux bonnes sources d'information.

---

## Module RH

### 1. Assistant RH qui connaît les vraies règles
Aujourd'hui, un employé qui veut savoir s'il a droit à un congé doit connaître les règles par cœur (règle des 4×, quotas d'autorisation, types de congé limités en jours...). L'idée : le chatbot lit ces règles (déjà présentes dans le code et les référentiels) et répond correctement, exemple : *"Non, le congé maladie est limité à 2 jours ouvrables et nécessite un certificat médical."*

**Pourquoi c'est utile :** réduit les questions répétitives posées au service RH.

### 2. Assistant qui connaît MES données personnelles
Le chatbot répond à des questions comme *"où en est ma demande de télétravail ?"* ou *"combien de jours de congé me reste-t-il ?"* en interrogeant directement le profil de la personne connectée — jamais celui d'un autre employé.

**Pourquoi c'est utile :** évite d'ouvrir plusieurs pages pour retrouver une info simple.

### 3. Détecteur d'anomalies RH
Un système (pas forcément une IA complexe, des règles suffisent au départ) qui repère des comportements inhabituels : quelqu'un qui est souvent en retard, qui pose beaucoup de congés maladie sans justificatif, etc. Il alerte le RH pour qu'il regarde de plus près.

**Pourquoi c'est utile :** aide le RH à repérer des problèmes sans devoir tout vérifier manuellement.

### 4. Aide à la rédaction
Quand un manager refuse une demande, l'IA propose une formulation professionnelle et polie du motif de refus, à partir d'une note rapide du manager.

**Pourquoi c'est utile :** gagne du temps et garde un ton cohérent dans toute l'entreprise.

---

## Module Projets & Plans médias

### 5. Assistant "où en est mon projet ?"
Le chef de projet (ou le client) pose une question comme *"quelles tâches sont en retard sur le projet X ?"* et l'IA va chercher la vraie réponse dans les tâches, réunions et plans médias de ce projet précis.

**Attention importante :** si le client pose la question, l'IA ne doit voir QUE les données de ses propres projets, jamais celles des autres clients.

**Pourquoi c'est utile :** évite de fouiller manuellement dans le tableau Kanban ou les plans médias pour connaître l'avancement.

### 6. Générateur d'idées de plan média
Le chef de projet donne un brief en texte libre ("un client veut une campagne pour un lancement de produit bio") et l'IA propose des idées de contenu, de formats et de plateformes adaptées, en s'inspirant éventuellement des plans médias passés qui ont bien fonctionné.

**Pourquoi c'est utile :** aide à démarrer une campagne plus vite, surtout pour les petites équipes.

### 7. Résumé automatique de réunion
Après une réunion projet, l'IA résume les points discutés et liste les actions à faire, à partir des notes ou d'une transcription.

**Pourquoi c'est utile :** tout le monde n'a pas le temps de relire un compte-rendu entier.

### 8. Suggestion d'attribution de tâches
Quand une nouvelle tâche est créée, l'IA suggère à qui l'assigner en fonction des compétences de l'équipe et de leur charge de travail actuelle.

**Pourquoi c'est utile :** évite de surcharger toujours les mêmes personnes.

---

## Module Finance

### 9. Assistant financier en langage naturel
Le responsable finance demande *"quel est le total des factures impayées de plus de 30 jours ?"* et l'IA calcule la vraie réponse à partir des factures enregistrées, sans avoir besoin de construire un filtre manuellement.

**Pourquoi c'est utile :** rend les infos financières accessibles sans savoir utiliser des filtres complexes.

### 10. Lecture automatique de factures fournisseurs
On prend en photo ou on scanne une facture reçue (loyer, abonnement, etc.), et l'IA lit automatiquement le montant, la date et le fournisseur pour préremplir l'enregistrement de la charge.

**Pourquoi c'est utile :** évite la saisie manuelle, source d'erreurs et de perte de temps.

### 11. Prévision simple de trésorerie
En regardant les factures à encaisser et les charges à payer dans les prochaines semaines, le système donne une estimation de l'argent disponible, avec une explication en langage simple générée par l'IA.

**Pourquoi c'est utile :** aide à anticiper les périodes difficiles avant qu'elles n'arrivent.

### 12. Rédaction automatique des relances clients
Selon le nombre de jours de retard de paiement, l'IA propose un e-mail de relance avec le ton adapté (rappel doux au début, plus ferme si le retard est long).

**Pourquoi c'est utile :** fait gagner du temps et garde un ton professionnel constant.

### 13. Explication du bulletin de paie
Un employé demande *"pourquoi mon salaire net est-il différent ce mois-ci ?"* et l'IA explique en se basant sur son vrai bulletin de paie et le barème IRPP en vigueur.

**Pourquoi c'est utile :** réduit les questions au service paie sur des calculs qui semblent obscurs.

---

## Idées Blockchain — expliquées simplement

La blockchain, en très simple, c'est une façon d'enregistrer des informations de sorte que **personne ne puisse les modifier discrètement après coup**, sans que ça se voie. Chaque nouvelle information est reliée mathématiquement à la précédente (comme une chaîne), donc si quelqu'un essaie de trafiquer une ancienne entrée, la chaîne "casse" visiblement.

### Idée principale recommandée : un registre infalsifiable des décisions importantes

Antigone enregistre déjà l'historique des décisions (qui a validé un congé, qui a modifié un solde, qui a émis une facture). L'idée blockchain, c'est de rendre cet historique **impossible à trafiquer** :

1. Chaque décision importante est transformée en une empreinte numérique unique (un "hash").
2. Cette empreinte est reliée à celle de la décision précédente, formant une chaîne.
3. Si quelqu'un modifie une ancienne décision dans la base de données, la chaîne ne correspond plus — la fraude devient visible immédiatement.

**Deux niveaux possibles :**
- **Version simple** : la chaîne est stockée directement dans notre base PostgreSQL. Facile à mettre en place, démontre bien le concept.
- **Version plus avancée** : on ancre régulièrement cette chaîne sur un vrai réseau blockchain externe (par exemple un réseau de test Polygon), pour prouver que même Antigone elle-même ne pourrait pas trafiquer discrètement ses propres données.

**Pourquoi c'est utile concrètement :** en cas de litige (un employé conteste un refus de congé, un client conteste une facture), on peut prouver que la décision enregistrée n'a jamais été modifiée après coup.

### Idée complémentaire : certifier les documents générés

Quand le système génère un document officiel (attestation de travail, bulletin de paie, facture PDF), on calcule son empreinte numérique et on l'enregistre. Un QR code sur le document permet à n'importe qui (une banque, l'administration) de vérifier que ce document est authentique et n'a pas été modifié depuis sa création.

**Pourquoi c'est utile :** un employé pourrait présenter son attestation de travail à une banque, qui peut vérifier son authenticité sans avoir besoin d'appeler l'entreprise.

### Idées à éviter (trop complexes pour peu de bénéfice)

- Faire valider les congés "directement sur la blockchain" (smart contracts) : complique énormément le système pour un gain quasi nul dans une petite agence.
- Automatiser les paiements de factures via la blockchain : demande une vraie crypto-monnaie ou un système de paiement compatible, hors sujet ici.

---

## Comment présenter tout ça dans le rapport

Plutôt que de présenter l'IA et la blockchain comme deux ajouts séparés, un seul fil conducteur les relie bien : **rendre la plateforme plus digne de confiance**.

- L'**IA** rend l'information plus **accessible** (on pose une question, on a une réponse claire).
- La **blockchain** rend l'information plus **fiable** (on est sûr qu'elle n'a pas été trafiquée).

Ensemble, ça raconte une histoire cohérente : "Antigone devient une plateforme où l'information est facile à obtenir ET où on peut lui faire confiance."

---

## Résumé rapide — quoi choisir en priorité ?

| Idée | Effort estimé | Impact démonstratif |
|---|---|---|
| Assistant RH avec RAG sur les vraies règles + données personnelles | Moyen | Élevé — facile à montrer en direct |
| Assistant financier en langage naturel | Moyen | Élevé — très concret en soutenance |
| Registre infalsifiable des décisions (version simple) | Moyen | Élevé — bonne histoire technique à raconter |
| Résumé automatique de réunion | Faible | Moyen |
| Certification des documents (QR code) | Moyen | Moyen-Élevé |
| Lecture automatique de factures (OCR) | Élevé | Moyen |
| Prévision de trésorerie | Élevé | Moyen |
| Smart contracts de validation | Très élevé | Faible (risqué à défendre) |

# API Documentation — Assistant IA Antigone

Backend de l'assistant conversationnel d'Antigone : génération de media plan, rédaction de relances clients et explication de bulletins de paie.

- **Base URL locale** : `http://localhost:8080`
- **Préfixe des endpoints IA** : `/api/v1`
- **Swagger UI** : `http://localhost:8080/swagger-ui.html` — OpenAPI brut sur `/v3/api-docs`
- **Authentification** : JWT `Bearer`, identique au reste de l'application (`POST /api/auth/login`)

> Ce document décrit le comportement **réellement implémenté**. Quand il s'écarte du cahier des charges initial, l'écart est signalé et justifié — voir [Écarts assumés](#écarts-assumés-par-rapport-au-cahier-des-charges).

---

## Table des matières

1. [Authentification et rôles](#1-authentification-et-rôles)
2. [Format des erreurs](#2-format-des-erreurs)
3. [Conversations](#3-conversations)
4. [Streaming SSE — contrat d'événements](#4-streaming-sse--contrat-dévénements)
5. [Media Plan](#5-media-plan)
6. [Relances clients](#6-relances-clients)
7. [Bulletin de paie](#7-bulletin-de-paie)
8. [Administration de l'assistant](#8-administration-de-lassistant)
9. [Profil de marque (champs `Client`)](#9-profil-de-marque-champs-client)
10. [Configuration](#10-configuration)
11. [Tests](#11-tests)
12. [Écarts assumés](#écarts-assumés-par-rapport-au-cahier-des-charges)

---

## 1. Authentification et rôles

### `POST /api/auth/login`

```json
{ "username": "sonia.b", "password": "••••••" }
```

Réponse :

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenExpiresAt": "2026-08-26T22:14:00Z",
    "username": "sonia.b",
    "roles": ["SOCIAL_MEDIA"],
    "permissions": ["VIEW_MEDIA_PLAN", "VIEW_MES_PROJETS"]
  }
}
```

Toutes les requêtes `/api/v1/**` exigent l'en-tête :

```
Authorization: Bearer <token>
```

### Correspondance rôles ↔ capacités

Antigone n'utilise pas d'énumération de rôles : les rôles sont des lignes en base, et les droits s'expriment par **permissions**. Les capacités de l'assistant s'y rattachent ainsi :

| Capacité | Condition d'accès | Périmètre de données |
|---|---|---|
| Media Plan | permission `VIEW_MEDIA_PLAN` ou `VIEW_TOUS_MEDIA_PLAN`, ou rôle `ADMIN` | `VIEW_MEDIA_PLAN` → uniquement les clients assignés via `media_plan_assignments`. `VIEW_TOUS_MEDIA_PLAN` / `ADMIN` → tous les clients. |
| Relances clients | rôle `ADMIN` ou permission `VIEW_FINANCE` | toutes les factures |
| Bulletin de paie | tout compte employé authentifié | `ADMIN`/`VIEW_FINANCE` → n'importe quel employé. Sinon → **uniquement son propre bulletin**. |
| Conversations | tout compte authentifié | uniquement ses propres conversations |

Le contrôle d'accès n'est jamais délégué au prompt : chaque outil et chaque service repasse par `AiAccessScope`.

---

## 2. Format des erreurs

Les endpoints `/api/v1/**` renvoient :

```json
{ "error": "message lisible", "code": "CODE_MACHINE" }
```

| HTTP | `code` | Cause |
|---|---|---|
| 400 | `BAD_REQUEST` | paramètre invalide (mois non parsable, facture déjà soldée…) |
| 403 | `FORBIDDEN` | capacité hors périmètre, marque non assignée, bulletin d'un tiers, envoi direct désactivé |
| 403 | *(pas de corps)* | jeton absent, expiré ou invalide — voir la note ci-dessous |
| 404 | `NOT_FOUND` | ressource inexistante **ou appartenant à un autre compte** |
| 422 | `VALIDATION_ERROR` | corps de requête invalide (`month` mal formé, `content` vide…) |
| 429 | `RATE_LIMITED` | quota d'appels IA dépassé |
| 500 | `INTERNAL_ERROR` | erreur inattendue |
| 503 | `AI_UNAVAILABLE` | aucune clé LLM configurée, ou assistant désactivé |

Exemples :

```json
{ "error": "Acces refuse : la marque 42 n'est pas dans votre perimetre.", "code": "FORBIDDEN" }
```
```json
{ "error": "month : month doit etre au format YYYY-MM", "code": "VALIDATION_ERROR" }
```
```json
{ "error": "Quota d'appels a l'assistant atteint. Reessayez dans 34 secondes.", "code": "RATE_LIMITED" }
```

> **403 et non 401 sur un jeton manquant ou expiré.** L'application ne déclare pas d'`AuthenticationEntryPoint`, donc Spring Security refuse les requêtes anonymes en 403 — sur **tous** ses endpoints, pas seulement ceux de l'assistant. Comportement historique, volontairement conservé : le passer à 401 changerait la façon dont les trois frontends déjà en production interprètent une session expirée. À trancher séparément si vous voulez la sémantique HTTP stricte.

> **404 plutôt que 403 sur une ressource d'autrui.** Accéder à la conversation d'un autre compte renvoie 404 : répondre 403 confirmerait son existence.

---

## 3. Conversations

### `GET /api/v1/conversations`

Liste paginée des conversations du compte connecté.

| Paramètre | Défaut | Description |
|---|---|---|
| `page` | `0` | index de page |
| `size` | `20` | taille (max 100) |
| `sort` | `updatedAt` | champ de tri |
| `direction` | `DESC` | `ASC` ou `DESC` |

```json
{
  "content": [
    {
      "id": 12,
      "title": "Media plan juillet Alpha",
      "pinned": true,
      "messageCount": 8,
      "createdAt": "2026-08-20T09:12:03",
      "updatedAt": "2026-08-26T11:40:55"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### `GET /api/v1/conversations/{id}`

Détail avec l'historique complet.

```json
{
  "id": 12,
  "title": "Media plan juillet Alpha",
  "pinned": true,
  "summary": "L'utilisateur prépare le media plan de juillet pour Alpha...",
  "createdAt": "2026-08-20T09:12:03",
  "updatedAt": "2026-08-26T11:40:55",
  "messages": [
    {
      "id": 101,
      "role": "USER",
      "content": "Génère le media plan de juillet pour Alpha",
      "toolCalls": null,
      "structuredResult": null,
      "capability": null,
      "sequence": 0,
      "createdAt": "2026-08-26T11:39:10"
    },
    {
      "id": 102,
      "role": "ASSISTANT",
      "content": "Voici la proposition pour juillet...",
      "toolCalls": "[{\"tool\":\"BrandInfoTool\",\"args\":\"{\\\"clientId\\\":7}\",\"status\":\"success\"}]",
      "structuredResult": null,
      "capability": "MEDIA_PLAN",
      "sequence": 1,
      "createdAt": "2026-08-26T11:40:55"
    }
  ]
}
```

`role` ∈ `USER | ASSISTANT | TOOL | SYSTEM`. `capability` ∈ `MEDIA_PLAN | REMINDER | PAYSLIP | GENERAL`.

**Erreurs** : `404 NOT_FOUND` (inexistante, supprimée, ou appartenant à un autre compte).

### `POST /api/v1/conversations`

```json
{ "title": "Media plan juillet" }
```

Le corps est optionnel. Sans titre, la conversation démarre sur `"Nouvelle conversation"` puis est **renommée automatiquement** depuis le premier message.

Réponse `200` : objet `ConversationSummary` (même forme que dans la liste).

**Erreurs** : `422 VALIDATION_ERROR` si `title` dépasse 200 caractères.

### `PATCH /api/v1/conversations/{id}`

```json
{ "title": "Nouveau titre", "pinned": true }
```

Les deux champs sont optionnels et indépendants. Réponse `200` : objet mis à jour.

**Erreurs** : `404 NOT_FOUND`, `422 VALIDATION_ERROR`.

### `DELETE /api/v1/conversations/{id}`

Réponse `204 No Content`.

Suppression **logique** : la conversation disparaît de l'API mais reste en base pour l'audit. La mémoire LangChain4j associée est, elle, réellement purgée.

**Erreurs** : `404 NOT_FOUND`.

### `POST /api/v1/conversations/{id}/messages`

Envoie un message et déclenche l'orchestration IA.

- **Requête** : `Content-Type: application/json`
- **Réponse** : `Content-Type: text/event-stream` — connexion maintenue pendant toute la génération (jusqu'à ~90 s)

```json
{ "content": "Génère le media plan de juillet pour Alpha" }
```

Le backend classe l'intention, monte le prompt et les outils correspondants, puis diffuse la réponse. Voir [§4](#4-streaming-sse--contrat-dévénements).

**Erreurs (avant ouverture du flux)** : `404 NOT_FOUND`, `422 VALIDATION_ERROR` (contenu vide ou > 8000 caractères), `429 RATE_LIMITED`, `503 AI_UNAVAILABLE`.

---

## 4. Streaming SSE — contrat d'événements

Tous les endpoints IA diffusent selon le même contrat. Chaque événement est nommé, et son `data` est un objet JSON.

### Événements

| `event` | `data` | Quand |
|---|---|---|
| `token` | `{ "delta": "fragment..." }` | à chaque fragment de texte généré |
| `tool_call_start` | `{ "tool": "GoogleDriveTool", "args": { ... } }` | avant l'exécution d'un outil ou d'une étape de pipeline |
| `tool_call_end` | `{ "tool": "GoogleDriveTool", "status": "success"\|"error", "detail": "..." }` | après |
| `heartbeat` | `{}` | toutes les 15 s **uniquement** pendant les phases sans token |
| `structured_result` | objet métier complet | une seule fois, à la fin, si applicable |
| `error` | `{ "code": "...", "message": "..." }` | en cas d'échec |
| `done` | `{}` | **toujours** en dernier — le client ferme alors la connexion |

### Codes d'erreur du flux

`FORBIDDEN`, `NOT_FOUND`, `BAD_REQUEST`, `RATE_LIMITED`, `AI_UNAVAILABLE`, `AI_BUSY` (pool saturé), `GENERATION_FAILED`.

### Exemple de flux (génération de media plan)

```
event:tool_call_start
data:{"tool":"BrandInfoTool","args":{"clientId":7}}

event:tool_call_end
data:{"tool":"BrandInfoTool","status":"success","detail":"Alpha"}

event:tool_call_start
data:{"tool":"GoogleDriveTool","args":{"clientNom":"Alpha","mois":"2026-07"}}

event:heartbeat
data:{}

event:heartbeat
data:{}

event:tool_call_end
data:{"tool":"GoogleDriveTool","status":"success","detail":"https://drive.google.com/..."}

event:structured_result
data:{"clientId":7,"month":"2026-07","items":[...]}

event:done
data:{}
```

### Consommation côté React

`EventSource` natif **ne sait pas émettre de POST**. Le backend expose ces flux en POST (le corps porte le message ou les paramètres), donc le client doit lire le flux avec `fetch` :

```ts
export async function streamMessage(
  conversationId: number,
  content: string,
  token: string,
  handlers: {
    onToken?: (delta: string) => void;
    onToolStart?: (tool: string, args: unknown) => void;
    onToolEnd?: (tool: string, status: string) => void;
    onResult?: (result: unknown) => void;
    onError?: (code: string, message: string) => void;
  },
  signal?: AbortSignal,
) {
  const response = await fetch(`/api/v1/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ content }),
    signal,
  });

  if (!response.ok) {
    const { error, code } = await response.json();
    handlers.onError?.(code, error);
    return;
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // Les trames SSE sont séparées par une ligne vide.
    const frames = buffer.split('\n\n');
    buffer = frames.pop() ?? '';

    for (const frame of frames) {
      let event = 'message';
      let data = '';
      for (const line of frame.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim();
        else if (line.startsWith('data:')) data += line.slice(5).trim();
      }
      if (!data) continue;
      const payload = JSON.parse(data);

      switch (event) {
        case 'token':            handlers.onToken?.(payload.delta); break;
        case 'tool_call_start':  handlers.onToolStart?.(payload.tool, payload.args); break;
        case 'tool_call_end':    handlers.onToolEnd?.(payload.tool, payload.status); break;
        case 'structured_result': handlers.onResult?.(payload); break;
        case 'error':            handlers.onError?.(payload.code, payload.message); break;
        case 'done':             return;
      }
    }
  }
}
```

**Points d'implémentation**

- Utiliser `tool_call_start` / `tool_call_end` pour afficher une progression lisible (« Récupération de la marque… », « Création du dossier Drive… ») plutôt qu'un spinner figé pendant une minute.
- Ignorer les `heartbeat` côté affichage : ils n'existent que pour empêcher un proxy de fermer la connexion.
- **Si le flux est interrompu, rien n'est perdu** : le message assistant est persisté côté serveur *avant* toute tentative d'émission finale. Un `GET /api/v1/conversations/{id}` le retrouve intégralement.
- Passer un `AbortSignal` pour annuler proprement à la fermeture du composant.

### Variante JSON bloquante

`/media-plans/generate`, `/reminders/generate` et `/payslip/explain` répondent aussi en JSON classique si la requête envoie `Accept: application/json`. Utile pour les intégrations et les tests ; **déconseillé pour le frontend**, qui attendrait jusqu'à 90 s sans aucun retour visible.

---

## 5. Media Plan

### `POST /api/v1/media-plans/generate`

**Accès** : `VIEW_MEDIA_PLAN`, `VIEW_TOUS_MEDIA_PLAN` ou rôle `ADMIN`, **et** la marque doit être dans le périmètre de l'utilisateur.

```json
{ "clientId": 7, "month": "2026-07" }
```

#### Pipeline exécuté (dans cet ordre)

| Étape | `tool` émis en SSE | Rôle |
|---|---|---|
| 1 | `BrandInfoTool` | identité, activité, positionnement, objectifs |
| 2 | `ProjectInfoTool` | projets et actions actifs sur la période |
| 3 | `PreviousMediaPlansTool` | 3 mois d'historique + recherche hybride |
| 4 | `RealizedContentTool` | formats / plateformes / types déjà exploités |
| 5 | `MediaPlanGenerator` | appel LLM à sortie JSON stricte |
| 6 | `GoogleDriveTool` | création / résolution du dossier du mois |
| 7 | `MediaPlanPersistence` | écriture des lignes `media_plans` |

#### Réponse (`structured_result` en SSE, ou corps JSON)

```json
{
  "clientId": 7,
  "clientNom": "Alpha",
  "month": "2026-07",
  "status": "EN_ATTENTE",
  "drivePending": false,
  "syntheseEditoriale": "Mois centré sur l'usage produit, en contrepoint des coulisses d'atelier déjà traitées.",
  "thematiquesEvitees": ["Coulisses de l'atelier", "Portraits d'artisanes"],
  "items": [
    {
      "id": 4821,
      "datePublication": "2026-07-03",
      "heure": "09:30",
      "titre": "Le nouveau rituel du matin",
      "texteSurVisuel": "3 gestes, 5 minutes",
      "inspiration": "Format « get ready with me » revisité",
      "autresElements": "#antigone #madeintunisia",
      "platforme": "Instagram",
      "format": "Reel",
      "type": "Brand content",
      "lienDrive": "https://drive.google.com/drive/folders/abc123",
      "etatPublication": "PAS_ENCORE",
      "statut": "EN_ATTENTE",
      "remarques": "Sert l'objectif de notoriété, jour de forte audience."
    }
  ]
}
```

**Noms de champs** : ce sont ceux de l'entité `MediaPlan` réelle, pas les libellés génériques du cahier des charges. Correspondance :

| Cahier des charges | Champ réel |
|---|---|
| `titre` | `titre` |
| `texte` | `texteSurVisuel` |
| `inspiration` | `inspiration` |
| `autre` | `autresElements` |
| `plateforme` | `platforme` *(orthographe de la base)* |
| `lienDrive` | `lienDrive` |
| `etat` | `etatPublication` |
| — | `heure`, `format`, `type`, `statut`, `remarques` |

La **justification éditoriale** de chaque publication est écrite dans `remarques`, donc consultable depuis l'écran Media Plan existant.

#### Résilience Google Drive

Si Drive est indisponible, la génération **aboutit quand même** : les lignes sont persistées avec `lienDrive: "PENDING"` et `drivePending: true`. Aucun contenu généré n'est perdu.

**Erreurs** : `403 FORBIDDEN` (permission manquante ou marque hors périmètre), `404 NOT_FOUND` (client inconnu), `422 VALIDATION_ERROR`, `429 RATE_LIMITED`, `503 AI_UNAVAILABLE`.

### `POST /api/v1/media-plans/{clientId}/{month}/retry-drive`

Reprend l'approvisionnement Drive des publications restées en `PENDING`. **Seule l'étape Drive est rejouée** — la génération n'est jamais relancée, donc aucun appel LLM supplémentaire.

```json
{
  "clientId": 7,
  "month": "2026-07",
  "updated": 9,
  "message": "9 publication(s) approvisionnee(s)."
}
```

`updated: 0` signifie soit qu'il n'y avait rien en attente, soit que Drive est toujours indisponible.

---

## 6. Relances clients

Deux étapes distinctes : l'assistant **rédige**, l'utilisateur **relit et envoie**. L'IA n'expédie jamais de courrier d'elle-même.

### `POST /api/v1/reminders/generate`

**Accès** : rôle `ADMIN` ou permission `VIEW_FINANCE`.

```json
{ "invoiceId": 314 }
```

#### Paliers de ton

Le palier est **calculé par le serveur** depuis les jours de retard réels, puis imposé au modèle. Bornes configurables via `app.ai.reminder.*`.

| Jours de retard | `tone` | Registre |
|---|---|---|
| 0 – 7 | `SOFT` | rappel bienveillant, on suppose un oubli |
| 8 – 30 | `FIRM` | ferme et courtois, date de règlement demandée |
| 31 et + | `FORMAL` | formel, suites possibles évoquées factuellement |

#### Réponse

```json
{
  "reminderId": 77,
  "invoiceId": 314,
  "invoiceNumero": "FAC-2026-0314",
  "clientNom": "Alpha",
  "clientEmail": "compta@alpha.tn",
  "subject": "Rappel — facture FAC-2026-0314 échue le 12/07/2026",
  "body": "Madame,

Sauf erreur de notre part, la facture FAC-2026-0314...

L'équipe Antigone",
  "htmlPreview": "<!DOCTYPE html>…",
  "tone": "FORMAL",
  "daysLate": 45,
  "amountDue": 3000.0,
  "sent": false
}
```

`sent` vaut **toujours `false`** ici : la génération produit un brouillon, jamais un envoi. `reminderId` est la référence à passer à l'endpoint d'envoi. `htmlPreview` est le message mis en page tel que le client le recevra.

**Erreurs** : `400 BAD_REQUEST` (facture soldée, ou document de type devis), `403 FORBIDDEN`, `404 NOT_FOUND`, `422 VALIDATION_ERROR`, `429`, `503`.

### `POST /api/v1/reminders/{reminderId}/send`

Expédie le brouillon **après relecture par l'utilisateur**.

```json
{ "reminderId": 77, "invoiceNumero": "FAC-2026-0314", "sent": true, "…": "…" }
```

Le serveur envoie **le texte enregistré, sans régénération** : le client reçoit exactement ce que l'utilisateur a validé. L'email est mis en page aux couleurs d'Antigone (violet `#683b77`, même charte que les e-mails d'identifiants et de réinitialisation), avec un récapitulatif montant / échéance en tête.

Un second appel sur une relance déjà partie **ne la renvoie pas** — le client recevrait deux fois le même rappel. La réponse indique simplement `sent: true`.

Aucun quota IA sur cet endpoint : aucune génération n'a lieu, seul un envoi d'e-mail.

**Erreurs** : `400 BAD_REQUEST` (aucune adresse e-mail sur la fiche client), `403 FORBIDDEN`, `404 NOT_FOUND` (brouillon inconnu).

> **Pourquoi deux étapes.** Une relance engage la relation commerciale. La validation doit venir de quelqu'un qui a lu le texte — un indicateur de configuration ne sait pas distinguer un message relu d'un message simplement généré. Chaque brouillon est historisé dans `relances_clients` (objet, corps, ton, destinataire, date d'envoi) et reste consultable depuis l'écran Finance.

---

## 7. Bulletin de paie

### `POST /api/v1/payslip/explain`

**Accès** : tout compte employé authentifié.

```json
{ "employeeId": 42, "month": "2026-07" }
```

> **Mois demandé absent.** Si aucun bulletin n'existe pour le mois indiqué — cas courant quand la paie du mois en cours n'est pas close — l'API se rabat automatiquement sur **le dernier bulletin disponible** et le signale dans l'explication. Un `404` n'est renvoyé que si l'employé n'a **aucun** bulletin.

> **Règle d'isolation.** `employeeId` n'est honoré que pour un compte `ADMIN` / `VIEW_FINANCE`. Pour tout autre compte, il est **remplacé par l'identifiant du JWT**, et une demande visant explicitement un tiers est refusée en `403`. Le refus intervient avant toute lecture en base : aucune donnée d'un collègue n'entre jamais dans le contexte envoyé au modèle. Un employé peut donc omettre `employeeId`.

#### Réponse

```json
{
  "explanation": "Votre net à payer passe de 2 310,000 DT en juin à 2 130,000 DT en juillet, soit 180,000 DT de moins. Deux causes : un acompte de 150,000 DT vous a été versé en cours de mois, et votre IRPP mensuel augmente de 60,000 DT...",
  "comparison": {
    "previousNet": 2310.0,
    "currentNet": 2130.0,
    "delta": -180.0,
    "deltaReasons": [
      "Acompte de 150,000 DT déduit du net à payer",
      "IRPP mensuel passé de 240,000 à 300,000 DT"
    ]
  }
}
```

**Exactitude des montants** : `previousNet`, `currentNet` et `delta` sont **écrasés par les valeurs de la base** après la génération, et les écarts sont calculés en Java avant d'être fournis au modèle. Le LLM rédige l'explication, il ne calcule rien.

**Décomposition du net.** Le contexte transmis au modèle contient, déjà chiffrée, chaque étape du calcul — c'est ce qui permet à l'assistant d'expliquer *pourquoi* le montant vaut ce qu'il vaut, et non de se contenter de le répéter :

| # | Formule | Fourni au modèle |
|---|---|---|
| 1 | Brut effectif = salaire brut + bonus − absences | montant |
| 2 | CNSS salarié = base CNSS × taux CNSS | taux + montant |
| 3 | Salaire imposable = brut (ajusté IRPP) − CNSS | montant |
| 4 | Abattement = salaire imposable × taux d'abattement | taux + montant |
| 5 | Revenu net imposable = salaire imposable − abattement | montant |
| 6 | Contribution de solidarité (CSS) = base × taux CSS | taux + montant |
| 7 | IRPP mensuel = irppAnnuel(revenu net imposable × 12) ÷ 12 | barème + montant |
| 8 | Net = brut effectif − CNSS − CSS − IRPP | montant |
| 9 | Net à payer = Net − acomptes déjà versés | montant |

Les charges employeur (CNSS patronale + TFP + FOPROLOS + AT) et le coût total sont également fournis, signalés comme n'affectant pas le net.

Les contrats **CIVP, Freelance et Stage** sont exonérés : le contexte l'indique explicitement et l'assistant annonce d'emblée que le net est égal au brut, sans CNSS, CSS ni IRPP.

Le barème IRPP annuel en vigueur accompagne le tout, tranche par tranche.

Sans bulletin du mois précédent, `previousNet` et `delta` valent `null` et `deltaReasons` est vide.

**Erreurs** : `403 FORBIDDEN` (bulletin d'un tiers, ou compte sans employé rattaché), `404 NOT_FOUND` (aucun bulletin pour le mois), `422`, `429`, `503`.

---

## 7bis. Règlement intérieur

Pas d'endpoint dédié : les questions sur le règlement intérieur (horaires, congés, confidentialité, sécurité, sanctions et procédure disciplinaire, usage du matériel, formation) passent par le chat libre (`POST /api/v1/conversations/{id}/messages`), classées `GENERAL`.

Le document (`Backend/src/main/resources/reglement_interieur.txt`, extrait du PDF partagé par l'agence) est indexé dans le RAG hybride, **un chunk par article** (`AiSourceType.POLICY`, `clientId` null — document transverse, lisible par tout employé indépendamment de son périmètre de marques). `InternalPolicyLookupTool` interroge cet index et rend les articles pertinents ; le modèle cite leur contenu tel quel plutôt que de répondre de mémoire, et dit explicitement quand rien ne correspond plutôt que d'inventer une règle. Réindexé par `POST /api/v1/ai/admin/reindex`, comme les marques, projets et media plans.

---

## 8. Administration de l'assistant

Réservé au rôle `ADMIN`.

### `GET /api/v1/ai/admin/status`

```json
{
  "enabled": true,
  "chatModelReady": true,
  "embeddingModelReady": true,
  "chatModel": "gpt-4o",
  "embeddingModel": "text-embedding-3-small",
  "denseSearch": "pgvector",
  "textSearchConfig": "french",
  "denseWeight": 0.6,
  "memoryWindow": 20,
  "reminderTones": { "softMaxDays": 7, "firmMaxDays": 30 }
}
```

`denseSearch` vaut `pgvector` (recherche vectorielle native) ou `java-cosine-fallback` si l'extension n'est pas disponible sur l'instance PostgreSQL.

### `POST /api/v1/ai/admin/reindex`

Réindexe marques, projets, media plans, et le règlement intérieur (`reglement_interieur.txt`, un chunk par article). Incrémental : seuls les contenus dont le hash a changé sont réécrits et ré-embeddés.

```json
{ "written": 14, "unchanged": 231, "embedded": 14, "errors": 0, "durationMs": 3120 }
```

### `GET /api/v1/ai/admin/rag/search?q=...&topK=8`

Inspecte la recherche hybride et **détaille la contribution de chaque branche** — l'outil qui rend la fusion vérifiable.

```json
{
  "query": "lancement produit",
  "denseSearch": "pgvector",
  "count": 2,
  "results": [
    {
      "chunkId": 918,
      "sourceType": "MEDIA_PLAN",
      "sourceId": 4102,
      "clientId": 7,
      "provenance": "dense+lexical",
      "denseRank": 2,
      "sparseRank": 1,
      "fusedScore": 0.0158,
      "excerpt": "Publication media plan Marque : Alpha Date : 2026-06-03 Titre : Sortie officielle..."
    }
  ]
}
```

`provenance` ∈ `dense` | `lexical` | `dense+lexical`. Le périmètre appliqué est celui du compte appelant — cet endpoint diagnostique la pertinence, il ne contourne pas le cloisonnement.

---

## 9. Profil de marque (champs `Client`)

Le RAG a besoin de l'identité de marque, absente du schéma d'origine. Quatre colonnes `TEXT` ont été ajoutées à `clients` et exposées sur les endpoints existants.

### `GET /api/clients/{id}` — champs ajoutés

```json
{
  "id": 7,
  "nom": "Alpha",
  "identite": "Marque de prêt-à-porter féminin, ton chaleureux et direct",
  "activite": "Confection artisanale, vente en boutique et en ligne",
  "positionnement": "Accessible, engagée sur le fait-main tunisien",
  "objectifs": "Augmenter la notoriété locale et générer du trafic en boutique"
}
```

### `POST` / `PUT /api/clients` — paramètres ajoutés

Formulaire `multipart/form-data`, tous optionnels : `identite`, `activite`, `positionnement`, `objectifs`.

> Ces champs vides, la génération reste fonctionnelle mais se rabat sur `notes` et `description` — l'assistant le signale alors explicitement dans sa réponse. **Les renseigner est le levier n°1 de qualité du media plan généré.**

---

## 10. Configuration

Variables d'environnement (préfixe `app.ai` côté YAML) :

| Variable | Défaut | Description |
|---|---|---|
| `OPENAI_API_KEY` | *(vide)* | **Sans elle, `/api/v1/**` répond 503** et le reste du backend fonctionne normalement |
| `OPENAI_CHAT_MODEL` | `gpt-4o` | modèle de chat |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | modèle d'embedding (1536 dimensions) |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | endpoint compatible OpenAI |
| `AI_ENABLED` | `true` | coupe complètement l'assistant |
| `AI_REINDEX_ON_STARTUP` | `false` (prod : `true`) | réindexation du RAG au démarrage |

### Tâches longues — aucune limite de durée

L'assistant **n'impose aucune limite de temps** à une génération : une réponse valide ne doit jamais être coupée en plein milieu.

| Maillon | Réglage livré | Où |
|---|---|---|
| Client LLM | `timeout: 0s` (aucune limite) | `app.ai.chat.timeout` |
| Embeddings | `timeout: 0s` | `app.ai.embedding.timeout` |
| Flux SSE | `timeout: 0s` (le flux ne s'interrompt jamais de lui-même) | `app.ai.sse.timeout` |
| Async Spring MVC | `request-timeout: -1` | `spring.mvc.async.request-timeout` |
| Heartbeat | `15s` | `app.ai.sse.heartbeat-interval` |
| Threads | pool dédié `ai-gen-` (4→16) | `AiAsyncConfig` |

`0s` signifie « aucune limite ». Une borne de sécurité de 2 h subsiste au niveau du socket HTTP — sans elle, une connexion morte immobiliserait un thread indéfiniment. Elle n'interrompt aucune génération légitime.

**C'est le heartbeat, et lui seul, qui maintient la connexion.** Sans limite de durée, rien d'autre n'empêche un proxy de fermer un flux inactif.

⚠️ **Reverse proxy.** Si un Nginx / ALB / API Gateway est devant l'application, son `proxy_read_timeout` (60 s par défaut sur Nginx) doit être largement augmenté, et le buffering désactivé sur les routes SSE :

```nginx
location /api/v1/ {
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
    proxy_buffering off;
    proxy_cache off;
}
```

Sans cela le proxy coupe le flux, quelle que soit la configuration Spring.

### Quota

20 requêtes IA par minute et par compte (`app.ai.rate-limit`). Compteur **en mémoire** : en déploiement multi-instances, le quota devient par instance.

---

## 11. Tests

```bash
cd Backend
./mvnw test      # 79 tests unitaires — aucun Docker requis
./mvnw verify    # + 79 tests d'integration — PostgreSQL reel via Testcontainers
```

Les tests d'integration sont nommes `*IT` et pilotes par **failsafe**, pas surefire : `mvn test` reste executable sur un poste sans Docker. Si `mvn verify` y est tout de meme lance, `DockerAvailableCondition` les saute proprement au lieu d'echouer.

L'image utilisee est `pgvector/pgvector:pg16`, donc **la recherche dense native (`<=>`) et la recherche lexicale (`tsvector`) sont reellement exercees**, pas simulees. Aucun appel a OpenAI n'a lieu : les agents sont doubles par `@MockitoBean`.

### Traçabilité des scénarios du cahier des charges

| # | Scénario | Classe de test |
|---|---|---|
| 1 | Media plan — cas nominal (non-répétition, `lienDrive` renseigné) | `MediaPlanGenerationIT` |
| 2 | Media plan — marque sans historique | `MediaPlanGenerationIT` |
| 3 | Media plan — RBAC (403) | `MediaPlanGenerationIT`, `AiAccessScopeTest` |
| 4 | Relance — palier doux (3 j) | `ReminderIT`, `InvoiceLateInfoTest` |
| 5 | Relance — palier ferme/formel (45 j) | `ReminderIT`, `InvoiceLateInfoTest` |
| 6 | Relance — RBAC (403) | `ReminderIT`, `AiAccessScopeTest` |
| 7 | Paie — écart expliqué avec les vrais chiffres | `PayslipIsolationIT` |
| 8 | Paie — isolation entre employés (prompt injection) | `PayslipIsolationIT`, `AiAccessScopeTest` |
| 9 | Mémoire conversationnelle et résumé automatique | `ConversationMemoryIT` |
| 10 | CRUD conversations et permissions | `ConversationCrudIT` |
| 11 | RAG hybride — les deux branches contribuent | `RagHybridIT`, `HybridRetrieverTest`, `LexicalQueryTest` |
| 12 | Résilience Google Drive (`PENDING` + reprise ciblée) | `MediaPlanGenerationIT`, `DriveProvisioningServiceTest` |
| 13 | Streaming, tâche longue et heartbeat | `MediaPlanStreamingIT`, `AiSseSessionTest`, `AiTimeoutConfigurationTest` |
| — | Construction réelle des beans LLM et des proxys `AiServices` | `AiContextBootIT` |

**Ce qui est testé, et ce qui ne peut pas l'être.** Le LLM est doublé, donc aucun test n'affirme que le modèle *évite effectivement* les répétitions ou *adopte* le bon ton — ce sont des comportements du modèle, non déterministes. Ce qui est vérifié est la partie qu'une régression peut casser silencieusement : que l'historique lui est bien fourni avec la consigne de non-répétition, que le palier de ton transmis est le bon, et que les montants exposés viennent de la base et non de la génération.

---

## Écarts assumés par rapport au cahier des charges

Le cahier des charges a été rédigé contre un schéma supposé. Voici les écarts, et pourquoi.

| Point du cahier | Réalité livrée | Raison |
|---|---|---|
| Entités `Brand`, `Invoice`, `Payslip`, `User` | `Client`, `Facture`, `BulletinPaie`, `Compte`/`Employe` | Ces tables existent et sont riches ; les dupliquer aurait créé deux sources de vérité. |
| `MediaPlan` (en-tête) + `MediaPlanItem` (lignes) | `MediaPlan` = **une ligne par publication** | Le schéma réel n'a pas d'entité englobante. Un plan mensuel se désigne par le couple (`clientId`, `month`) — d'où l'absence de `mediaPlanId` en réponse. |
| Rôles `SOCIAL_MEDIA` / `ADMIN` / `EMPLOYEE` | permissions `VIEW_MEDIA_PLAN`, `VIEW_TOUS_MEDIA_PLAN`, `VIEW_FINANCE` + rôle `ADMIN` | L'application utilise des rôles dynamiques en base et un catalogue de permissions, administrable depuis l'écran Rôles & Permissions. Créer un rôle figé aurait doublonné ce mécanisme. |
| Champs `texte`, `autre`, `plateforme`, `etat` | `texteSurVisuel`, `autresElements`, `platforme`, `etatPublication` | Noms réels de l'entité, pour que le frontend Media Plan existant consomme la sortie sans traduction. |
| Java 21 / Spring Boot 3.3 | Java 17 / Spring Boot 3.5.6 | Configuration existante du projet ; LangChain4j 1.19 requiert Java 17+. |
| Réponse JSON pour `/media-plans/generate` | SSE **et** JSON, par négociation de contenu (`Accept`) | Le cahier exige le streaming partout mais documente une réponse JSON. Les deux sont fournies : SSE pour le frontend, JSON pour les intégrations et les tests. |
| Scénario 13 : outil ralenti de 45 s | test à ~1,2 s avec heartbeat à 150 ms, **plus** un test dédié sur la configuration livrée | Attendre 45 s prouverait exactement la même propriété en immobilisant la CI. La tenue réelle à 90 s est vérifiée sur le YAML de production par `AiTimeoutConfigurationTest`. |

### Points restés ouverts

- **Barème IRPP** : lu depuis la table `baremes_irpp` déjà versionnée par date d'effet, alimentée manuellement. Aucune source externe officielle n'a été branchée — à trancher avec le métier.
- **Formulations légales du palier `FORMAL`** : le prompt interdit toute menace chiffrée ou référence juridique inventée, mais le texte produit **doit être validé juridiquement avant mise en production**.
- **Envoi automatique des relances** : livré désactivé. L'activer est une décision métier.

# Guide d'intégration — `@antigone/ai-chat-widget`

Comment monter l'assistant IA dans une application Antigone. Le widget est déjà intégré dans **RH** (`:3001`, capacité `PAYSLIP`) et **Finance** (`:3002`, capacité `REMINDER`) ; ce guide sert à en ajouter une troisième — typiquement **Projets** (`:3000`, capacité `MEDIA_PLAN`).

Prérequis : lire [`THEME_AUDIT.md`](THEME_AUDIT.md) pour les tokens de charte, et [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md) pour le contrat backend.

---

## 1. Ce que fait le widget, et ce qu'il ne fait pas

**Il fait** : conversations (créer, lister, rechercher, renommer, épingler, supprimer), streaming SSE token par token, progression des outils, rendu des résultats structurés, gestion des erreurs, reprise après coupure réseau.

**Il ne fait pas**, délibérément :

| Non pris en charge | Pourquoi |
|---|---|
| Authentification | Chaque app a déjà sa chaîne d'auth. En créer une seconde, c'est deux endroits où un jeton peut expirer différemment. Le widget reçoit `getAuthToken` et délègue. |
| Choix de la capacité métier | C'est le backend qui classe l'intention et applique le RBAC. La prop `capability` sert l'affichage (titre, suggestions), pas le routage. |
| Envoi direct d'emails | Refusé en 403 par le backend par défaut. Le widget propose « copier » et `mailto:` — la validation humaine est le comportement voulu. |
| Sa propre charte | Aucune couleur en dur. Tout vient des tokens fournis par l'hôte. |

---

## 2. Intégration en quatre étapes

### Étape 1 — Déclarer la dépendance

Le monorepo utilise **npm workspaces** (`packages/*` est déjà déclaré à la racine). Aucun registre, aucune publication.

```jsonc
// frontend-projects/package.json
{
  "dependencies": {
    "@antigone/ai-chat-widget": "*"
  }
}
```

```bash
npm install --workspaces --include-workspace-root
```

Le package expose ses **sources TypeScript** (`"main": "./src/index.ts"`) : Vite les transpile directement, il n'y a pas d'étape de build à orchestrer. En contrepartie, le `tsc -b` de l'app type-vérifie aussi le widget — c'est voulu, une erreur de type y est détectée au build de l'app.

### Étape 2 — Créer le composant de montage

Un seul fichier par app, `src/components/ai/AiAssistant.tsx`. Copiez celui de Finance et changez la capacité :

```tsx
import React, { useCallback, useMemo } from 'react';
import { AiChatWidget } from '@antigone/ai-chat-widget';
import { API_BASE } from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../hooks/useTheme';
import { clearAuthSnapshot, getAccessToken } from '../../utils/authStorage';

const AiAssistant: React.FC = () => {
  const { isAuthenticated, user } = useAuth();
  const { theme } = useTheme();

  const isAdmin = useMemo(
    () => !!user?.roles?.includes('ADMIN'),
    [user],
  );

  const handleUnauthorized = useCallback(() => {
    clearAuthSnapshot();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
  }, []);

  if (!isAuthenticated) return null;

  return (
    <AiChatWidget
      apiBaseUrl={API_BASE}
      getAuthToken={getAccessToken}
      onUnauthorized={handleUnauthorized}
      capability="MEDIA_PLAN"
      isAdmin={isAdmin}
      launcherLabel="Assistant media plan"
      theme={{
        mode: theme,
        light: {
          colorPrimary: 'var(--brand)',
          colorPrimarySoft: 'var(--brand-light)',
          colorBackground: 'var(--bg)',
          colorSurface: 'var(--surface)',
          colorBorder: 'var(--border)',
          colorTextPrimary: 'var(--text-1)',
          colorTextSecondary: 'var(--text-2)',
          colorTextMuted: 'var(--text-3)',
          fontFamily: 'var(--app-font)',
          radiusMd: 'var(--radius-md)',
          radiusLg: 'var(--radius-lg)',
          shadow: 'var(--shadow-md)',
        },
        dark: {
          colorPrimary: 'var(--brand-mid)',
          colorPrimaryText: '#150b1a',
          fontFamily: 'var(--app-font)',
        },
      }}
    />
  );
};

export default AiAssistant;
```

> ⚠️ **Le piège du mode sombre.** Dans la charte Antigone, les variables `:root` (`--bg`, `--surface`, `--text-1`, `--border`) **ne changent pas** en mode sombre : seuls les utilitaires Tailwind `dark:` basculent. Les réutiliser telles quelles pour les tokens sombres donnerait un chat clair sur une application sombre. Les tokens sombres doivent donc être **explicites**, ou omis pour reprendre les valeurs par défaut du widget. Seuls `--brand`, `--brand-mid`, `--app-font` et les rayons sont valides dans les deux modes.

### Étape 3 — Monter dans le layout

```tsx
// src/components/layout/MainLayout.tsx
import AiAssistant from '../ai/AiAssistant';

<AppSwitchButton />
<AiAssistant />   // ← après AppSwitchButton
```

Le lanceur se positionne en `fixed right-6 bottom-24`, calé au-dessus d'`AppSwitchButton` qui occupe `bottom-6`. Si votre app place un autre élément flottant à droite, ajustez `--aicw-*` ou passez `hideLauncher` et fournissez votre propre déclencheur (étape 4).

### Étape 4 — (optionnel) Point d'entrée contextuel

Pour ouvrir l'assistant avec une demande déjà formulée depuis une page métier — comme le bouton « Générer une relance » de la fiche facture Finance.

1. Copiez `frontend-finance/src/components/ai/AiAssistantContext.tsx`.
2. Enveloppez le layout : `<AiAssistantProvider>` autour de l'`Outlet`.
3. Passez les props de pilotage au widget :

```tsx
const assistant = useAiAssistant();

<AiChatWidget
  /* … */
  open={assistant?.open}
  onOpenChange={assistant?.setOpen}
  initialMessage={assistant?.pendingMessage}
  onInitialMessageConsumed={assistant?.consumePendingMessage}
/>
```

4. Depuis la page :

```tsx
const assistant = useAiAssistant();

{assistant && (
  <button onClick={() => assistant.askAssistant(`Génère le media plan de ${client.nom} pour juillet`)}>
    Générer avec l'IA
  </button>
)}
```

Le message est **pré-rempli dans la zone de saisie, pas envoyé** : l'utilisateur le relit et peut l'ajuster avant d'engager un appel au modèle.

---

## 3. Ajouter un rendu de résultat structuré

Le backend n'étiquette pas le type de son `structured_result` : il est déduit de la forme du payload. Ajouter une capacité est donc une addition, jamais une réécriture.

`MediaPlanResult.tsx` **existe déjà** et est branché — un montage `capability="MEDIA_PLAN"` affichera correctement les media plans générés sans code supplémentaire.

Pour une capacité entièrement nouvelle, trois ajouts :

```ts
// 1. src/api/types.ts — le type et sa garde
export interface MonResultatPayload { /* … */ }

export function isMonResultat(value: unknown): value is MonResultatPayload {
  const candidate = value as MonResultatPayload;
  return !!candidate && typeof candidate === 'object' && typeof candidate.champDistinctif === 'string';
}
```

```tsx
// 2. src/components/results/MonResultat.tsx — le rendu
export const MonResultat: React.FC<{ result: MonResultatPayload }> = ({ result }) => (
  <section className="aicw-result">{/* … */}</section>
);
```

```tsx
// 3. src/components/results/index.tsx — une entrée dans le registre
const REGISTRY: ResultEntry[] = [
  /* … */
  { matches: isMonResultat, render: ((v) => <MonResultat result={v} />) as ResultEntry['render'] },
];
```

> **Choisissez une garde discriminante.** Le registre retient la **première** entrée qui correspond : une garde trop laxiste capterait les payloads d'une autre capacité. Ciblez un champ que seul votre type possède.

Un `structured_result` de forme inconnue n'affiche **rien** — la réponse en prose de l'assistant reste visible et porte déjà l'information. Afficher du JSON brut serait pire.

Ajoutez aussi les suggestions d'accueil dans `EmptyState.tsx` (`SUGGESTIONS` et `HEADLINE`) et, si votre capacité expose de nouveaux outils backend, leurs libellés dans `api/toolLabels.ts`.

---

## 4. Référence des props

| Prop | Type | Requis | Rôle |
|---|---|---|---|
| `apiBaseUrl` | `string` | ✅ | Racine de l'API. Le préfixe `/api/v1` est ajouté par le widget. |
| `getAuthToken` | `() => string \| null` | ✅ | Appelé à **chaque** requête — ne mémoïsez pas le jeton. |
| `capability` | `'GENERAL' \| 'MEDIA_PLAN' \| 'REMINDER' \| 'PAYSLIP'` | ✅ | Titre et suggestions. N'influence pas le routage backend. |
| `onUnauthorized` | `() => void` | — | Appelé sur un 403 **sans corps** (jeton absent ou expiré). |
| `theme` | `{ light?, dark?, mode? }` | — | Surcharge **partielle** : ne redéfinissez que ce qui diffère. |
| `isAdmin` | `boolean` | — | Défense en profondeur, jamais en remplacement du contrôle serveur. |
| `hideLauncher` | `boolean` | — | Masque le lanceur flottant si vous fournissez le vôtre. |
| `launcherLabel` | `string` | — | Libellé du lanceur. |
| `open` / `onOpenChange` | `boolean` / `(open) => void` | — | Pilotage externe de l'ouverture. |
| `initialMessage` / `onInitialMessageConsumed` | `string \| null` / `() => void` | — | Message pré-rempli, consommé une seule fois. |

### Tokens de thème

Dix-sept tokens, tous facultatifs (les manquants reprennent la charte Antigone) :

`colorPrimary`, `colorPrimaryText`, `colorPrimarySoft`, `colorBackground`, `colorSurface`, `colorSurfaceAlt`, `colorBorder`, `colorTextPrimary`, `colorTextSecondary`, `colorTextMuted`, `colorSuccess`, `colorWarning`, `colorDanger`, `fontFamily`, `radiusMd`, `radiusLg`, `shadow`.

Ils deviennent des variables CSS `--aicw-*` **scopées sur le conteneur racine du widget** : rien ne fuit vers l'app, rien ne vient la polluer.

**Contraste** : `colorPrimaryText` doit atteindre 4.5:1 avec `colorPrimary`. L'audit a montré que les teintes `*-500` de `success` et `error` de la charte Antigone **ne passent pas AA en texte** sur fond blanc — le widget utilise les teintes `600`/`700` par défaut. Si vous les surchargez, vérifiez le ratio plutôt que de reprendre le `500`.

---

## 5. Performance

Le panneau est chargé **à la demande** (`React.lazy`) : il tire le rendu markdown, la coloration syntaxique et la virtualisation, soit ~429 kB (133 kB gzip) isolés dans leur propre chunk. Un utilisateur qui n'ouvre jamais l'assistant n'en paie rien — seul le lanceur est dans le bundle principal.

Une fois ouvert, le panneau **reste monté** même fermé : le démonter interromprait une génération en cours, qui peut durer une minute et a déjà été payée au modèle.

Le streaming n'entraîne pas un rendu par caractère : les fragments sont accumulés dans une `ref` et vidés une fois par frame (`requestAnimationFrame`).

Dépendances ajoutées par le widget : `@tanstack/react-query`, `react-markdown`, `remark-gfm`, `rehype-highlight`, `react-virtuoso`. Si votre app en possède déjà une, npm workspaces la dédoublonne automatiquement dès que les versions sont compatibles.

---

## 6. Tests

```bash
npm run test:widget        # 70 tests unitaires, composants et intégration (Vitest + MSW)
npm run typecheck:widget   # vérification de types du package
npm run test:e2e           # end-to-end Playwright sur RH et Finance
```

Les tests d'intégration simulent un vrai flux SSE avec MSW : token par token, appels d'outils, heartbeats, **et coupure de flux en cours de route** pour vérifier la resynchronisation.

Si vous ajoutez une capacité, complétez au minimum :
- une garde de type dans `types.ts` (test unitaire) ;
- le rendu du résultat avec un payload **repris de `API_DOCUMENTATION.md`**, pas inventé.

---

## 7. Dépannage

| Symptôme | Cause probable |
|---|---|
| Chat en clair sur une app en sombre | Tokens sombres absents ou pointant vers des variables `:root` qui ne basculent pas (voir étape 2). |
| Le lanceur recouvre un autre bouton flottant | Ajustez le positionnement, ou `hideLauncher` + votre propre déclencheur. |
| 403 en boucle | `getAuthToken` mémoïsé sur un jeton expiré. Il doit lire la source à chaque appel. |
| Le panneau reste vide | `apiBaseUrl` pointe vers `/api` au lieu de la racine — le widget ajoute `/api/v1` lui-même. |
| Erreur de type au build de l'app | Normal : le package expose ses sources et est type-vérifié avec l'app. Corrigez dans le package. |
| Résultat structuré non affiché | Aucune garde du registre ne correspond au payload (voir §3). La prose reste visible. |

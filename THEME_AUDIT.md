# THEME_AUDIT — RH & Finance

Audit préalable à l'intégration du widget de chat IA (étape 0 de la spécification frontend).
Réalisé sur l'état réel des deux applications, pas sur une hypothèse.

---

## Résultat principal

**RH et Finance partagent aujourd'hui exactement le même thème.** Leurs fichiers `src/index.css` et `src/hooks/useTheme.ts` sont **identiques octet pour octet** (274 lignes chacun, `diff` vide). Même palette violette, même typographie, même système de mode sombre.

Cela a deux conséquences pour la mission :

1. La *Definition of Done* demandait de vérifier le thème « sur RH **et** Finance avec des thèmes différents ». Ce n'est pas vérifiable en l'état — les thèmes ne diffèrent pas. La contrainte a donc été respectée autrement : **le widget ne lit aucune couleur en dur et dérive ses tokens des variables CSS de l'app hôte** (`var(--brand)`, `var(--surface)`…). Si les deux apps divergent un jour, le chat suit automatiquement. Un accent distinct par app est en outre exposé en prop, pour rendre la thémabilité démontrable dès maintenant.
2. La duplication elle-même est un point de dette signalé en fin de document.

---

## 1. Stack technique (identique dans les deux apps)

| | RH (`:3001`) | Finance (`:3002`) |
|---|---|---|
| Build | Vite 7.3 | Vite 7.3 |
| Framework | React 19.2 | React 19.2 |
| Langage | TypeScript 5.9 (`"type": "module"`) | TypeScript 5.9 |
| Style | Tailwind CSS 4.1 via `@tailwindcss/vite` | Tailwind CSS 4.1 |
| Lib de composants | **Ant Design 5.24** | *(aucune)* |
| HTTP | axios 1.13 | axios 1.13 |
| Animation | framer-motion 12.38 | framer-motion 12.38 |
| Icônes | react-icons 5.6 (`Hi` / Heroicons v1) | react-icons 5.6 |
| Routage | react-router-dom 7.13 | react-router-dom 7.13 |
| Graphiques | recharts 3.7 | recharts 3.7 |
| Divers | three / @react-three/fiber / drei, moment | — |

**Monorepo** : npm workspaces déjà configuré à la racine (`package.json` → `workspaces: [frontend-projects, frontend-rh, frontend-finance]`). Aucun Turborepo / Nx / pnpm / Lerna.
→ *Le package partagé est ajouté comme quatrième workspace.* Rien à installer, rien à publier.

**Absent des deux apps** (donc à ajouter au widget) : TanStack Query, une librairie markdown, une librairie de virtualisation, tout outillage de test (Vitest, RTL, MSW, Playwright).

---

## 2. Palette

Définie deux fois : en variables CSS « applicatives » sur `:root`, et en tokens Tailwind 4 dans `@theme`.

### Variables applicatives (`:root`) — la source à utiliser

| Rôle | Variable | Valeur (clair) |
|---|---|---|
| Fond de page | `--bg` | `#f5f4f1` |
| Surface / carte | `--surface` | `#ffffff` |
| Marque | `--brand` | `#683b77` (violet Antigone) |
| Marque survolée | `--brand-hover` | `#562f64` |
| Marque atténuée | `--brand-light` | `rgba(104, 59, 119, 0.1)` |
| Marque médiane | `--brand-mid` | `#ab78c3` |
| Texte principal | `--text-1` | `#1a1814` |
| Texte secondaire | `--text-2` | `#5c5a55` |
| Texte désactivé | `--text-3` | `#9c9a94` |
| Bordure | `--border` | `#e8e6e0` |

### Tokens Tailwind (`@theme`)

- `--color-brand-*` : alias de `--color-secondary-*` (violet, `25` → `950`, base `500` = `#683B77`).
- `--color-gray-*` : échelle neutre `25` → `950` + `--color-gray-dark: #1a2231`.
- `--color-success-*` : vert, base `500` = `#12b76a`.
- `--color-error-*` : rouge, base `500` = `#f04438`.
- `--color-orange-*` : orange, base `500` = `#fb6514`.

> Le rouge `error` et le vert `success` couvrent les besoins du chat. **L'orange sert de palier intermédiaire** pour le badge de ton `FIRM` des relances.

### Mode sombre

Pas de redéfinition des variables `:root` en sombre. Le mode sombre passe **exclusivement par les utilitaires Tailwind `dark:`**, avec la variante déclarée en tête de fichier :

```css
@custom-variant dark (&:is(.dark *));
```

Conséquence directe pour le widget : `--bg` et `--surface` gardent leurs valeurs claires même en mode sombre. **Le widget ne peut donc pas se contenter de consommer ces variables** — il doit recevoir un couple de valeurs (clair / sombre) et basculer sur l'état `theme` fourni par `useTheme()`.

---

## 3. Typographie

- Police chargée : **Outfit** (Google Fonts, `100..900`), exposée en token Tailwind `--font-outfit`.
- Police effective : pilotée à l'exécution par l'utilisateur via `--app-font`, au choix entre **Inter** et **Poppins** (défaut `Inter`). Écrite sur `document.documentElement` par `useTheme`.
- Taille de base : **configurable par l'utilisateur** — `13px`, `14px`, `15px` ou `16px` (défaut `14px`), appliquée sur `root.style.fontSize` et exposée en `--app-font-size`.
- Échelle Tailwind personnalisée : `--text-theme-xs` (12px), `--text-theme-sm` (14px), `--text-theme-xl` (20px), plus une série `--text-title-*` de 30px à 72px.
- `--breakpoint-*` réinitialisés puis redéfinis : `2xsm` 375, `xsm` 425, `sm` 640, `md` 768, `lg` 1024, `xl` 1280, `2xl` 1536.

> **Point d'attention** : la taille de police étant réglable par l'utilisateur, le widget doit dimensionner en unités relatives (`rem`/`em`) et non en `px` figés, sous peine de ne pas suivre le réglage de l'utilisateur.

---

## 4. Rayons, ombres, transitions

| Propriété | Variable | Valeur |
|---|---|---|
| Rayon moyen | `--radius-md` | `10px` |
| Rayon large | `--radius-lg` | `16px` |
| Ombre | `--shadow-md` | `0 4px 16px rgba(0, 0, 0, 0.08)` |
| Transition | `--transition` | `all 0.22s cubic-bezier(0.4, 0, 0.2, 1)` |

Éléments flottants existants (`AppSwitchButton`) : `rounded-full`, `bg-white/90 dark:bg-gray-800/90`, `backdrop-blur-xl`, `shadow-2xl shadow-black/10 dark:shadow-black/30`, bordure `gray-200/90 dark:gray-700/90`.
→ *Le lanceur du chat reprend ce vocabulaire visuel pour ne pas détonner.*

---

## 5. Mode sombre — mécanisme exact

Implémenté dans `src/hooks/useTheme.ts` (identique dans les deux apps) :

- Classe `dark` ajoutée/retirée sur `document.documentElement`.
- Persistance sur **trois niveaux** : cookie partagé (`theme`, `path=/`, `SameSite=Lax`, 1 an) → `localStorage` → `prefers-color-scheme`.
- Le cookie est le mécanisme clé : `localStorage` étant cloisonné par port, c'est lui qui **propage le thème entre les trois apps** (`:3000` / `:3001` / `:3002`).
- Re-synchronisation depuis le cookie sur `visibilitychange`, pour refléter un changement fait dans une autre app.
- `useTheme()` expose également `font` / `setFont` et `fontSize` / `setFontSize`.
- Une *View Transition API* est câblée en CSS pour animer le basculement (`::view-transition-old/new(root)`).

→ *Le widget consomme `theme` via une prop `mode` fournie par l'hôte, et ne lit jamais le DOM ni le cookie lui-même.*

---

## 6. Authentification (à réutiliser, jamais à dupliquer)

Chaîne identique dans les deux apps :

| Élément | Emplacement | Rôle |
|---|---|---|
| `AuthContext` / `useAuth()` | `src/context/AuthContext.tsx` | expose `user`, `login`, `logout`, `isAuthenticated` |
| `getAccessToken()` | `src/utils/authStorage.ts` | **source unique du JWT** |
| `getAuthSnapshot()` / `saveAuthSnapshot()` | `src/utils/authStorage.ts` | persistance + contrôle d'expiration |
| `relayAuthSnapshotForSwitch()` | `src/utils/authStorage.ts` | relais de session inter-apps via `window.name` |
| Intercepteur | `src/api/axios.ts` | injecte `Authorization: Bearer`, purge + redirige sur **401** |
| `API_BASE` | exporté par `src/api/axios.ts` | `VITE_API_URL`, sinon `localhost:8080`, sinon Render |

Forme du `user` (`LoginResponse`) : `compteId`, `employeId`, `username`, `nom`, `prenom`, `email`, `roles: string[]`, `permissions: string[]`, `token?`, `tokenExpiresAt?`, `mustChangePassword`, `genre`, `imageUrl`.

Session : 4 h (`SESSION_DURATION_MS`), plus contrôle de `tokenExpiresAt`.

→ *Le widget reçoit `getAuthToken` et `onUnauthorized` en props et délègue entièrement à cette chaîne. Il ne lit ni `localStorage`, ni cookie, ni `window.name`.*

### ⚠️ Écart relevé entre le front et le back

L'intercepteur axios ne traite que le **401**. Or le backend renvoie **403** pour un jeton absent ou expiré — l'application ne déclare pas d'`AuthenticationEntryPoint` (documenté en §2 de `API_DOCUMENTATION.md`).

**Un jeton expiré ne déclenche donc aujourd'hui aucune déconnexion automatique** : l'utilisateur reste sur une interface qui échoue silencieusement. Le widget compense pour son propre périmètre en appelant `onUnauthorized()` sur un 403 sans corps, mais **le reste des deux applications conserve ce défaut**. Correction à traiter séparément (ajouter `403` à l'intercepteur, ou déclarer un `AuthenticationEntryPoint` côté backend).

---

## 7. Points de montage disponibles

- `MainLayout.tsx` — **identique dans les deux apps**, monte `<Sidebar />`, `<Outlet />` et `<AppSwitchButton />`. C'est le point d'insertion commun retenu.
- `AppSwitchButton` occupe `fixed right-6 bottom-6 z-[10000]`.
  → *Le lanceur du chat se place au-dessus (`bottom` ≈ 6rem) pour ne pas le recouvrir.*
- RH possède un `Header.tsx`, **Finance non**. Un montage dans le header n'aurait donc pas été symétrique : le lanceur flottant est le seul emplacement réellement commun aux deux apps.
- Les deux `Sidebar.tsx` filtrent leurs entrées par permission, avec des listes différentes — les modifier aurait impliqué deux logiques distinctes pour un même besoin.

---

## 8. Contraste (WCAG AA)

| Combinaison | Ratio | AA texte normal (4.5:1) |
|---|---|---|
| `--text-1` `#1a1814` sur `--surface` `#ffffff` | ≈ 16.5:1 | ✅ |
| `--text-2` `#5c5a55` sur `--surface` `#ffffff` | ≈ 6.8:1 | ✅ |
| `--text-3` `#9c9a94` sur `--surface` `#ffffff` | ≈ 2.8:1 | ❌ — **réservé au texte décoratif**, jamais à du contenu |
| `#ffffff` sur `--brand` `#683b77` | ≈ 8.6:1 | ✅ |
| `--brand` `#683b77` sur `--bg` `#f5f4f1` | ≈ 7.6:1 | ✅ |
| `--color-error-500` `#f04438` sur `#ffffff` | ≈ 3.5:1 | ❌ — le widget utilise `error-600`/`700` pour le texte |
| `--color-success-500` `#12b76a` sur `#ffffff` | ≈ 2.6:1 | ❌ — idem, `success-600`/`700` pour le texte |

**Deux points signalés plutôt que contournés silencieusement**, conformément à la consigne :
- `--text-3` est sous le seuil AA. Le widget ne s'en sert que pour des éléments non informatifs (horodatages secondaires, placeholder atténué) et emploie `--text-2` dès qu'il s'agit de contenu.
- Les teintes `500` de `success` et `error` ne passent pas AA en texte sur fond blanc. Elles restent utilisables en **fond** (avec texte blanc) ou en **icône**, mais le widget prend les teintes `600`/`700` pour tout texte coloré.

---

## 9. Dette signalée (hors périmètre de cette mission)

1. **`index.css` et `useTheme.ts` dupliqués à l'identique** entre RH et Finance (et probablement Projets). Toute évolution de charte doit aujourd'hui être répliquée manuellement, avec un risque de dérive silencieuse. Un quatrième workspace `@antigone/theme` réglerait le problème — l'infrastructure de partage existe désormais avec ce package.
2. **403 non traité par l'intercepteur axios** (cf. §6) — un jeton expiré ne déconnecte pas l'utilisateur.
3. **RH embarque Ant Design** pour un usage partiel, en plus de Tailwind. Le widget ne s'appuie sur aucune des deux : il est en CSS natif thémé par variables, donc neutre vis-à-vis de cet arbitrage.

---

## 10. Décisions de theming qui en découlent

| Décision | Motif |
|---|---|
| Widget en **CSS natif + variables `--aicw-*`**, sans Tailwind | La spécification interdit d'imposer une librairie de style à l'hôte. Évite aussi de configurer le scan de contenu Tailwind 4 dans chaque app. |
| Tokens fournis **par paire clair/sombre** | Les variables `:root` de l'hôte ne changent pas en mode sombre (§2) : les consommer seules donnerait un chat blanc sur une app sombre. |
| Dimensionnement en **`rem`/`em`** | La taille de police est réglable par l'utilisateur (§3). |
| Variables **scopées sur le conteneur racine** du widget | Aucune fuite vers l'hôte, aucune pollution depuis l'hôte. |
| Lanceur **flottant**, calé au-dessus de `AppSwitchButton` | Seul emplacement réellement commun aux deux apps (§7). |
| `--text-3`, `error-500`, `success-500` **écartés pour le texte** | Sous le seuil WCAG AA (§8). |

# DevOps — Antigone 360

Ce document décrit la chaîne DevOps de la plateforme : **conteneurisation Docker** de
tous les composants et **pipeline CI/CD GitHub Actions** exécuté à chaque commit.

- [Vue d'ensemble](#vue-densemble)
- [Fichiers ajoutés ou modifiés](#fichiers-ajoutés-ou-modifiés)
- [Conteneurisation](#conteneurisation)
- [Lancer la plateforme avec Docker](#lancer-la-plateforme-avec-docker)
- [Pipeline CI/CD](#pipeline-cicd)
- [Sécurité (DevSecOps)](#sécurité-devsecops)
- [Utiliser les images publiées](#utiliser-les-images-publiées)
- [Flux de travail Git](#flux-de-travail-git)
- [Dépannage](#dépannage)

---

## Vue d'ensemble

```
             push (toute branche)
                     │
   ┌─────────┬───────┼────────┬──────────────┬──────────────────────────────┐
   ▼         ▼       ▼        ▼              ▼                              │
backend  frontend   e2e   secret-scan      docker                           │
 Maven    tsc +   Playwright Gitleaks   build 4 images                      │
 verify   Vitest  (Chromium)            compose up + smoke tests            │
 (+ IT    + build                       scan Grype                          │
 Postgres)                                                                  │
   └─────────┴───────┴────────┴──────────────┘                              │
                     │ tout est vert ET branche = master                    │
                     ▼                                                      │
                  publish ──► ghcr.io/yosrkheriji/antigone/{backend,frontend-rh,frontend-finance,frontend-projects}
```

| Composant | Technologie | Image | Port |
|---|---|---|---|
| API | Spring Boot 3.5 / Java 17 | `antigone/backend` | 8080 |
| App RH | React 19 + Vite, servie par nginx | `antigone/frontend-rh` | 3001 |
| App Finance | React 19 + Vite, servie par nginx | `antigone/frontend-finance` | 3002 |
| App Projets + portail client | React 19 + Vite, servie par nginx | `antigone/frontend-projects` | 3000 |
| Base de données | PostgreSQL 16 + pgvector | `pgvector/pgvector:pg16` | 5432 |

## Fichiers ajoutés ou modifiés

| Fichier | Rôle |
|---|---|
| `.github/workflows/ci-cd.yml` | Pipeline CI/CD complet |
| `.github/dependabot.yml` | Mises à jour hebdomadaires des dépendances (Maven, npm, Docker, Actions) |
| `Backend/Dockerfile` | Image multi-étapes de l'API (build Maven → JRE Alpine, utilisateur non-root, healthcheck) |
| `Backend/entrypoint.sh` | Transmet `JAVA_OPTS` à la JVM |
| `docker/frontend.Dockerfile` | Image multi-étapes commune aux 3 frontends (argument `APP`) |
| `docker/nginx.conf` | Service de la SPA : fallback `index.html`, cache des assets, en-têtes de sécurité, `/healthz` |
| `docker-compose.yml` | Pile complète : db + backend + 3 frontends |
| `.env.example` | Modèle des variables d'environnement (à copier en `.env`, jamais commité) |
| `.dockerignore` | Contexte de build minimal des frontends (liste blanche) |
| `.gitleaks.toml` | Configuration du scan de secrets |
| `.gitattributes` | Force les fins de ligne LF des scripts shell, Dockerfiles et YAML |
| `Backend/src/main/resources/application.yml` | **Clé OpenAI et mot de passe SMTP en clair supprimés**, remplacés par `${OPENAI_API_KEY:}` et `${MAIL_PASSWORD:}` |
| `frontend-*/vite.config.ts` | Le chemin de base vient de `VITE_BASE_PATH` (défaut `/`) au lieu d'être forcé dès qu'on est dans GitHub Actions |

## Conteneurisation

### Backend — `Backend/Dockerfile`

1. **Étape build** (`maven:3.9-eclipse-temurin-17`) : `mvn dependency:go-offline` sur le
   `pom.xml` seul (couche mise en cache tant que les dépendances ne changent pas), puis
   `mvn package -DskipTests`. Les tests ne tournent pas ici : c'est le rôle du job `backend`.
2. **Étape runtime** (`eclipse-temurin:17-jre-alpine`) : JRE seul + JAR + installateur de
   l'agent de bureau. Exécution sous l'utilisateur `appuser` (non-root).
   - `HEALTHCHECK` sur `/v3/api-docs` (public, répond quand Spring est prêt et la base joignable) ;
   - `JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"` : le tas suit la mémoire du conteneur.

Aucun secret n'est dans l'image : tout passe par des variables d'environnement
(`DATABASE_URL`, `JWT_SECRET`, `OPENAI_API_KEY`, `MAIL_*`, `GOOGLE_DRIVE_*_B64`…).

### Frontends — `docker/frontend.Dockerfile`

Le contexte de build est la racine du dépôt, car les 3 apps partagent
`@antigone/ai-chat-widget` via les workspaces npm.

```bash
docker build -f docker/frontend.Dockerfile --build-arg APP=rh       -t antigone/frontend-rh .
docker build -f docker/frontend.Dockerfile --build-arg APP=finance  -t antigone/frontend-finance .
docker build -f docker/frontend.Dockerfile --build-arg APP=projects -t antigone/frontend-projects .
```

1. **Build** (`node:22-alpine`) : `npm ci` (manifests copiés d'abord pour le cache), puis
   `tsc -b && vite build` de l'app choisie.
2. **Runtime** (`nginxinc/nginx-unprivileged:1.30-alpine`, non-root, port 8080) :
   toute route inconnue renvoie `index.html` (React Router), `/assets/` en cache 1 an,
   en-têtes `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, sonde `/healthz`.

Build-arg optionnel `VITE_API_URL` : URL de l'API injectée à la compilation. Vide (défaut),
l'application utilise `http://localhost:8080` lorsqu'elle est ouverte sur `localhost`.

## Lancer la plateforme avec Docker

Prérequis : Docker Desktop (ou Docker Engine + plugin Compose).

```bash
cp .env.example .env              # renseigner les clés voulues (toutes optionnelles)
docker compose up -d --build      # construit et démarre les 5 conteneurs
docker compose ps                 # état et santé des services
docker compose logs -f backend    # logs de l'API
docker compose down               # arrêt (ajouter -v pour effacer la base)
```

| URL | Service |
|---|---|
| http://localhost:3001 | Application RH |
| http://localhost:3002 | Application Finance |
| http://localhost:3000 | Application Projets / portail client |
| http://localhost:8080/swagger-ui.html | Documentation de l'API |

Sans `OPENAI_API_KEY`, l'API démarre normalement et seuls les endpoints `/api/v1/**`
(assistant IA) répondent 503. Sans identifiants SMTP/Brevo, l'envoi d'emails est inactif.

> **Développement local sans Docker** : la clé OpenAI et le mot de passe SMTP ne sont plus
> écrits dans `application.yml`. Définissez-les en variables d'environnement avant de lancer
> le backend, par ex. PowerShell : `$env:OPENAI_API_KEY="sk-..."; $env:MAIL_PASSWORD="..."`.

## Pipeline CI/CD

Fichier : `.github/workflows/ci-cd.yml`.

**Déclencheurs** : chaque `push` sur n'importe quelle branche (sauf commits ne touchant que
la documentation : `*.md`, `*.tex`, `*.pdf`, `rapport-pfe/`…) et lancement manuel
(`workflow_dispatch`, onglet *Actions → CI/CD → Run workflow*). Un nouveau push sur la même
branche annule le run précédent (`concurrency`).

| Job | Ce qu'il fait | Échoue si |
|---|---|---|
| `backend` | JDK 17 + cache Maven, `mvn -B verify` : compilation, tests unitaires (surefire), tests d'intégration `*IT` (failsafe) sur un vrai PostgreSQL/pgvector démarré par **Testcontainers**. Rapport JUnit publié dans l'onglet *Checks*, rapports bruts en artefact. | compilation ou test KO |
| `frontend` | Node 22 + cache npm, `npm ci`, typecheck du widget IA, tests **Vitest**, build des 3 apps (`tsc -b && vite build`). Les `dist/` sont en artefact. | erreur de type, test ou build KO |
| `e2e` | Installe Chromium, lance les apps RH et Finance (Vite), exécute les scénarios **Playwright** de l'assistant IA (backend simulé par `page.route`). Traces en artefact si échec. | scénario KO |
| `secret-scan` | **Gitleaks** sur l'arborescence du dépôt (règles par défaut + `.gitleaks.toml`). | secret détecté |
| `docker` | Build des 4 images avec cache GitHub Actions (`type=gha`), `docker compose up --wait` de la pile complète, **smoke tests** (voir ci-dessous), logs du backend, puis scan **Grype** des 4 images (résumé dans la page du run, rapport complet en artefact). | image non constructible, conteneur non sain, smoke test KO |
| `publish` | **Uniquement sur `master`** et si les 5 jobs précédents sont verts : login `ghcr.io` avec `GITHUB_TOKEN`, push des 4 images taguées `latest` et `sha-<commit>` (couches reprises du cache : l'image publiée est celle testée), suppression des anciennes versions (2 conservées). | — |

**Smoke tests** exécutés contre la pile Docker Compose :

- `GET  :8080/v3/api-docs` → 200 (API démarrée, base connectée)
- `POST :8080/api/auth/login` avec de mauvais identifiants → 400/401/403 (sécurité + base OK)
- `GET  :8080/api/employes` sans jeton → 401/403 (route protégée)
- pour chaque frontend : `/` → 200 et contient `id="root"`, une route profonde → 200 (fallback SPA), `/healthz` → 200

**Où voir les résultats** : onglet **Actions** du dépôt → run *CI/CD* ; le statut s'affiche
aussi sur chaque commit et chaque Pull Request. Artefacts téléchargeables en bas de la page
du run : `backend-test-reports`, `frontend-dist`, `vulnerability-reports`,
`playwright-results` (si échec).

**Aucun secret à configurer** : le pipeline n'utilise que le `GITHUB_TOKEN` fourni
automatiquement par GitHub, avec des permissions minimales (`contents: read` par défaut,
`checks: write` pour le rapport de tests, `packages: write` pour la publication seulement).

## Sécurité (DevSecOps)

- **Secrets retirés du code** : la clé OpenAI et le mot de passe d'application Gmail étaient
  en clair dans `application.yml`. Ils sont remplacés par des variables d'environnement et
  Gitleaks bloque toute réintroduction.
  ⚠️ Ils restent visibles dans **l'historique Git** (commit `firstCommit`) : il faut
  **révoquer la clé OpenAI** (platform.openai.com → API keys) et **le mot de passe
  d'application Gmail** (myaccount.google.com → Mots de passe des applications), puis en
  générer de nouveaux. Ne rendez pas le dépôt public avant cette rotation.
- **Scan de vulnérabilités** des images (Grype, vulnérabilités ayant un correctif) à chaque run.
  Le job est informatif (il n'échoue pas sur une CVE d'une image de base) ; le résumé
  Critiques/Élevées est affiché dans la page du run.
- **Dependabot** ouvre chaque semaine des PR de mise à jour (Maven, npm groupé, images Docker,
  actions GitHub) ; chacune passe par le pipeline complet.
- **Conteneurs non-root** (backend `appuser`, nginx-unprivileged) et **aucun secret dans les images**.
- **Moindre privilège** sur le `GITHUB_TOKEN` (voir ci-dessus).

## Utiliser les images publiées

Après un merge sur `master`, les images sont dans l'onglet **Packages** du profil GitHub :
`ghcr.io/yosrkheriji/antigone/backend`, `.../frontend-rh`, `.../frontend-finance`,
`.../frontend-projects`. Le dépôt étant privé, les packages le sont aussi.

```bash
# Jeton GitHub (classic) avec le scope read:packages
echo <TOKEN> | docker login ghcr.io -u yosrkheriji --password-stdin

# dans .env :
#   IMAGE_PREFIX=ghcr.io/yosrkheriji/antigone
#   IMAGE_TAG=latest          # ou sha-xxxxxxx pour une version précise
docker compose pull
docker compose up -d --no-build
```

Le tag `sha-<commit>` permet de revenir exactement à une version antérieure (rollback).

## Flux de travail Git

1. Créer une branche : `git checkout -b feature/ma-fonctionnalite`
2. Pousser : le pipeline tourne sur la branche (onglet *Actions*).
3. Ouvrir une Pull Request vers `master` : le statut des 6 jobs y est affiché.
4. Fusionner quand tout est vert → le run sur `master` publie les images.

Recommandé : *Settings → Branches → Add branch protection rule* sur `master`, cocher
*Require status checks to pass before merging* et sélectionner les jobs
`Backend · build & tests`, `Frontend · typecheck, tests & build`,
`Frontend · end-to-end tests (Playwright)`, `Security · secret scan (Gitleaks)`,
`Docker · build, smoke test & scan`.

## Dépannage

| Symptôme | Cause / solution |
|---|---|
| `secret-scan` échoue | Un secret a été commité. Le retirer du code (variable d'environnement), **le révoquer**, recommiter. Faux positif : ajouter le chemin dans `.gitleaks.toml`. |
| `backend` : tests `*IT` ignorés en local | Normal sans Docker : `DockerAvailableCondition` les saute. En CI, Docker est toujours présent. |
| `docker` : le backend n'est pas `healthy` | Lire l'étape *Container status and logs* du job ; en local `docker compose logs backend`. |
| Le frontend Docker n'atteint pas l'API | Ouvrir l'app via `localhost` (pas l'IP de la machine), ou rebuild avec `--build-arg VITE_API_URL=http://<hote>:8080` et ajouter l'origine à `FRONTEND_URL`. |
| `publish` ne tourne pas | Normal hors `master`, ou si un job précédent a échoué. |
| Quota de stockage Packages | Dépôt privé gratuit = 500 Mo ; seules 2 versions de chaque image sont gardées. |

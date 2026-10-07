# syntax=docker/dockerfile:1
#
# Image d'un frontend React/Vite du monorepo (RH, Finance ou Projets).
# Le contexte de build est la RACINE du depot : les trois applications partagent
# le paquet @antigone/ai-chat-widget via les workspaces npm.
#
#   docker build -f docker/frontend.Dockerfile --build-arg APP=rh -t antigone-rh/frontend-rh .
#
# APP                    : rh | finance | projects
# VITE_API_URL           : URL du backend injectee au build (vide = auto-detection :
#                          http://localhost:8080 quand l'app est servie sur localhost).
# VITE_*_APP_URL         : URLs des trois apps, pour la connexion (portee par l'app
#                          Projets) et le bouton de bascule entre apps. Par defaut,
#                          les ports publies par docker-compose.yml.
# VITE_POWERBI_*_URL     : liens d'integration Power BI (optionnels).
# Les fichiers .env.production des apps ne sont PAS copies (cf. .dockerignore) :
# toute la configuration de l'image passe par ces arguments.

# ---------- Etape 1 : build ----------
FROM node:22-alpine AS build
ARG APP
ARG VITE_API_URL=""
ARG VITE_PROJECTS_APP_URL="http://localhost:3000"
ARG VITE_RH_APP_URL="http://localhost:3001"
ARG VITE_FINANCE_APP_URL="http://localhost:3002"
ARG VITE_POWERBI_PRESENCE_URL=""
ARG VITE_POWERBI_FINANCE_URL=""
ARG VITE_POWERBI_PROJETS_URL=""
RUN test -n "$APP" || (echo "Build-arg APP obligatoire (rh|finance|projects)" && false)
WORKDIR /repo

# Manifests d'abord : la couche npm ci reste en cache tant qu'aucune dependance ne change.
COPY package.json package-lock.json ./
COPY packages/ai-chat-widget/package.json packages/ai-chat-widget/
COPY frontend-rh/package.json frontend-rh/
COPY frontend-finance/package.json frontend-finance/
COPY frontend-projects/package.json frontend-projects/
RUN npm ci --no-audit --no-fund

COPY packages packages
COPY frontend-${APP} frontend-${APP}
ENV VITE_API_URL=${VITE_API_URL} \
    VITE_APP_KIND=${APP} \
    VITE_PROJECTS_APP_URL=${VITE_PROJECTS_APP_URL} \
    VITE_RH_APP_URL=${VITE_RH_APP_URL} \
    VITE_FINANCE_APP_URL=${VITE_FINANCE_APP_URL} \
    VITE_POWERBI_PRESENCE_URL=${VITE_POWERBI_PRESENCE_URL} \
    VITE_POWERBI_FINANCE_URL=${VITE_POWERBI_FINANCE_URL} \
    VITE_POWERBI_PROJETS_URL=${VITE_POWERBI_PROJETS_URL}
RUN npm run build -w frontend-${APP}

# ---------- Etape 2 : service statique ----------
# nginx-unprivileged : le processus ne tourne pas en root et ecoute sur 8080.
FROM nginxinc/nginx-unprivileged:1.30-alpine
ARG APP
LABEL org.opencontainers.image.title="antigone-frontend-${APP}" \
      org.opencontainers.image.description="Antigone 360 - frontend ${APP} (React + Vite servi par nginx)"
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/frontend-${APP}/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1

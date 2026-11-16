# ACMS frontend image: the production build of the React app, served by Caddy (deploy/compose.yaml).
# The Caddyfile is not baked in: deploy/compose.yaml mounts it, so changing it needs no rebuild.

# ---------- 1. Build the app with Node ----------
FROM node:24-slim AS build
WORKDIR /app
# Dependencies first: this layer is cached and only reinstalled when package-lock.json changes.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# VITE_API_BASE_URL stays empty: the app calls /api on its own address, and Caddy forwards it.
RUN npm run build

# ---------- 2. Serve the built files ----------
FROM caddy:2-alpine
COPY --from=build /app/dist /srv

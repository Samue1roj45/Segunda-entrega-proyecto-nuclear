# =========================================================
# Frontend: Angular (build) + nginx (servir estáticos y proxy API)
# =========================================================

# ---------- Etapa 1: compilación ----------
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration docker

# ---------- Etapa 2: servidor web ----------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

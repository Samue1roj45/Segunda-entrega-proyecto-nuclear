# SOMA · interfaz única (Angular 21). Etapa 1: compila. Etapa 2: nginx sirve el estático y reenvía /api.
FROM node:22-alpine AS compilacion
WORKDIR /fuente
ENV NG_CLI_ANALYTICS=false \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
COPY package.json package-lock.json ./
# npm ci rechaza este lockfile (faltan @emnapi/*). npm install respeta las versiones fijadas.
RUN npm install --no-audit --no-fund
COPY . .
RUN npx ng build --configuration=production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN sed -i 's/\r$//' /etc/nginx/conf.d/default.conf
COPY --from=compilacion /fuente/dist/soma/browser /usr/share/nginx/html
EXPOSE 80

# Etapa de construcción: genera los archivos estáticos de la SPA.
# Node 22, la misma versión que verifica el frontend en CI.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# true: los adaptadores consultan la API real (/api/v1); sin valor, una compilación de producción usa los datos
# simulados del frontend. Compose lo fija en true porque nginx publica la API en el mismo origen.
ARG VITE_API_REAL
ENV VITE_API_REAL=${VITE_API_REAL}
RUN npm run build

# Etapa de ejecución: sirve los archivos estáticos con nginx sin privilegios de root, en el puerto 8080.
FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-cabeceras.conf /etc/nginx/cabeceras.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080

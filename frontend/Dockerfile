# Kliento sąsajos atvaizdas kuriamas dviem etapais: kompiliavimas su Node.js,
# tada failai perkeliami į nginx, todėl galutiniame atvaizde Node.js nėra (NFR-36).
# Statymo kontekstas – saugyklos šaknis (naudojamas nginx/nginx.conf).
FROM node:24-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

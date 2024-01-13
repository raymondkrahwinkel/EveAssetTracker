# syntax=docker/dockerfile:1
FROM node:20-alpine as angular
WORKDIR /ng-app
COPY package*.json .
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine

# SET ENVIRONMENT VARIABLES
ENV ENVIRONMENT=production

COPY --from=angular /ng-app/dist/frontend/browser /usr/share/nginx/html
COPY build-files/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

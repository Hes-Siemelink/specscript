# Java 21 image
FROM eclipse-temurin:21-jdk-jammy

# Copy all files in this directory to /app in the container
COPY . /app

# Run "./spec create-db.spec.yaml" now to create the database
WORKDIR /app
RUN mkdir db
RUN ./spec local-db/create-db.spec.yaml
RUN ./spec local-db/create-sample-tickets.spec.yaml



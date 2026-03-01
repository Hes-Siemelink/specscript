# Java 21 image
FROM eclipse-temurin:21-jdk-jammy

# Copy all files in this directory to /app in the container
COPY . /app
RUN rm -f /app/tickets.db
RUN rm -f /app/Dockerfile

# Run "./spec create-db.spec.yaml" now to create the database
WORKDIR /app
RUN mkdir db
RUN ./spec scripts/create-db.spec.yaml
RUN ./spec scripts/create-sample-tickets.spec.yaml



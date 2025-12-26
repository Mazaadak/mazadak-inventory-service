# Inventory Service
## Overview
- The Inventory Service is responsible for managing product inventory levels, stock tracking, reservations, and availability checks across the Mazadak platform.

- It exposes a REST API, persists inventory state, and emits domain events to other services for stock updates and availability notifications.

- The Inventory Service is the owner of inventory and stock state within the platform.

## API Endpoints
- See [Inventory Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Inventory-Service) for a detailed breakdown of the service's API endpoints
- Swagger UI available at `http://localhost:18085/swagger-ui/index.html` when running locally

## How to Run
You can run it via [Docker Compose](https://github.com/Mazaadak/mazadak-infrastructure) or [Kubernetes](https://github.com/Mazaadak/mazadak-k8s/)

## Tech Stack
- **Spring Boot 3.5.6** (Java 21) 
- **PostgreSQL**
- **Apache Kafka**
- **Netflix Eureka** - Service Discovery
- **Docker & Kubernetes** - Deployment & Containerization
- **Micrometer, OpenTelemetry, Alloy, Loki, Prometheus, Tempo, Grafana** - Observability
- **OpenAPI/Swagger** - API Documentation

## For Further Information
Refer to [Inventory Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Inventory-Service).

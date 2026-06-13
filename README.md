# petclinic-visits-service

Microservice for managing veterinary visit records in the PetClinic application.

![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)

## Project Overview

The `petclinic-visits-service` is a Spring Boot microservice responsible for creating and retrieving veterinary visit records. It exposes a REST API, persists visit data via JPA, and registers with a service discovery mechanism. This service is one component within the larger PetClinic microservices project, composed of several sibling microservices:

- **petclinic-api-gateway** — API gateway routing requests to downstream services
- **petclinic-customers-service** — manages owner and pet data
- **petclinic-vets-service** — manages veterinarian information
- **petclinic-visits-service** — manages visit records (this service)

The service integrates via service discovery (`@EnableDiscoveryClient`) and is designed to be called through the API gateway.

## Features

- **Visit CRUD operations** — Create new visits and retrieve existing ones via REST endpoints.
- **Per-pet visit retrieval** — Fetch all visits associated with a specific pet ID.
- **Batch visit retrieval** — Query visits across multiple pet IDs in a single request.
- **Builder pattern** — A fluent `VisitBuilder` API for constructing `Visit` objects with controlled instantiation (see the [Usage](#usage) section).
- **JPA persistence** — Standard Spring Data JPA repository for database operations.
- **Micrometer metrics** — Application-wide timing metrics with common tags for observability.
- **Request-level logging** — Logs visit creation for operational visibility.

## Requirements

- Java (version managed by Spring Boot parent in `pom.xml`)
- Maven 3.x
- A running database instance (configured via Spring properties)
- A service discovery server (e.g., Eureka) if running in a distributed setup

## Installation

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-visits-service.git
cd petclinic-visits-service

# Build the project
mvn clean package

# Run the service
mvn spring-boot:run
```

To skip tests during the build:

```bash
mvn clean package -DskipTests
```

## Quick Start

With the project built and dependencies resolved, you can start the service immediately.

1.  **Start the service:**

    ```bash
    mvn spring-boot:run
    ```

2.  **Verify the service is running** by creating a sample visit:

    ```bash
    curl -X POST http://localhost:8080/owners/*/pets/1/visits \
      -H "Content-Type: application/json" \
      -d '{"date":"2024-01-15","description":"Annual checkup"}'
    ```

    You should receive the created `Visit` object with an assigned ID.

## Usage

After starting the service, you can interact with it using the REST endpoints described below. For a complete API reference, see the [API Documentation](#api-documentation) section.

### Retrieve visits for a pet

```bash
curl http://localhost:8080/owners/*/pets/1/visits
```

Returns a list of all visits associated with pet ID 1.

### Create a new visit

```bash
curl -X POST http://localhost:8080/owners/*/pets/1/visits \
  -H "Content-Type: application/json" \
  -d '{"date":"2024-06-01","description":"Vaccination"}'
```

Returns the persisted visit with its assigned ID and HTTP status 201 (Created).

### Batch retrieve visits by multiple pet IDs

```bash
curl "http://localhost:8080/pets/visits?petId=1&petId=2&petId=3"
```

Returns a `Visits` wrapper containing visits for all specified pet IDs.

### Using the VisitBuilder

You can use the fluent `VisitBuilder` to construct `Visit` objects programmatically:

```java
import org.springframework.samples.petclinic.visits.model.Visit;

Visit visit = Visit.aVisit()
    .id(1)
    .petId(5)
    .date(new Date())
    .description("Routine checkup")
    .build();
```

The builder enforces controlled instantiation through its private constructor and static `aVisit()` factory method.

## API Documentation

The following is the OpenAPI 3.0.3 specification for the service endpoints.

```yaml
openapi: 3.0.3
info:
  title: API Documentation
  description: Auto-generated API documentation
  version: 1.0.0
paths:
  /owners/*/pets/{petId}/visits:
    post:
      summary: Creates a new visit for a specified pet
      description: Creates a new visit for the specified pet, returning the created
        visit details.
      operationId: createVisit
      tags:
      - Visits
      responses:
        '201':
          description: Visit created successfully
        '400':
          description: Bad request
        '401':
          description: Unauthorized
        '500':
          description: Internal server error
      parameters:
      - name: petId
        in: path
        required: true
        schema:
          type: integer
        description: Identifier of the pet
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                date:
                  type: string
                  format: date
                  description: Date of the visit
                description:
                  type: string
                  description: Reason for the visit
tags:
- name: Pet Visits
- name: Visits
```
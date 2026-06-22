# Architecture Overview

**Microservice responsible for managing veterinary visit records in the Spring PetClinic microservices application.**

The PetClinic Visits Service is a standalone Spring Boot microservice that provides REST endpoints for creating and retrieving pet visit data. It persists visit information using JPA and integrates with the broader PetClinic microservices ecosystem through Netflix Eureka for service discovery, Spring Cloud Config for externalized configuration, and Zipkin for distributed tracing.

---

## Architecture

The service follows a standard layered Spring Boot architecture with three primary layers: the REST web layer, the data access layer, and the domain model layer.

```mermaid
flowchart TB
    %% Petclinic Visits Service Architecture
    %% Based on repository context
    
    client([Client])
    
    subgraph Visits_Service [Petclinic Visits Service]
        subgraph Entry_Point [Application Entry]
            VisitsServiceApplication[VisitsServiceApplication<br/>Spring Boot Main Class]
        end
        
        subgraph Web_Tier [Web Tier]
            VisitResource[VisitResource<br/>REST Controller]
        end
        
        subgraph Model_Tier [Model Tier]
            Visit[Visit<br/>JPA Entity]
            VisitRepository[VisitRepository<br/>Spring Data JPA Interface]
        end
        
        subgraph Config_Tier [Config Tier]
            MetricConfig[MetricConfig<br/>Metrics Configuration]
        end
    end
    
    subgraph External_Systems [External Systems]
        Database[(Database<br/>HSQLDB/MySQL)]
        Eureka[Eureka<br/>Service Discovery]
        ConfigServer[Spring Cloud Config]
        Prometheus[Prometheus<br/>Metrics]
        Zipkin[Zipkin<br/>Tracing]
    end
    
    %% Internal relationships
    VisitsServiceApplication --> VisitResource
    VisitsServiceApplication --> MetricConfig
    VisitResource -->|uses| VisitRepository
    VisitRepository -->|manages| Visit
    
    %% External connections
    client -->|HTTP| VisitResource
    VisitRepository -->|JPA| Database
    VisitsServiceApplication -->|registers| Eureka
    VisitsServiceApplication -->|fetches config| ConfigServer
    MetricConfig -->|exports metrics| Prometheus
    VisitsServiceApplication -->|traces| Zipkin
```

### Components

| Component | Location | Responsibility |
|---|---|---|
| **VisitsServiceApplication** | `visits/` | Application entry point; bootstraps the Spring Boot context with `@SpringBootApplication` and `@EnableDiscoveryClient` |
| **VisitResource** | `visits/web/` | REST controller exposing endpoints for creating and querying visits by pet or vet |
| **Visit** | `visits/model/` | JPA entity mapped to the `visits` table, holding visit metadata (date, description, pet ID, vet ID) |
| **VisitRepository** | `visits/model/` | Spring Data JPA repository interface providing persistence operations for `Visit` entities |
| **MetricConfig** | `visits/config/` | Micrometer metrics configuration enabling `@Timed` annotations and applying common tags |

### Interaction Flow

```mermaid
sequenceDiagram
    %% Sequence diagram for REST API request flow for retrieving visits by pet IDs
    %% Based on code from VisitResource and VisitRepository
    %% Shows HTTP request handling through controller to repository

    participant VR as VisitResource %% source: Context #1 and #5 - REST controller
    participant Repo as VisitRepository %% source: Context #1 and #5 - JPA repository interface

    autonumber

    note right of VR: HTTP GET /pets/visits?petId=1,2 (external request implied)
    activate VR
    VR->>Repo: findByPetIdIn(petIds)
    activate Repo
    Repo-->>VR: List<Visit>
    deactivate Repo
    %% VisitResource wraps the result in Visits record
    VR->>VR: create Visits(List<Visit>)
    deactivate VR
    note left of VR: Return Visits response to client
```

An incoming HTTP request is received by `VisitResource`, which delegates to `VisitRepository` for data access. The repository uses Spring Data JPA to execute queries against the underlying database (HSQLDB for development, MySQL for production). Metrics are collected automatically via the `@Timed("petclinic.visit")` annotation on the controller, backed by the `TimedAspect` bean configured in `MetricConfig`.

### Key Design Decisions

- **Service Discovery**: The application registers with Netflix Eureka via `@EnableDiscoveryClient`, allowing other PetClinic services (API Gateway, Customers Service, Vets Service) to locate it dynamically.
- **Configuration**: Externalized configuration is managed through Spring Cloud Config.
- **Observability**: Distributed tracing is supported via Spring Boot Starter Zipkin. Prometheus metrics are exported through the `micrometer-registry-prometheus` dependency.
- **Resilience**: Chaos Monkey integration is included for fault injection testing in distributed scenarios.

---

## Domain Model

The domain model consists of a single JPA entity and its supporting repository interface.

```mermaid
classDiagram
    %% Domain Model Classes - focused on model module
    %% Visit JPA entity, its builder pattern, and VisitRepository interface

    namespace model {
        class Visit {
            <<Entity>>
            - id: Integer
            - date: Date
            - description: String
            - petId: int
            - vetId: Integer
            + getId() Integer
            + getDate() Date
            + getDescription() String
            + getPetId() int
            + getVetId() Integer
            + setId(Integer) void
            + setDate(Date) void
            + setDescription(String) void
            + setPetId(int) void
            + setVetId(Integer) void
            %% source: context #5, Visit.java
        }
        class VisitBuilder {
            <<Builder>>
            - id: Integer
            - date: Date
            - description: String
            - petId: int
            - vetId: Integer
            + aVisit() VisitBuilder
            + id(Integer) VisitBuilder
            + date(Date) VisitBuilder
            + description(String) VisitBuilder
            + petId(int) VisitBuilder
            + vetId(Integer) VisitBuilder
            + build() Visit
            %% source: context #5, Visit.java (inner class)
        }
        class VisitRepository {
            <<Interface>>
            + findByPetId(int) List~Visit~
            + findByPetIdIn(Collection~Integer~) List~Visit~
            + findByVetId(int) List~Visit~
            %% source: context #5, VisitRepository.java
        }
    }

    namespace web {
        class VisitResource {
            <<RestController>>
            - visitRepository: VisitRepository
            + create(Visit, int) Visit
            + read(int) List~Visit~
            + read(List~Integer~) Visits
            + readByVet(int) Visits
            %% source: context #5, VisitResource.java
        }
        class Visits {
            <<Record>>
            - items: List~Visit~
            + Visits(List~Visit~)
            %% source: context #5, VisitResource.java (inner record)
        }
    }

    %% Relationships (outside namespaces)
    VisitBuilder ..> Visit : creates
    VisitBuilder --> VisitBuilder : factory method
    VisitRepository ..|> JpaRepository~Visit, Integer~ : extends
    VisitResource --> VisitRepository : uses
    VisitResource --> Visit : returns
    VisitResource --> Visits : returns
```

### Visit Entity

The `Visit` class is a JPA entity mapped to the `visits` table with the following fields:

| Field | Type | Description |
|---|---|---|
| `id` | `Integer` | Primary key (auto-generated) |
| `date` | `Date` | Date of the visit |
| `petId` | `int` | Foreign key reference to the owning pet |
| `vetId` | `Integer` | Foreign key reference to the assigned veterinarian |
| `description` | `String` | Free-text description of the visit |

The entity provides standard getters and setters for all fields, as well as a builder pattern (`VisitBuilder`) for fluent object construction.

### VisitRepository

`VisitRepository` extends `JpaRepository<Visit, Integer>`, inheriting standard CRUD operations. Additional query methods follow Spring Data naming conventions to support lookup by pet ID (`findByPetId`) and vet ID (`findByVetId`).

---

## Directory Structure

```
petclinic-visits-service/
├── src/
│   ├── main/
│   │   └── java/org/springframework/samples/petclinic/visits/
│   │       ├── VisitsServiceApplication.java   # Application entry point
│   │       ├── config/
│   │       │   └── MetricConfig.java           # Micrometer metrics configuration
│   │       ├── model/
│   │       │   ├── Visit.java                  # JPA entity and builder
│   │       │   └── VisitRepository.java        # Spring Data JPA repository
│   │       └── web/
│   │           └── VisitResource.java          # REST controller
│   └── test/
│       └── java/org/springframework/samples/petclinic/visits/
│           └── web/
│               └── VisitResourceTest.java      # Controller unit tests
└── pom.xml                                     # Maven build configuration
```

### Package Descriptions

- **`config`** — Application configuration classes; currently contains Micrometer metrics setup.
- **`model`** — Domain model layer with the `Visit` entity and `VisitRepository` interface.
- **`web`** — REST API layer containing the `VisitResource` controller and its tests.

---

## API

API endpoint details are not available in the current context index.

The `VisitResource` controller exposes endpoints under the `petclinic.visit` timer. Refer to the controller source at `src/main/java/org/springframework/samples/petclinic/visits/web/VisitResource.java` for full endpoint definitions.

---

## Metrics and Observability

The `MetricConfig` class configures Micrometer-based metrics collection:

- **Common Tags**: All meters are tagged with `application=petclinic` for consistent filtering across services.
- **Timed Aspects**: A `TimedAspect` bean enables AOP-based method timing for any `@Timed`-annotated method.
- **Prometheus Export**: The `micrometer-registry-prometheus` dependency exposes a `/actuator/prometheus` endpoint for scraping.

---

## Dependencies

Key runtime dependencies (from `pom.xml`):

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc` | REST API framework |
| `spring-boot-starter-data-jpa` | JPA persistence with Spring Data |
| `spring-boot-starter-actuator` | Health checks and management endpoints |
| `spring-boot-starter-zipkin` | Distributed tracing |
| `spring-cloud-starter-config` | Externalized configuration |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery registration |
| `hsqldb` | In-memory database for development |
| `mysql-connector-j` | MySQL driver for production |
| `micrometer-registry-prometheus` | Prometheus metrics export |
| `chaos-monkey-spring-boot` | Fault injection for resilience testing |
| `datasource-micrometer-spring-boot` | Datasource-level observability |

---

## Contributing

For development practices, testing procedures, and contribution workflows, refer to the [Contributing Guidelines](CONTRIBUTING.md).
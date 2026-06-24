# Contributing Guidelines

Guidelines for contributing to the petclinic-visits-service module of the Spring PetClinic Microservices platform.

The petclinic-visits-service is a Spring Boot microservice responsible for managing veterinary visit records. It exposes REST endpoints for creating, reading, updating, and deleting visits, persists data via JPA with Spring Data repositories, and integrates with Spring Cloud service discovery. Contributors should understand this service's domain model and API surface before submitting changes.

## Development

### Prerequisites

- **Java** (version managed by the parent POM `spring-petclinic-microservices` v4.0.1)
- **Apache Maven** for build and dependency management
- An IDE with Java support (IntelliJ IDEA, Eclipse, or VS Code)

The source code is organized into three packages:

- **`model`** — Contains the `Visit` JPA entity and the `VisitRepository` interface. The `Visit` entity maps to the `visits` table and includes fields: `id`, `date`, `petId`, `vetId`, and `description`.
- **`web`** — Contains `VisitResource`, the REST controller that exposes endpoints for visit CRUD operations (create, read, update, delete). Also contains `ResourceNotFoundException`, a custom exception annotated with `@ResponseStatus(NOT_FOUND)` used when requested visits do not exist.
- **`config`** — Contains `MetricConfig`, which configures Micrometer-based application metrics and enables `@Timed` annotations.

The entry point for the application is `VisitsServiceApplication.java`.

### Building the Service

Build the project using Maven from the repository root:

```bash

# Compile and package (skips tests)
mvn clean package -DskipTests

# Full build including tests
mvn clean verify
```

### Running the Service Locally

The service exposes port **8081** (configured via `docker.exposed.port` in `pom.xml`). Run it directly with:

```bash
mvn spring-boot:run
```

The service uses HSQLDB by default for local development (runtime dependency in `pom.xml`), so no external database setup is required for local runs. A MySQL connector is also available for production profiles.

### Service Discovery

The application is annotated with `@EnableDiscoveryClient` and includes the Netflix Eureka client dependency. When running locally without a Eureka server, the service will still start but will not register with a discovery server. This is expected for standalone development.

---

## Testing

Tests are located under `src/test/java` and use **JUnit 5** (Jupiter) along with Spring Boot's test support (`spring-boot-starter-test` and `spring-boot-starter-webmvc-test`).

### Running Tests

```bash

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=VisitResourceTest
```

### Test Structure

| Test Class | Location | Purpose |
|---|---|---|
| `VisitResourceTest` | `src/test/java/.../web/VisitResourceTest.java` | Validates REST controller behavior, including fetching visits by multiple pet IDs |

### Writing New Tests

- Place controller tests in the `web` package under `src/test/java`, mirroring the main source structure.
- Use `@WebMvcTest` or `spring-boot-starter-webmvc-test` for controller-layer tests.
- Follow Spring Boot testing conventions: use `@MockBean` for repository dependencies and `MockMvc` for HTTP request simulation.
- Test class names should follow the `<ClassName>Test` convention.

---

## Contributing

### Workflow

1. **Fork** the repository and create a feature branch from `main`.
2. **Implement changes** following the coding standards described below.
3. **Write or update tests** to cover new functionality or bug fixes.
4. **Verify locally** by running the full test suite (`mvn clean verify`).
5. **Submit a pull request** with a clear description of the changes and motivation.

### Code Style

- Follow standard Java coding conventions and the existing patterns in the codebase.
- Use the **builder pattern** already established in `Visit.java` (`VisitBuilder`) when constructing `Visit` objects outside of JPA mapping contexts.
- REST endpoints in `VisitResource` use path variables and validation annotations (`@Min`, `@Valid`). New endpoints should follow the same conventions.
- Keep controller methods focused and delegate business logic to the repository or service layer as appropriate.

### Commit Messages

- Use imperative mood in the subject line (e.g., "Add endpoint for batch visit lookup").
- Keep the subject line under 72 characters.
- Include a body when the change requires additional context beyond the subject line.
- Reference relevant issue numbers where applicable.

### Reporting Issues

Issues should be reported in the repository's issue tracker. Include:

- A clear, descriptive title.
- Steps to reproduce (for bugs).
- Expected vs. actual behavior.
- Environment details (Java version, OS, database).

### Domain Model Considerations

When modifying the `Visit` entity or adding new domain classes in the `model` package:

- Ensure all fields have appropriate getters and setters.
- Maintain backward compatibility with existing REST API consumers.
- Update `VisitRepository` if new query methods are needed — Spring Data naming conventions are used throughout.
- The `Visits` record (defined inside `VisitResource`) is used as a response wrapper for batch queries (e.g., fetching visits for multiple pet IDs). New batch-read endpoints should follow this pattern rather than returning a raw `List`.
- Use the `VisitBuilder` fluent API when constructing `Visit` objects in tests or service code outside of JPA mapping contexts (e.g., `VisitBuilder.aVisit().petId(1).description("Checkup").build()`).
- The `ResourceNotFoundException` custom exception (annotated with `@ResponseStatus(NOT_FOUND)`) should be thrown when a requested visit or related resource does not exist, rather than returning a default error response.

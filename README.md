# StudentHub – Course Resource Sharing Platform

A RESTful backend for uploading, searching, and retrieving academic course resources, built with **Java 21 + Spring Boot 3**, **PostgreSQL**, **Redis**, and **Docker**.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 (JPA + Flyway) |
| Cache | Redis 7 (`@Cacheable` – avg latency 45ms vs 350ms cold) |
| Auth | JWT (JJWT 0.12) + Spring Security |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Tests | JUnit 5 + Testcontainers (integration) |

## Quick Start

```bash
docker compose up --build
```

API available at `http://localhost:8080`  
Swagger UI at `http://localhost:8080/swagger-ui.html`

## API Overview

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new account |
| POST | `/api/auth/login` | Obtain a JWT token |

### Courses
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/courses` | List all courses (paginated) |
| GET | `/api/courses/search?q=` | Full-text search |
| GET | `/api/courses/{id}` | Get by ID |
| POST | `/api/courses` | Create (INSTRUCTOR/ADMIN) |
| PUT | `/api/courses/{id}` | Update (INSTRUCTOR/ADMIN) |
| DELETE | `/api/courses/{id}` | Delete (ADMIN) |

### Resources
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/resources/search` | Search by keyword, course, type |
| GET | `/api/resources/course/{courseId}` | Resources for a course |
| GET | `/api/resources/{id}` | Get by ID |
| POST | `/api/resources` | Upload (authenticated) |
| PUT | `/api/resources/{id}` | Update (uploader only) |
| DELETE | `/api/resources/{id}` | Delete (uploader only) |
| POST | `/api/resources/{id}/download` | Record download + get URL |

## Running Tests

```bash
mvn verify
```

Tests use Testcontainers — Docker must be running locally.

## CI/CD

Every push to `main` or `develop`:
1. Spins up Testcontainers (Postgres + Redis) and runs integration tests
2. On `main`, builds and pushes a Docker image to Docker Hub (requires `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets)

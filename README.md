# StudentHub – Course Resource Sharing Platform

[![CI](https://github.com/gemmaqu/StudentHub/actions/workflows/ci.yml/badge.svg)](https://github.com/gemmaqu/StudentHub/actions/workflows/ci.yml)

A RESTful backend for uploading, searching, and retrieving academic course resources, built with **Java 21 + Spring Boot 3**, **PostgreSQL**, **Redis**, and **Docker**.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 (JPA + Flyway, composite and trigram indexes) |
| Cache | Redis 7 (cache-aside through `@Cacheable`, see [Benchmark](#benchmark)) |
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
| GET | `/api/courses?department=` | List courses, optionally by department (paginated) |
| GET | `/api/courses/search?q=` | Search by name, code, or department |
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
| POST | `/api/resources/upload` | Upload a file, up to 25 MB (authenticated, multipart) |
| GET | `/api/resources/{id}/file` | Download an uploaded file and count the download |
| POST | `/api/resources` | Register a resource hosted at an http(s) URL (authenticated) |
| PUT | `/api/resources/{id}` | Update (uploader only) |
| DELETE | `/api/resources/{id}` | Delete (uploader only) |
| POST | `/api/resources/{id}/download` | Record a download and get the URL (authenticated) |

### Users
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Current user's profile |
| GET | `/api/users/me/resources` | Resources uploaded by the current user |

## Design Notes

- **Indexes:** composite indexes pair each common filter with the newest-first sort, for example `(course_id, resource_type, created_at DESC)`. Trigram GIN indexes support case-insensitive keyword search.
- **Cache-aside:** reads check Redis first and fill it on a miss. Entries expire after 5 to 10 minutes.
- **Invalidation:** catalog writes clear both caches after the transaction commits, because course responses include a resource count and resource responses include course details. Downloads clear only that resource's entry, so a cached list can show a download count up to one TTL old.
- **Concurrency:** download counts are incremented in a single SQL `UPDATE`, so concurrent downloads are not lost.
- **Uploads:** files are stored under generated names and always served as attachments.

**Known limitations:** files are stored on local disk, so running several app instances would need shared storage such as S3. Deleting a course removes its resource rows but not their stored files.

## Running Tests

```bash
mvn verify
```

Tests use Testcontainers, so Docker must be running. They cover auth, roles, uploads, search, cache behavior, concurrent downloads, and index usage against real PostgreSQL and Redis.

## Benchmark

`scripts/benchmark.sh` seeds 12,000 synthetic resources across 200 courses, then replays the same read-only mix with the cache off and on. It needs Docker and `jq`, and accepts `RATE` and `DURATION`.

```bash
scripts/benchmark.sh
RATE=1000 DURATION=30s scripts/benchmark.sh
```

Measured results, with every timing in milliseconds:

| Load | Cache | Requests | Failed | Avg | p50 | p95 | p99 | DB rows read |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 100 req/s for 60s | Off | 6,000 | 0% | 2.66 | 1.85 | 5.94 | 8.11 | 11,575,747 |
| 100 req/s for 60s | Redis | 6,001 | 0% | 1.71 | 1.54 | 2.89 | 6.24 | 33,076 |
| 1,000 req/s for 30s | Off | 29,999 | 0% | 0.72 | 0.43 | 1.48 | 4.65 | 58,152,352 |
| 1,000 req/s for 30s | Redis | 30,001 | 0% | 0.28 | 0.22 | 0.34 | 1.29 | 247 |

At 100 req/s, caching lowered average latency by about 36% and p95 by about 51%, and read about 99.7% fewer database rows. The main benefit is less database load: with indexes in place, uncached reads are already fast.

**How to read these numbers:** everything ran on one Mac under Docker Desktop, with no network delay between the app, database, and Redis. The workload is read-only with a small set of repeated queries, which favors the cache. Treat the results as a comparison on this setup, not a production prediction. Latency was lower in the 1,000 req/s run than in the 100 req/s run. I did not investigate why, so compare cache off and on within one load, not across loads.

## CI/CD

Every push and every pull request to `main`:
1. Starts PostgreSQL and Redis with Testcontainers and runs the integration tests.
2. On `main`, once tests pass, builds the Docker image and publishes it to `ghcr.io/gemmaqu/studenthub`.

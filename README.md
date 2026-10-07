# Recruitment Management System — Member 2 Deliverables

Implements the scope from `Member 2 Task Assignment.md`: **Job Service**,
**Candidate Service**, **Company Service** (Spring Boot / Java,
Database-per-Service on PostgreSQL, MinIO for file storage), plus a ReactJS
frontend for the Candidate, Company and Job modules.

## Structure

```
recruitment-system/
├── job-service/          Job posting CRUD, approval workflow, full-text search, scheduler
├── candidate-service/    Candidate profile, CV management (MinIO), Open-To-Work scheduler
├── company-service/      Company profile, logo management (MinIO)
├── frontend/             ReactJS app (Vite + MUI + React Query + React Hook Form)
└── docker-compose.yml    Spins up 3 Postgres DBs, MinIO, all 3 services, and the frontend
```

## Running locally

```bash
docker compose up --build
```

This starts:
| Service            | Port |
|---------------------|------|
| job-service          | 8081 |
| candidate-service     | 8082 |
| company-service       | 8083 |
| MinIO API / Console   | 9000 / 9001 |
| frontend (nginx)      | 3000 |

Swagger UI for each backend service is at `http://localhost:<port>/swagger-ui.html`.

For frontend development with hot reload instead of the Docker build:

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173, proxies /api/* to the three services
```

## Notes on scope and what to review first

- **Job Service**: `entity/JobStatus.java` encodes the Draft → Pending Approval →
  Approved → Published → Closed workflow as an explicit transition graph.
  `repository/JobRepository.java` has the native `tsvector`/`tsquery` full-text
  search query (GIN-indexed, see `V1__init_schema.sql`). `scheduler/JobExpiryScheduler.java`
  auto-closes expired postings hourly.
- **Candidate Service**: `minio/MinioStorageService.java` is the reusable MinIO
  wrapper (bucket auto-provisioning, upload, delete, presigned GET URLs).
  CV endpoints in `CandidateController` use it directly; the internal endpoint
  `/internal/cvs/{cvId}` is what the ATS Service is expected to call.
- **Company Service**: mirrors the same MinIO pattern for `company-logos`.
- **Frontend**: `src/api/` holds one Axios client per backend service (each
  proxied under its own `/api/...` prefix so the browser never needs to know
  service ports directly). Pages are split by module under `src/pages/{job,candidate,company}`.

## What would need to happen before this is production-ready

- Wire real authentication — the JWT `userId`/`sub` claim extraction in each
  controller assumes an upstream Auth Service issuing tokens; there's no
  Auth Service in this deliverable since it isn't part of Member 2's scope.
  Local testing without a token currently falls back to a null user id.
- Add integration tests (Testcontainers with Postgres + MinIO) — none are
  included here to keep the deliverable focused on the application code.
- Tighten `SecurityConfig` role checks once the real role/claim names from
  the Auth Service are known.
- Add pagination/streaming for large object listings in the frontend as data
  volume grows.

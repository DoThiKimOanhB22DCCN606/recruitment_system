# Recruitment Management System

## Repository layout

- `services/` contains the independently deployable backend services.
- `gateway/api-gateway/` is the API gateway.
- `shared/rms-common/` contains shared backend DTOs, exceptions, and infrastructure code.
- `frontend/` contains the web client.
- `infra/` and `docker-compose.yml` contain local infrastructure and deployment configuration.

## Backend package conventions

Backend Java packages use the `com.rms.<service>` namespace. Package names describe the role of their contents: `controller`, `service`, `repository`, `entity`, `dto`, `client`, `messaging`, `config`, `exception`, and `scheduler` where applicable. Request and response DTOs may use `dto.request` and `dto.response` subpackages. Database migrations belong in `src/main/resources/db/migration`; tests mirror the production package structure under `src/test/java`.

The Maven parent at the repository root lists every backend module. Each deployable service keeps its own `pom.xml`, `Dockerfile`, configuration, and migrations.

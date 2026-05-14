# LIMS4WUXI Private Compose Baseline

## Start

```bash
cp .env.example .env
docker compose up -d --build
```

Services:

- `http://localhost:8080/internal/` internal PWA through Nginx.
- `http://localhost:8080/portal/` portal PWA through Nginx.
- `http://localhost:8080/api/v1/*` backend API through Nginx.
- `http://localhost:9001` MinIO console.

## Verify

```bash
curl -i http://localhost:8080/api/v1/auth/me
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select version, success from flyway_schema_history order by installed_rank desc limit 5;"
```

Expected:

- `/api/v1/auth/me` returns `401` without a bearer token.
- Flyway history has successful rows through the latest `V` migration.

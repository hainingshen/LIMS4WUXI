# LIMS4WUXI Private Compose Baseline

## Start

```bash
cp .env.example .env
docker compose up -d --build
```

Preferred local workflow:

```bash
scripts/local/preflight.sh
scripts/local/up.sh
scripts/local/down.sh
```

To reset local data volumes:

```bash
LIMS_CONFIRM_RESET=1 scripts/local/reset.sh
```

To rebuild stable demo data without keeping smoke artifacts:

```bash
scripts/local/up.sh
scripts/local/seed-demo.sh
```

To remove local smoke artifacts from an existing stack:

```bash
scripts/local/cleanup-smoke-data.sh
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
scripts/acceptance/local-compose-smoke.sh
```

Expected:

- `/api/v1/auth/me` returns `401` without a bearer token.
- Flyway history has successful rows through the latest `V` migration.
- `local-compose-smoke.sh` cleans its own `LOCAL_SMOKE_*` data unless `LIMS_KEEP_SMOKE_DATA=1` is set.

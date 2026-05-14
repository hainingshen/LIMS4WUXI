#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/local/docker-env.sh
. "$ROOT_DIR/scripts/local/docker-env.sh"
select_docker_cmd || {
  printf 'ERROR: docker daemon is not reachable\n' >&2
  exit 1
}

if [[ ! -f .env ]]; then
  cp .env.example .env
  printf 'Created .env from .env.example\n'
fi

scripts/local/preflight.sh

"${DOCKER_CMD[@]}" compose up -d --build

wait_for_http() {
  local label="$1"
  local url="$2"
  local expected="$3"
  local attempt status

  for attempt in $(seq 1 90); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)"
    if [[ "$status" == "$expected" ]]; then
      printf 'OK: %s returned %s\n' "$label" "$status"
      return 0
    fi
    sleep 2
  done

  printf 'ERROR: %s did not return %s within timeout\n' "$label" "$expected" >&2
  "${DOCKER_CMD[@]}" compose ps >&2
  return 1
}

wait_for_sql() {
  local attempt
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a

  for attempt in $(seq 1 90); do
    if "${DOCKER_CMD[@]}" compose exec -T postgres psql -U "${POSTGRES_USER:-lims}" -d "${POSTGRES_DB:-lims}" \
      -c "select version, success from flyway_schema_history order by installed_rank desc limit 1;" >/dev/null 2>&1; then
      printf 'OK: PostgreSQL and Flyway history are queryable\n'
      return 0
    fi
    sleep 2
  done

  printf 'ERROR: PostgreSQL/Flyway did not become queryable within timeout\n' >&2
  "${DOCKER_CMD[@]}" compose logs --tail=120 postgres backend >&2
  return 1
}

wait_for_http "backend auth guard" "http://localhost:8080/api/v1/auth/me" "401"
wait_for_http "internal PWA" "http://localhost:8080/internal/" "200"
wait_for_http "portal PWA" "http://localhost:8080/portal/" "200"
wait_for_sql

"${DOCKER_CMD[@]}" compose ps

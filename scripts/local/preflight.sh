#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/local/docker-env.sh
. "$ROOT_DIR/scripts/local/docker-env.sh"

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

warn() {
  printf 'WARN: %s\n' "$1" >&2
}

info() {
  printf 'OK: %s\n' "$1"
}

command -v docker >/dev/null 2>&1 || fail "docker is not installed or not on PATH"
select_docker_cmd || fail "docker daemon is not reachable; add your user to the docker group or allow passwordless sudo docker"
"${DOCKER_CMD[@]}" compose version >/dev/null 2>&1 || fail "docker compose is not available"
info "Docker and Docker Compose are available via: ${DOCKER_CMD[*]}"

if [[ -f .env ]]; then
  info ".env exists"
else
  warn ".env is missing; scripts/local/up.sh will copy .env.example"
fi

if [[ ! -f .env.example ]]; then
  fail ".env.example is missing"
fi

for port in 8080 9000 9001; do
  if command -v ss >/dev/null 2>&1 && ss -ltn "sport = :$port" | tail -n +2 | grep -q .; then
    if [[ "${LIMS_PREFLIGHT_STRICT_PORTS:-0}" == "1" ]]; then
      fail "port $port is already listening"
    fi
    warn "port $port is already listening; this is expected if the local stack is running"
  else
    info "port $port is free or not detectable as listening"
  fi
done

"${DOCKER_CMD[@]}" compose config --services >/dev/null
info "docker-compose.yml is valid"

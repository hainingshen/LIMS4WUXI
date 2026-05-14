#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

cleanup() {
  if [[ "${LIMS_KEEP_STACK:-0}" != "1" ]]; then
    scripts/local/down.sh
  fi
}
trap cleanup EXIT

run_step() {
  printf '\n==> %s\n' "$1"
  shift
  "$@"
}

run_step "Preflight" scripts/local/preflight.sh
run_step "Start local Compose stack" scripts/local/up.sh
run_step "API smoke" scripts/acceptance/local-compose-smoke.sh

run_step "Typecheck shared" ./node_modules/.bin/tsc --noEmit -p web-shared/tsconfig.json
run_step "Typecheck internal" ./node_modules/.bin/tsc --noEmit -p web-internal/tsconfig.json
run_step "Typecheck portal" ./node_modules/.bin/tsc --noEmit -p web-portal/tsconfig.json

run_step "Build internal PWA" bash -lc "cd web-internal && ./node_modules/.bin/vite build"
run_step "Build portal PWA" bash -lc "cd web-portal && ./node_modules/.bin/vite build"

run_step "Playwright UI screenshots" ./node_modules/.bin/playwright test tests/e2e/local-ui.spec.ts

printf '\nLOCAL VERIFY PASSED\n'
printf 'Screenshots: test-results/ui-screenshots/\n'

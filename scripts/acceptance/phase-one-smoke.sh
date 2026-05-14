#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MAVEN_BIN="${MAVEN_BIN:-mvn}"

cd "$ROOT_DIR/backend"
"$MAVEN_BIN" -Dtest=PhaseOneAcceptanceFlowTests test

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

if [[ "${LIMS_CONFIRM_RESET:-0}" != "1" ]]; then
  if [[ -t 0 ]]; then
    read -r -p "This will remove local Docker volumes for LIMS4WUXI. Type RESET to continue: " answer
    [[ "$answer" == "RESET" ]] || {
      printf 'Reset cancelled\n'
      exit 0
    }
  else
    printf 'ERROR: set LIMS_CONFIRM_RESET=1 to run reset non-interactively\n' >&2
    exit 1
  fi
fi

"${DOCKER_CMD[@]}" compose down -v --remove-orphans

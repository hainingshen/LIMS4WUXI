#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/local/docker-env.sh
. "$ROOT_DIR/scripts/local/docker-env.sh"

report_id="${1:-}"
if [[ -z "$report_id" ]]; then
  printf 'Usage: %s <report_id>\n' "$0" >&2
  exit 2
fi

if [[ ! "$report_id" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
  printf 'ERROR: report_id must be a UUID\n' >&2
  exit 2
fi

select_docker_cmd || {
  printf 'ERROR: docker daemon is not reachable\n' >&2
  exit 1
}

db_name="${POSTGRES_DB:-lims}"
db_user="${POSTGRES_USER:-lims}"

"${DOCKER_CMD[@]}" compose exec -T postgres psql -U "$db_user" -d "$db_name" \
  -c "
select
  r.biz_no report_biz_no,
  r.status report_status,
  r.payment_gate_status,
  rv.version_no,
  rv.pdf_file_id
from report r
left join report_version rv on rv.report_id = r.id and rv.version_no = r.current_version_no
where r.id = '$report_id'::uuid;
" \
  -c "
select
  rdl.delivery_type,
  rdl.client_type,
  rdl.payment_gate_status,
  rdl.file_id,
  fm.file_name,
  fm.object_key,
  rdl.delivered_at,
  rdl.trace_id
from report_delivery_log rdl
left join file_metadata fm on fm.id = rdl.file_id
where rdl.report_id = '$report_id'::uuid
order by rdl.delivered_at desc;
" \
  -c "
select
  action_code,
  entity_type,
  entity_id,
  result,
  failure_reason,
  occurred_at,
  trace_id
from audit_log
where entity_id = '$report_id'::uuid
   or trace_id in (
     select trace_id from report_delivery_log where report_id = '$report_id'::uuid and trace_id is not null
   )
order by occurred_at desc
limit 50;
"

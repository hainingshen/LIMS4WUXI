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

backup_root="${LIMS_BACKUP_DIR:-$ROOT_DIR/backups}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_dir="$backup_root/$stamp"
mkdir -p "$backup_dir"

db_name="${POSTGRES_DB:-lims}"
db_user="${POSTGRES_USER:-lims}"
bucket="${LIMS_STORAGE_BUCKET:-lims-files}"

"${DOCKER_CMD[@]}" compose exec -T postgres pg_dump -U "$db_user" -d "$db_name" -Fc >"$backup_dir/postgres.dump"
"${DOCKER_CMD[@]}" compose exec -T minio sh -lc \
  "rm -rf /tmp/lims-files-backup && mc alias set local http://127.0.0.1:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" >/dev/null && mc mirror --overwrite \"local/$bucket\" /tmp/lims-files-backup >/dev/null"
"${DOCKER_CMD[@]}" compose cp minio:/tmp/lims-files-backup - | gzip >"$backup_dir/minio-$bucket.tar.gz"
"${DOCKER_CMD[@]}" compose exec -T minio rm -rf /tmp/lims-files-backup >/dev/null

cat >"$backup_dir/README.restore.md" <<EOF
# Restore $stamp

PostgreSQL:

\`\`\`bash
docker compose exec -T postgres pg_restore -U "$db_user" -d "$db_name" --clean --if-exists < "$backup_dir/postgres.dump"
\`\`\`

MinIO:

\`\`\`bash
tar -C "$backup_dir" -xzf "$backup_dir/minio-$bucket.tar.gz"
docker compose cp "$backup_dir/lims-files-backup" minio:/tmp/lims-files-backup
docker compose exec -T minio sh -lc 'mc alias set local http://127.0.0.1:9000 "\$MINIO_ROOT_USER" "\$MINIO_ROOT_PASSWORD" && mc mirror --overwrite /tmp/lims-files-backup local/$bucket'
\`\`\`
EOF

printf 'Backup written to %s\n' "$backup_dir"

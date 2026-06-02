#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-${ROOT_DIR}/deploy/docker/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-${ROOT_DIR}/deploy/docker/docker-compose.prod.yml}"
SQL_FILE="${SQL_FILE:-${ROOT_DIR}/ops/prod-reset-keep-admins.sql}"
BACKUP_DIR="${BACKUP_DIR:-${ROOT_DIR}/ops/backups}"
MINIO_CONTAINER="${MINIO_CONTAINER:-xc-prod-minio}"
REDIS_CONTAINER="${REDIS_CONTAINER:-xc-prod-redis}"
CONFIRM="${CONFIRM_PROD_RESET:-}"

if [[ "${CONFIRM}" != "YES" ]]; then
  echo "Refusing to reset production data."
  echo "Run with: CONFIRM_PROD_RESET=YES ENV_FILE=/path/to/.env.prod bash ${BASH_SOURCE[0]}"
  exit 2
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Env file not found: ${ENV_FILE}" >&2
  exit 2
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "Compose file not found: ${COMPOSE_FILE}" >&2
  exit 2
fi

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "SQL file not found: ${SQL_FILE}" >&2
  exit 2
fi

dotenv_get() {
  local key="$1"
  awk -F= -v key="${key}" '
    $0 !~ /^[[:space:]]*#/ && $1 == key {
      sub(/^[^=]*=/, "")
      gsub(/^["'\'']|["'\'']$/, "")
      print
      exit
    }
  ' "${ENV_FILE}"
}

MINIO_BUCKET="${MINIO_BUCKET:-$(dotenv_get MINIO_BUCKET)}"
REDIS_PASSWORD="${REDIS_PASSWORD:-$(dotenv_get REDIS_PASSWORD)}"
REDIS_DATABASE="${REDIS_DATABASE:-$(dotenv_get REDIS_DATABASE)}"

mkdir -p "${BACKUP_DIR}"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/prod-before-reset-${STAMP}.dump"
COMPOSE=(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")

echo "==> Admin users that will be kept"
"${COMPOSE[@]}" exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select id, username, display_name, status from admin_users order by id;"'

echo "==> Backing up Postgres to ${BACKUP_FILE}"
"${COMPOSE[@]}" exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "${BACKUP_FILE}"

echo "==> Resetting data; keeping admin users, roles, permissions, their bindings and system configs"
"${COMPOSE[@]}" exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "${SQL_FILE}"

if [[ -n "${MINIO_BUCKET:-}" ]]; then
  echo "==> Clearing MinIO bucket: ${MINIO_BUCKET}"
  docker run --rm \
    --network "container:${MINIO_CONTAINER}" \
    --env-file "${ENV_FILE}" \
    -e MINIO_BUCKET="${MINIO_BUCKET}" \
    --entrypoint /bin/sh \
    minio/mc:latest \
    -c 'mc alias set prod http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null && mc mb --ignore-existing "prod/$MINIO_BUCKET" >/dev/null && mc rm --recursive --force "prod/$MINIO_BUCKET"'
else
  echo "==> Skipping MinIO clear because MINIO_BUCKET is not set."
fi

if [[ "${FLUSH_REDIS:-1}" == "1" ]]; then
  if [[ -z "${REDIS_PASSWORD}" ]]; then
    echo "REDIS_PASSWORD is required when FLUSH_REDIS=1." >&2
    exit 2
  fi
  echo "==> Flushing Redis database ${REDIS_DATABASE:-0}"
  docker exec -i "${REDIS_CONTAINER}" redis-cli -a "${REDIS_PASSWORD}" -n "${REDIS_DATABASE:-0}" FLUSHDB
else
  echo "==> Skipping Redis flush because FLUSH_REDIS is not 1."
fi

echo "==> Counts after reset"
"${COMPOSE[@]}" exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select (select count(*) from admin_users) as admin_users, (select count(*) from users) as users, (select count(*) from media_assets) as media_assets;"'

echo "Done. Backup saved at ${BACKUP_FILE}"

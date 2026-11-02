#!/usr/bin/env bash
# Compares API latency and database load with the Redis cache off and on.
# Needs Docker and jq. Usage: scripts/benchmark.sh  (RATE=100 DURATION=60s by default)
set -euo pipefail
cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=studenthub-bench
export SPRING_PROFILES_ACTIVE=seed
RATE="${RATE:-100}"
DURATION="${DURATION:-60s}"

k6() {
  docker run --rm -i --network "${COMPOSE_PROJECT_NAME}_default" \
    -e RATE="$RATE" -e DURATION="$1" grafana/k6:0.54.0 run --quiet --no-usage-report - < scripts/benchmark.js 2>/dev/null
}

psql_value() {
  docker compose exec -T db psql -U postgres -d studenthub -Atc "$1"
}

db_reads() {
  psql_value "SELECT tup_fetched + tup_returned FROM pg_stat_database WHERE datname = 'studenthub'"
}

wait_for_app() {
  for _ in $(seq 1 90); do
    curl -fs -o /dev/null http://localhost:8080/api/courses && return 0
    sleep 2
  done
  echo "app did not start" >&2
  return 1
}

run_mode() {
  local cache="$1"
  SPRING_CACHE_TYPE="$cache" docker compose up -d --wait db redis >/dev/null
  SPRING_CACHE_TYPE="$cache" docker compose up -d --build app >/dev/null 2>&1
  wait_for_app
  docker compose exec -T redis redis-cli flushall >/dev/null
  k6 20s >/dev/null  # warm up the JVM and, when enabled, the cache

  sleep 2  # let pg_stat_database catch up
  local before after result
  before=$(db_reads)
  result=$(k6 "$DURATION")
  sleep 2
  after=$(db_reads)
  jq -c --arg cache "$cache" --argjson reads "$((after - before))" \
    '. + {cache: $cache, db_rows_read: $reads}' <<<"$result"
}

docker compose down -v >/dev/null 2>&1 || true
trap 'docker compose down -v >/dev/null 2>&1 || true' EXIT

baseline=$(run_mode none)
cached=$(run_mode redis)
hits=$(docker compose exec -T redis redis-cli info stats | tr -d '\r' | awk -F: '/keyspace_hits/{h=$2} /keyspace_misses/{m=$2} END{printf "%d hits, %d misses", h, m}')

echo "Seeded resources: $(psql_value 'SELECT COUNT(*) FROM resources')"
echo "Load: $RATE requests/s for $DURATION after a 20s warm-up"
echo "Redis, whole cached run including warm-up: $hits"
echo
echo "| Cache | Requests | Failed | Avg (ms) | p50 (ms) | p95 (ms) | p99 (ms) | DB rows read |"
echo "|---|---:|---:|---:|---:|---:|---:|---:|"
for row in "$baseline" "$cached"; do
  jq -r '"| \(.cache) | \(.requests) | \(.failed * 100 | . * 100 | round / 100)% | \(.avg * 100 | round / 100) | \(.p50 * 100 | round / 100) | \(.p95 * 100 | round / 100) | \(.p99 * 100 | round / 100) | \(.db_rows_read) |"' <<<"$row"
done

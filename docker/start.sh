#!/bin/sh
set -eu

export PORT="${PORT:-8080}"
export SPRING_PORT="${SPRING_PORT:-8081}"
export SOCKETIO_PORT="${SOCKETIO_PORT:-9092}"
export INTERNAL_API_BASE_URL="${INTERNAL_API_BASE_URL:-http://127.0.0.1:${SPRING_PORT}}"

envsubst '${PORT} ${SPRING_PORT} ${SOCKETIO_PORT}' \
  < /app/nginx.conf.template > /tmp/nginx.conf

java -Dserver.port="${SPRING_PORT}" -jar /app/app.jar &
JAVA_PID=$!

cleanup() {
  kill "${JAVA_PID}" 2>/dev/null || true
  wait "${JAVA_PID}" 2>/dev/null || true
}
trap cleanup INT TERM

# Aguarda o Spring responder antes de expor o proxy.
for _ in $(seq 1 60); do
  if wget -q -O /dev/null "http://127.0.0.1:${SPRING_PORT}/api/meta" 2>/dev/null; then
    break
  fi
  sleep 1
done

exec nginx -c /tmp/nginx.conf -g 'daemon off;'

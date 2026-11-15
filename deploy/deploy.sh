#!/usr/bin/env bash
# Runs ON the server (started by the GitHub Actions deploy job). Pulls the new image, starts it, waits for it to
# become healthy, and rolls back to the previous version if it does not.
#
# Inputs (environment): IMAGE, TAG, REGISTRY_USER.  The registry token arrives on stdin.
set -euo pipefail

cd /opt/healverse
: "${IMAGE:?IMAGE is not set}" "${TAG:?TAG is not set}" "${REGISTRY_USER:?REGISTRY_USER is not set}"
[ -f .env ] || { echo "/opt/healverse/.env is missing (see docs/deploy-aws-ec2.md)"; exit 1; }

previous=""
[ -f .last-good-tag ] && previous="$(cat .last-good-tag)"

echo "Logging in to ghcr.io"
docker login ghcr.io -u "$REGISTRY_USER" --password-stdin >/dev/null
trap 'docker logout ghcr.io >/dev/null 2>&1 || true' EXIT

export IMAGE TAG
echo "Pulling ${IMAGE}:${TAG}"
docker compose pull
docker compose up -d --remove-orphans

echo "Waiting for the app to become ready"
healthy=""
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1/actuator/health/readiness >/dev/null 2>&1; then
    healthy=1
    break
  fi
  sleep 3
done

if [ -z "$healthy" ]; then
  echo "New version did not become healthy. Last log lines:"
  docker compose logs --tail 80 api || true
  if [ -n "$previous" ] && [ "$previous" != "$TAG" ]; then
    echo "Rolling back to ${previous}"
    TAG="$previous" docker compose up -d --remove-orphans
  fi
  exit 1
fi

echo "$TAG" > .last-good-tag
docker image prune -f >/dev/null
echo "Deployed ${IMAGE}:${TAG}"

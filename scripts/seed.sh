#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/infra/.env"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC2046
  source "$ENV_FILE"
  set +a
fi
pushd "$ROOT_DIR/svc-java" >/dev/null
./gradlew seedFixtures --args="$@"
popd >/dev/null

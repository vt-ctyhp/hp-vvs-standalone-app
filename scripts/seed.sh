#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
FIXTURES_DIR="$REPO_ROOT/fixtures"
SVC_DIR="$REPO_ROOT/svc-java"

if [ ! -d "$FIXTURES_DIR" ]; then
  echo "Fixtures directory not found: $FIXTURES_DIR" >&2
  exit 1
fi

pushd "$SVC_DIR" >/dev/null
if ./gradlew -q help --task flywayMigrate >/dev/null 2>&1; then
  ./gradlew flywayMigrate >/dev/null
else
  echo "flywayMigrate task not available; skipping schema migration"
fi
./gradlew runSeed -PfixturesDir="$FIXTURES_DIR"
popd >/dev/null

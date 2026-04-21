#!/bin/bash -eu

# Run Maestro smoke tests against a wasabi debug build on an emulator.
#
# Prerequisites:
#   - Maestro CLI installed on the agent
#   - Android emulator running
#   - MAESTRO_WOO_EMAIL, MAESTRO_WOO_PASSWORD, MAESTRO_WOO_STORE_URL secrets set
#
# This is an on-demand step (manual trigger) since it requires a running
# emulator and test store credentials.

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type validation; then
  echo "Skipping Maestro tests — no relevant changes"
  exit 0
fi

echo "--- :hammer: Building and installing wasabi debug APK"
"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"
./gradlew :WooCommerce:installWasabiDebug

echo "--- :maestro: Running Maestro smoke tests"

# Pass every MAESTRO_* env var to maestro as -e NAME=VALUE with the
# prefix stripped. Maestro CLI 2.x does not auto-import MAESTRO_-prefixed
# env vars (the mobile.dev docs predate the rebrand), so we do it here.
#
# We iterate over `env | grep '^MAESTRO_'` to stay compatible with both
# Linux CI agents (bash 4+) and macOS (bash 3.2 default) — avoiding
# both the `compgen -e` single-line quirk and the `${!name}` + `set -u`
# indirect-expansion bug. The local runner script uses the same
# pattern; keep them in sync.
MAESTRO_ARGS=(
  test
  --format junit
  --output .maestro/report.xml
  --include-tags=smoke
)
while IFS='=' read -r name value; do
  [[ -n "$name" ]] && MAESTRO_ARGS+=(-e "${name#MAESTRO_}=${value}")
done < <(env | grep '^MAESTRO_' || true)
MAESTRO_ARGS+=(.maestro/)

set +e
maestro "${MAESTRO_ARGS[@]}"
MAESTRO_EXIT_STATUS=$?
set -e

if [[ "$MAESTRO_EXIT_STATUS" -ne 0 ]]; then
  echo "^^^ +++"
  echo "Maestro smoke tests failed!"
fi

echo "--- 🚦 Collecting results"
mkdir -p WooCommerce/build/buildkite-test-analytics
if [[ -f .maestro/report.xml ]]; then
  cp .maestro/report.xml WooCommerce/build/buildkite-test-analytics/maestro-report.xml
fi

exit $MAESTRO_EXIT_STATUS

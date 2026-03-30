#!/bin/bash -eu

# Run Maestro smoke tests against a wasabi debug build on an emulator.
#
# Prerequisites:
#   - Maestro CLI installed on the agent
#   - Android emulator running
#   - MAESTRO_WOO_* secrets set (see .maestro/env.example for the full list)
#
# This is an on-demand step (manual trigger) since it requires a running
# emulator and test store credentials.

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type validation; then
  echo "Skipping Maestro tests — no relevant changes"
  exit 0
fi

# ─── Validate required secrets ───
echo "--- :key: Validating Maestro credentials"
MISSING_VARS=()
for var in MAESTRO_WOO_EMAIL MAESTRO_WOO_PASSWORD MAESTRO_WOO_STORE_URL; do
  if [[ -z "${!var:-}" ]]; then
    MISSING_VARS+=("$var")
  fi
done

if [[ ${#MISSING_VARS[@]} -gt 0 ]]; then
  echo "^^^ +++"
  echo "ERROR: Missing required Maestro secrets: ${MISSING_VARS[*]}"
  echo "Configure them as Buildkite agent secrets. See .maestro/env.example for details."
  exit 1
fi

# Warn about optional login scenario vars (non-blocking)
for var in MAESTRO_WOO_NOT_WOO_STORE_URL MAESTRO_WOO_NOT_WOO_EMAIL MAESTRO_WOO_NOT_WOO_PASSWORD \
           MAESTRO_WOO_WRONG_ACCOUNT_STORE_URL MAESTRO_WOO_WRONG_ACCOUNT_EMAIL MAESTRO_WOO_WRONG_ACCOUNT_PASSWORD \
           MAESTRO_WOO_JETPACK_STORE_URL MAESTRO_WOO_JETPACK_EMAIL MAESTRO_WOO_JETPACK_PASSWORD; do
  if [[ -z "${!var:-}" ]]; then
    echo "Warning: Optional secret $var is not set. Some login scenario flows may be skipped."
  fi
done

echo "--- :hammer: Building and installing wasabi debug APK"
"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"
./gradlew :WooCommerce:installWasabiDebug

echo "--- :maestro: Running Maestro smoke tests"
set +e
maestro test \
  --format junit \
  --output .maestro/report.xml \
  --include-tags=smoke \
  -e WOO_EMAIL="$MAESTRO_WOO_EMAIL" \
  -e WOO_PASSWORD="$MAESTRO_WOO_PASSWORD" \
  -e WOO_STORE_URL="$MAESTRO_WOO_STORE_URL" \
  -e WOO_NOT_WOO_STORE_URL="${MAESTRO_WOO_NOT_WOO_STORE_URL:-}" \
  -e WOO_NOT_WOO_EMAIL="${MAESTRO_WOO_NOT_WOO_EMAIL:-}" \
  -e WOO_NOT_WOO_PASSWORD="${MAESTRO_WOO_NOT_WOO_PASSWORD:-}" \
  -e WOO_WRONG_ACCOUNT_STORE_URL="${MAESTRO_WOO_WRONG_ACCOUNT_STORE_URL:-}" \
  -e WOO_WRONG_ACCOUNT_EMAIL="${MAESTRO_WOO_WRONG_ACCOUNT_EMAIL:-}" \
  -e WOO_WRONG_ACCOUNT_PASSWORD="${MAESTRO_WOO_WRONG_ACCOUNT_PASSWORD:-}" \
  -e WOO_JETPACK_STORE_URL="${MAESTRO_WOO_JETPACK_STORE_URL:-}" \
  -e WOO_JETPACK_EMAIL="${MAESTRO_WOO_JETPACK_EMAIL:-}" \
  -e WOO_JETPACK_PASSWORD="${MAESTRO_WOO_JETPACK_PASSWORD:-}" \
  -e WOO_CUSTOMER_NAME="${MAESTRO_WOO_CUSTOMER_NAME:-John Doe}" \
  .maestro/
MAESTRO_EXIT_STATUS=$?
set -e

if [[ "$MAESTRO_EXIT_STATUS" -ne 0 ]]; then
  echo "^^^ +++"
  echo "Maestro smoke tests failed!"
fi

echo "--- :checkered_flag: Collecting results"
mkdir -p WooCommerce/build/buildkite-test-analytics
if [[ -f .maestro/report.xml ]]; then
  cp .maestro/report.xml WooCommerce/build/buildkite-test-analytics/maestro-report.xml
fi

exit $MAESTRO_EXIT_STATUS

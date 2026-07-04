#!/bin/bash
set -euo pipefail

# Run the Maestro smoke suite through the repository runner.
#
# Expected CI secrets:
#   MAESTRO_WOO_SHARED_STORE_URL
#   MAESTRO_WOO_SHARED_EMAIL
#   MAESTRO_WOO_SHARED_PASSWORD
#   MAESTRO_WOO_SHARED_CONSUMER_KEY
#   MAESTRO_WOO_SHARED_CONSUMER_SECRET
#
# Optional controls:
#   MAESTRO_STORE=shared|lab
#   MAESTRO_INCLUDE_TAGS=smoke_core,smoke_extended,destructive
#   MAESTRO_REPEAT=3
#   MAESTRO_APK_PATH=/path/to/beta.apk

if .buildkite/commands/should-skip-job.sh --job-type validation; then
  echo "Skipping Maestro tests - no relevant changes"
  exit 0
fi

STORE="${MAESTRO_STORE:-shared}"
INCLUDE_TAGS="${MAESTRO_INCLUDE_TAGS:-smoke_core}"
REPEAT="${MAESTRO_REPEAT:-1}"
OUTPUT_DIR="${MAESTRO_OUTPUT_DIR:-WooCommerce/build/outputs/maestro-smoke}"

if [[ -n "${MAESTRO_APK_PATH:-}" ]]; then
  APK_PATH="$MAESTRO_APK_PATH"
else
  echo "--- Building and installing wasabi debug APK"
  "$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"
  ./gradlew :WooCommerce:installWasabiDebug
  APK_PATH=""
fi

echo "--- Running Maestro smoke tests"
set +e
if [[ -n "$APK_PATH" ]]; then
  .maestro/scripts/run-smoke-tests.sh \
    --store "$STORE" \
    --include-tags "$INCLUDE_TAGS" \
    --repeat "$REPEAT" \
    --output-dir "$OUTPUT_DIR" \
    --no-open \
    --apk "$APK_PATH"
else
  .maestro/scripts/run-smoke-tests.sh \
    --store "$STORE" \
    --include-tags "$INCLUDE_TAGS" \
    --repeat "$REPEAT" \
    --output-dir "$OUTPUT_DIR" \
    --no-open
fi
MAESTRO_EXIT_STATUS=$?
set -e

echo "--- Collecting Maestro results"
mkdir -p WooCommerce/build/buildkite-test-analytics
LATEST_REPORT_DIR="$(find "$OUTPUT_DIR" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)"
if [[ -n "$LATEST_REPORT_DIR" && -f "$LATEST_REPORT_DIR/report.xml" ]]; then
  cp "$LATEST_REPORT_DIR/report.xml" WooCommerce/build/buildkite-test-analytics/maestro-report.xml
fi

if [[ "$MAESTRO_EXIT_STATUS" -ne 0 ]]; then
  echo "^^^ +++"
  echo "Maestro smoke tests were not clean. Check flaky/failure details in artifacts."
fi

exit "$MAESTRO_EXIT_STATUS"

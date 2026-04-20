#!/bin/bash -eu

# Run the Maestro smoke-test suite locally and produce a self-contained
# HTML report plus a screenshots/logs artifact directory for triage.
#
# This is a developer-facing wrapper. CI uses
# `.buildkite/commands/run-maestro-tests.sh`, which emits JUnit XML for
# Buildkite Test Analytics — a different output consumer. We keep them
# separate because `maestro test --format` accepts only one value per run
# (junit OR html), so one script can't serve both needs without paying
# for the test suite twice.
#
# Docs referenced:
#   https://docs.maestro.dev/cli/test-suites-and-reports
#   https://docs.maestro.dev/maestro-cli/maestro-cli-commands-and-options
#
# Usage:
#   .maestro/scripts/run-smoke-tests.sh                       # all flows
#   .maestro/scripts/run-smoke-tests.sh -t login              # by tag
#   .maestro/scripts/run-smoke-tests.sh .maestro/flows/x.yaml # single flow
#   .maestro/scripts/run-smoke-tests.sh --no-open             # don't auto-open report
#
# Env vars required (see .maestro/env.example):
#   MAESTRO_WOO_EMAIL, MAESTRO_WOO_PASSWORD, MAESTRO_WOO_STORE_URL

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FLOWS_DIR="$REPO_ROOT/.maestro/flows"
OUTPUT_ROOT="$REPO_ROOT/.maestro/output"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="$OUTPUT_ROOT/$TIMESTAMP"
REPORT_FILE="$OUTPUT_DIR/report.html"
ARTIFACTS_DIR="$OUTPUT_DIR/artifacts"

TAG=""
TARGET=""
OPEN_REPORT="auto"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--tag)
      TAG="$2"
      shift 2
      ;;
    --no-open)
      OPEN_REPORT="no"
      shift
      ;;
    -h|--help)
      sed -n '3,25p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      if [[ -n "$TARGET" ]]; then
        echo "Unexpected argument: $1" >&2
        exit 2
      fi
      TARGET="$1"
      shift
      ;;
  esac
done

echo "--- :mag: Pre-flight checks"

if ! command -v maestro >/dev/null 2>&1; then
  echo "^^^ +++"
  echo "maestro CLI not found. Install with:"
  echo "  curl -fsSL \"https://get.maestro.mobile.dev\" | bash"
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "^^^ +++"
  echo "adb not found. Ensure Android SDK platform-tools is on PATH."
  exit 1
fi

DEVICE_COUNT=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "^^^ +++"
  echo "No Android device/emulator connected. Start an emulator or plug in a device."
  exit 1
fi

MISSING_ENV=()
for var in MAESTRO_WOO_EMAIL MAESTRO_WOO_PASSWORD MAESTRO_WOO_STORE_URL; do
  if [[ -z "${!var:-}" ]]; then
    MISSING_ENV+=("$var")
  fi
done
if [[ ${#MISSING_ENV[@]} -gt 0 ]]; then
  echo "^^^ +++"
  echo "Missing required env vars: ${MISSING_ENV[*]}"
  echo "See .maestro/env.example for setup."
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "--- :maestro: Running Maestro smoke tests"
echo "Flows:      ${TARGET:-$FLOWS_DIR}"
echo "Tag filter: ${TAG:-<none>}"
echo "Output:     $OUTPUT_DIR"

MAESTRO_ARGS=(
  test
  --format html
  --output "$REPORT_FILE"
  --test-output-dir "$ARTIFACTS_DIR"
)

# Pass every MAESTRO_* env var to maestro as -e NAME=VALUE with the prefix
# stripped. The documented MAESTRO_ auto-import doesn't apply in maestro
# CLI 2.x (the mobile.dev docs predate the rebrand), so ${VAR} inside a
# flow resolves to the literal string "undefined" unless we pass vars
# explicitly. This keeps the shell-env setup (`source .env.local`) working.
while IFS= read -r name; do
  MAESTRO_ARGS+=(-e "${name#MAESTRO_}=${!name}")
done < <(compgen -e | grep '^MAESTRO_' || true)

if [[ -n "$TAG" ]]; then
  MAESTRO_ARGS+=(--include-tags="$TAG")
fi
MAESTRO_ARGS+=("${TARGET:-$FLOWS_DIR}")

set +e
maestro "${MAESTRO_ARGS[@]}"
MAESTRO_EXIT_STATUS=$?
set -e

echo "--- :page_facing_up: Report"
if [[ -f "$REPORT_FILE" ]]; then
  echo "HTML report:   $REPORT_FILE"
  echo "Artifacts dir: $ARTIFACTS_DIR"
else
  echo "No HTML report was produced (maestro may have failed before writing output)."
fi

if [[ "$MAESTRO_EXIT_STATUS" -ne 0 ]]; then
  echo "^^^ +++"
  echo "Maestro smoke tests failed (exit $MAESTRO_EXIT_STATUS)."
fi

# Auto-open the report on macOS for interactive runs, unless suppressed
# or running in CI (BUILDKITE / CI env vars).
if [[ -f "$REPORT_FILE" && "$OPEN_REPORT" == "auto" ]]; then
  if [[ -z "${CI:-}" && -z "${BUILDKITE:-}" && "$(uname)" == "Darwin" ]]; then
    open "$REPORT_FILE" || true
  fi
fi

exit $MAESTRO_EXIT_STATUS

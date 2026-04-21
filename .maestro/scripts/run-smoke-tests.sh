#!/bin/bash -eu

# Run the WooCommerce Android Maestro smoke-test suite in the exact order
# declared in the Smoke Testing P2 post, capture a per-flow screen
# recording, KEEP the recording only when the flow fails, and emit a
# self-contained HTML report at the end.
#
# Artifacts (recordings, logs, report) are written OUTSIDE the repository
# by default — to $HOME/woocommerce-maestro-output/<timestamp>/ — so runs
# don't pollute the working tree. Override with --output-dir <path> or the
# WOO_MAESTRO_OUTPUT_DIR env var.
#
# P2 reference:
#   https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/
# Maestro docs used:
#   https://docs.maestro.dev/cli/test-suites-and-reports
#   https://docs.maestro.dev/maestro-cli/maestro-cli-commands-and-options
#   https://docs.maestro.dev/advanced/configuring-workspaces
#
# Usage:
#   .maestro/scripts/run-smoke-tests.sh                             # run every P2 flow
#   .maestro/scripts/run-smoke-tests.sh --apk path/to/app.apk       # install APK first
#   .maestro/scripts/run-smoke-tests.sh -t login                    # only login flows
#   .maestro/scripts/run-smoke-tests.sh --output-dir /tmp/run1      # custom output dir
#   .maestro/scripts/run-smoke-tests.sh --no-record                 # skip recording
#   .maestro/scripts/run-smoke-tests.sh --no-open                   # don't auto-open report
#   .maestro/scripts/run-smoke-tests.sh .maestro/flows/orders_create.yaml  # single flow
#
# Env vars (see .maestro/env.example):
#   MAESTRO_WOO_EMAIL       — required
#   MAESTRO_WOO_PASSWORD    — required
#   MAESTRO_WOO_STORE_URL   — required (primary Woo store)
#   MAESTRO_WOO_*           — optional, used by specific error-path flows
#
# The script also sources .maestro/.env.local automatically if present.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FLOWS_DIR="$REPO_ROOT/.maestro/flows"
ENV_FILE="$REPO_ROOT/.maestro/.env.local"

DEFAULT_OUTPUT_ROOT="${WOO_MAESTRO_OUTPUT_DIR:-$HOME/woocommerce-maestro-output}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

APK_PATH=""
TAG=""
TARGET=""
OPEN_REPORT="auto"
RECORD="yes"
OUTPUT_ROOT=""

usage() {
  sed -n '3,37p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK_PATH="$2"
      shift 2
      ;;
    --output-dir)
      OUTPUT_ROOT="$2"
      shift 2
      ;;
    -t|--tag)
      TAG="$2"
      shift 2
      ;;
    --no-open)
      OPEN_REPORT="no"
      shift
      ;;
    --no-record)
      RECORD="no"
      shift
      ;;
    -h|--help)
      usage
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

OUTPUT_ROOT="${OUTPUT_ROOT:-$DEFAULT_OUTPUT_ROOT}"
OUTPUT_DIR="$OUTPUT_ROOT/$TIMESTAMP"
RECORDINGS_DIR="$OUTPUT_DIR/recordings"
LOGS_DIR="$OUTPUT_DIR/logs"
REPORT_FILE="$OUTPUT_DIR/report.html"
JUNIT_FILE="$OUTPUT_DIR/report.xml"

# ─────────────────────────────────────────────────────────────────────────
# Execution order — mirrors the Smoke Testing P2 category ordering.
#
# Within the Login group, login_successful is intentionally LAST so the
# app ends the login sequence authenticated with the primary Woo account
# (MAESTRO_WOO_EMAIL / MAESTRO_WOO_STORE_URL). Every subsequent flow then
# reuses that session via subflows/ensure_logged_in.yaml — avoiding the
# WPCom security screens (magic-link, CAPTCHA) that fire when the same
# account re-authenticates too many times in quick succession.
# ─────────────────────────────────────────────────────────────────────────
P2_ORDERED_FLOWS=(
  # ── Login ─────────────────────────────────────────────────────────────
  # P2 lists successful-login first, but we run it LAST so the error-path
  # flows (which clearState) don't wipe the logged-in session downstream.
  login_not_wp_site.yaml
  login_wrong_credentials.yaml
  login_help.yaml
  login_not_woo_store.yaml
  login_wrong_account.yaml
  login_no_jetpack.yaml
  login_google.yaml
  login_successful.yaml          # ← leaves the app logged in

  # ── Dashboard / Stats ────────────────────────────────────────────────
  dashboard_stats.yaml
  dashboard_view_all_analytics.yaml
  dashboard_customize.yaml

  # ── Orders ────────────────────────────────────────────────────────────
  orders_list_and_search.yaml
  orders_create.yaml
  orders_details_and_actions.yaml
  orders_mark_complete.yaml
  orders_cash_payment.yaml
  orders_refund.yaml

  # ── Products ──────────────────────────────────────────────────────────
  products_list_and_sort.yaml
  products_detail.yaml
  products_variations_and_tags.yaml
  products_create.yaml
  products_media_upload.yaml

  # ── Hub Menu ─────────────────────────────────────────────────────────
  hub_menu_settings.yaml
  hub_menu_payments.yaml
  hub_menu_coupons.yaml
  hub_menu_customers_inbox.yaml
  hub_menu_admin_and_store.yaml

  # ── Blaze ─────────────────────────────────────────────────────────────
  blaze_campaign.yaml

  # ── Google for Woo ────────────────────────────────────────────────────
  google_for_woo.yaml

  # ── POS (tablet only) ─────────────────────────────────────────────────
  pos_search_and_coupons.yaml
  pos_cash_payment.yaml
)

# ─────────────────────────────────────────────────────────────────────────
# Pre-flight checks
# ─────────────────────────────────────────────────────────────────────────
echo "--- :mag: Pre-flight checks"

if ! command -v maestro >/dev/null 2>&1; then
  echo "maestro CLI not found. Install: curl -fsSL \"https://get.maestro.mobile.dev\" | bash" >&2
  exit 1
fi
if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Ensure Android SDK platform-tools is on PATH." >&2
  exit 1
fi

# Source .env.local if it exists (git-ignored). Both the required-var
# check below and the MAESTRO_* passthrough pick vars up from the shell
# env once this runs.
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

MISSING_ENV=()
for var in MAESTRO_WOO_EMAIL MAESTRO_WOO_PASSWORD MAESTRO_WOO_STORE_URL; do
  if [[ -z "${!var:-}" ]]; then
    MISSING_ENV+=("$var")
  fi
done
if [[ ${#MISSING_ENV[@]} -gt 0 ]]; then
  echo "Missing required env vars: ${MISSING_ENV[*]}" >&2
  echo "Populate $ENV_FILE (see .maestro/env.example) and re-run." >&2
  exit 1
fi

DEVICE_COUNT=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "No Android device/emulator connected. Start an emulator or plug in a device." >&2
  exit 1
fi
DEVICE_SERIAL=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')

# ─────────────────────────────────────────────────────────────────────────
# Optional APK install
# ─────────────────────────────────────────────────────────────────────────
if [[ -n "$APK_PATH" ]]; then
  if [[ ! -f "$APK_PATH" ]]; then
    echo "APK not found at: $APK_PATH" >&2
    exit 1
  fi
  echo "--- :package: Installing APK: $APK_PATH"
  # -r reinstalls keeping data; -g grants runtime permissions so flows
  # aren't blocked by permission dialogs.
  adb -s "$DEVICE_SERIAL" install -r -g "$APK_PATH"
fi

mkdir -p "$LOGS_DIR"
if [[ "$RECORD" == "yes" ]]; then
  mkdir -p "$RECORDINGS_DIR"
fi

# ─────────────────────────────────────────────────────────────────────────
# Maestro Android driver preflight
#
# Maestro 2.2.0 has a regression where `maestro test` doesn't reliably
# auto-install the `dev.mobile.maestro` driver APK or auto-start its
# instrumentation. When it fails, every flow dies after ~2s with:
#     io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
#     Caused by: Connection refused: localhost/…:7001
# — the gRPC driver port is simply not listening on the device.
#
# We work around it by:
#   1. Extracting the bundled maestro-app.apk + maestro-server.apk from
#      maestro-client.jar (ships with the brew install) if they aren't
#      already installed.
#   2. Starting `am instrument -w …/AndroidJUnitRunner` in the
#      background so the MaestroDriverService binds port 7001 on the
#      device.
#   3. Setting up `adb forward tcp:7001 tcp:7001` and polling it until
#      it accepts a connection (up to 30s).
# An EXIT trap tears the instrumentation + forward back down when the
# script finishes, so we don't leave zombie processes behind between
# runs.
# ─────────────────────────────────────────────────────────────────────────
MAESTRO_DRIVER_PORT=7001
MAESTRO_DRIVER_PID=""

cleanup_maestro_driver() {
  if [[ -n "$MAESTRO_DRIVER_PID" ]]; then
    kill "$MAESTRO_DRIVER_PID" 2>/dev/null || true
  fi
  adb -s "$DEVICE_SERIAL" forward --remove "tcp:$MAESTRO_DRIVER_PORT" 2>/dev/null || true
  adb -s "$DEVICE_SERIAL" shell "am force-stop dev.mobile.maestro; am force-stop dev.mobile.maestro.test" >/dev/null 2>&1 || true
}
trap cleanup_maestro_driver EXIT

# Resolve the real path of the maestro binary (brew wraps it in a
# couple of symlinks). We need this to find the bundled APK jar.
resolve_maestro_bin() {
  local p
  p="$(command -v maestro)"
  while [[ -L "$p" ]]; do
    local target
    target="$(readlink "$p")"
    case "$target" in
      /*) p="$target" ;;
      *)  p="$(cd "$(dirname "$p")" && cd "$(dirname "$target")" && pwd)/$(basename "$target")" ;;
    esac
  done
  printf '%s' "$p"
}

install_maestro_driver_apks() {
  local maestro_bin maestro_root client_jar tmpdir
  maestro_bin="$(resolve_maestro_bin)"
  # Homebrew layout: .../Cellar/maestro/<ver>/libexec/lib/maestro-client.jar
  maestro_root="$(cd "$(dirname "$maestro_bin")/.." && pwd)"
  client_jar=""
  for candidate in \
    "$maestro_root/libexec/lib/maestro-client.jar" \
    "$maestro_root/lib/maestro-client.jar"; do
    if [[ -f "$candidate" ]]; then
      client_jar="$candidate"
      break
    fi
  done
  if [[ -z "$client_jar" ]]; then
    echo "⚠️  Couldn't locate maestro-client.jar near $maestro_bin." >&2
    echo "    Install the driver manually or upgrade Maestro." >&2
    return 1
  fi
  tmpdir="$(mktemp -d)"
  (cd "$tmpdir" && unzip -qo "$client_jar" maestro-app.apk maestro-server.apk)
  adb -s "$DEVICE_SERIAL" install -r -t "$tmpdir/maestro-app.apk"    >/dev/null
  adb -s "$DEVICE_SERIAL" install -r -t "$tmpdir/maestro-server.apk" >/dev/null
  rm -rf "$tmpdir"
}

start_maestro_driver() {
  # `am instrument -w` blocks until the test class exits; back it with
  # nohup + background so the instrumented service stays up for the
  # life of this script.
  adb -s "$DEVICE_SERIAL" shell "am instrument -w dev.mobile.maestro.test/androidx.test.runner.AndroidJUnitRunner" \
    >/dev/null 2>&1 &
  MAESTRO_DRIVER_PID=$!
  adb -s "$DEVICE_SERIAL" forward "tcp:$MAESTRO_DRIVER_PORT" "tcp:$MAESTRO_DRIVER_PORT" >/dev/null

  local waited=0
  while (( waited < 30 )); do
    if nc -z localhost "$MAESTRO_DRIVER_PORT" 2>/dev/null; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  echo "⚠️  Maestro driver didn't start listening on :$MAESTRO_DRIVER_PORT within 30s." >&2
  return 1
}

echo "--- :wrench: Maestro driver preflight"
if ! adb -s "$DEVICE_SERIAL" shell "pm list packages dev.mobile.maestro" 2>/dev/null | grep -q '^package:dev.mobile.maestro$'; then
  echo "Installing driver APKs…"
  install_maestro_driver_apks || true
fi
if ! adb -s "$DEVICE_SERIAL" shell "pm list packages dev.mobile.maestro.test" 2>/dev/null | grep -q '^package:dev.mobile.maestro.test$'; then
  echo "Installing driver test APKs…"
  install_maestro_driver_apks || true
fi
# Always (re)start the instrumentation — the driver service only binds
# port 7001 while its test runner is alive.
echo "Starting instrumentation (dev.mobile.maestro.test)…"
start_maestro_driver || true

# ─────────────────────────────────────────────────────────────────────────
# Build the flow execution list
# ─────────────────────────────────────────────────────────────────────────
ORDERED_FLOWS=()

if [[ -n "$TARGET" ]]; then
  # Single-flow mode — target wins over ordering and tag filter.
  if [[ ! -f "$TARGET" ]]; then
    echo "Target flow not found: $TARGET" >&2
    exit 1
  fi
  ORDERED_FLOWS+=("$(cd "$(dirname "$TARGET")" && pwd)/$(basename "$TARGET")")
else
  for name in "${P2_ORDERED_FLOWS[@]}"; do
    flow_path="$FLOWS_DIR/$name"
    if [[ ! -f "$flow_path" ]]; then
      echo "⚠️  P2-ordered flow missing on disk: $name (skipping)" >&2
      continue
    fi
    if [[ -n "$TAG" ]]; then
      # Match the flow only if $TAG appears under the YAML `tags:` block.
      if ! awk '/^---/{exit} /^tags:/{intags=1; next} intags && /^[[:space:]]*-[[:space:]]*'"$TAG"'[[:space:]]*$/{print "MATCH"; exit}' "$flow_path" | grep -q MATCH; then
        continue
      fi
    fi
    ORDERED_FLOWS+=("$flow_path")
  done
fi

if [[ ${#ORDERED_FLOWS[@]} -eq 0 ]]; then
  echo "No flows matched the current filters." >&2
  exit 1
fi

# Build the -e passthrough args once; reused across every flow invocation.
# Maestro CLI 2.x does NOT auto-import MAESTRO_*-prefixed env vars (the
# mobile.dev docs describing that predate the rebrand), so we strip the
# prefix ourselves and forward each var explicitly.
#
# We read from `env | grep '^MAESTRO_'` instead of `compgen -e` or the
# `${!MAESTRO_*}` indirect-expansion form. Both of those have bash-3.2
# bugs (compgen emits all names on a single space-separated line;
# `${!name}` under `set -u` spuriously reports "unbound variable") and
# macOS ships bash 3.2 by default. `env` is POSIX and emits one
# NAME=VALUE per line on every platform. `IFS='=' read -r n v` splits
# on the first `=` only, so values containing `=` (e.g. URLs with a
# query string) survive intact.
MAESTRO_ENV_ARGS=()
while IFS='=' read -r name value; do
  [[ -n "$name" ]] && MAESTRO_ENV_ARGS+=(-e "${name#MAESTRO_}=${value}")
done < <(env | grep '^MAESTRO_' || true)

# Results accumulator. Each entry: "<status>|<flow_basename>|<duration>|<rec_rel>|<log_rel>|<error_line>"
RESULTS=()
SUITE_START=$(date +%s)
TOTAL=${#ORDERED_FLOWS[@]}
PASSED=0
FAILED=0

# tty-only colours so piped/CI output stays clean.
if [[ -t 1 ]]; then
  C_RESET=$'\033[0m'
  C_BOLD=$'\033[1m'
  C_DIM=$'\033[2m'
  C_GREEN=$'\033[32m'
  C_RED=$'\033[31m'
  C_BLUE=$'\033[34m'
  C_YELLOW=$'\033[33m'
else
  C_RESET=""; C_BOLD=""; C_DIM=""; C_GREEN=""; C_RED=""; C_BLUE=""; C_YELLOW=""
fi

echo "--- :rocket: Running $TOTAL flow(s) in P2 order"
echo "Output:     $OUTPUT_DIR"
echo "Recording:  $RECORD (kept for failures only)"
echo "Tag filter: ${TAG:-<none>}"

# ─────────────────────────────────────────────────────────────────────────
# Recording helpers
# ─────────────────────────────────────────────────────────────────────────
# `adb shell screenrecord` produces a truncated file if the on-device
# process is still running when we pull. pkill (INT) asks it to finalize
# the MP4 moov atom; the fallback handles images without pkill. `sleep 2`
# gives the encoder a moment to flush.
stop_screenrecord() {
  adb -s "$DEVICE_SERIAL" shell "pkill -INT screenrecord 2>/dev/null || { pids=\$(pgrep screenrecord); [ -n \"\$pids\" ] && kill -INT \$pids; } || true" >/dev/null 2>&1 || true
  sleep 2
}

strip_ansi() {
  sed -E 's/\x1b\[[0-9;]*[A-Za-z]//g'
}

# ─────────────────────────────────────────────────────────────────────────
# Run each flow
# ─────────────────────────────────────────────────────────────────────────
INDEX=0
for flow in "${ORDERED_FLOWS[@]}"; do
  INDEX=$((INDEX + 1))
  base="$(basename "$flow" .yaml)"
  log_file="$LOGS_DIR/$base.log"
  remaining=$((TOTAL - INDEX))

  echo ""
  echo "--- ${C_BOLD}[${INDEX}/${TOTAL}] ▶ ${base}${C_RESET}"
  printf '%s\n' "${C_DIM}progress: ${PASSED} passed · ${FAILED} failed · ${remaining} remaining${C_RESET}"

  RECORDER_PID=""
  device_recording=""
  host_recording=""

  if [[ "$RECORD" == "yes" ]]; then
    device_recording="/sdcard/maestro_${base}.mp4"
    host_recording="$RECORDINGS_DIR/$base.mp4"
    # Make sure no previous screenrecord is holding the file descriptor.
    stop_screenrecord
    adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording"
    # 180s is the hard limit per screenrecord invocation. All smoke flows
    # stay well under that.
    adb -s "$DEVICE_SERIAL" shell "screenrecord --time-limit 180 --bit-rate 4000000 $device_recording" \
      >/dev/null 2>&1 &
    RECORDER_PID=$!
    sleep 1  # grace period so the encoder is capturing before frame 1
  fi

  flow_start=$(date +%s)
  set +e
  maestro test "${MAESTRO_ENV_ARGS[@]}" "$flow" 2>&1 | tee "$log_file" | strip_ansi > "$log_file.clean"
  flow_exit=${PIPESTATUS[0]}
  set -e
  mv "$log_file.clean" "$log_file"
  flow_end=$(date +%s)
  duration=$((flow_end - flow_start))

  if [[ "$RECORD" == "yes" ]]; then
    stop_screenrecord
    wait "$RECORDER_PID" 2>/dev/null || true
    if adb -s "$DEVICE_SERIAL" shell "[ -f $device_recording ]"; then
      adb -s "$DEVICE_SERIAL" pull "$device_recording" "$host_recording" >/dev/null 2>&1 || true
      adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording" >/dev/null 2>&1 || true
    fi
  fi

  if [[ "$flow_exit" -eq 0 ]]; then
    # Passed — discard recording and log; only failures are kept.
    [[ -n "$host_recording" ]] && rm -f "$host_recording"
    rm -f "$log_file"
    RESULTS+=("PASS|$base|${duration}|||")
    PASSED=$((PASSED + 1))
    printf '%s\n' "${C_GREEN}${C_BOLD}✅ [${INDEX}/${TOTAL}] ${base} passed${C_RESET}${C_DIM} in ${duration}s · ${PASSED} passed · ${FAILED} failed · ${remaining} remaining${C_RESET}"
  else
    error_line=""
    if [[ -f "$log_file" ]]; then
      error_line=$(grep -E '^\[Failed\]|Assertion|Couldn|Could not find|Timeout' "$log_file" | head -n 1 | tr '|' '/' || true)
    fi
    rec_rel=""
    if [[ -n "$host_recording" && -f "$host_recording" ]]; then
      rec_rel="recordings/$base.mp4"
    fi
    log_rel="logs/$base.log"
    RESULTS+=("FAIL|$base|${duration}|$rec_rel|$log_rel|$error_line")
    FAILED=$((FAILED + 1))
    printf '%s\n' "${C_RED}${C_BOLD}❌ [${INDEX}/${TOTAL}] ${base} failed${C_RESET}${C_DIM} in ${duration}s · ${PASSED} passed · ${FAILED} failed · ${remaining} remaining — log + recording kept${C_RESET}"
  fi
done

SUITE_END=$(date +%s)
SUITE_DURATION=$((SUITE_END - SUITE_START))

# ─────────────────────────────────────────────────────────────────────────
# JUnit XML (machine-readable, stable schema)
# ─────────────────────────────────────────────────────────────────────────
echo ""
echo "--- :page_facing_up: Generating reports"

{
  printf '<?xml version="1.0" encoding="UTF-8"?>\n'
  printf '<testsuite name="woocommerce-android-smoke" tests="%d" failures="%d" time="%d">\n' \
    "${#ORDERED_FLOWS[@]}" "$FAILED" "$SUITE_DURATION"
  for r in "${RESULTS[@]}"; do
    IFS='|' read -r status name duration rec log_rel err <<< "$r"
    printf '  <testcase classname="maestro" name="%s" time="%s">' "$name" "$duration"
    if [[ "$status" == "FAIL" ]]; then
      err_xml=$(printf '%s' "$err" | python3 -c 'import sys,html; print(html.escape(sys.stdin.read()))' 2>/dev/null || printf '%s' "$err")
      printf '<failure message="%s"/>' "$err_xml"
    fi
    printf '</testcase>\n'
  done
  printf '</testsuite>\n'
} > "$JUNIT_FILE"

# ─────────────────────────────────────────────────────────────────────────
# Self-contained HTML report (summary table + embedded failure videos)
# ─────────────────────────────────────────────────────────────────────────
{
  cat <<'HTML_HEAD'
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>WooCommerce Android — Maestro smoke-test report</title>
<style>
  :root { color-scheme: light dark; }
  body { font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 0; padding: 24px; max-width: 960px; margin-left: auto; margin-right: auto; }
  h1 { margin: 0 0 4px; font-size: 22px; }
  h2 { margin: 24px 0 12px; font-size: 18px; }
  .meta { color: #666; margin-bottom: 20px; font-size: 13px; }
  .summary { display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
  .pill { padding: 6px 14px; border-radius: 999px; font-weight: 600; font-size: 13px; }
  .pill.pass { background: #e6f6ec; color: #1f7a3a; }
  .pill.fail { background: #fde8e8; color: #a11212; }
  .pill.total { background: #eef1f5; color: #333; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 32px; }
  th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e1e4e8; font-size: 13px; }
  th { background: #f6f8fa; font-weight: 600; }
  .status-pass { color: #1f7a3a; font-weight: 600; }
  .status-fail { color: #a11212; font-weight: 600; }
  details.failure { border: 1px solid #e1e4e8; border-radius: 6px; padding: 12px 16px; margin-bottom: 14px; }
  details.failure[open] { background: #fafbfc; }
  details.failure summary { cursor: pointer; font-weight: 600; font-size: 15px; }
  details.failure summary::marker { color: #a11212; }
  details.failure .error { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; background: #fde8e8; color: #6a0e0e; padding: 8px 10px; border-radius: 4px; margin: 10px 0; font-size: 12px; white-space: pre-wrap; word-break: break-word; }
  details.failure video { width: 100%; max-width: 480px; display: block; margin: 12px 0; border-radius: 4px; background: #000; }
  /* Tips block needs hard-coded dark text so the content stays readable when
     the OS/browser is in dark mode — without this, `color-scheme: light dark`
     flips the inherited text colour to white, which is invisible on the pale
     yellow background. */
  details.failure .tips { background: #fff8e1; border-left: 4px solid #f9a825; padding: 10px 14px; margin: 10px 0; font-size: 13px; color: #1a1a1a; }
  details.failure .tips strong { color: #000; font-weight: 700; }
  details.failure .tips a { color: #0b5394; }
  details.failure .tips code { background: rgba(0,0,0,0.06); color: #1a1a1a; padding: 1px 4px; border-radius: 3px; }
  details.failure .tips ul { margin: 6px 0 0 18px; padding: 0; }
  details.failure .tips li { margin: 2px 0; }
  details.failure a.log-link { font-size: 12px; color: #0366d6; }
  .no-failures { padding: 16px; background: #e6f6ec; border-radius: 6px; color: #1f7a3a; font-weight: 600; }
  .footer { margin-top: 32px; padding-top: 16px; border-top: 1px solid #e1e4e8; font-size: 12px; color: #888; }
  .footer a { color: #0366d6; }
</style>
</head>
<body>
HTML_HEAD

  echo "<h1>WooCommerce Android — Maestro smoke-test report</h1>"
  echo "<div class=\"meta\">Run $TIMESTAMP &middot; device <code>$DEVICE_SERIAL</code> &middot; ${SUITE_DURATION}s total &middot; output <code>$OUTPUT_DIR</code></div>"
  echo "<div class=\"summary\">"
  echo "  <span class=\"pill total\">${#ORDERED_FLOWS[@]} flows</span>"
  echo "  <span class=\"pill pass\">$PASSED passed</span>"
  echo "  <span class=\"pill fail\">$FAILED failed</span>"
  echo "</div>"

  echo "<h2>Per-flow results (P2 order)</h2>"
  echo "<table><thead><tr><th>#</th><th>Flow</th><th>Status</th><th>Duration</th></tr></thead><tbody>"
  i=0
  for r in "${RESULTS[@]}"; do
    i=$((i+1))
    IFS='|' read -r status name duration rec log_rel err <<< "$r"
    cls=$([[ "$status" == PASS ]] && echo status-pass || echo status-fail)
    echo "<tr><td>$i</td><td><code>$name</code></td><td class=\"$cls\">$status</td><td>${duration}s</td></tr>"
  done
  echo "</tbody></table>"

  if [[ "$FAILED" -eq 0 ]]; then
    echo "<div class=\"no-failures\">✓ All flows passed. No recordings retained.</div>"
  else
    echo "<h2>Failures</h2>"
    for r in "${RESULTS[@]}"; do
      IFS='|' read -r status name duration rec log_rel err <<< "$r"
      [[ "$status" != "FAIL" ]] && continue

      tips=()
      case "$err" in
        *Couldn*keyboard*)
          tips+=("The screen uses a custom IME — remove the <code>hideKeyboard</code> step. The Continue/submit button is usually visible above the keyboard anyway.")
          ;;
        *"is visible"*|*Assertion*visible*)
          tips+=("The app reached a different screen than expected. Watch the recording against the flow's final <code>extendedWaitUntil</code> assertion; a selector may have changed, or the app may have taken an error branch.")
          tips+=("Check <code>.env.local</code> — a wrong store URL or credential is the most common cause of 'assertion false' on login flows.")
          ;;
        *"Could not find"*|*"Element"*"not found"*)
          tips+=("Selector didn't match any node. Remember that <code>testTagsAsResourceId</code> is only enabled under the app's <code>WooTheme</code>; libs/login screens expose no resource IDs, so prefer visible text selectors there.")
          ;;
        *Timeout*|*timeout*)
          tips+=("The flow's timeout fired before the element appeared. Possible causes: slow backend, flaky network, or the app silently bouncing back (watch for a 'Logging in' spinner that returns to the login screen).")
          ;;
      esac
      case "$name" in
        login_google*)
          tips+=("Google login requires the on-device Google identity to be <em>linked</em> on WP.com — a plain WP.com account with the same email is not enough. Symptom: app shows 'Logging in', then returns to the empty email screen.")
          ;;
        login_no_jetpack*)
          tips+=("<code>WOO_JN_SITE_URL</code> must point to a WP site that genuinely lacks Jetpack. <code>*.mystagingwebsite.com</code> staging sites ship with Jetpack pre-connected.")
          ;;
        login_not_woo_store*|login_wrong_account*)
          tips+=("These flows depend on the account/site mapping on WP.com. If the account gained or lost access to the test store, the expected error text changes.")
          ;;
        orders_cash_payment*)
          tips+=("This flow consumes one Pending-payment order per run — if the staging store has none, it fails immediately. Seed a new pending order and retry.")
          ;;
      esac
      if [[ ${#tips[@]} -eq 0 ]]; then
        tips+=("Open the recording to see exactly where the flow stopped. The linked log file contains Maestro's full stderr for the run.")
      fi

      echo "<details class=\"failure\" open>"
      echo "  <summary>$name</summary>"
      if [[ -n "$err" ]]; then
        err_html=$(printf '%s' "$err" | python3 -c 'import sys,html; print(html.escape(sys.stdin.read()))' 2>/dev/null || printf '%s' "$err")
        echo "  <div class=\"error\">$err_html</div>"
      fi
      if [[ -n "$rec" && -f "$OUTPUT_DIR/$rec" ]]; then
        echo "  <video controls preload=\"metadata\" src=\"$rec\"></video>"
      elif [[ "$RECORD" == "no" ]]; then
        echo "  <p><em>Recording disabled (--no-record).</em></p>"
      else
        echo "  <p><em>No recording captured.</em></p>"
      fi
      echo "  <div class=\"tips\"><strong>Likely causes</strong><ul>"
      for t in "${tips[@]}"; do
        echo "    <li>$t</li>"
      done
      echo "  </ul></div>"
      if [[ -n "$log_rel" && -f "$OUTPUT_DIR/$log_rel" ]]; then
        echo "  <a class=\"log-link\" href=\"$log_rel\">View full Maestro log</a>"
      fi
      echo "</details>"
    done
  fi

  cat <<HTML_FOOT
<div class="footer">
  P2 reference: <a href="https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/">Smoke Testing P2</a> ·
  <a href="report.xml">JUnit XML</a> ·
  Artifacts at <code>$OUTPUT_DIR</code>
</div>
</body></html>
HTML_FOOT
} > "$REPORT_FILE"

echo ""
echo "Report: $REPORT_FILE"
echo "JUnit:  $JUNIT_FILE"
if [[ "$FAILED" -eq 0 ]]; then
  printf '%s\n' "${C_GREEN}${C_BOLD}Result: $PASSED/$TOTAL passed, $FAILED failed (${SUITE_DURATION}s)${C_RESET}"
else
  printf '%s\n' "${C_RED}${C_BOLD}Result: $PASSED/$TOTAL passed, $FAILED failed (${SUITE_DURATION}s)${C_RESET}"
fi

if [[ -f "$REPORT_FILE" && "$OPEN_REPORT" == "auto" ]]; then
  if [[ -z "${CI:-}" && -z "${BUILDKITE:-}" && "$(uname)" == "Darwin" ]]; then
    open "$REPORT_FILE" || true
  fi
fi

[[ "$FAILED" -eq 0 ]] && exit 0 || exit 1

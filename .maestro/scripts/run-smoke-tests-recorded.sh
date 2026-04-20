#!/bin/bash -eu

# Run the Maestro smoke-test suite one flow at a time with per-flow screen
# recordings, keeping recordings ONLY for flows that fail, and emitting a
# self-contained HTML report that embeds each failure's recording plus a
# short troubleshooting hint based on the Maestro error message.
#
# This complements `run-smoke-tests.sh` (which runs the whole suite in one
# Maestro invocation and uses Maestro's native HTML report). Recording
# per flow requires launching Maestro once per flow so we can start/stop
# `adb shell screenrecord` around each run.
#
# Usage:
#   .maestro/scripts/run-smoke-tests-recorded.sh                     # run all flows
#   .maestro/scripts/run-smoke-tests-recorded.sh --apk build.apk     # install first
#   .maestro/scripts/run-smoke-tests-recorded.sh --tag login         # filter by tag
#   .maestro/scripts/run-smoke-tests-recorded.sh --no-open           # suppress auto-open
#   .maestro/scripts/run-smoke-tests-recorded.sh .maestro/flows/foo.yaml  # single flow
#
# Execution order: every flow whose filename starts with `login_` runs
# first (in lexical order), then the remaining flows. Login breakage
# invalidates downstream flows, so surfacing those failures first keeps
# the report's top half useful.
#
# Env vars: same as run-smoke-tests.sh; reads .maestro/.env.local if
# present (variables prefixed with MAESTRO_ are forwarded to Maestro
# with the prefix stripped, matching the other wrapper script).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FLOWS_DIR="$REPO_ROOT/.maestro/flows"
ENV_FILE="$REPO_ROOT/.maestro/.env.local"
OUTPUT_ROOT="$REPO_ROOT/.maestro/output"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="$OUTPUT_ROOT/$TIMESTAMP"
RECORDINGS_DIR="$OUTPUT_DIR/recordings"
LOGS_DIR="$OUTPUT_DIR/logs"
REPORT_FILE="$OUTPUT_DIR/report.html"

APK_PATH=""
TAG=""
TARGET=""
OPEN_REPORT="auto"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK_PATH="$2"
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
  echo "maestro CLI not found. Install: curl -fsSL \"https://get.maestro.mobile.dev\" | bash" >&2
  exit 1
fi
if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Ensure Android SDK platform-tools is on PATH." >&2
  exit 1
fi

# Load env file if present, then forward MAESTRO_* to the shell env so both
# the required-var check below and the -e passthrough below pick them up.
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
  echo "No Android device/emulator connected." >&2
  exit 1
fi
DEVICE_SERIAL=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')

if [[ -n "$APK_PATH" ]]; then
  if [[ ! -f "$APK_PATH" ]]; then
    echo "APK not found at: $APK_PATH" >&2
    exit 1
  fi
  echo "--- :package: Installing APK: $APK_PATH"
  adb -s "$DEVICE_SERIAL" install -r -g "$APK_PATH"
fi

mkdir -p "$RECORDINGS_DIR" "$LOGS_DIR"

# Build the flow execution list.
ALL_FLOWS=()
if [[ -n "$TARGET" ]]; then
  # Single-flow mode: skip tag filtering; the caller targeted this one.
  if [[ ! -f "$TARGET" ]]; then
    echo "Target flow not found: $TARGET" >&2
    exit 1
  fi
  ALL_FLOWS+=("$TARGET")
elif [[ -n "$TAG" ]]; then
  # Tag filtering: grep the yaml frontmatter tags block. Simpler than a
  # maestro round-trip and good enough for the curated tag set we ship.
  while IFS= read -r flow; do
    if awk '/^---/{exit} /^tags:/{intags=1; next} intags && /^[[:space:]]*-[[:space:]]*'"$TAG"'[[:space:]]*$/{print "MATCH"; exit}' "$flow" | grep -q MATCH; then
      ALL_FLOWS+=("$flow")
    fi
  done < <(find "$FLOWS_DIR" -maxdepth 1 -name '*.yaml' | sort)
else
  while IFS= read -r flow; do
    ALL_FLOWS+=("$flow")
  done < <(find "$FLOWS_DIR" -maxdepth 1 -name '*.yaml' | sort)
fi

LOGIN_FLOWS=()
OTHER_FLOWS=()
for flow in "${ALL_FLOWS[@]}"; do
  base="$(basename "$flow")"
  if [[ "$base" == login_* ]]; then
    LOGIN_FLOWS+=("$flow")
  else
    OTHER_FLOWS+=("$flow")
  fi
done
# With `set -u` an empty array expansion is a fatal error; the `+x`
# pattern substitutes an empty token when the array has no elements.
ORDERED_FLOWS=("${LOGIN_FLOWS[@]+"${LOGIN_FLOWS[@]}"}" "${OTHER_FLOWS[@]+"${OTHER_FLOWS[@]}"}")

if [[ ${#ORDERED_FLOWS[@]} -eq 0 ]]; then
  echo "No flows matched." >&2
  exit 1
fi

# Build the -e args once; reused for every flow.
MAESTRO_ENV_ARGS=()
while IFS= read -r name; do
  MAESTRO_ENV_ARGS+=(-e "${name#MAESTRO_}=${!name}")
done < <(compgen -e | grep '^MAESTRO_' || true)

# Results accumulator. Each entry: "<status>|<flow_basename>|<duration>|<recording_rel_path>|<error_line>"
RESULTS=()
SUITE_START=$(date +%s)

echo "--- :video_camera: Running ${#ORDERED_FLOWS[@]} flow(s) with per-flow screen recording"
echo "Output: $OUTPUT_DIR"

# Strip ANSI color escapes from captured logs — Maestro emits them and the
# HTML report looks cleaner without them.
strip_ansi() {
  sed -E 's/\x1b\[[0-9;]*[A-Za-z]//g'
}

# Hard-kill any previous screenrecord; `adb shell screenrecord` writes an
# unusable (truncated) file if the pid is still running when we pull.
# Use `pkill -INT` (toybox-supported on modern Android images) and fall
# back to `kill $(pgrep …)` if pkill is missing — avoiding `xargs -r`
# which is GNU-specific.
stop_screenrecord() {
  adb -s "$DEVICE_SERIAL" shell "pkill -INT screenrecord 2>/dev/null || { pids=\$(pgrep screenrecord); [ -n \"\$pids\" ] && kill -INT \$pids; } || true" >/dev/null 2>&1 || true
  # Give the encoder a moment to finalize the MP4 moov atom.
  sleep 2
}

for flow in "${ORDERED_FLOWS[@]}"; do
  base="$(basename "$flow" .yaml)"
  log_file="$LOGS_DIR/$base.log"
  device_recording="/sdcard/maestro_${base}.mp4"
  host_recording="$RECORDINGS_DIR/$base.mp4"

  echo ""
  echo "--- :arrow_forward: $base"

  # Start recording in the background on the device. 180s is the adb
  # screenrecord hard limit per invocation; smoke flows stay well under.
  stop_screenrecord
  adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording"
  adb -s "$DEVICE_SERIAL" shell "screenrecord --time-limit 180 --bit-rate 4000000 $device_recording" \
    >/dev/null 2>&1 &
  RECORDER_PID=$!

  # Short grace period so the recorder is actually capturing before the
  # flow's first frame.
  sleep 1

  flow_start=$(date +%s)
  set +e
  maestro test "${MAESTRO_ENV_ARGS[@]}" "$flow" 2>&1 | tee "$log_file" | strip_ansi > "$log_file.clean"
  flow_exit=${PIPESTATUS[0]}
  set -e
  mv "$log_file.clean" "$log_file"
  flow_end=$(date +%s)
  duration=$((flow_end - flow_start))

  # Stop the recorder and pull the file.
  stop_screenrecord
  # The backgrounded `adb shell screenrecord` exits on its own once the
  # on-device process receives SIGINT. Reap it to keep the subshell clean.
  wait "$RECORDER_PID" 2>/dev/null || true

  if adb -s "$DEVICE_SERIAL" shell "[ -f $device_recording ]"; then
    adb -s "$DEVICE_SERIAL" pull "$device_recording" "$host_recording" >/dev/null 2>&1 || true
    adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording" >/dev/null 2>&1 || true
  fi

  if [[ "$flow_exit" -eq 0 ]]; then
    # Passed — remove the recording and log to keep the output dir tight.
    rm -f "$host_recording" "$log_file"
    RESULTS+=("PASS|$base|${duration}|||")
    echo ":white_check_mark: $base passed (${duration}s)"
  else
    # Failed — keep recording; extract the first error line for the report.
    error_line=""
    if [[ -f "$log_file" ]]; then
      error_line=$(grep -E '^\[Failed\]|Assertion|Couldn|Could not find|Timeout' "$log_file" | head -n 1 | tr '|' '/' || true)
    fi
    rec_rel=""
    if [[ -f "$host_recording" ]]; then
      rec_rel="recordings/$base.mp4"
    fi
    log_rel="logs/$base.log"
    RESULTS+=("FAIL|$base|${duration}|$rec_rel|$log_rel|$error_line")
    echo ":x: $base failed (${duration}s) — recording kept"
  fi
done

SUITE_END=$(date +%s)
SUITE_DURATION=$((SUITE_END - SUITE_START))
PASSED=0
FAILED=0
for r in "${RESULTS[@]}"; do
  [[ "$r" == PASS* ]] && PASSED=$((PASSED+1)) || FAILED=$((FAILED+1))
done

echo ""
echo "--- :page_facing_up: Generating report"

# HTML report. Self-contained (inline CSS, relative video paths) so the
# whole $OUTPUT_DIR is shareable as a zip.
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
  .meta { color: #666; margin-bottom: 20px; font-size: 13px; }
  .summary { display: flex; gap: 12px; margin-bottom: 24px; }
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
  details.failure .tips { background: #fff8e1; border-left: 4px solid #f9a825; padding: 8px 12px; margin: 10px 0; font-size: 13px; }
  details.failure .tips ul { margin: 4px 0 0 18px; padding: 0; }
  details.failure a.log-link { font-size: 12px; color: #0366d6; }
  .no-failures { padding: 16px; background: #e6f6ec; border-radius: 6px; color: #1f7a3a; font-weight: 600; }
</style>
</head>
<body>
HTML_HEAD

  echo "<h1>Maestro smoke-test report</h1>"
  echo "<div class=\"meta\">Run $TIMESTAMP &middot; device $DEVICE_SERIAL &middot; ${SUITE_DURATION}s total</div>"
  echo "<div class=\"summary\">"
  echo "  <span class=\"pill total\">${#ORDERED_FLOWS[@]} flows</span>"
  echo "  <span class=\"pill pass\">$PASSED passed</span>"
  echo "  <span class=\"pill fail\">$FAILED failed</span>"
  echo "</div>"

  # Per-flow summary table (gives the reader one place to scan overall state).
  echo "<table><thead><tr><th>Flow</th><th>Status</th><th>Duration</th></tr></thead><tbody>"
  for r in "${RESULTS[@]}"; do
    IFS='|' read -r status name duration rec log_rel err <<< "$r"
    cls=$([[ "$status" == PASS ]] && echo status-pass || echo status-fail)
    echo "<tr><td>$name</td><td class=\"$cls\">$status</td><td>${duration}s</td></tr>"
  done
  echo "</tbody></table>"

  if [[ "$FAILED" -eq 0 ]]; then
    echo "<div class=\"no-failures\">All flows passed. No recordings retained.</div>"
  else
    echo "<h2>Failures</h2>"
    for r in "${RESULTS[@]}"; do
      IFS='|' read -r status name duration rec log_rel err <<< "$r"
      [[ "$status" != "FAIL" ]] && continue

      # Troubleshooting tips inferred from the error line. These are
      # heuristics — intentionally broad, because the error wording from
      # Maestro is consistent enough to map to a short list of causes.
      tips=()
      case "$err" in
        *Couldn*keyboard*)
          tips+=("The screen uses a custom IME — remove the <code>hideKeyboard</code> step. The Continue/submit button is usually visible above the keyboard anyway.")
          ;;
        *"is visible"*|*Assertion*visible*)
          tips+=("The app reached a different screen than expected. Open the recording and compare against the flow's final <code>extendedWaitUntil</code> assertion; the selector may have changed, or the app may have taken an error branch.")
          tips+=("Check <code>.env.local</code> — a wrong store URL or credential is the most common cause of 'assertion false' on login flows.")
          ;;
        *"Could not find"*|*"Element"*"not found"*)
          tips+=("Selector didn't match any node. If it's a Compose-rendered screen, remember that <code>testTagsAsResourceId</code> is only enabled under the app's <code>WooTheme</code>; libs/login screens expose no resource IDs, so prefer visible text selectors.")
          ;;
        *Timeout*|*timeout*)
          tips+=("The flow's timeout fired before the element appeared. Possible causes: slow backend, flaky network, or the app silently bouncing back (watch the recording for a 'Logging in' spinner that returns to the login screen).")
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
          tips+=("These flows depend on the account/site mapping configured on WP.com. If the account gained or lost access to the test store, the expected error text changes.")
          ;;
      esac
      if [[ ${#tips[@]} -eq 0 ]]; then
        tips+=("Open the recording to see exactly where the flow stopped. The log file (linked below) contains Maestro's full stderr for the run.")
      fi

      echo "<details class=\"failure\" open>"
      echo "  <summary>$name</summary>"
      if [[ -n "$err" ]]; then
        err_html=$(printf '%s' "$err" | python3 -c 'import sys,html; print(html.escape(sys.stdin.read()))')
        echo "  <div class=\"error\">$err_html</div>"
      fi
      if [[ -n "$rec" && -f "$OUTPUT_DIR/$rec" ]]; then
        echo "  <video controls preload=\"metadata\" src=\"$rec\"></video>"
      else
        echo "  <p><em>No recording captured for this flow.</em></p>"
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

  echo "</body></html>"
} > "$REPORT_FILE"

echo ""
echo "Report: $REPORT_FILE"
echo "Result: $PASSED passed, $FAILED failed (${SUITE_DURATION}s)"

if [[ -f "$REPORT_FILE" && "$OPEN_REPORT" == "auto" ]]; then
  if [[ -z "${CI:-}" && -z "${BUILDKITE:-}" && "$(uname)" == "Darwin" ]]; then
    open "$REPORT_FILE" || true
  fi
fi

[[ "$FAILED" -eq 0 ]] && exit 0 || exit 1

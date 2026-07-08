#!/bin/bash
set -euo pipefail

# WooCommerce Android Maestro smoke-test runner.
#
# Defaults match the v2 smoke-test plan:
#   - lab store by default
#   - smoke_core only by default
#   - flaky_quarantine excluded unless explicitly requested
#   - no REST fixture seed unless --seed is passed
#   - animation settings captured and restored
#   - one retry per failed flow, recorded as flaky

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FLOWS_DIR="$REPO_ROOT/.maestro/flows"
ENV_FILE="$REPO_ROOT/.maestro/.env.local"
STRINGS_ENV_FILE="$REPO_ROOT/.maestro/strings.env"
SEED_SCRIPT="$REPO_ROOT/.maestro/scripts/seed-fixtures.py"

RUN_STAMP="$(date +%Y%m%d%H%M%S)"
RUN_HASH="$(printf '%s-%s-%s' "$RUN_STAMP" "$$" "${RANDOM:-0}" | cksum | awk '{print $1}')"
SUITE_RUN_ID="SUITE-${RUN_STAMP}-${RUN_HASH}"
TIMESTAMP="$RUN_STAMP"

DEFAULT_OUTPUT_ROOT="${WOO_MAESTRO_OUTPUT_DIR:-$HOME/woocommerce-maestro-output}"
STORE="lab"
APK_PATH=""
DEVICE_SELECTOR=""
TARGET=""
OPEN_REPORT="auto"
RECORD="yes"
SEED="no"
CLEANUP="yes"
SWEEP_DRY_RUN="no"
OUTPUT_ROOT=""
REPEAT=1
INCLUDE_TAGS=("smoke_core")
EXCLUDE_TAGS=("flaky_quarantine")
INCLUDE_TAGS_EXPLICIT="no"
EXCLUDE_TAGS_EXPLICIT="no"

usage() {
  cat <<'USAGE'
WooCommerce Android Maestro smoke-test runner.

Defaults:
  - lab store
  - smoke_core only
  - flaky_quarantine excluded unless explicitly requested
  - no REST fixture seed unless --seed is passed
  - animation settings captured and restored
  - one retry per failed flow, recorded as flaky

Usage:
  .maestro/scripts/run-smoke-tests.sh
  .maestro/scripts/run-smoke-tests.sh --include-tags smoke_extended --store lab
  .maestro/scripts/run-smoke-tests.sh --store shared --include-tags smoke_core
  .maestro/scripts/run-smoke-tests.sh --device emulator-5554 --apk path/to/app.apk
  .maestro/scripts/run-smoke-tests.sh --repeat 5 --store shared --include-tags smoke_core,smoke_extended
  .maestro/scripts/run-smoke-tests.sh .maestro/flows/orders_list_and_search.yaml

Options:
  --store lab|shared          Select fixture/credential namespace. Default: lab.
  --device serial|avd-name    Device serial or emulator AVD name.
  --apk path                  Install APK before running.
  --repeat N                  Run the selected flow set N times.
  -t, --tag tag               Alias for --include-tags.
  --include-tags a,b          Include flows with any listed tag. Default: smoke_core.
  --exclude-tags a,b          Exclude flows with any listed tag. Default: flaky_quarantine.
  --seed                      Run REST fixture seed/cleanup. Requires Woo REST API credentials.
  --no-seed                   Skip REST fixture seed/cleanup.
  --no-cleanup                Leave seeded manifest entities behind.
  --sweep-dry-run             Log stale-orphan sweep candidates without deleting.
  --no-record                 Disable failure videos.
  --no-open                   Do not open the HTML report on macOS.
  --output-dir path           Override output root.
USAGE
}

add_csv_tags() {
  local target_name="$1"
  local csv="$2"
  local old_ifs="$IFS"
  IFS=","
  read -r -a parts <<< "$csv"
  IFS="$old_ifs"
  local part
  for part in "${parts[@]}"; do
    part="$(printf '%s' "$part" | xargs)"
    [[ -z "$part" ]] && continue
    eval "$target_name+=(\"\$part\")"
  done
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK_PATH="${2:?--apk requires a path}"
      shift 2
      ;;
    --device)
      DEVICE_SELECTOR="${2:?--device requires a serial or AVD name}"
      shift 2
      ;;
    --store)
      STORE="${2:?--store requires lab or shared}"
      shift 2
      ;;
    --repeat)
      REPEAT="${2:?--repeat requires a number}"
      shift 2
      ;;
    --output-dir)
      OUTPUT_ROOT="${2:?--output-dir requires a path}"
      shift 2
      ;;
    -t|--tag|--include-tags)
      if [[ "$INCLUDE_TAGS_EXPLICIT" == "no" ]]; then
        INCLUDE_TAGS=()
        INCLUDE_TAGS_EXPLICIT="yes"
      fi
      add_csv_tags INCLUDE_TAGS "${2:?--include-tags requires a tag}"
      shift 2
      ;;
    --exclude-tags)
      if [[ "$EXCLUDE_TAGS_EXPLICIT" == "no" ]]; then
        EXCLUDE_TAGS=()
        EXCLUDE_TAGS_EXPLICIT="yes"
      fi
      add_csv_tags EXCLUDE_TAGS "${2:?--exclude-tags requires a tag}"
      shift 2
      ;;
    --no-seed)
      SEED="no"
      shift
      ;;
    --seed)
      SEED="yes"
      shift
      ;;
    --no-cleanup)
      CLEANUP="no"
      shift
      ;;
    --sweep-dry-run)
      SWEEP_DRY_RUN="yes"
      shift
      ;;
    --no-record)
      RECORD="no"
      shift
      ;;
    --no-open)
      OPEN_REPORT="no"
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

if [[ "$STORE" != "lab" && "$STORE" != "shared" ]]; then
  echo "--store must be lab or shared" >&2
  exit 2
fi
if ! [[ "$REPEAT" =~ ^[0-9]+$ ]] || [[ "$REPEAT" -lt 1 ]]; then
  echo "--repeat must be a positive integer" >&2
  exit 2
fi
if [[ "$INCLUDE_TAGS_EXPLICIT" == "yes" ]]; then
  for tag in "${INCLUDE_TAGS[@]}"; do
    if [[ "$tag" == "flaky_quarantine" && "$EXCLUDE_TAGS_EXPLICIT" == "no" ]]; then
      EXCLUDE_TAGS=()
    fi
  done
fi

OUTPUT_ROOT="${OUTPUT_ROOT:-$DEFAULT_OUTPUT_ROOT}"
OUTPUT_DIR="$OUTPUT_ROOT/$TIMESTAMP"
RECORDINGS_DIR="$OUTPUT_DIR/recordings"
SCREENSHOTS_DIR="$OUTPUT_DIR/screenshots"
LOGS_DIR="$OUTPUT_DIR/logs"
TMP_DIR="$OUTPUT_DIR/tmp"
REPORT_FILE="$OUTPUT_DIR/report.html"
JUNIT_FILE="$OUTPUT_DIR/report.xml"
MANIFEST_FILE="$TMP_DIR/run-manifest.json"
RUN_ENV_FILE="$TMP_DIR/run-env.sh"
SWEEP_REPORT="$TMP_DIR/orphan-sweep.json"

export MAESTRO_SUITE_RUN_ID="$SUITE_RUN_ID"
mkdir -p "$LOGS_DIR" "$TMP_DIR" "$SCREENSHOTS_DIR"

P2_ORDERED_FLOWS=(
  login_not_wp_site.yaml
  login_wrong_credentials.yaml
  login_help.yaml
  login_not_woo_store.yaml
  login_wrong_account.yaml
  login_no_jetpack.yaml
  login_google.yaml
  login_successful.yaml
  dashboard_stats.yaml
  dashboard_view_all_analytics.yaml
  dashboard_customize.yaml
  orders_list_and_search.yaml
  orders_create.yaml
  orders_details_and_actions.yaml
  orders_mark_complete.yaml
  orders_cash_payment.yaml
  orders_refund.yaml
  products_list_and_sort.yaml
  products_detail.yaml
  products_variations_and_tags.yaml
  products_create.yaml
  products_media_upload.yaml
  hub_menu_settings.yaml
  hub_menu_payments.yaml
  hub_menu_coupons.yaml
  hub_menu_customers_inbox.yaml
  hub_menu_admin_and_store.yaml
  blaze_campaign.yaml
  google_for_woo.yaml
  pos_search_and_coupons.yaml
  pos_cash_payment.yaml
)

echo "--- Pre-flight checks"
if ! command -v maestro >/dev/null 2>&1; then
  echo "maestro CLI not found. Install: curl -fsSL \"https://get.maestro.mobile.dev\" | bash" >&2
  exit 1
fi
if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Ensure Android SDK platform-tools is on PATH." >&2
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi
if [[ -f "$STRINGS_ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$STRINGS_ENV_FILE"
  set +a
fi

map_store_env() {
  local target="$1"
  shift
  local source value
  for source in "$@"; do
    value="${!source:-}"
    if [[ -n "$value" ]]; then
      export "$target=$value"
      return
    fi
  done
}

alias_env() {
  local target="$1"
  local source="$2"
  if [[ -z "${!target:-}" && -n "${!source:-}" ]]; then
    export "$target=${!source}"
  fi
}

select_store_env() {
  local upper
  upper="$(printf '%s' "$STORE" | tr '[:lower:]' '[:upper:]')"
  map_store_env \
    MAESTRO_WOO_JETPACK_STORE_URL \
    "MAESTRO_WOO_${upper}_JETPACK_STORE_URL" \
    "MAESTRO_WOO_${upper}_STORE_URL"
  map_store_env MAESTRO_WOO_WPCOM_EMAIL "MAESTRO_WOO_${upper}_WPCOM_EMAIL" "MAESTRO_WOO_${upper}_EMAIL"
  map_store_env \
    MAESTRO_WOO_WPCOM_PASSWORD \
    "MAESTRO_WOO_${upper}_WPCOM_PASSWORD" \
    "MAESTRO_WOO_${upper}_PASSWORD"

  alias_env MAESTRO_WOO_STORE_URL MAESTRO_WOO_JETPACK_STORE_URL
  alias_env MAESTRO_WOO_EMAIL MAESTRO_WOO_WPCOM_EMAIL
  alias_env MAESTRO_WOO_PASSWORD MAESTRO_WOO_WPCOM_PASSWORD

  map_store_env MAESTRO_WOO_CONSUMER_KEY "MAESTRO_WOO_${upper}_CONSUMER_KEY"
  map_store_env MAESTRO_WOO_CONSUMER_SECRET "MAESTRO_WOO_${upper}_CONSUMER_SECRET"

  alias_env MAESTRO_WOO_NO_JETPACK_SITE_URL MAESTRO_WOO_JN_SITE_URL
  alias_env MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_USERNAME MAESTRO_WOO_JN_USERNAME
  alias_env MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_PASSWORD MAESTRO_WOO_JN_PASSWORD
}
select_store_env

flow_tags() {
  awk '
    /^---/ { exit }
    /^tags:/ { in_tags = 1; next }
    in_tags && /^[[:space:]]*-[[:space:]]*/ {
      gsub(/^[[:space:]]*-[[:space:]]*/, "", $0)
      gsub(/[[:space:]]+$/, "", $0)
      print $0
      next
    }
    in_tags && /^[^[:space:]]/ { in_tags = 0 }
  ' "$1"
}

flow_has_any_tag() {
  local flow="$1"
  shift
  [[ $# -eq 0 ]] && return 1
  local tags tag wanted
  tags="$(flow_tags "$flow")"
  for wanted in "$@"; do
    while IFS= read -r tag; do
      [[ "$tag" == "$wanted" ]] && return 0
    done <<< "$tags"
  done
  return 1
}

url_host() {
  local value="${1:-}"
  value="${value#http://}"
  value="${value#https://}"
  value="${value%%/*}"
  value="${value%%:*}"
  printf '%s' "$value" | tr '[:upper:]' '[:lower:]'
}

flow_uses_wpcom_credentials() {
  local flow name
  local wpcom_ref_pattern
  wpcom_ref_pattern='\$\{WOO_(JETPACK_STORE_URL|WPCOM_EMAIL|WPCOM_PASSWORD)\}'
  for flow in "${ORDERED_FLOWS[@]}"; do
    name="$(basename "$flow")"
    case "$name" in
      login_help.yaml|login_no_jetpack.yaml|login_not_wp_site.yaml)
        continue
        ;;
    esac
    if grep -Eq "$wpcom_ref_pattern|subflows/(ensure_logged_in|login)\\.yaml" "$flow"; then
      return 0
    fi
  done
  return 1
}

validate_login_store_env() {
  flow_uses_wpcom_credentials || return 0

  local selected_host no_jetpack_host upper
  upper="$(printf '%s' "$STORE" | tr '[:lower:]' '[:upper:]')"
  selected_host="$(url_host "${MAESTRO_WOO_JETPACK_STORE_URL:-}")"
  no_jetpack_host="$(url_host "${MAESTRO_WOO_NO_JETPACK_SITE_URL:-}")"

  if [[ -n "$selected_host" && -n "$no_jetpack_host" && "$selected_host" == "$no_jetpack_host" ]]; then
    cat >&2 <<EOF
Setup error: selected --store $STORE points the Jetpack store at the same host as the no-Jetpack site.

The selected flow set includes WP.com/Jetpack login flows. Set the $STORE store
block to a Jetpack-connected WooCommerce store with MAESTRO_WOO_${upper}_JETPACK_STORE_URL,
MAESTRO_WOO_${upper}_WPCOM_EMAIL, and MAESTRO_WOO_${upper}_WPCOM_PASSWORD.
Keep MAESTRO_WOO_NO_JETPACK_* only for login_no_jetpack.yaml.

To run only the no-Jetpack flow:
  .maestro/scripts/run-smoke-tests.sh --no-seed .maestro/flows/login_no_jetpack.yaml
EOF
    exit 1
  fi
}

ORDERED_FLOWS=()
if [[ -n "$TARGET" ]]; then
  if [[ ! -f "$TARGET" ]]; then
    echo "Target flow not found: $TARGET" >&2
    exit 1
  fi
  ORDERED_FLOWS+=("$(cd "$(dirname "$TARGET")" && pwd)/$(basename "$TARGET")")
else
  for name in "${P2_ORDERED_FLOWS[@]}"; do
    flow_path="$FLOWS_DIR/$name"
    [[ -f "$flow_path" ]] || continue
    if [[ ${#INCLUDE_TAGS[@]} -gt 0 ]] && ! flow_has_any_tag "$flow_path" "${INCLUDE_TAGS[@]}"; then
      continue
    fi
    if [[ ${#EXCLUDE_TAGS[@]} -gt 0 ]] && flow_has_any_tag "$flow_path" "${EXCLUDE_TAGS[@]}"; then
      continue
    fi
    ORDERED_FLOWS+=("$flow_path")
  done
fi
if [[ ${#ORDERED_FLOWS[@]} -eq 0 ]]; then
  echo "No flows matched the current filters." >&2
  exit 1
fi
validate_login_store_env

SUITE_HAS_DESTRUCTIVE="no"
for flow in "${ORDERED_FLOWS[@]}"; do
  if flow_has_any_tag "$flow" destructive; then
    SUITE_HAS_DESTRUCTIVE="yes"
  fi
done
if [[ "$STORE" == "shared" && "$SUITE_HAS_DESTRUCTIVE" == "yes" && -z "${CI:-}" && -z "${BUILDKITE:-}" ]]; then
  echo "Refusing to run destructive flows against the shared store outside CI." >&2
  echo "Use --store lab for destructive iteration, or remove destructive flows from the selection." >&2
  exit 1
fi

validate_referenced_env() {
  local missing=()
  local flow ref var
  for flow in "${ORDERED_FLOWS[@]}"; do
    while IFS= read -r ref; do
      [[ -z "$ref" ]] && continue
      var="MAESTRO_${ref}"
      if [[ -z "${!var:-}" ]]; then
        missing+=("$var")
      fi
    done < <(grep -Eoh '\$\{WOO_[A-Z0-9_]+\}' "$flow" | sed 's/[${}]//g' | sort -u)
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    printf '%s\n' "${missing[@]}" | sort -u | sed 's/^/Missing required env var: /' >&2
    echo "Populate $ENV_FILE from .maestro/env.example. Values are intentionally not echoed." >&2
    exit 1
  fi
}
validate_referenced_env

DEVICE_SERIALS=()
while read -r serial state _rest; do
  if [[ "${state:-}" == "device" ]]; then
    DEVICE_SERIALS+=("$serial")
  fi
done < <(adb devices)

resolve_device() {
  local selector="$1"
  local serial avd
  if [[ ${#DEVICE_SERIALS[@]} -eq 0 ]]; then
    echo "No Android device/emulator connected. Start one and retry." >&2
    exit 1
  fi
  if [[ -n "$selector" ]]; then
    for serial in "${DEVICE_SERIALS[@]}"; do
      if [[ "$serial" == "$selector" ]]; then
        printf '%s' "$serial"
        return
      fi
      avd="$(adb -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1 || true)"
      if [[ "$avd" == "$selector" ]]; then
        printf '%s' "$serial"
        return
      fi
    done
    echo "No attached device matched --device $selector" >&2
    exit 1
  fi
  if [[ ${#DEVICE_SERIALS[@]} -eq 1 ]]; then
    printf '%s' "${DEVICE_SERIALS[0]}"
    return
  fi
  echo "Multiple Android devices are connected:" >&2
  local index=1
  for serial in "${DEVICE_SERIALS[@]}"; do
    avd="$(adb -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1 || true)"
    echo "  $index) $serial ${avd:+($avd)}" >&2
    index=$((index + 1))
  done
  if [[ -n "${CI:-}" || -n "${BUILDKITE:-}" || ! -t 0 ]]; then
    echo "Pass --device when multiple devices are connected." >&2
    exit 1
  fi
  read -r -p "Select device number: " selected
  if ! [[ "$selected" =~ ^[0-9]+$ ]] || [[ "$selected" -lt 1 || "$selected" -gt ${#DEVICE_SERIALS[@]} ]]; then
    echo "Invalid device selection." >&2
    exit 1
  fi
  printf '%s' "${DEVICE_SERIALS[$((selected - 1))]}"
}

DEVICE_SERIAL="$(resolve_device "$DEVICE_SELECTOR")"
MAESTRO_DEVICE_ARGS=(--device "$DEVICE_SERIAL")
echo "Device: $DEVICE_SERIAL"

ANIMATION_KEYS=(window_animation_scale transition_animation_scale animator_duration_scale)
ORIGINAL_ANIMATION_VALUES=()
SETTINGS_CAPTURED="no"
LOCK_ACQUIRED="no"
RECORDER_PID=""

capture_animation_settings() {
  local key value
  ORIGINAL_ANIMATION_VALUES=()
  for key in "${ANIMATION_KEYS[@]}"; do
    value="$(adb -s "$DEVICE_SERIAL" shell settings get global "$key" 2>/dev/null | tr -d '\r' || true)"
    ORIGINAL_ANIMATION_VALUES+=("${value:-1}")
    adb -s "$DEVICE_SERIAL" shell settings put global "$key" 0 >/dev/null
  done
  SETTINGS_CAPTURED="yes"
}

restore_animation_settings() {
  [[ "$SETTINGS_CAPTURED" == "yes" ]] || return 0
  local index key value
  index=0
  for key in "${ANIMATION_KEYS[@]}"; do
    value="${ORIGINAL_ANIMATION_VALUES[$index]:-1}"
    adb -s "$DEVICE_SERIAL" shell settings put global "$key" "$value" >/dev/null 2>&1 || true
    index=$((index + 1))
  done
}

stop_screenrecord() {
  adb -s "$DEVICE_SERIAL" shell "pkill -INT screenrecord 2>/dev/null || true" >/dev/null 2>&1 || true
  sleep 1
}

collapse_system_ui() {
  adb -s "$DEVICE_SERIAL" shell cmd statusbar collapse >/dev/null 2>&1 || true
}

cleanup_on_exit() {
  local exit_code=$?
  if [[ -n "$RECORDER_PID" ]]; then
    stop_screenrecord
    wait "$RECORDER_PID" 2>/dev/null || true
  fi
  if [[ "$CLEANUP" == "yes" && "$SEED" == "yes" && -f "$MANIFEST_FILE" ]]; then
    "$SEED_SCRIPT" cleanup --manifest "$MANIFEST_FILE" --store "$STORE" || true
  fi
  if [[ "$LOCK_ACQUIRED" == "yes" && -f "$MANIFEST_FILE" ]]; then
    "$SEED_SCRIPT" unlock --manifest "$MANIFEST_FILE" --store shared || true
  fi
  restore_animation_settings
  exit "$exit_code"
}
trap cleanup_on_exit EXIT INT TERM

capture_animation_settings

if [[ -n "$APK_PATH" ]]; then
  if [[ ! -f "$APK_PATH" ]]; then
    echo "APK not found at: $APK_PATH" >&2
    exit 1
  fi
  echo "--- Installing APK"
  adb -s "$DEVICE_SERIAL" install -r -g "$APK_PATH"
fi

if [[ "$SEED" == "yes" ]]; then
  echo "--- Stale automation orphan sweep"
  sweep_args=(sweep --store "$STORE" --report "$SWEEP_REPORT")
  if [[ "$SWEEP_DRY_RUN" == "yes" ]]; then
    sweep_args+=(--dry-run)
  fi
  "$SEED_SCRIPT" "${sweep_args[@]}"

  if [[ "$STORE" == "shared" && "$SUITE_HAS_DESTRUCTIVE" == "yes" ]]; then
    echo "--- Acquiring shared-store destructive lock"
    "$SEED_SCRIPT" lock --store shared --run-id "$SUITE_RUN_ID" --manifest "$MANIFEST_FILE" >/dev/null
    LOCK_ACQUIRED="yes"
  fi

  echo "--- Seeding deterministic fixtures"
  "$SEED_SCRIPT" seed --store "$STORE" --run-id "$SUITE_RUN_ID" --manifest "$MANIFEST_FILE" --env-file "$RUN_ENV_FILE"
  # shellcheck disable=SC1090
  source "$RUN_ENV_FILE"
fi

MAESTRO_ENV_ARGS=()
while IFS='=' read -r name value; do
  [[ -n "$name" ]] && MAESTRO_ENV_ARGS+=(-e "${name#MAESTRO_}=${value}")
done < <(env | grep '^MAESTRO_' || true)

# Forward only the STRING_* variables the selected flows reference; forwarding
# all generated strings would risk exceeding ARG_MAX.
while IFS= read -r ref; do
  [[ -z "$ref" ]] && continue
  if [[ -z "${!ref:-}" ]]; then
    echo "Missing generated string variable: $ref (regenerate .maestro/strings.env)" >&2
    exit 1
  fi
  MAESTRO_ENV_ARGS+=(-e "${ref}=${!ref}")
done < <(grep -Eoh '\$\{STRING_[A-Z0-9_]+\}' "${ORDERED_FLOWS[@]}" 2>/dev/null | sed 's/[${}]//g' | sort -u || true)

scrub_log() {
  local file="$1"
  python3 - "$file" <<'PY'
import os
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text(errors="replace")
for key, value in os.environ.items():
    if key.startswith("MAESTRO_WOO_") and value:
        text = text.replace(value, "[REDACTED]")
path.write_text(text)
PY
}

xml_escape() {
  python3 -c 'import html,sys; print(html.escape(sys.stdin.read()), end="")'
}

RESULTS=()
PASSED=0
FLAKY=0
FAILED=0
TOTAL_RUNS=$((${#ORDERED_FLOWS[@]} * REPEAT))
SUITE_START=$(date +%s)

echo "--- Running Maestro flows"
echo "Run ID:       $SUITE_RUN_ID"
echo "Store:        $STORE"
echo "Output:       $OUTPUT_DIR"
echo "Repeat:       $REPEAT"
echo "Include tags: ${INCLUDE_TAGS[*]:-<none>}"
echo "Exclude tags: ${EXCLUDE_TAGS[*]:-<none>}"
echo "Recording:    $RECORD (shared-store credential paths use screenshots only)"

run_one_attempt() {
  local flow="$1"
  local base="$2"
  local attempt="$3"
  local repeat_index="$4"
  local log_file="$LOGS_DIR/${repeat_index}_${base}_attempt${attempt}.log"
  local attempt_screenshots_dir="$SCREENSHOTS_DIR/${repeat_index}_${base}_attempt${attempt}"
  local attempt_screenshots_rel="screenshots/${repeat_index}_${base}_attempt${attempt}/"
  local media_rel=""
  local device_recording=""
  local host_recording=""
  local screenshot_file=""
  local use_video="$RECORD"
  if [[ "$STORE" == "shared" ]]; then
    use_video="no"
  fi
  if [[ "$use_video" == "yes" ]]; then
    mkdir -p "$RECORDINGS_DIR"
    device_recording="/sdcard/maestro_${repeat_index}_${base}_attempt${attempt}.mp4"
    host_recording="$RECORDINGS_DIR/${repeat_index}_${base}_attempt${attempt}.mp4"
    stop_screenrecord
    collapse_system_ui
    adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording" >/dev/null
    adb -s "$DEVICE_SERIAL" shell "screenrecord --time-limit 180 --bit-rate 4000000 $device_recording" >/dev/null 2>&1 &
    RECORDER_PID=$!
    sleep 1
  else
    collapse_system_ui
  fi

  local started ended exit_code
  started=$(date +%s)
  set +e
  maestro test "${MAESTRO_DEVICE_ARGS[@]}" "${MAESTRO_ENV_ARGS[@]}" "$flow" >"$log_file" 2>&1
  exit_code=$?
  set -e
  ended=$(date +%s)

  mkdir -p "$attempt_screenshots_dir"
  while IFS= read -r screenshot_name; do
    [[ -z "$screenshot_name" ]] && continue
    screenshot_name="${screenshot_name%.png}"
    if [[ -f "$REPO_ROOT/$screenshot_name.png" ]]; then
      mv "$REPO_ROOT/$screenshot_name.png" "$attempt_screenshots_dir/"
    fi
  done < <(
    awk -F'takeScreenshot:[[:space:]]*' '
      /takeScreenshot:/ {
        name = $2
        gsub(/^[[:space:]"'\''"]+|[[:space:]"'\''"]+$/, "", name)
        if (name != "") print name
      }
    ' "$flow"
  )
  if compgen -G "$attempt_screenshots_dir/*.png" >/dev/null; then
    media_rel="$attempt_screenshots_rel"
  else
    rmdir "$attempt_screenshots_dir" 2>/dev/null || true
  fi

  if [[ "$use_video" == "yes" ]]; then
    stop_screenrecord
    wait "$RECORDER_PID" 2>/dev/null || true
    RECORDER_PID=""
    if adb -s "$DEVICE_SERIAL" shell "[ -f $device_recording ]" >/dev/null 2>&1; then
      adb -s "$DEVICE_SERIAL" pull "$device_recording" "$host_recording" >/dev/null 2>&1 || true
      adb -s "$DEVICE_SERIAL" shell "rm -f $device_recording" >/dev/null 2>&1 || true
      if [[ -f "$host_recording" ]]; then
        media_rel="recordings/$(basename "$host_recording")"
      fi
    fi
  elif [[ "$exit_code" -ne 0 ]]; then
    screenshot_file="$SCREENSHOTS_DIR/${repeat_index}_${base}_attempt${attempt}.png"
    device_screenshot="/sdcard/maestro_${repeat_index}_${base}_attempt${attempt}.png"
    adb -s "$DEVICE_SERIAL" shell "screencap -p $device_screenshot" >/dev/null 2>&1 || true
    adb -s "$DEVICE_SERIAL" pull "$device_screenshot" "$screenshot_file" >/dev/null 2>&1 || true
    adb -s "$DEVICE_SERIAL" shell "rm -f $device_screenshot" >/dev/null 2>&1 || true
    if [[ -f "$screenshot_file" ]]; then
      media_rel="screenshots/$(basename "$screenshot_file")"
    fi
  fi

  scrub_log "$log_file"
  local error_line recovery_count
  error_line="$(grep -E '^\[Failed\]|Assertion|Couldn|Could not find|Timeout|Exception|Error' "$log_file" | head -n 1 | tr '|' '/' || true)"
  recovery_count="$(grep -c '✅.*recovery_' "$log_file" || true)"
  printf '%s|%s|%s|%s|%s|%s\n' "$exit_code" "$((ended - started))" "$media_rel" "logs/$(basename "$log_file")" "$error_line" "$recovery_count"
}

run_index=0
for repeat_index in $(seq 1 "$REPEAT"); do
  for flow in "${ORDERED_FLOWS[@]}"; do
    run_index=$((run_index + 1))
    base="$(basename "$flow" .yaml)"
    echo "[$run_index/$TOTAL_RUNS] $base (repeat $repeat_index/$REPEAT)"

    first="$(run_one_attempt "$flow" "$base" 1 "$repeat_index")"
    IFS='|' read -r first_exit first_duration first_media first_log first_error first_recovery <<< "$first"
    status="PASS"
    duration="$first_duration"
    media="$first_media"
    log_rel="$first_log"
    error="$first_error"
    recovery="$first_recovery"

    if [[ "$first_exit" -ne 0 ]]; then
      echo "  first attempt failed; retrying once"
      retry="$(run_one_attempt "$flow" "$base" 2 "$repeat_index")"
      IFS='|' read -r retry_exit retry_duration retry_media retry_log retry_error retry_recovery <<< "$retry"
      duration=$((first_duration + retry_duration))
      if [[ "$retry_exit" -eq 0 ]]; then
        status="FLAKY"
        FLAKY=$((FLAKY + 1))
      else
        status="FAIL"
        FAILED=$((FAILED + 1))
        media="${retry_media:-$first_media}"
        log_rel="$retry_log"
        error="${retry_error:-$first_error}"
      fi
      recovery=$((first_recovery + retry_recovery))
    elif [[ "${first_recovery:-0}" -gt 0 ]]; then
      status="FLAKY_RECOVERY"
      FLAKY=$((FLAKY + 1))
    else
      PASSED=$((PASSED + 1))
      [[ -n "$first_log" ]] && rm -f "$OUTPUT_DIR/$first_log" 2>/dev/null || true
      [[ -n "$first_media" ]] && rm -f "$OUTPUT_DIR/$first_media" 2>/dev/null || true
    fi

    RESULTS+=("$status|$repeat_index|$base|$duration|$media|$log_rel|$error|$recovery")
    echo "  $status in ${duration}s"
  done
done

SUITE_END=$(date +%s)
SUITE_DURATION=$((SUITE_END - SUITE_START))

echo "--- Generating reports"
{
  printf '<?xml version="1.0" encoding="UTF-8"?>\n'
  printf '<testsuite name="woocommerce-android-maestro-smoke" tests="%d" failures="%d" time="%d">\n' \
    "$TOTAL_RUNS" "$((FAILED + FLAKY))" "$SUITE_DURATION"
  for result in "${RESULTS[@]}"; do
    IFS='|' read -r status repeat_index name duration media log_rel error recovery <<< "$result"
    printf '  <testcase classname="maestro.%s" name="%s" time="%s">' "$repeat_index" "$name" "$duration"
    if [[ "$status" == "FAIL" || "$status" == "FLAKY" || "$status" == "FLAKY_RECOVERY" ]]; then
      msg="$(printf '%s' "${error:-$status}" | xml_escape)"
      printf '<failure message="%s">%s</failure>' "$status" "$msg"
    fi
    printf '</testcase>\n'
  done
  printf '</testsuite>\n'
} > "$JUNIT_FILE"

{
  cat <<HTML_HEAD
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>WooCommerce Android Maestro smoke report</title>
<style>
body { font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 24px auto; max-width: 1100px; padding: 0 20px; }
table { width: 100%; border-collapse: collapse; }
th, td { border-bottom: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }
th { background: #f6f8fa; }
.PASS { color: #136f2d; font-weight: 700; }
.FAIL, .FLAKY, .FLAKY_RECOVERY { color: #9a1111; font-weight: 700; }
code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
</style>
</head>
<body>
<h1>WooCommerce Android Maestro smoke report</h1>
<p><strong>Run:</strong> <code>$SUITE_RUN_ID</code> | <strong>Store:</strong> $STORE | <strong>Device:</strong> <code>$DEVICE_SERIAL</code> | <strong>Duration:</strong> ${SUITE_DURATION}s</p>
<p><strong>Result:</strong> $PASSED passed, $FLAKY flaky, $FAILED failed out of $TOTAL_RUNS executions.</p>
<table>
<thead><tr><th>Repeat</th><th>Flow</th><th>Status</th><th>Duration</th><th>Recovery</th><th>Artifact</th><th>Error</th></tr></thead>
<tbody>
HTML_HEAD
  for result in "${RESULTS[@]}"; do
    IFS='|' read -r status repeat_index name duration media log_rel error recovery <<< "$result"
    artifact=""
    if [[ -n "$media" ]]; then
      artifact="<a href=\"$media\">media</a>"
    fi
    if [[ -n "$log_rel" && -f "$OUTPUT_DIR/$log_rel" ]]; then
      artifact="$artifact ${artifact:+| }<a href=\"$log_rel\">log</a>"
    fi
    error_html="$(printf '%s' "$error" | xml_escape)"
    printf '<tr><td>%s</td><td><code>%s</code></td><td class="%s">%s</td><td>%ss</td><td>%s</td><td>%s</td><td>%s</td></tr>\n' \
      "$repeat_index" "$name" "$status" "$status" "$duration" "${recovery:-0}" "$artifact" "$error_html"
  done
  cat <<HTML_FOOT
</tbody>
</table>
<p><a href="report.xml">JUnit XML</a> | <a href="tmp/run-manifest.json">run manifest</a> | <a href="tmp/orphan-sweep.json">orphan sweep report</a></p>
</body>
</html>
HTML_FOOT
} > "$REPORT_FILE"

echo "Report: $REPORT_FILE"
echo "JUnit:  $JUNIT_FILE"
echo "Result: $PASSED passed, $FLAKY flaky, $FAILED failed out of $TOTAL_RUNS executions (${SUITE_DURATION}s)"

if [[ -f "$REPORT_FILE" && "$OPEN_REPORT" == "auto" && -z "${CI:-}" && -z "${BUILDKITE:-}" && "$(uname)" == "Darwin" ]]; then
  open "$REPORT_FILE" || true
fi

if [[ "$FAILED" -gt 0 || "$FLAKY" -gt 0 ]]; then
  exit 1
fi
exit 0

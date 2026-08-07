#!/usr/bin/env bash

set +x
set -euo pipefail

readonly EXIT_USAGE=10
readonly EXIT_PROFILE=11
readonly EXIT_DEVICE=12
readonly EXIT_TARGET=13
readonly EXIT_STAGE=15
readonly EXIT_START=16
readonly EXIT_OUTCOME_UNKNOWN=17
readonly EXIT_CONFLICT=20
readonly EXIT_INVALID_REQUEST=22
readonly EXIT_AUTH_REQUIRES_2FA=24
readonly EXIT_AUTH_FAILED=25
readonly EXIT_SITE_FAILED=26
readonly EXIT_INTERNAL_ERROR=30

readonly MAX_PAYLOAD_BYTES=16384
readonly ACTION="com.woocommerce.android.debug.AUTO_LOGIN"
readonly ACTIVITY="com.woocommerce.android.ui.login.auto.AutoLoginActivity"

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPOSITORY_ROOT="$(cd "$SCRIPT_DIRECTORY/../.." && pwd -P)"

PROFILE_NAME="default"
FLAVOR=""
SERIAL=""
ANDROID_USER=""

usage() {
    cat >&2 <<'EOF'
Usage: agent-login.sh --flavor dev|prealpha [--profile NAME] [--serial SERIAL] [--user USER]
EOF
}

emit() {
    printf '%s\n' "$1"
    exit "$2"
}

error() {
    printf 'agent-login: %s\n' "$1" >&2
}

quote_remote_script() {
    local escaped
    escaped="$(printf '%s' "$1" | sed "s/'/'\"'\"'/g")"
    printf "'%s'" "$escaped"
}

while (($# > 0)); do
    case "$1" in
        --profile)
            (($# >= 2)) || { usage; exit "$EXIT_USAGE"; }
            PROFILE_NAME="$2"
            shift 2
            ;;
        --flavor)
            (($# >= 2)) || { usage; exit "$EXIT_USAGE"; }
            FLAVOR="$2"
            shift 2
            ;;
        --serial)
            (($# >= 2)) || { usage; exit "$EXIT_USAGE"; }
            SERIAL="$2"
            shift 2
            ;;
        --user)
            (($# >= 2)) || { usage; exit "$EXIT_USAGE"; }
            ANDROID_USER="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            usage
            exit "$EXIT_USAGE"
            ;;
    esac
done

if [[ -z "$FLAVOR" ]]; then
    usage
    exit "$EXIT_USAGE"
fi
if [[ ! "$PROFILE_NAME" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$ ]] ||
    [[ "$PROFILE_NAME" == "." || "$PROFILE_NAME" == ".." ]]; then
    error "invalid profile name"
    exit "$EXIT_USAGE"
fi
if [[ -n "$SERIAL" ]] && [[ ! "$SERIAL" =~ ^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$ ]]; then
    error "invalid device serial"
    exit "$EXIT_USAGE"
fi
if [[ -n "$ANDROID_USER" ]] && [[ ! "$ANDROID_USER" =~ ^(0|[1-9][0-9]{0,9})$ ]]; then
    error "invalid Android user"
    exit "$EXIT_USAGE"
fi

case "$FLAVOR" in
    dev)
        PACKAGE="com.woocommerce.android.dev"
        ;;
    prealpha)
        PACKAGE="com.woocommerce.android.prealpha"
        ;;
    *)
        error "unsupported target flavor"
        exit "$EXIT_USAGE"
        ;;
esac
readonly PACKAGE
readonly COMPONENT="$PACKAGE/$ACTIVITY"

if [[ -n "${XDG_CONFIG_HOME:-}" ]]; then
    CONFIG_ROOT="$XDG_CONFIG_HOME/woocommerce-android/auto-login"
else
    CONFIG_ROOT="${HOME:?HOME is required}/.config/woocommerce-android/auto-login"
fi
PROFILE_FILE="$CONFIG_ROOT/profiles/$PROFILE_NAME.json"

if [[ ! -f "$PROFILE_FILE" || -L "$PROFILE_FILE" || ! -r "$PROFILE_FILE" ]]; then
    error "profile is unavailable"
    emit "PROFILE_ERROR" "$EXIT_PROFILE"
fi

if ! PROFILE_DIRECTORY="$(cd "$(dirname "$PROFILE_FILE")" && pwd -P)" ||
    [[ "$PROFILE_DIRECTORY/" == "$REPOSITORY_ROOT/"* ]]; then
    error "profile must be stored outside the repository"
    emit "PROFILE_ERROR" "$EXIT_PROFILE"
fi

case "$(uname -s)" in
    Darwin)
        MODE="$(stat -f '%Lp' "$PROFILE_FILE" 2>/dev/null || true)"
        ;;
    *)
        MODE="$(stat -c '%a' "$PROFILE_FILE" 2>/dev/null || true)"
        ;;
esac
if [[ -z "$MODE" ]]; then
    error "profile permissions could not be checked"
    emit "PROFILE_ERROR" "$EXIT_PROFILE"
fi
if [[ "$MODE" != "600" ]]; then
    error "profile must have mode 0600"
    emit "PROFILE_ERROR" "$EXIT_PROFILE"
fi

PROFILE_SIZE="$(wc -c <"$PROFILE_FILE")"
PROFILE_SIZE="${PROFILE_SIZE//[[:space:]]/}"
if [[ ! "$PROFILE_SIZE" =~ ^[0-9]+$ ]] ||
    ((PROFILE_SIZE == 0 || PROFILE_SIZE > MAX_PAYLOAD_BYTES)); then
    error "profile must contain between 1 and 16384 bytes"
    emit "PROFILE_ERROR" "$EXIT_PROFILE"
fi

if ! command -v adb >/dev/null 2>&1; then
    error "adb is unavailable"
    emit "DEVICE_UNAVAILABLE" "$EXIT_DEVICE"
fi

if ! ADB_OUTPUT="$(adb devices 2>&1)"; then
    error "device discovery failed"
    emit "DEVICE_UNAVAILABLE" "$EXIT_DEVICE"
fi
if [[ -z "$SERIAL" ]]; then
    AUTHORIZED_DEVICE_COUNT="$(
        awk 'NF >= 2 && $2 == "device" { count++ } END { print count + 0 }' <<<"$ADB_OUTPUT"
    )"
    if [[ "$AUTHORIZED_DEVICE_COUNT" -ne 1 ]]; then
        error "specify a serial when exactly one authorized device is not available"
        emit "DEVICE_SELECTION_REQUIRED" "$EXIT_DEVICE"
    fi
    SERIAL="$(awk 'NF >= 2 && $2 == "device" { print $1; exit }' <<<"$ADB_OUTPUT")"
elif ! awk -v serial="$SERIAL" \
    'NF >= 2 && $1 == serial && $2 == "device" { found=1 } END { exit !found }' <<<"$ADB_OUTPUT"; then
    error "the selected device is not authorized"
    emit "DEVICE_UNAVAILABLE" "$EXIT_DEVICE"
fi

if [[ -z "$ANDROID_USER" ]]; then
    if ! ANDROID_USER="$(adb -s "$SERIAL" shell -T am get-current-user 2>/dev/null)"; then
        error "Android user resolution failed"
        emit "DEVICE_UNAVAILABLE" "$EXIT_DEVICE"
    fi
    ANDROID_USER="${ANDROID_USER%$'\r'}"
    if [[ ! "$ANDROID_USER" =~ ^(0|[1-9][0-9]{0,9})$ ]]; then
        error "Android user resolution returned an invalid value"
        emit "DEVICE_UNAVAILABLE" "$EXIT_DEVICE"
    fi
fi

if ! ADB_OUTPUT="$(adb -s "$SERIAL" shell -T pm path --user "$ANDROID_USER" "$PACKAGE" 2>&1)" ||
    ! grep -q '^package:' <<<"$ADB_OUTPUT"; then
    error "the selected debug package is not installed for this user"
    emit "TARGET_UNAVAILABLE" "$EXIT_TARGET"
fi
if ! adb -s "$SERIAL" shell -T run-as "$PACKAGE" --user "$ANDROID_USER" \
    sh -c true >/dev/null 2>&1; then
    error "run-as is unavailable; the package must be debuggable"
    emit "TARGET_UNAVAILABLE" "$EXIT_TARGET"
fi

read -r -d '' REMOTE_STAGE <<'EOF' || true
set -eu
umask 077
directory=no_backup/auto-login
mkdir -p "$directory"
chmod 700 "$directory"
rm -f \
    "$directory/request.tmp" \
    "$directory/request.ready" \
    "$directory/status.tmp" \
    "$directory/status.ready"
cat >"$directory/request.tmp"
bytes=$(wc -c <"$directory/request.tmp")
if [ "$bytes" -le 0 ] || [ "$bytes" -gt 16384 ]; then
    rm -f "$directory/request.tmp"
    exit 65
fi
chmod 600 "$directory/request.tmp"
mv "$directory/request.tmp" "$directory/request.ready"
printf '%s\n' AGENT_LOGIN_STAGED
EOF
REMOTE_STAGE_ARGUMENT="$(quote_remote_script "$REMOTE_STAGE")"
REMOTE_REQUEST_CLEANUP_ARGUMENT="$(
    quote_remote_script 'rm -f no_backup/auto-login/request.tmp no_backup/auto-login/request.ready'
)"
REMOTE_TERMINAL_CLEANUP_ARGUMENT="$(
    quote_remote_script \
        'rm -f no_backup/auto-login/request.tmp no_backup/auto-login/request.ready \
no_backup/auto-login/status.tmp no_backup/auto-login/status.ready'
)"

if ! STAGE_OUTPUT="$(
    adb -s "$SERIAL" shell -T run-as "$PACKAGE" --user "$ANDROID_USER" \
        sh -c "$REMOTE_STAGE_ARGUMENT" <"$PROFILE_FILE" 2>&1
)" || [[ "$STAGE_OUTPUT" != "AGENT_LOGIN_STAGED" ]]; then
    error "secure device staging failed"
    emit "STAGE_FAILED" "$EXIT_STAGE"
fi

if ! LAUNCH_OUTPUT="$(
    adb -s "$SERIAL" shell -T am start --user "$ANDROID_USER" -W \
        -a "$ACTION" -n "$COMPONENT" 2>&1
)" || grep -qiE '(^|[[:space:]])Error(:| type)|Permission Denial|SecurityException' <<<"$LAUNCH_OUTPUT"; then
    adb -s "$SERIAL" shell -T run-as "$PACKAGE" --user "$ANDROID_USER" \
        sh -c "$REMOTE_REQUEST_CLEANUP_ARGUMENT" \
        >/dev/null 2>&1 || true
    error "the auto-login activity did not start"
    emit "START_FAILED" "$EXIT_START"
fi

read -r -d '' REMOTE_POLL <<'EOF' || true
status=no_backup/auto-login/status.ready
if [ -f "$status" ]; then
    cat "$status"
    printf %s AGENT_LOGIN_STATUS_END
else
    exit 44
fi
EOF
REMOTE_POLL_ARGUMENT="$(quote_remote_script "$REMOTE_POLL")"

STATUS=""
for ((attempt = 1; attempt <= 360; attempt++)); do
    set +e
    STATUS_OUTPUT="$(
        adb -s "$SERIAL" shell -T run-as "$PACKAGE" --user "$ANDROID_USER" \
            sh -c "$REMOTE_POLL_ARGUMENT" 2>/dev/null
    )"
    POLL_RESULT=$?
    set -e

    if [[ "$POLL_RESULT" -eq 0 ]]; then
        for ALLOWED_STATUS in \
            SUCCESS \
            ALREADY_ACTIVE \
            CONFLICT \
            INVALID_REQUEST \
            AUTH_REQUIRES_2FA \
            AUTH_FAILED \
            SITE_FAILED \
            INTERNAL_ERROR; do
            if [[ "$STATUS_OUTPUT" == "$ALLOWED_STATUS"$'\n'"AGENT_LOGIN_STATUS_END" ]]; then
                STATUS="$ALLOWED_STATUS"
                break
            fi
        done
        if [[ -z "$STATUS" ]]; then
            error "the device returned a malformed status"
            emit "OUTCOME_UNKNOWN" "$EXIT_OUTCOME_UNKNOWN"
        fi
        break
    fi
    if [[ "$POLL_RESULT" -ne 44 ]]; then
        error "status polling failed"
        emit "OUTCOME_UNKNOWN" "$EXIT_OUTCOME_UNKNOWN"
    fi
    sleep 1
done

if [[ -z "$STATUS" ]]; then
    error "status timed out"
    emit "OUTCOME_UNKNOWN" "$EXIT_OUTCOME_UNKNOWN"
fi

adb -s "$SERIAL" shell -T run-as "$PACKAGE" --user "$ANDROID_USER" \
    sh -c "$REMOTE_TERMINAL_CLEANUP_ARGUMENT" \
    >/dev/null 2>&1 || error "terminal status was received, but cleanup was incomplete"

case "$STATUS" in
    SUCCESS|ALREADY_ACTIVE)
        emit "$STATUS" 0
        ;;
    CONFLICT)
        emit "$STATUS" "$EXIT_CONFLICT"
        ;;
    INVALID_REQUEST)
        emit "$STATUS" "$EXIT_INVALID_REQUEST"
        ;;
    AUTH_REQUIRES_2FA)
        error "use a dedicated revocable WordPress.com Application Password; OTP automation is unsupported"
        emit "$STATUS" "$EXIT_AUTH_REQUIRES_2FA"
        ;;
    AUTH_FAILED)
        emit "$STATUS" "$EXIT_AUTH_FAILED"
        ;;
    SITE_FAILED)
        emit "$STATUS" "$EXIT_SITE_FAILED"
        ;;
    INTERNAL_ERROR)
        emit "$STATUS" "$EXIT_INTERNAL_ERROR"
        ;;
esac

#!/usr/bin/env bash

set -euo pipefail

readonly RELEASES_URL="https://github.com/woocommerce/woocommerce-android/releases"
readonly MAPPING_ENTRY="BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"

usage() {
    cat >&2 <<'EOF'
Usage: retrace.sh VERSION [LOG_FILE]

Restores the original method names and line numbers in the stack traces of a release build's app log.
VERSION is the app version from the log or the support ticket, e.g. 26.0 or 26.0-rc-1.
Without LOG_FILE, the log is read from standard input.
EOF
}

find_retrace() {
    if command -v retrace >/dev/null 2>&1; then
        command -v retrace
        return
    fi
    local tool="${ANDROID_HOME:-$HOME/Library/Android/sdk}/cmdline-tools/latest/bin/retrace"
    if [[ ! -x "$tool" ]]; then
        echo "retrace not found. Install Android SDK Command-line Tools from Android Studio's SDK Manager." >&2
        exit 1
    fi
    echo "$tool"
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
    usage
    exit 1
fi

VERSION="$1"
LOG_FILE="${2:-}"
if [[ -n "$LOG_FILE" && ! -f "$LOG_FILE" ]]; then
    echo "Log file not found: $LOG_FILE" >&2
    exit 1
fi
RETRACE="$(find_retrace)"

WORK_DIRECTORY="$(mktemp -d)"
trap 'rm -rf "$WORK_DIRECTORY"' EXIT
trap 'exit 130' INT TERM

echo "Downloading the $VERSION app bundle..." >&2
if ! curl --fail --location --silent --show-error --output "$WORK_DIRECTORY/app.aab" \
    "$RELEASES_URL/download/$VERSION/WooCommerce-$VERSION.aab"; then
    echo "No app bundle found for $VERSION. Check the version against $RELEASES_URL" >&2
    exit 1
fi

unzip -p "$WORK_DIRECTORY/app.aab" "$MAPPING_ENTRY" > "$WORK_DIRECTORY/mapping.txt"
rm "$WORK_DIRECTORY/app.aab"

if [[ -n "$LOG_FILE" ]]; then
    "$RETRACE" --quiet "$WORK_DIRECTORY/mapping.txt" "$LOG_FILE"
else
    "$RETRACE" --quiet "$WORK_DIRECTORY/mapping.txt"
fi

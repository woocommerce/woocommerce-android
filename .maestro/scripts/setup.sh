#!/bin/bash
# Prepare this machine for the Maestro smoke suite. Every step is skipped when it
# is already done, so it is safe to run again at any time, for example when a
# run reports that the lab store expired.
#
# Usage:
#   .maestro/scripts/setup.sh
#
# Afterwards:
#   .maestro/scripts/run-smoke-tests.sh --profile full

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS="$REPO_ROOT/.maestro/scripts"
ENV_FILE="$REPO_ROOT/.maestro/.env.local"
SECRETS_DIR="$HOME/.configure/woocommerce-android/secrets"
APP_CREDS="$SECRETS_DIR/secrets.properties"
# Shared-store lines, decrypted from the mobile secrets repository by configure apply.
SHARED_ENV="$SECRETS_DIR/maestro.txt"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
PHONE_AVD="Maestro_Phone"
TABLET_AVD="Maestro_Tablet"
SYSTEM_IMAGE_API="35"
MAX_LOGIN_ATTEMPTS=3
# The store counts as ready after this many order requests in a row each answer within
# STORE_PROBE_TIMEOUT seconds, well under the 45 seconds the fixture seeder allows.
STORE_PROBE_STREAK=3
STORE_PROBE_TIMEOUT=10
STORE_SETTLE_MAX_SECONDS=600
JN_CREATE_URL="https://jurassic.ninja/create/?woocommerce&woocommerce-import-sample-data"

STEP=0
TOTAL_STEPS=7
step() { STEP=$(( STEP + 1 )); printf '\n\033[1m[%s/%s]\033[0m %s\n' "$STEP" "$TOTAL_STEPS" "$1"; }
ok() { printf '  \033[32mok\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!!\033[0m %s\n' "$*"; }
die() { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }

# Values go through the environment, never argv, so they don't show up in `ps`.
env_value() {
  [ -f "$ENV_FILE" ] || return 0
  ENV_FILE="$ENV_FILE" ENV_KEY="$1" python3 -c '
import os, re, shlex
for line in open(os.environ["ENV_FILE"], encoding="utf-8"):
    m = re.match(r"\s*" + re.escape(os.environ["ENV_KEY"]) + r"=(.*)$", line)
    if m:
        try:
            parts = shlex.split(m.group(1).strip(), comments=True)
        except ValueError:
            parts = [m.group(1).strip()]
        print(parts[0] if parts else "")
        break
'
}

set_env_value() {
  ENV_FILE="$ENV_FILE" ENV_KEY="$1" ENV_VALUE="$2" python3 -c '
import os, re, shlex
path, key, value = os.environ["ENV_FILE"], os.environ["ENV_KEY"], os.environ["ENV_VALUE"]
line = f"{key}={shlex.quote(value)}\n"
lines = open(path, encoding="utf-8").readlines()
pattern = re.compile(r"\s*#?\s*" + re.escape(key) + r"=")
for index, existing in enumerate(lines):
    if pattern.match(existing):
        lines[index] = line
        break
else:
    lines.append(line)
open(path, "w", encoding="utf-8").writelines(lines)
'
}

app_cred() {
  APP_CREDS="$APP_CREDS" KEY="$1" python3 -c '
import os
for line in open(os.environ["APP_CREDS"], encoding="utf-8"):
    key, _, value = line.partition("=")
    if key.strip() == os.environ["KEY"]:
        print(value.strip())
        break
'
}

# Prints nothing on success, or why WordPress.com refused the account.
wpcom_login_error() {
  WPCOM_CLIENT_ID="$(app_cred wc.oauth.app_id)" WPCOM_CLIENT_SECRET="$(app_cred wc.oauth.app_secret)" \
    WPCOM_USER="$1" WPCOM_PASS="$2" python3 -c '
import json, os, urllib.error, urllib.parse, urllib.request
data = urllib.parse.urlencode({
    "client_id": os.environ["WPCOM_CLIENT_ID"],
    "client_secret": os.environ["WPCOM_CLIENT_SECRET"],
    "grant_type": "password",
    "username": os.environ["WPCOM_USER"],
    "password": os.environ["WPCOM_PASS"],
    "wpcom_supports_2fa": "true",
}).encode()
try:
    with urllib.request.urlopen("https://public-api.wordpress.com/oauth2/token", data, timeout=30) as response:
        body = json.load(response)
except urllib.error.HTTPError as error:
    try:
        body = json.load(error)
    except ValueError:
        body = {"error_description": f"HTTP {error.code}"}
except (urllib.error.URLError, TimeoutError) as error:
    print(f"could not reach WordPress.com: {error}")
    raise SystemExit
if body.get("access_token"):
    raise SystemExit
if "2fa" in json.dumps(body).lower() or "two_step" in json.dumps(body).lower():
    print("the account has two-factor authentication on; use a test account without it")
else:
    print(body.get("error_description") or body.get("error") or "unknown error")
'
}

cd "$REPO_ROOT" || die "cannot enter $REPO_ROOT"

step "Toolchain (Java 21 and the pinned Maestro)"
# shellcheck source=/dev/null
source "$SCRIPTS/configure-toolchain.sh" || die "fix the toolchain problem above, then run this again"
ok "toolchain ready"

step "App credentials and shared secrets"
# configure apply decrypts the files listed in .configure, using the key from the mobile
# secrets checkout, which it moves to the pinned commit and back by itself. maestro.txt
# is only expected once .configure lists it.
needs_configure_apply() {
  [ -f "$APP_CREDS" ] || return 0
  grep -q '"~/.configure/woocommerce-android/secrets/maestro.txt"' "$REPO_ROOT/.configure" && [ ! -f "$SHARED_ENV" ]
}
if needs_configure_apply; then
  [ -d "$HOME/.mobile-secrets/.git" ] || [ -n "${CONFIGURE_ENCRYPTION_KEY:-}" ] \
    || die "~/.mobile-secrets is missing. Clone the Automattic mobile-secrets repository there, then run this again."
  command -v bundle >/dev/null 2>&1 || die "bundler is missing. Install Ruby $(cat "$REPO_ROOT/.ruby-version"), then run this again."
  if ! bundle check >/dev/null 2>&1; then
    printf '  installing Ruby gems...\n'
    bundle install >/dev/null || die "bundle install failed; run it yourself to see why"
  fi
  printf '  decrypting secrets with configure apply...\n'
  # Not silenced: it asks before overwriting a local file that differs from the decrypted one.
  FASTLANE_SKIP_UPDATE_CHECK=1 FASTLANE_HIDE_PLUGINS_TABLE=1 FASTLANE_HIDE_TIMESTAMP=1 \
    bundle exec fastlane run configure_apply \
    || die "configure apply failed; see the messages above"
fi
[ -f "$APP_CREDS" ] || die "$APP_CREDS is still missing after configure apply"
[ -n "$(app_cred wc.oauth.app_id)" ] || die "wc.oauth.app_id is missing from $APP_CREDS"
ok "found"

step "Shared test stores"
if [ ! -f "$ENV_FILE" ]; then
  cp "$REPO_ROOT/.maestro/env.example" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  ok "created .maestro/.env.local"
fi
if [ -f "$SHARED_ENV" ]; then
  while IFS='=' read -r key value; do
    [[ "$key" =~ ^MAESTRO_[A-Z0-9_]+$ ]] || continue
    value="$(VALUE="$value" python3 -c 'import os, shlex; v = os.environ["VALUE"].strip(); print((shlex.split(v) or [""])[0] if v else "")')"
    # Empty lines are the ones each developer fills in, so they must not clear local values.
    if [ -n "$value" ]; then set_env_value "$key" "$value"; fi
  done < "$SHARED_ENV"
  ok "filled from $SHARED_ENV"
elif [ -n "$(env_value MAESTRO_WOO_SHARED_WPCOM_PASSWORD)" ]; then
  ok "already in .env.local"
else
  warn "$SHARED_ENV not found and the shared-store lines in .maestro/.env.local are empty."
  warn "Fill them in from the Smoke Testing Guide, then run this again."
fi

step "Your WordPress.com test account (for your own lab store)"
LAB_EMAIL="$(env_value MAESTRO_WOO_LAB_WPCOM_EMAIL)"
LAB_PASS="$(env_value MAESTRO_WOO_LAB_WPCOM_PASSWORD)"
if [ -n "$LAB_EMAIL" ] && [ -n "$LAB_PASS" ]; then
  ok "already in .env.local ($LAB_EMAIL)"
else
  [ -t 0 ] || die "no terminal to ask for the account on. Run this script directly in a terminal."
  printf '  Use a WordPress.com account you own, with two-factor authentication off.\n'
  printf '  It is only stored in .maestro/.env.local.\n'
  attempt=1
  while :; do
    printf '  Email: '
    read -r LAB_EMAIL
    printf '  Password: '
    read -rs LAB_PASS
    printf '\n'
    if [ -z "$LAB_EMAIL" ] || [ -z "$LAB_PASS" ]; then
      error="email and password are both required"
    else
      error="$(wpcom_login_error "$LAB_EMAIL" "$LAB_PASS")"
    fi
    [ -z "$error" ] && break
    warn "WordPress.com refused it: $error"
    [ "$attempt" -lt "$MAX_LOGIN_ATTEMPTS" ] || die "giving up after $MAX_LOGIN_ATTEMPTS attempts. Run this again when ready."
    attempt=$(( attempt + 1 ))
  done
  set_env_value MAESTRO_WOO_LAB_WPCOM_EMAIL "$LAB_EMAIL"
  set_env_value MAESTRO_WOO_LAB_WPCOM_PASSWORD "$LAB_PASS"
  ok "signed in and saved"
fi

step "Your lab store"
# Prints the HTTP status of a products request, or "missing" when no store is configured.
lab_store_status() {
  (set -a; . "$ENV_FILE"; set +a
    [ -n "${MAESTRO_WOO_LAB_JETPACK_STORE_URL:-}" ] || { echo missing; exit; }
    curl -s -o /dev/null -w '%{http_code}' --max-time 20 \
      -u "${MAESTRO_WOO_LAB_CONSUMER_KEY:-}:${MAESTRO_WOO_LAB_CONSUMER_SECRET:-}" \
      "$MAESTRO_WOO_LAB_JETPACK_STORE_URL/wp-json/wc/v3/products?per_page=1")
}

# Prints the HTTP status of one order request, or 000 when it times out.
order_request_status() {
  (set -a; . "$ENV_FILE"; set +a
    curl -s -o /dev/null -w '%{http_code}' --max-time "$STORE_PROBE_TIMEOUT" \
      -u "${MAESTRO_WOO_LAB_CONSUMER_KEY:-}:${MAESTRO_WOO_LAB_CONSUMER_SECRET:-}" \
      "$MAESTRO_WOO_LAB_JETPACK_STORE_URL/wp-json/wc/v3/orders?per_page=1")
}

# A store that was just filled with orders keeps importing them into analytics for a
# few minutes, and requests to it time out meanwhile. Wait until it answers quickly
# several times in a row.
wait_for_store_to_settle() {
  local fast=0 deadline=$(( $(date +%s) + STORE_SETTLE_MAX_SECONDS ))
  while [ "$fast" -lt "$STORE_PROBE_STREAK" ]; do
    if [ "$(order_request_status)" = "200" ]; then
      fast=$(( fast + 1 ))
      continue
    fi
    fast=0
    if [ "$(date +%s)" -ge "$deadline" ]; then
      warn "the store is still slow after $(( STORE_SETTLE_MAX_SECONDS / 60 )) minutes; the first smoke run may time out"
      return
    fi
    printf '  the store is busy, probably still importing new data; checking again in 15 seconds...\n'
    sleep 15
  done
  ok "answers quickly"
}

create_store_with_agent() {
  if command -v claude >/dev/null 2>&1; then
    printf '  Starting Claude Code. Exit it once the store is ready to continue here.\n\n'
    claude "/setup-test-stores"
  else
    printf '  Ask your coding agent to run /setup-test-stores in this repository. It needs the\n'
    printf '  ContextA8C MCP. Press Enter here once it is done. '
    read -r _
  fi
}

create_store_in_browser() {
  printf '  Opening %s\n' "$JN_CREATE_URL"
  open "$JN_CREATE_URL" 2>/dev/null || printf '  Open that address in your browser.\n'
  printf '  Once the site is ready, enter its address (like funny-dino.jurassic.ninja).\n'
  printf '  The script then asks for the admin password from the notice in its /wp-admin.\n'
  local site=""
  while [ -z "$site" ]; do
    printf '  Site: '
    read -r site
    site="${site#http://}"; site="${site#https://}"; site="${site%%/*}"
  done
  "$SCRIPTS/setup-jn-store.sh" --site "$site" || warn "setting up $site failed; see the messages above"
}

STORE_STATUS="$(lab_store_status)"
case "$STORE_STATUS" in
  200) ;;
  missing) warn "you don't have a lab store yet" ;;
  *) warn "$(env_value MAESTRO_WOO_LAB_JETPACK_STORE_URL) is gone or expired (HTTP $STORE_STATUS)" ;;
esac
if [ "$STORE_STATUS" != "200" ] && [ -t 0 ]; then
  printf '  Create one now:\n'
  printf '    1) with a coding agent (automatic, needs the ContextA8C MCP)\n'
  printf '    2) in your browser on jurassic.ninja\n'
  printf '    3) skip for now\n'
  printf '  Choice [1]: '
  read -r choice
  case "${choice:-1}" in
    1) create_store_with_agent ;;
    2) create_store_in_browser ;;
  esac
  STORE_STATUS="$(lab_store_status)"
fi
NEEDS_STORE="no"
if [ "$STORE_STATUS" = "200" ]; then
  ok "$(env_value MAESTRO_WOO_LAB_JETPACK_STORE_URL) is up"
  wait_for_store_to_settle
else
  NEEDS_STORE="yes"
  [ ! -t 0 ] || warn "still no working lab store"
fi

step "Emulators ($PHONE_AVD and $TABLET_AVD)"
EMULATOR="$SDK/emulator/emulator"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
[ -x "$EMULATOR" ] || die "no Android emulator in $SDK. Install it from Android Studio > SDK Manager, or set ANDROID_HOME."
case "$(uname -m)" in
  arm64) ABI="arm64-v8a" ;;
  *) ABI="x86_64" ;;
esac
IMAGE="system-images;android-$SYSTEM_IMAGE_API;google_apis;$ABI"
create_avd() {
  local name="$1" device="$2"
  if "$EMULATOR" -list-avds 2>/dev/null | grep -qx "$name"; then
    ok "$name exists"
    return
  fi
  [ -x "$AVDMANAGER" ] || die "Android SDK Command-line Tools are missing. Install them from Android Studio > SDK Manager > SDK Tools."
  if [ ! -d "$SDK/system-images/android-$SYSTEM_IMAGE_API/google_apis/$ABI" ]; then
    printf '  downloading the Android %s system image, this takes a few minutes...\n' "$SYSTEM_IMAGE_API"
    # `yes` dies of SIGPIPE when sdkmanager exits, which pipefail would count as a failure.
    yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$SDK" "$IMAGE" >/dev/null
    [ "${PIPESTATUS[1]}" -eq 0 ] || die "could not install $IMAGE"
  fi
  echo no | "$AVDMANAGER" create avd --name "$name" --package "$IMAGE" --device "$device" >/dev/null \
    || die "could not create the $name emulator"
  ok "created $name"
}
create_avd "$PHONE_AVD" "pixel_8"
create_avd "$TABLET_AVD" "pixel_tablet"

step "Checking everything"
if [ "$NEEDS_STORE" = "yes" ]; then
  warn "skipped until you have a lab store"
else
  "$SCRIPTS/doctor.sh" --profile full --device "$PHONE_AVD" --pos-device "$TABLET_AVD" \
    || die "fix the [FAIL] lines above, then run this again"
fi

printf '\n'
if [ "$NEEDS_STORE" = "yes" ]; then
  printf 'One more step: run this script again in a terminal to create your lab store.\n\n'
else
  printf 'All set. Run the smoke tests with:\n\n'
  printf '  .maestro/scripts/run-smoke-tests.sh --profile full\n\n'
fi
